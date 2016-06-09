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

import org.jetbrains.org.objectweb.asm.Type
import org.llvm.*

class LLVMState {

    val modules = linkedMapOf<String, Module>()

    val name2Function = linkedMapOf<String, Value>()

    fun createClass(name: String): Module {
        return Module.createWithName(name.LLVMName).apply { modules.put(name, this) }
    }

    fun createFunction(name: String, type: TypeRef, clazz: Module): Value {
        return clazz.addFunction(name, type).apply {
            name2Function[name] = this
        }
    }

    fun getAllModulesText() = modules.values.map { it.getContent() }.joinToString("\n")

    fun evaluateFunction(fName: String, vararg params: String): String {
        synchronized(LLVMState::class.java) {
            val function = name2Function[fName] ?: return "Can't find function $fName"
            val globalParent = function.globalParent
            globalParent.verify()

            val ee = ExecutionEngine.createForModule(globalParent)
            // Compile and run!

            val llvmParams = params.map { GenericValue.createInt(TypeRef.int32Type(), java.lang.Long.valueOf(it), true) }.toTypedArray()
            val result = ee.runFunction(function, *llvmParams)
            return ("" + result.toInt(true)).apply {
                //result.dispose()
                //ee.dispose()
            }
        }
    }

    fun free() {
        //modules.values.forEach { it.dispose() }
    }

}


val Type.LLVMName: String
    get() = internalName.LLVMName

val String.LLVMName: String
    get() = replace('/', '_')


val Type.LLVMType: TypeRef
    get() = when (this) {
        Type.VOID_TYPE -> TypeRef.voidType()
        Type.BOOLEAN_TYPE -> TypeRef.int1Type()
        Type.BYTE_TYPE -> TypeRef.int8Type()
        Type.SHORT_TYPE -> TypeRef.int16Type()
        Type.INT_TYPE -> TypeRef.int32Type()
        Type.LONG_TYPE -> TypeRef.int64Type()
        Type.CHAR_TYPE -> TypeRef.int8Type()
        Type.FLOAT_TYPE -> TypeRef.floatType()
        Type.DOUBLE_TYPE -> TypeRef.doubleType()
        else -> {
            if (this.sort == Type.ARRAY) {
                val elementType = this.elementType
                elementType.LLVMType.pointerType()
            }
            else {
                TypeRef.structTypeNamed(this.LLVMName)
            }
        }
    }

fun llvmConstant(type: Type, value: Any?): Value {
    val llvmType = type.LLVMType
    return when (type) {
        Type.BOOLEAN_TYPE -> TypeRef.int1Type().constInt(if (true == value) 1 else 0, false)
        Type.BYTE_TYPE, Type.SHORT_TYPE, Type.LONG_TYPE, Type.INT_TYPE -> llvmType.constInt((value as Number).toLong(), false)
        Type.SHORT_TYPE -> TypeRef.int16Type().constInt((value as Number).toLong(), false)
        Type.CHAR_TYPE -> TypeRef.int8Type().constInt((value as Character).charValue().toLong(), true)
        Type.FLOAT_TYPE -> TypeRef.floatType().constReal((value as Number).toDouble())
        Type.DOUBLE_TYPE -> TypeRef.doubleType().constReal((value as Number).toDouble())
        else -> {
            throw UnsupportedOperationException("$value : $type")
        }
    }
}