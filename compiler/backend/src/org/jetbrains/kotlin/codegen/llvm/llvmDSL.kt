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

import org.llvm.Builder
import org.llvm.Module
import org.llvm.TypeRef
import org.llvm.Value

class Variable(val name: String, val slot: Value) {

    fun load(builder: Builder): Value {
        return builder.buildLoad(slot, name)
    }

    fun store(builder: Builder, value: Value): Value {
        return builder.buildStore(value, slot)
    }
}

fun Builder.variable(name: String, type: TypeRef): Variable {
    val slot = buildAlloca(type, name)
    return Variable(name, slot)
}

fun Builder.load(variable: Variable): Value {
    return variable.load(this)
}

fun Builder.store(value: Value, variable: Variable) {
    variable.store(this, value)
}

inline fun Module.addFunction(name: String, functionType: TypeRef, init: Value.() -> Unit): Value {
    val function = addFunction(name, functionType)
    function.init()
    return function
}

inline fun Value.param(index: Int, update: Value.() -> Unit) {
    getParam(index).update()
}

fun Value.param(index: Int): Value {
    return getParam(index)
}

inline fun Builder.build(operations: Builder.() -> Unit) {
    this.operations()
}

