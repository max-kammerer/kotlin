/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.llvm

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.llvm.Builder
import org.llvm.Value

class ExpressionCodegenLLVM(
        val llvmFunction: Value,
        val javaCodegen: ExpressionCodegen,
        val builder: Builder
) : ExpressionCodegen(object : MethodVisitor(Opcodes.ASM5){}, javaCodegen.frameMap, javaCodegen.returnType, javaCodegen.context, javaCodegen.state, javaCodegen.parentCodegen, LLVMStackValueFactory(builder)) {

    val typeMapper = state.typeMapper

    val entry = llvmFunction.appendBasicBlock("entry")

    val descToVariable = hashMapOf<DeclarationDescriptor, StackValue>()

    val llvmFactory = stackValueFactory as LLVMStackValueFactory

    init {
        builder.positionBuilderAtEnd(entry)
        frameMap.currentVars.forEachIndexed { i, typeAndDescriptor ->
            with(builder) {
                val stackValue = if (typeAndDescriptor.descriptor != null) {
                    descToVariable.getOrPut(typeAndDescriptor.descriptor) {
                        stackValueFactory.local(i, typeAndDescriptor.type)
                    }
                }
                else {
                    stackValueFactory.local(i, typeAndDescriptor.type)
                }
                stackValue.store(Param(llvmFunction.param(i), typeAndDescriptor.type, builder), v)
            }
        }
    }

    override fun stackValueForLocal(descriptor: DeclarationDescriptor, index: Int): StackValue? {
        return descToVariable.getOrPut(descriptor) {
            super.stackValueForLocal(descriptor, index)
        }
    }

    override fun enter(descriptor: DeclarationDescriptor, type: Type): StackValue? {
        return descToVariable.getOrPut(descriptor) {
            super.enter(descriptor, type)
        }
    }

    override fun generateIfExpression(expression: KtIfExpression, isStatement: Boolean): StackValue? {
        val asmType = if (isStatement) Type.VOID_TYPE else expressionType(expression)
        val condition = gen(expression.condition)

        val thenExpression = expression.then
        val elseExpression = expression.`else`

        return LLVMOperation(asmType, builder) {
            val iftrue = llvmFunction.appendBasicBlock("iftrue")
            val iffalse = llvmFunction.appendBasicBlock("iffalse")
            val end = llvmFunction.appendBasicBlock("end")

            builder.buildCondBr(condition.put(condition.type, v).toLLVMResult, iftrue, iffalse)
            builder.positionBuilderAtEnd(iftrue)
            gen(thenExpression, asmType)
            builder.buildBr(end)

            builder.positionBuilderAtEnd(iffalse)
            gen(elseExpression, asmType)
            builder.buildBr(end)
            builder.positionBuilderAtEnd(end)
            //TODO support expression
            stackValueFactory.constant(1, Type.INT_TYPE).toLLVMValue
        }
    }


    companion object {
        @JvmStatic
        fun returnExpression(javaCodegen: ExpressionCodegen, expr: KtExpression?) {
            val mv = javaCodegen.originalVisitor as LLVMMethodVisitor
            ExpressionCodegenLLVM(mv.function, javaCodegen, Builder.createBuilder()).returnExpression(expr)
        }
    }
}

val StackValue.toLLVMResult: Value
    get() = (this as LLVMStackValue).toLLVMValue()


val StackValue.toLLVMValue: Value
    get() = (this as LLVMStackValue).toLLVMValue()
