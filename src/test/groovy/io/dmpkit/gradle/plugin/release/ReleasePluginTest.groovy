/*
 * Copyright, 2019, CleverDATA, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dmpkit.gradle.plugin.release

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.notNullValue
import static org.hamcrest.Matchers.nullValue


class ReleasePluginTest {

    private static final String PLUGIN_NAME = 'dmpkit-gradle-release'

    @DataProvider
    private Object[][] tasks() {[
        ['dmpkitPrintCurrentVersion'],
        ['dmpkitPrintReleaseVersion'],
        ['dmpkitPrintDefaultBranch'],
        ['dmpkitPrintCurrentBranch'],
        ['dmpkitPrintReleaseBranch'],
        ['dmpkitPrintReleaseTag'],
        ['dmpkitPrintRevision'],
        ['dmpkitCreateReleaseBranch'],
        ['dmpkitUpdateVersion'],
        ['dmpkitCommitVersion'],
        ['dmpkitTagReleaseBranch'],
        ['dmpkitPushReleaseTag'],
        ['dmpkitCheckoutDefaultBranch'],
        ['dmpkitRemoveReleaseBranch'],
        ['dmpkitReleaseInit'],
        ['dmpkitReleaseCleanup'],
        ['dmpkitReleaseFinish']
    ]}

    @Test(dataProvider = "tasks")
    void apply_should_addTaskToProject(String task) throws Exception {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply PLUGIN_NAME

        assertThat(project.tasks.findByName(task), notNullValue())
    }

    @Test
    void apply_should_notAddUnexpectedTasks() throws Exception {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply PLUGIN_NAME

        assertThat(project.tasks.findAll { it.name.startsWith('dmpkit') }, hasSize(tasks().length))
    }

    @Test
    @SuppressWarnings("GroovyAssignabilityCheck")
    void apply_should_registerExtension() throws Exception {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply PLUGIN_NAME

        def extension = project.dmpkitRelease
        assertThat(extension, notNullValue())
        assertThat(extension.snapshotSuffix, equalTo('-SNAPSHOT'))
        assertThat(extension.envBuildNumber, nullValue())
        assertThat(extension.defaultBranch, equalTo('master'))
        assertThat(extension.dryRun, equalTo(false))
        assertThat(extension.scmDir, nullValue())
    }

}
