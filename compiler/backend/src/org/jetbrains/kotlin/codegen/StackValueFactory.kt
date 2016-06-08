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

package org.jetbrains.kotlin.codegen

import com.intellij.psi.tree.IElementType
import org.jetbrains.org.objectweb.asm.Type

interface StackValueFactory {

    fun none(): StackValue

    fun onStack(type: Type): StackValue

    fun local0(): StackValue

    fun local(index: Int, type: Type): StackValue

    fun constant(value: Any?, type: Type): StackValue

    fun cmp(opToken: IElementType, type: Type, left: StackValue, right: StackValue): StackValue

    fun not(stackValue: StackValue): StackValue

    fun or(left: StackValue, right: StackValue): StackValue

    fun and(left: StackValue, right: StackValue): StackValue

    fun compareIntWithZero(argument: StackValue, operation: Int): StackValue

    fun compareWithNull(argument: StackValue, operation: Int): StackValue

    fun arrayElement(type: Type, array: StackValue, index: StackValue): StackValue

}

object ByteCodeStackValueFactory : StackValueFactory {

    override fun none() = StackValue.none()

    override fun onStack(type: Type) = StackValue.onStack(type)

    override fun local0() = StackValue.local0()

    override fun local(index: Int, type: Type) = StackValue.local(index, type)

    override fun constant(value: Any?, type: Type) = StackValue.constant(value, type)

    override fun cmp(opToken: IElementType, type: Type, left: StackValue, right: StackValue) = StackValue.cmp(opToken, type, left, right)

    override fun not(stackValue: StackValue) = StackValue.not(stackValue)

    override fun or(left: StackValue, right: StackValue) = StackValue.or(left, right)

    override fun and(left: StackValue, right: StackValue) = StackValue.and(left, right)

    override fun compareIntWithZero(argument: StackValue, operation: Int) = StackValue.compareIntWithZero(argument, operation)

    override fun compareWithNull(argument: StackValue, operation: Int) = StackValue.compareWithNull(argument, operation)

    override fun arrayElement(type: Type, array: StackValue, index: StackValue) = StackValue.arrayElement(type, array, index)

}