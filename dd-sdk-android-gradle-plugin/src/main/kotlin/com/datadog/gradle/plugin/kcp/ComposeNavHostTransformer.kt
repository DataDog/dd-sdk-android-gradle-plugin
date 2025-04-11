package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal class ComposeNavHostTransformer(
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
    private val visitedScopes = ArrayDeque<IrDeclarationParent?>()

    private lateinit var trackEffectFunctionSymbol: IrSimpleFunctionSymbol
    private lateinit var navHostControllerClassSymbol: IrClassSymbol
    private lateinit var applyFunctionSymbol: IrSimpleFunctionSymbol

    @Suppress("ReturnCount")
    fun initReferences(): Boolean {
        trackEffectFunctionSymbol = pluginContextUtils.getDatadogTrackEffectSymbol() ?: run {
            error(ERROR_MISSING_DATADOG_COMPOSE_INTEGRATION)
            return false
        }

        navHostControllerClassSymbol = pluginContextUtils.getNavHostControllerClassSymbol() ?: run {
            error(ERROR_MISSING_COMPOSE_NAV)
            return false
        }
        applyFunctionSymbol = pluginContextUtils.getApplySymbol() ?: run {
            error(ERROR_MISSING_KOTLIN_STDLIB)
            return false
        }
        return true
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
        // We should visit Jetpack Compose content lambda body as function for instrumentation.
        expression.function.body?.accept(this, null)
        return super.visitFunctionExpression(expression)
    }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        val declarationName = declaration.name
        val functionName = if (!isAnonymousFunction(declarationName)) {
            declarationName.toString()
        } else {
            visitedFunctions.lastOrNull() ?: declarationName.toString()
        }
        if (pluginContextUtils.isComposeInstrumentationTargetFunc(declaration, annotationModeEnabled)) {
            visitedFunctions.add(functionName)
            visitedBuilders.add(DeclarationIrBuilder(pluginContext, declaration.symbol))
            visitedScopes.add(declaration)
            val irStatement = super.visitFunctionNew(declaration)
            visitedFunctions.removeLast()
            visitedBuilders.removeLast()
            visitedScopes.removeLast()
            return irStatement
        } else {
            return declaration
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val builder = visitedBuilders.lastOrNull() ?: return super.visitCall(
            expression
        )

        val irSimpleFunction = expression.symbol.owner
        if (pluginContextUtils.isNavHostCall(irSimpleFunction)) {
            expression.logExpressionBeforeTransformation()
            irSimpleFunction.valueParameters.forEachIndexed { index, irValueParameter ->
                if (irValueParameter.type.getClass()?.symbol == navHostControllerClassSymbol) {
                    expression.getValueArgument(index)?.let { argument ->
                        val irExpression = appendTrackingEffect(argument, builder)
                        expression.putValueArgument(index, irExpression)
                    }
                }
            }
            expression.logExpressionAfterTransformation()
        }
        return super.visitCall(expression)
    }

    private fun appendTrackingEffect(
        expression: IrExpression,
        builder: DeclarationIrBuilder
    ): IrExpression {
        // Build apply{ } call with function symbol
        val applyIrCall = builder.irCall(
            applyFunctionSymbol
        )

        // Assign expression return type to `apply{}` -> `apply<NavHostController>{}`
        applyIrCall.putTypeArgument(0, expression.type)

        // Assign expression as the dispatch receiver of `apply{ }` -> `navHost.apply<NavHostController>{ }`
        applyIrCall.extensionReceiver = expression

        // Build NavigationViewTrackingEffect function call
        val trackEffectCall: IrCall = builder.irCall(trackEffectFunctionSymbol)

        // Build lambda for apply function
        val lambda = createLocalAnonymousFunc(
            builder,
            navHostControllerClassSymbol.defaultType,
            trackEffectCall
        )

        lambda.function.extensionReceiverParameter?.let {
            trackEffectCall.putValueArgument(0, builder.irGet(it))
        }

        // Set the lambda as the argument of `apply` call
        applyIrCall.putValueArgument(0, lambda)
        return applyIrCall
    }

    private fun createLocalAnonymousFunc(
        builder: DeclarationIrBuilder,
        navHostControllerType: IrType,
        irCall: IrCall
    ): IrFunctionExpression {
        val scope = visitedScopes.lastOrNull()
        val irBlockBody = builder.irBlockBody {
            +irCall
        }
        return pluginContext.irUnitLambdaExpression(irBlockBody, scope, navHostControllerType)
    }

    private fun IrExpression.logExpressionBeforeTransformation() {
        messageCollector.report(
            CompilerMessageSeverity.LOGGING,
            "Expression Before Transformation:\n ${dumpKotlinLike()}"
        )
    }

    private fun IrExpression.logExpressionAfterTransformation() {
        messageCollector.report(
            CompilerMessageSeverity.LOGGING,
            "Expression After Transformation:\n ${dumpKotlinLike()}"
        )
    }

    private fun isAnonymousFunction(name: Name): Boolean = name == SpecialNames.ANONYMOUS

    private fun error(message: String) {
        messageCollector.report(CompilerMessageSeverity.ERROR, message)
    }

    companion object {
        private const val ERROR_MISSING_DATADOG_COMPOSE_INTEGRATION =
            "Missing com.datadoghq:dd-sdk-android-compose dependency."
        private const val ERROR_MISSING_COMPOSE_NAV =
            "Missing androidx.navigation:navigation-compose dependency."
        private const val ERROR_MISSING_KOTLIN_STDLIB =
            "Missing org.jetbrains.kotlin:kotlin-stdlib dependency."
    }
}
