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

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.StackValueFactory
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.llvm.Builder
import org.llvm.Value

class LLVMStackValueFactory(val builder: Builder) : StackValueFactory {
    override fun none(): StackValue {
        return StackValue.none()
    }

    override fun onStack(type: Type): StackValue {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun local0(): StackValue {
        return local(0, AsmTypes.OBJECT_TYPE)
    }

    override fun local(index: Int, type: Type): StackValue {
        return LLVMLocal(builder.variable("local$index", type.LLVMType), type, builder)
    }

    override fun constant(value: Any?, type: Type): StackValue {
        return LLVMConstant(value, builder, type)
    }

    override fun cmp(opToken: IElementType, type: Type, left: StackValue, right: StackValue): StackValue {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun not(stackValue: StackValue): StackValue {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun or(left: StackValue, right: StackValue): StackValue {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun and(left: StackValue, right: StackValue): StackValue {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun compareIntWithZero(argument: StackValue, operation: Int): StackValue {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun compareWithNull(argument: StackValue, operation: Int): StackValue {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun arrayElement(type: Type, array: StackValue, index: StackValue): StackValue {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun returnInsn(returnType: Type, value: StackValue?, v: InstructionAdapter) {
        if (returnType.equals(Type.VOID_TYPE)) {
            builder.buildRetVoid()
        }
        else {
            builder.buildRet((value as LLVMStackValue).toLLVMValue());
        }
    }

}

abstract class LLVMStackValue(val builder: Builder, type: Type) : StackValue(type) {
    abstract fun toLLVMValue(): Value

    abstract fun putLLVMSelector(type: Type): Value

    override fun putSelector(type: Type, v: InstructionAdapter): StackValue? {
        return put(type, v)
    }

    override fun put(type: Type, v: InstructionAdapter): StackValue? {
        return LLVMResult(putLLVMSelector(type), type, builder)
    }
}

class LLVMConstant(val value: Any?, builder: Builder, type: Type) : LLVMStackValue(builder, type) {
    override fun putLLVMSelector(type: Type): Value {
        val llvmConstant = toLLVMValue()
        return llvmConstant
    }

    override fun toLLVMValue(): Value = llvmConstant(type, value)
}

class LLVMLocal(val variable: Variable, type: Type, builder: Builder) : LLVMStackValue(builder, type) {
    override fun putLLVMSelector(type: Type): Value {
        return variable.load(builder)
    }

    override fun store(value: StackValue, v: InstructionAdapter, skipReceiver: Boolean) {
        //assert(value is LLVMStackValue) { "Not LLVM stack value" }
        variable.store(builder, value.put(type, v).toLLVMResult)
    }

    override fun toLLVMValue(): Value = variable.load(builder)
}

class Param(val param: Value, val type: Type, builder: Builder) : LLVMStackValue(builder, type) {
    override fun putLLVMSelector(type: Type): Value {
        return toLLVMValue()
    }

    override fun toLLVMValue(): Value = param
}

class LLVMResult(val result: Value, val type: Type, builder: Builder) : LLVMStackValue(builder, type) {
    override fun putLLVMSelector(type: Type): Value {
        return toLLVMValue()
    }

    override fun toLLVMValue(): Value = result
}

class LLVMOperation(val type: Type, builder: Builder, val operation: LLVMOperation.() -> Value) : LLVMStackValue(builder, type) {
    override fun putLLVMSelector(type: Type): Value {
        return toLLVMValue()
    }

    override fun toLLVMValue(): Value = operation()
}