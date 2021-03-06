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

package org.jetbrains.kotlin

import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.codeStyle.PackageEntry
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractImportsTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() = KotlinTestUtils.getHomeDirectory()
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected fun doTest(testPath: String) {
        val settingManager = CodeStyleSettingsManager.getInstance()
        val tempSettings = settingManager.currentSettings.clone()
        settingManager.temporarySettings = tempSettings

        val codeStyleSettings = KotlinCodeStyleSettings.getInstance(project)

        try {
            val fixture = myFixture
            val dependencyPath = testPath.replace(".kt", ".dependency.kt")
            if (File(dependencyPath).exists()) {
                fixture.configureByFile(dependencyPath)
            }
            val javaDependencyPath = testPath.replace(".kt", ".dependency.java")
            if (File(javaDependencyPath).exists()) {
                fixture.configureByFile(javaDependencyPath)
            }

            fixture.configureByFile(testPath)

            val file = fixture.file as KtFile

            val fileText = file.text

            codeStyleSettings.NAME_COUNT_TO_USE_STAR_IMPORT = InTextDirectivesUtils.getPrefixedInt(fileText, "// NAME_COUNT_TO_USE_STAR_IMPORT:") ?: nameCountToUseStarImportDefault
            codeStyleSettings.NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS = InTextDirectivesUtils.getPrefixedInt(fileText, "// NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS:") ?: nameCountToUseStarImportForMembersDefault
            codeStyleSettings.IMPORT_NESTED_CLASSES = InTextDirectivesUtils.getPrefixedBoolean(fileText, "// IMPORT_NESTED_CLASSES:") ?: false

            InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// PACKAGE_TO_USE_STAR_IMPORTS:").forEach {
                codeStyleSettings.PACKAGES_TO_USE_STAR_IMPORTS.addEntry(PackageEntry(false, it.trim(), false))
            }
            InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// PACKAGES_TO_USE_STAR_IMPORTS:").forEach {
                codeStyleSettings.PACKAGES_TO_USE_STAR_IMPORTS.addEntry(PackageEntry(false, it.trim(), true))
            }

            project.executeWriteCommand("") {
                doTest(file)
            }

            KotlinTestUtils.assertEqualsToFile(File(testPath + ".after"), myFixture.file.text)
        }
        finally {
            settingManager.dropTemporarySettings()
        }
    }

    protected abstract fun doTest(file: KtFile)

    protected open val nameCountToUseStarImportDefault: Int
        get() = 1

    protected open val nameCountToUseStarImportForMembersDefault: Int
        get() = 3
}