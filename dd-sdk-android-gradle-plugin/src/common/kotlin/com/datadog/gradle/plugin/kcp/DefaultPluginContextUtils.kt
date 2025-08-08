package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Utility class to provide access to various symbols and functions used in the plugin.
 * It provides methods to retrieve symbols for Modifier, NavHostController, and other
 * relevant components in the Compose UI framework.
 *
 * Internal use only.
 */
@UnsafeDuringIrConstructionAPI
class DefaultPluginContextUtils(
    private val pluginContext: IrPluginContext,
    private val messageCollector: MessageCollector
) : PluginContextUtils {

    private val modifierClassId = ClassId(composeUiPackageName, modifierClassRelativeName)
    private val modifierThenCallableId = CallableId(modifierClassId, modifierThenIdentifier)
    private val navHostControllerCallableId = ClassId(navigationPackageName, navHostControllerIdentifier)
    private val datadogTrackEffectCallableId = CallableId(datadogTrackEffectPackageName, datadogTrackEffectIdentifier)
    private val datadogModifierCallableId = CallableId(
        packageName = datadogPackageName,
        callableName = datadogModifierIdentifier
    )
    private val applyCallableId = CallableId(
        packageName = kotlinPackageName,
        callableName = applyFunctionIdentifier
    )

    override fun getModifierCompanionClass(): IrClassSymbol? {
        return getModifierClassSymbol()?.owner?.companionObject()?.symbol ?: warnNotFound(
            MODIFIER_COMPANION_NAME
        )
    }

    override fun getModifierClassSymbol(): IrClassSymbol? {
        return pluginContext
            .referenceClass(modifierClassId) ?: warnNotFound(
            modifierClassId.asString()
        )
    }

    override fun getDatadogModifierSymbol(): IrSimpleFunctionSymbol? {
        return referenceSingleFunction(datadogModifierCallableId, isCritical = true)
    }

    override fun getModifierThen(): IrSimpleFunctionSymbol? {
        return referenceSingleFunction(modifierThenCallableId)
    }

    override fun getDatadogTrackEffectSymbol(): IrSimpleFunctionSymbol? {
        return referenceSingleFunction(datadogTrackEffectCallableId, isCritical = true)
    }

    override fun getNavHostControllerClassSymbol(): IrClassSymbol? {
        return pluginContext
            .referenceClass(navHostControllerCallableId) ?: warnNotFound(
            navHostControllerCallableId.asString()
        )
    }

    override fun getApplySymbol(): IrSimpleFunctionSymbol? {
        return referenceSingleFunction(applyCallableId)
    }

    override fun isNavHostCall(owner: IrFunction): Boolean {
        return owner.kotlinFqName == navHostCallName
    }

    override fun isComposableFunction(owner: IrFunction): Boolean {
        return !isAndroidX(owner) && hasComposableAnnotation(owner)
    }

    override fun isFoundationImage(owner: IrFunction, parent: IrPackageFragment): Boolean {
        return owner.name == ImageIdentifier && parent.packageFqName == foundationPackageName
    }

    override fun isMaterialIcon(owner: IrFunction, parent: IrPackageFragment): Boolean {
        return owner.name == IconIdentifier &&
            (parent.packageFqName == materialPackageName || parent.packageFqName == material3PackageName)
    }

    override fun isCoilAsyncImage(owner: IrFunction, parent: IrPackageFragment): Boolean {
        return owner.name == AsyncImageIdentifier && parent.packageFqName == coilPackageName
    }

    override fun referenceSingleFunction(callableId: CallableId, isCritical: Boolean): IrSimpleFunctionSymbol? {
        return pluginContext
            .referenceFunctions(callableId)
            .singleOrNull() ?: logSingleMatchError(callableId.callableName.asString(), isCritical)
    }

    override fun isComposeInstrumentationTargetFunc(
        irFunction: IrFunction,
        annotationModeEnabled: Boolean
    ): Boolean {
        return isComposableFunction(irFunction) && (
            !annotationModeEnabled ||
                irFunction.hasAnnotation(ComposeInstrumentationAnnotationName)
            )
    }

    private fun <T> logSingleMatchError(target: String, isCritical: Boolean): T? {
        val level = if (isCritical) CompilerMessageSeverity.ERROR else CompilerMessageSeverity.STRONG_WARNING
        messageCollector.report(level, ERROR_SINGLE_MATCH.format(target))
        return null
    }

    private fun <T> warnNotFound(target: String): T? {
        messageCollector.report(CompilerMessageSeverity.WARNING, ERROR_NOT_FOUND.format(target))
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
        private val datadogPackageName = FqName("com.datadog.android.compose")
        private val datadogModifierIdentifier = Name.identifier("instrumentedDatadog")
        private val composeUiPackageName = FqName("androidx.compose.ui")
        private val modifierClassRelativeName = Name.identifier("Modifier")
        private val modifierThenIdentifier = Name.identifier("then")
        private val navigationPackageName = FqName("androidx.navigation")
        private val navHostControllerIdentifier = Name.identifier("NavHostController")
        private val navHostCallName = FqName("androidx.navigation.compose.NavHost")
        private val datadogTrackEffectPackageName = FqName("com.datadog.android.compose")
        private val datadogTrackEffectIdentifier = Name.identifier("InstrumentedNavigationViewTrackingEffect")
        private val kotlinPackageName = FqName("kotlin")
        private val applyFunctionIdentifier = Name.identifier("apply")
        private val foundationPackageName = FqName("androidx.compose.foundation")
        private val ImageIdentifier = Name.identifier("Image")
        private val materialPackageName = FqName("androidx.compose.material")
        private val material3PackageName = FqName("androidx.compose.material3")
        private val IconIdentifier = Name.identifier("Icon")
        private val coilPackageName = FqName("coil.compose")
        private val AsyncImageIdentifier = Name.identifier("AsyncImage")
        private val ComposeInstrumentationAnnotationName =
            FqName("com.datadog.android.compose.ComposeInstrumentation")
    }
}
