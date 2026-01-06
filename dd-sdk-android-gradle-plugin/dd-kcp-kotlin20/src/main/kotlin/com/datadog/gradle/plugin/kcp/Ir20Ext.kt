/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

@file:Suppress("TooManyFunctions")

package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.primaryConstructor

// Builds Lambda of (T.() -> Unit)
internal fun IrPluginContext.irUnitLambdaExpression(
    body: IrBody,
    irDeclarationParent: IrDeclarationParent?,
    receiverType: IrType
): IrFunctionExpression {
    return buildCompatIrFunctionExpression(
        symbols.irBuiltIns.functionN(0).defaultType,
        irSimpleFunction(
            name = SpecialNames.ANONYMOUS,
            visibility = DescriptorVisibilities.LOCAL,
            returnType = symbols.unit.defaultType,
            origin = getCompatLambdaOrigin(),
            body = body
        ).apply {
            irDeclarationParent?.let {
                this.parent = irDeclarationParent
            }
            this.extensionReceiverParameter = IrFactoryImpl.createValueParameter(
                startOffset,
                endOffset,
                getCompatDefinedOrigin(),
                symbol = getCompatValueParameterSymbolImpl(),
                name = Name.identifier("receiver"),
                index = -1,
                type = receiverType,
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false,
                isHidden = false,
                isAssignable = false
            ).apply {
                irDeclarationParent?.let {
                    this.parent = irDeclarationParent
                }
            }
        }
    )
}

private fun irSimpleFunction(
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

private fun getCompatLambdaOrigin(): IrDeclarationOriginImpl {
    return getCompatDeclarationOrigin("LOCAL_FUNCTION_FOR_LAMBDA") {
        IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
    }
}

private fun getCompatFakeOverrideOrigin(): IrDeclarationOriginImpl {
    return getCompatDeclarationOrigin("FAKE_OVERRIDE") {
        IrDeclarationOrigin.FAKE_OVERRIDE
    }
}

private fun getCompatDefinedOrigin(): IrDeclarationOriginImpl {
    return getCompatDeclarationOrigin("DEFINED") {
        IrDeclarationOrigin.DEFINED
    }
}

private fun getCompatDeclarationOrigin(
    name: String,
    default: () -> IrDeclarationOriginImpl
): IrDeclarationOriginImpl {
    // In Kotlin 1.9, `IrDeclarationOriginImpl` is an abstract class, not in 2.0
    return if (IrDeclarationOriginImpl::class.isAbstract) {
        val nestedClass = IrDeclarationOrigin::class.nestedClasses
            .firstOrNull { it.simpleName == name }
        val instance = nestedClass?.objectInstance
        instance as IrDeclarationOriginImpl
    } else {
        default.invoke()
    }
}

private fun getCompatLambdaStateOrigin(): IrStatementOriginImpl {
    // In Kotlin 1.9, `IrStatementOriginImpl` is an abstract class, not in 2.0
    return if (IrStatementOriginImpl::class.isAbstract) {
        val lambdaClass = IrStatementOrigin::class.nestedClasses
            .firstOrNull { it.simpleName == "LAMBDA" }
        val instance = lambdaClass?.objectInstance
        return instance as IrStatementOriginImpl
    } else {
        IrStatementOrigin.LAMBDA
    }
}

private fun getCompatSimpleFunctionSymbol(): IrSimpleFunctionSymbol {
    return IrSimpleFunctionSymbolImpl::class.createInstance()
}

private fun getCompatValueParameterSymbolImpl(): IrValueParameterSymbolImpl {
    return IrValueParameterSymbolImpl::class.createInstance()
}

private fun buildCompatIrFunctionExpression(
    type: IrType,
    function: IrSimpleFunction
): IrFunctionExpression {
    // Kotlin Version 1.9.23 ~ 1.9.25 has `IrFunctionExpressionImpl` class with public constructor.
    // Kotlin Version 2.0 ~ 2.0.10 has `IrFunctionExpressionImpl` class with internal constructor.
    // Kotlin Version 2.0.20 ~ 2.0.21 has added `IrFunctionExpressionImpl` function in `builders.kt`
    val primaryConstructor = IrFunctionExpressionImpl::class.primaryConstructor
    val kClass = getClassSafe("org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImplKt")
    val kFunction = kClass?.declaredFunctions?.firstOrNull { it.name == "IrFunctionExpressionImpl" }
    return kFunction?.let {
        it.call(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            function,
            getCompatLambdaStateOrigin()
        ) as? IrFunctionExpression
    } ?: primaryConstructor?.takeIf {
        it.visibility == KVisibility.PUBLIC
    }?.call(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        type,
        function,
        getCompatLambdaStateOrigin()
    ) ?: IrFunctionExpressionImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        type,
        function,
        getCompatLambdaStateOrigin()
    )
}

@Suppress("TooGenericExceptionCaught", "SwallowedException")
private fun getClassSafe(className: String): KClass<*>? {
    return try {
        Class.forName(className).kotlin
    } catch (e: Exception) {
        // Ignore all the exceptions and return null
        null
    }
}
