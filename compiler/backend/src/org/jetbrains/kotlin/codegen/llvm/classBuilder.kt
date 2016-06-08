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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.llvm.Module
import org.llvm.TypeRef
import org.llvm.Value
import kotlin.properties.Delegates

class LLVMClassBuilderFactory(val llvmState: LLVMState, delegate: ClassBuilderFactory) : DelegatingClassBuilderFactory(delegate) {

    override fun getClassBuilderMode(): ClassBuilderMode {
        return ClassBuilderMode.FULL
    }

    override fun newClassBuilder(origin: JvmDeclarationOrigin): DelegatingClassBuilder {
        return ClassBuilder(llvmState, delegate.newClassBuilder(origin))
    }

    override fun asText(builder: ClassBuilder?): String? {
        return "LLVM:\n" + llvmState.getAllModulesText() + "\nJAVA:\n" + super.asText(builder)
    }

    override fun close() {

    }
}

class ClassBuilder(val llvmState: LLVMState, val _delegate: ClassBuilder) : DelegatingClassBuilder() {

    var clazz by Delegates.notNull<Module>()

    override fun getDelegate(): ClassBuilder  = _delegate

    override fun defineClass(origin: PsiElement?, version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>) {
        delegate.defineClass(origin, version, access, name, signature, superName, interfaces)
        clazz = llvmState.createClass(name)
    }

    override fun newMethod(origin: JvmDeclarationOrigin, access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
        val newMethod = super.newMethod(origin, access, name, desc, signature, exceptions)
        val argumentTypes = Type.getArgumentTypes(desc)
        val returnType = Type.getReturnType(desc)
        val newFunction = clazz.addFunction(name, TypeRef.functionType(returnType.LLVMType, *argumentTypes.map { it.LLVMType }.toTypedArray()))

        return LLVMMethodVisitor(newFunction, newMethod)
    }
}

class LLVMMethodVisitor(val function: Value, mv: MethodVisitor) : MethodVisitor(Opcodes.ASM5, mv)