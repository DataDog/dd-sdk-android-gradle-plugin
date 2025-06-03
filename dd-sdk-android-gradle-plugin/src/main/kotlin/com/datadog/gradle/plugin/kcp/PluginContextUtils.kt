package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.CallableId

@Suppress("TooManyFunctions")
internal interface PluginContextUtils {
    fun getModifierCompanionClass(): IrClassSymbol?
    fun getModifierClassSymbol(): IrClassSymbol?
    fun getDatadogModifierSymbol(): IrSimpleFunctionSymbol?
    fun getModifierThen(): IrSimpleFunctionSymbol?
    fun isComposableFunction(owner: IrFunction): Boolean
    fun referenceSingleFunction(callableId: CallableId, isCritical: Boolean = false): IrSimpleFunctionSymbol?
    fun getDatadogTrackEffectSymbol(): IrSimpleFunctionSymbol?
    fun isNavHostCall(owner: IrFunction): Boolean
    fun getNavHostControllerClassSymbol(): IrClassSymbol?
    fun getApplySymbol(): IrSimpleFunctionSymbol?
    fun isFoundationImage(owner: IrFunction, parent: IrPackageFragment): Boolean
    fun isMaterialIcon(owner: IrFunction, parent: IrPackageFragment): Boolean
    fun isCoilAsyncImage(owner: IrFunction, parent: IrPackageFragment): Boolean
    fun isComposeInstrumentationTargetFunc(irFunction: IrFunction, annotationModeEnabled: Boolean): Boolean
}
