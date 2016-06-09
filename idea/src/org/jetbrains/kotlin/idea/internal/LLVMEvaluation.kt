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

package org.jetbrains.kotlin.idea.internal

import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.psi.KtFile


fun evaluate(state: GenerationState, file: KtFile): String {
    val sb = StringBuilder()
    val text = file.text
    val prefix = "//EVALUATE: "
    val split = text.split("\n".toRegex()).filter { it.startsWith(prefix) }
    for (line in split) {
        val expression = line.substringAfter(prefix)
        sb.append("$expression = ${evaluate(state, expression)}\n")
    }
    return sb.toString()
}

fun evaluate(state: GenerationState, line: String): String {
    val funName = line.substringBefore("(")
    val params = line.substringAfter("(").substringBefore(")").split(",").map { it.trim() }.filterNot { it.isEmpty() }.toTypedArray()
    return state.llvmState.evaluateFunction(funName, *params)
}