/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.CallableId

/**
 * Utility functions for the Kotlin Compiler Plugin.
 * Internal use only.
 */
@Suppress("TooManyFunctions")
interface PluginContextUtils {
    /**
     * Gets the Modifier.Companion class symbol.
     * @suppress Internal use only.
     */
    fun getModifierCompanionClass(): IrClassSymbol?

    /**
     * Gets the Modifier class symbol.
     * @suppress Internal use only.
     */
    fun getModifierClassSymbol(): IrClassSymbol?

    /**
     * Gets the Datadog Modifier function symbol.
     * @suppress Internal use only.
     */
    fun getDatadogModifierSymbol(): IrSimpleFunctionSymbol?

    /**
     * Gets the Modifier.then function symbol.
     * @suppress Internal use only.
     */
    fun getModifierThen(): IrSimpleFunctionSymbol?

    /**
     * Checks if a function is a Composable function.
     * @suppress Internal use only.
     */
    fun isComposableFunction(owner: IrFunction): Boolean

    /**
     * References a single function by its CallableId.
     * @suppress Internal use only.
     */
    fun referenceSingleFunction(callableId: CallableId, isCritical: Boolean = false): IrSimpleFunctionSymbol?

    /**
     * Gets the Datadog track Composable effect function symbol.
     * @suppress Internal use only.
     */
    fun getDatadogTrackEffectSymbol(): IrSimpleFunctionSymbol?

    /**
     * Checks if a function is a NavHost call.
     * @suppress Internal use only.
     */
    fun isNavHostCall(owner: IrFunction): Boolean

    /**
     * Gets the NavHostController class symbol.
     * @suppress Internal use only.
     */
    fun getNavHostControllerClassSymbol(): IrClassSymbol?

    /**
     * Gets the `apply` function symbol.
     * @suppress Internal use only.
     */
    fun getApplySymbol(): IrSimpleFunctionSymbol?

    /**
     * Checks if a function is a foundation Image Composable.
     * @suppress Internal use only.
     */
    fun isFoundationImage(owner: IrFunction, parent: IrPackageFragment): Boolean

    /**
     * Checks if a function is a material Icon Composable.
     * @suppress Internal use only.
     */
    fun isMaterialIcon(owner: IrFunction, parent: IrPackageFragment): Boolean

    /**
     * Checks if a function is a Coil AsyncImage Composable.
     * @suppress Internal use only.
     */
    fun isCoilAsyncImage(owner: IrFunction, parent: IrPackageFragment): Boolean

    /**
     * Checks if a function is a target for Compose instrumentation.
     * @suppress Internal use only.
     */
    fun isComposeInstrumentationTargetFunc(irFunction: IrFunction, annotationModeEnabled: Boolean): Boolean
}
