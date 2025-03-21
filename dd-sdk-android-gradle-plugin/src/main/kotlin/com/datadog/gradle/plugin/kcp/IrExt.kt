package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
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

// Builds Lambda of (T.() -> Unit)
internal fun IrPluginContext.irUnitLambdaExpression(
    body: IrBody,
    irDeclarationParent: IrDeclarationParent?,
    receiverType: IrType
): IrFunctionExpression {
    return irFunctionExpression(
        symbols.irBuiltIns.functionN(0).defaultType,
        irSimpleFunction(
            name = SpecialNames.ANONYMOUS,
            visibility = DescriptorVisibilities.LOCAL,
            returnType = symbols.unit.defaultType,
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
            body = body
        ).apply {
            irDeclarationParent?.let {
                this.parent = irDeclarationParent
            }
            this.extensionReceiverParameter = irFactory.createValueParameter(
                startOffset,
                endOffset,
                IrDeclarationOrigin.DEFINED,
                symbol = IrValueParameterSymbolImpl(),
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

internal fun irFunctionExpression(
    type: IrType,
    function: IrSimpleFunction
): IrFunctionExpression {
    return IrFunctionExpressionImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        type,
        function,
        IrStatementOrigin.LAMBDA
    )
}

internal fun irSimpleFunction(
    name: Name,
    visibility: DescriptorVisibility,
    returnType: IrType,
    origin: IrDeclarationOrigin,
    body: IrBody,
    symbol: IrSimpleFunctionSymbol = IrSimpleFunctionSymbolImpl(),
    modality: Modality = Modality.FINAL,
    isInline: Boolean = false,
    isExternal: Boolean = false,
    isTailrec: Boolean = false,
    isSuspend: Boolean = false,
    isOperator: Boolean = false,
    isInfix: Boolean = false,
    isExpect: Boolean = false,
    isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
    containerSource: DeserializedContainerSource? = null,
    factory: IrFactory = IrFactoryImpl
): IrSimpleFunction = factory.createSimpleFunction(
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
