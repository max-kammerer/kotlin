/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.AsmUtil.numberFunctionOperandType
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.llvm.ExpressionCodegenLLVM
import org.jetbrains.kotlin.codegen.llvm.LLVMResult
import org.jetbrains.kotlin.codegen.llvm.toLLVMResult
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type

class BinaryOp(private val opcode: Int) : IntrinsicMethod() {
    private fun shift(): Boolean =
            opcode == ISHL || opcode == ISHR || opcode == IUSHR

    override fun toCallable(method: CallableMethod): Callable {
        val returnType = method.returnType
        assert(method.getValueParameters().size == 1)
        val operandType = numberFunctionOperandType(returnType)
        val paramType = if (shift()) Type.INT_TYPE else operandType

        return createBinaryIntrinsicCallable(returnType, paramType, operandType) {
            codegen, refs ->
            if (codegen is ExpressionCodegenLLVM) {
                llvmOperation(codegen, refs, opcode, paramType)
            } else {
                codegen.v.visitInsn(returnType.getOpcode(opcode))
                if (operandType != returnType)
                    StackValue.coerce(operandType, returnType, codegen.v)
                StackValue.none()
            }
        }
    }

    private fun llvmOperation(codegen: ExpressionCodegenLLVM, args: List<StackValue>, opcode: Int, paramType: Type): StackValue {
        val builder = codegen.builder
        val value = when (opcode) {
            Opcodes.IADD, Opcodes.LADD,  Opcodes.FADD, Opcodes.DADD -> builder.buildAdd(args[0].toLLVMResult, args[1].toLLVMResult, "add")
            Opcodes.ISUB, Opcodes.LSUB,  Opcodes.FSUB, Opcodes.DSUB-> builder.buildSub(args[0].toLLVMResult, args[1].toLLVMResult, "sub")
            else -> throw UnsupportedOperationException("$opcode");
        }
        return LLVMResult(value, paramType, builder)
    }

}
