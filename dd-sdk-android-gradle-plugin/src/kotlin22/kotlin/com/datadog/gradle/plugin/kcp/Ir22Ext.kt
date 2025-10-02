
package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
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
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

// Builds Lambda of (T.() -> Unit)
@OptIn(DeprecatedForRemovalCompilerApi::class, UnsafeDuringIrConstructionAPI::class)
internal fun IrPluginContext.irUnitLambdaExpression(
    body: IrBody,
    irDeclarationParent: IrDeclarationParent?,
    receiverType: IrType
): IrFunctionExpression {
    return buildCompatIrFunctionExpression(
        type = symbols.irBuiltIns.functionN(0).defaultType,
        function = irSimpleFunction(
            name = SpecialNames.ANONYMOUS,
            visibility = DescriptorVisibilities.LOCAL,
            returnType = symbols.unit.owner.defaultType,
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
            body = body
        ).apply {
            irDeclarationParent?.let {
                this.parent = irDeclarationParent
            }
            this.extensionReceiverParameter = IrFactoryImpl.createValueParameter(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = IrDeclarationOrigin.DEFINED,
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
    isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
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
