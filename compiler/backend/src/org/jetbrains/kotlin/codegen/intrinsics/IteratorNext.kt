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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns.COLLECTIONS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.fileClasses.internalNameWithoutInnerClasses
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class IteratorNext : IntrinsicMethod() {
    override fun toCallable(method: CallableMethod): Callable {
        val type = AsmUtil.unboxType(method.returnType)
        return object : IntrinsicCallable(type, listOf(), AsmTypes.OBJECT_TYPE, null) {
            override fun genInvokeInstruction(codegen: ExpressionCodegen, generatedArgRefs: List<StackValue>): StackValue {
                val primitiveClassName = getKotlinPrimitiveClassName(returnType)
                codegen.v.invokevirtual(
                        getPrimitiveIteratorType(primitiveClassName).internalName,
                        "next${primitiveClassName.asString()}",
                        "()" + returnType.descriptor,
                        false
                )
                return StackValue.none()
            }
        }
    }

    companion object {
        // Type.CHAR_TYPE -> "Char"
        private fun getKotlinPrimitiveClassName(type: Type): Name {
            return JvmPrimitiveType.get(type.className).primitiveType.typeName
        }

        // "Char" -> type for kotlin.collections.CharIterator
        fun getPrimitiveIteratorType(primitiveClassName: Name): Type {
            val iteratorName = Name.identifier(primitiveClassName.asString() + "Iterator")
            return Type.getObjectType(COLLECTIONS_PACKAGE_FQ_NAME.child(iteratorName).internalNameWithoutInnerClasses)
        }
    }
}
