package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
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

    override fun getDatadogTrackEffectSymbol(): IrSimpleFunctionSymbol? {
        return referenceSingleFunction(datadogTrackEffectCallableId)
    }

    override fun getNavHostControllerClassSymbol(): IrClassSymbol? {
        return pluginContext
            .referenceClass(navHostControllerCallableId) ?: logNotFoundError(
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
        return owner.name == IconIdentifier && parent.packageFqName == materialPackageName
    }

    override fun isCoilAsyncImage(owner: IrFunction, parent: IrPackageFragment): Boolean {
        return owner.name == AsyncImageIdentifier && parent.packageFqName == coilPackageName
    }

    override fun referenceSingleFunction(callableId: CallableId): IrSimpleFunctionSymbol? {
        return pluginContext
            .referenceFunctions(callableId)
            .singleOrNull() ?: logSingleMatchError(callableId.callableName.asString())
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
        private val datadogPackageName = FqName("com.datadog.android.compose")
        private val datadogModifierIdentifier = Name.identifier("datadog")
        private val composeUiPackageName = FqName("androidx.compose.ui")
        private val modifierClassRelativeName = Name.identifier("Modifier")
        private val modifierThenIdentifier = Name.identifier("then")
        private val navigationPackageName = FqName("androidx.navigation")
        private val navHostControllerIdentifier = Name.identifier("NavHostController")
        private val navHostCallName = FqName("androidx.navigation.compose.NavHost")
        private val datadogTrackEffectPackageName = FqName("com.datadog.android.compose")
        private val datadogTrackEffectIdentifier = Name.identifier("NavigationViewTrackingEffect")
        private val kotlinPackageName = FqName("kotlin")
        private val applyFunctionIdentifier = Name.identifier("apply")
        private val foundationPackageName = FqName("androidx.compose.foundation")
        private val ImageIdentifier = Name.identifier("Image")
        private val materialPackageName = FqName("androidx.compose.material")
        private val IconIdentifier = Name.identifier("Icon")
        private val coilPackageName = FqName("coil.compose")
        private val AsyncImageIdentifier = Name.identifier("AsyncImage")
        private val ComposeInstrumentationAnnotationName =
            FqName("com.datadog.android.compose.ComposeInstrumentation")
    }
}
