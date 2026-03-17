/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

// Builds Lambda of (T.() -> Unit)
@OptIn(DeprecatedForRemovalCompilerApi::class, UnsafeDuringIrConstructionAPI::class)
internal fun IrPluginContext.irUnitLambdaExpression(
    body: IrBody,
    irDeclarationParent: IrDeclarationParent?,
    receiverType: IrType
): IrFunctionExpression {
    return buildCompatIrFunctionExpression(
        type = irBuiltIns.functionN(0).defaultType,
        function = irSimpleFunction(
            name = SpecialNames.ANONYMOUS,
            visibility = DescriptorVisibilities.LOCAL,
            returnType = irBuiltIns.unitType,
            origin = getCompatLambdaOrigin(),
            body = body
        ).apply {
            irDeclarationParent?.let {
                this.parent = irDeclarationParent
            }
            this.extensionReceiverParameter = IrFactoryImpl.createValueParameter(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = getCompatDefinedOrigin(),
                kind = IrParameterKind.ExtensionReceiver,
                name = Name.identifier("receiver"),
                type = receiverType,
                isAssignable = false,
                symbol = getCompatValueParameterSymbolImpl(),
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false,
                isHidden = false
            ).apply {
                irDeclarationParent?.let {
                    this.parent = irDeclarationParent
                }
            }
        }
    )
}

internal fun irSimpleFunction(
    name: Name,
    visibility: DescriptorVisibility,
    returnType: IrType,
    origin: IrDeclarationOrigin,
    body: IrBody,
    symbol: IrSimpleFunctionSymbol = getCompatSimpleFunctionSymbol(),
    modality: Modality = Modality.FINAL,
    isInline: Boolean = false,
    isExternal: Boolean = false,
    isTailrec: Boolean = false,
    isSuspend: Boolean = false,
    isOperator: Boolean = false,
    isInfix: Boolean = false,
    isExpect: Boolean = false,
    isFakeOverride: Boolean = origin == getCompatFakeOverrideOrigin(),
    containerSource: DeserializedContainerSource? = null
): IrSimpleFunction = IrFactoryImpl.createSimpleFunction(
    startOffset = UNDEFINED_OFFSET,
    endOffset = UNDEFINED_OFFSET,
    origin = origin,
    symbol = symbol,
    name = name,
    visibility = visibility,
    modality = modality,
    returnType = returnType,
    isInline = isInline,
    isExternal = isExternal,
    isTailrec = isTailrec,
    isSuspend = isSuspend,
    isOperator = isOperator,
    isInfix = isInfix,
    isExpect = isExpect,
    isFakeOverride = isFakeOverride,
    containerSource = containerSource
).apply {
    this.body = body
}

private fun getCompatLambdaOrigin(): IrDeclarationOrigin {
    return getCompatDeclarationOrigin("LOCAL_FUNCTION_FOR_LAMBDA") {
        IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
    }
}

private fun getCompatFakeOverrideOrigin(): IrDeclarationOrigin {
    return getCompatDeclarationOrigin("FAKE_OVERRIDE") {
        IrDeclarationOrigin.FAKE_OVERRIDE
    }
}

private fun getCompatDefinedOrigin(): IrDeclarationOrigin {
    return getCompatDeclarationOrigin("DEFINED") {
        IrDeclarationOrigin.DEFINED
    }
}

@Suppress("UNCHECKED_CAST")
private fun getCompatDeclarationOrigin(
    name: String,
    default: () -> IrDeclarationOrigin
): IrDeclarationOrigin {
    // In Kotlin 2.3.20+, IrDeclarationOriginImpl.Regular was introduced as a property delegate,
    // changing the JVM return type of companion property getters (binary incompatibility).
    // Use reflection to access the property by name when running on such versions.
    val hasRegularSubtype = IrDeclarationOriginImpl::class.nestedClasses
        .any { it.simpleName == "Regular" }
    return if (hasRegularSubtype) {
        IrDeclarationOrigin::class.companionObjectInstance?.let { instance ->
            instance::class.memberProperties
                .filterIsInstance<KProperty1<Any, *>>()
                .first { it.name == name }
                .get(instance) as IrDeclarationOrigin
        } ?: default()
    } else {
        default()
    }
}

private fun getCompatSimpleFunctionSymbol(): IrSimpleFunctionSymbol {
    return IrSimpleFunctionSymbolImpl::class.createInstance()
}

private fun getCompatValueParameterSymbolImpl(): IrValueParameterSymbol {
    return IrValueParameterSymbolImpl::class.createInstance()
}

private fun buildCompatIrFunctionExpression(
    type: IrType,
    function: IrSimpleFunction
): IrFunctionExpression {
    val primaryConstructor = IrFunctionExpressionImpl::class.primaryConstructor
    return primaryConstructor?.takeIf {
        it.visibility == KVisibility.PUBLIC
    }?.call(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        type,
        function,
        IrStatementOrigin.LAMBDA
    ) ?: IrFunctionExpressionImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        type,
        function,
        IrStatementOrigin.LAMBDA
    )
}
