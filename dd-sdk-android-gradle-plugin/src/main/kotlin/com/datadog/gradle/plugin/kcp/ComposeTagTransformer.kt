package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObjectValue
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal class ComposeTagTransformer(
    private val messageCollector: MessageCollector,
    private val pluginContext: IrPluginContext,
    private val annotationModeEnabled: Boolean,
    private val pluginContextUtils: PluginContextUtils = DefaultPluginContextUtils(
        pluginContext,
        messageCollector
    )
) : IrElementTransformerVoidWithContext() {

    private val visitedFunctions = ArrayDeque<String?>()
    private val visitedBuilders = ArrayDeque<DeclarationIrBuilder?>()
    private lateinit var datadogTagFunctionSymbol: IrSimpleFunctionSymbol
    private lateinit var modifierClass: IrClassSymbol
    private lateinit var modifierThenSymbol: IrSimpleFunctionSymbol
    private lateinit var modifierCompanionClassSymbol: IrClassSymbol

    @Suppress("ReturnCount")
    fun initReferences(): Boolean {
        datadogTagFunctionSymbol = pluginContextUtils.getDatadogModifierSymbol() ?: return false
        modifierClass = pluginContextUtils.getModifierClassSymbol() ?: return false
        modifierThenSymbol = pluginContextUtils.getModifierThen() ?: return false
        modifierCompanionClassSymbol = pluginContextUtils.getModifierCompanionClass() ?: return false
        return true
    }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        val declarationName = declaration.name
        val functionName = if (!isAnonymousFunction(declarationName)) {
            declarationName.toString()
        } else {
            visitedFunctions.lastOrNull() ?: declarationName.toString()
        }

        if (pluginContextUtils.isDatadogTagTargetFunc(declaration, annotationModeEnabled)) {
            visitedFunctions.add(functionName)
            visitedBuilders.add(DeclarationIrBuilder(pluginContext, declaration.symbol))
            val irStatement = super.visitFunctionNew(declaration)
            visitedFunctions.removeLast()
            visitedBuilders.removeLast()
            return irStatement
        } else {
            return declaration
        }
    }

    @Suppress("ReturnCount")
    override fun visitCall(expression: IrCall): IrExpression {
        val builder = visitedBuilders.lastOrNull() ?: return super.visitCall(
            expression
        )
        val dispatchReceiver = expression.dispatchReceiver
        // Chained function call should be skipped
        if (dispatchReceiver is IrCall) {
            return super.visitCall(expression)
        }
        expression.symbol.owner.valueParameters.forEachIndexed { index, irValueParameter ->
            // Locate where Modifier is accepted in the parameter list and replace it with the new expression.
            if (irValueParameter.type.classFqName == modifierClassFqName) {
                val irExpression = buildIrExpression(
                    expression = expression.getValueArgument(index),
                    builder = builder,
                    functionName = expression.symbol.owner.name.asString(),
                    isImageComposableFunction = isImageComposableFunction(expression)
                )
                expression.putValueArgument(index, irExpression)
            }
        }
        return super.visitCall(expression)
    }

    @Suppress("ReturnCount")
    private fun isImageComposableFunction(irExpression: IrExpression): Boolean {
        if (irExpression !is IrCall) {
            return false
        }
        val function = irExpression.symbol.owner

        // Look up the parents until the package level.
        var parent = function.parent
        while (parent !is IrPackageFragment && parent is IrDeclaration) {
            parent = parent.parent
        }
        if (parent !is IrPackageFragment) {
            return false
        }
        pluginContextUtils.apply {
            return isFoundationImage(function, parent) ||
                isMaterialIcon(function, parent) ||
                isCoilAsyncImage(function, parent)
        }
    }

    private fun buildIrExpression(
        expression: IrExpression?,
        builder: DeclarationIrBuilder,
        functionName: String,
        isImageComposableFunction: Boolean
    ): IrExpression {
        // TODO RUM-8813:Use Compose function name as the semantics tag
        val datadogTagModifier = buildDatadogTagModifierCall(
            builder = builder,
            functionName = functionName,
            isImageComposableFunction = isImageComposableFunction
        )
        // A Column(){} will be transformed into following code during FIR:
        // Column(modifier = // COMPOSITE {
        //    null
        // }, verticalArrangement = // COMPOSITE {
        //    null
        // }, horizontalAlignment = // COMPOSITE {
        //    null
        // }, content = @Composable
        // checking if the argument is the type of `COMPOSITE`
        // allows us to know if the modifier is absent in source code.
        val overwriteModifier = expression == null ||
            (expression is IrComposite && expression.type.classFqName == kotlinNothingFqName)
        if (overwriteModifier) {
            return datadogTagModifier
        } else {
            // Modifier.then()
            val thenCall = builder.irCall(
                modifierThenSymbol,
                modifierClass.owner.defaultType
            )
            thenCall.putValueArgument(0, expression)
            thenCall.dispatchReceiver = datadogTagModifier
            return thenCall
        }
    }

    private fun buildDatadogTagModifierCall(
        builder: DeclarationIrBuilder,
        functionName: String,
        isImageComposableFunction: Boolean = false
    ): IrCall {
        val datadogTagIrCall = builder.irCall(
            datadogTagFunctionSymbol,
            modifierClass.defaultType
        ).also {
            // Modifier
            it.extensionReceiver = builder.irGetObjectValue(
                type = modifierCompanionClassSymbol.createType(false, emptyList()),
                classSymbol = modifierCompanionClassSymbol
            )
            it.putValueArgument(0, builder.irString(functionName))

            // For the images, Role.Image must be assigned to Semantics.Role to ensure that
            // Session Replay is able to resolve the role.
            it.putValueArgument(1, builder.irBoolean(isImageComposableFunction))
        }
        return datadogTagIrCall
    }

    private fun isAnonymousFunction(name: Name): Boolean = name == SpecialNames.ANONYMOUS

    private companion object {
        private val modifierClassFqName = FqName("androidx.compose.ui.Modifier")
        private val kotlinNothingFqName = FqName("kotlin.Nothing")
    }
}
