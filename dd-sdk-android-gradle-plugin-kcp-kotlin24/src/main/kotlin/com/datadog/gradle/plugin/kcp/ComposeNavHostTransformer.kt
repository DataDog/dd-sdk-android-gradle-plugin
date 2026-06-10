/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/**
 * Transformer to instrument Jetpack Compose Navigation Host.
 * Internal use only.
 */
@UnsafeDuringIrConstructionAPI
@OptIn(DeprecatedForRemovalCompilerApi::class)
class ComposeNavHostTransformer(
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

    private lateinit var trackEffectFunctionSymbol: IrSimpleFunctionSymbol
    private lateinit var navHostControllerClassSymbol: IrClassSymbol
    private var acceptAllNavDestinationsConstructorSymbol: IrConstructorSymbol? = null
    private var datadogGetInstanceFunctionSymbol: IrSimpleFunctionSymbol? = null
    private var datadogObjectClassSymbol: IrClassSymbol? = null

    /**
     * Initializes references to required symbols for the transformer.
     */
    @Suppress("ReturnCount")
    fun initReferences(): Boolean {
        trackEffectFunctionSymbol = pluginContextUtils.getDatadogTrackEffectSymbol() ?: run {
            messageCollector.strongWarning(ERROR_MISSING_DATADOG_COMPOSE_INTEGRATION)
            return false
        }

        navHostControllerClassSymbol = pluginContextUtils.getNavHostControllerClassSymbol() ?: run {
            messageCollector.info(ERROR_MISSING_COMPOSE_NAV)
            return false
        }

        // Resolve symbols needed to fill all default arguments of InstrumentedNavigationViewTrackingEffect.
        // In Compose 2.4.0, @Composable functions no longer generate a $default static method,
        // so the transformer must supply every argument explicitly to avoid null IR slots.
        acceptAllNavDestinationsConstructorSymbol =
            pluginContextUtils.getAcceptAllNavDestinationsConstructorSymbol()
        if (acceptAllNavDestinationsConstructorSymbol == null) {
            messageCollector.strongWarning(ERROR_MISSING_DATADOG_RUM_TRACKING)
            return false
        }

        datadogObjectClassSymbol = pluginContextUtils.getDatadogObjectClassSymbol()
        datadogGetInstanceFunctionSymbol = pluginContextUtils.getDatadogGetInstanceFunctionSymbol()
        if (datadogGetInstanceFunctionSymbol == null) {
            messageCollector.strongWarning(ERROR_MISSING_DATADOG_CORE)
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
        val builder = visitedBuilders.lastOrNull() ?: return super.visitCall(expression)

        val irSimpleFunction = expression.symbol.owner
        if (pluginContextUtils.isNavHostCall(irSimpleFunction)) {
            expression.logExpressionBeforeTransformation()

            // Process children first so the navController argument and content lambda are
            // fully transformed before we capture the navController value.
            super.visitCall(expression)

            val allParams = irSimpleFunction.symbol.owner.parameters
            for (irValueParameter in allParams.filter {
                it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context
            }) {
                if (irValueParameter.type.getClass()?.symbol == navHostControllerClassSymbol) {
                    val paramIdx = allParams.indexOf(irValueParameter)
                    val navControllerArg = expression.arguments[paramIdx] ?: continue
                    expression.logExpressionAfterTransformation()
                    return appendTrackingEffect(navControllerArg, expression, paramIdx, builder)
                }
            }
            expression.logExpressionAfterTransformation()
            return expression
        }
        return super.visitCall(expression)
    }

    private fun appendTrackingEffect(
        navControllerArg: IrExpression,
        navHostCall: IrCall,
        navControllerIdx: Int,
        builder: DeclarationIrBuilder
    ): IrExpression {
        // Build the tracking effect call directly — NOT inside a lambda — so that the Compose
        // compiler processes it in the composable calling context and adds $composer/$changed.
        //
        // All arguments must be set explicitly. In Compose 2.4.0, @Composable functions no
        // longer generate a $default static method, so null IR argument slots (which normally
        // trigger the $default mechanism) cause a NoSuchMethodError at runtime.
        val trackEffectCall = builder.irCall(trackEffectFunctionSymbol)

        return builder.irBlock(resultType = navHostCall.type) {
            // Capture the navController once to avoid evaluating a potentially side-effectful
            // expression twice (once for the tracking call, once for NavHost).
            val tmpNavController = irTemporary(navControllerArg, nameHint = "navController")

            trackEffectCall.arguments[0] = irGet(tmpNavController)
            trackEffectCall.arguments[1] = irTrue()
            acceptAllNavDestinationsConstructorSymbol?.let { constructor ->
                trackEffectCall.arguments[2] = irCallConstructor(constructor, emptyList())
            }
            datadogGetInstanceFunctionSymbol?.let { getInstanceFunc ->
                val getInstanceCall = irCall(getInstanceFunc)
                datadogObjectClassSymbol?.let { datadogClass ->
                    val dispatchReceiverIdx = getInstanceFunc.owner.parameters
                        .indexOfFirst { it.kind == IrParameterKind.DispatchReceiver }
                    if (dispatchReceiverIdx >= 0) {
                        getInstanceCall.arguments[dispatchReceiverIdx] = irGetObject(datadogClass)
                    }
                }
                trackEffectCall.arguments[TRACK_EFFECT_SDK_CORE_ARG_IDX] = getInstanceCall
            }

            navHostCall.arguments[navControllerIdx] = irGet(tmpNavController)
            // InstrumentedNavigationViewTrackingEffect(navController, true, AcceptAllNavDestinations(), Datadog.getInstance())
            +trackEffectCall
            // NavHost(..., navController = navController, ...)
            +navHostCall
        }
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

    private companion object {
        private const val TRACK_EFFECT_SDK_CORE_ARG_IDX = 3
    }
}
