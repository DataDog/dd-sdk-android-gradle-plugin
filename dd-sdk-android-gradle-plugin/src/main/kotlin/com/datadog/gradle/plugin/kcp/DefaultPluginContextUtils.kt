package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class DefaultPluginContextUtils(
    private val pluginContext: IrPluginContext,
    private val messageCollector: MessageCollector
) : PluginContextUtils {

    private val modifierClassId = ClassId(composeUiPackageName, modifierClassRelativeName)
    private val modifierThenCallableId = CallableId(modifierClassId, modifierThenIdentifier)
    private val datadogModifierCallableId = CallableId(
        packageName = datadogPackageName,
        callableName = datadogModifierIdentifier
    )

    override fun getModifierCompanionClass(): IrClassSymbol? {
        return getModifierClassSymbol()?.owner?.companionObject()?.symbol ?: logNotFoundError(
            MODIFIER_COMPANION_NAME
        )
    }

    override fun getModifierClassSymbol(): IrClassSymbol? {
        return pluginContext
            .referenceClass(modifierClassId) ?: logNotFoundError(
            modifierClassId.asString()
        )
    }

    override fun getDatadogModifierSymbol(): IrSimpleFunctionSymbol? {
        return referenceSingleFunction(datadogModifierCallableId)
    }

    override fun getModifierThen(): IrSimpleFunctionSymbol? {
        return referenceSingleFunction(modifierThenCallableId)
    }

    override fun isComposableFunction(owner: IrFunction): Boolean {
        return !isAndroidX(owner) && hasComposableAnnotation(owner)
    }

    override fun referenceSingleFunction(callableId: CallableId): IrSimpleFunctionSymbol? {
        return pluginContext
            .referenceFunctions(callableId)
            .singleOrNull() ?: logSingleMatchError(callableId.callableName.asString())
    }

    private fun <T> logSingleMatchError(target: String): T? {
        messageCollector.report(CompilerMessageSeverity.ERROR, ERROR_SINGLE_MATCH.format(target))
        return null
    }

    private fun <T> logNotFoundError(target: String): T? {
        messageCollector.report(CompilerMessageSeverity.ERROR, ERROR_NOT_FOUND.format(target))
        return null
    }

    private fun isAndroidX(owner: IrFunction): Boolean {
        val packageName = owner.parent.kotlinFqName.asString()
        return packageName.startsWith("androidx")
    }

    private fun hasComposableAnnotation(owner: IrFunction): Boolean =
        owner.hasAnnotation(composableFqName)

    companion object {
        private const val ERROR_SINGLE_MATCH = "%s has none or several references."
        private const val ERROR_NOT_FOUND = "%s is not found."
        private const val MODIFIER_COMPANION_NAME = "Modifier.Companion"
        private val composableFqName = FqName("androidx.compose.runtime.Composable")
        private val datadogPackageName = FqName("com.datadog.kcp.compose")
        private val datadogModifierIdentifier = Name.identifier("datadog")
        private val composeUiPackageName = FqName("androidx.compose.ui")
        private val modifierClassRelativeName = Name.identifier("Modifier")
        private val modifierThenIdentifier = Name.identifier("then")
    }
}
