package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.CallableId

internal interface PluginContextUtils {
    fun getModifierCompanionClass(): IrClassSymbol?
    fun getModifierClassSymbol(): IrClassSymbol?
    fun getDatadogModifierSymbol(): IrSimpleFunctionSymbol?
    fun getModifierThen(): IrSimpleFunctionSymbol?
    fun isComposableFunction(owner: IrFunction): Boolean
    fun referenceSingleFunction(callableId: CallableId): IrSimpleFunctionSymbol?
}
