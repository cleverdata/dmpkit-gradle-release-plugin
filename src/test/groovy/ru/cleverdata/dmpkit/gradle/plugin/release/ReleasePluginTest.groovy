/*
 * Copyright (c) 2014, CleverDATA, LLC. All Rights Reserved.
 *
 * All information contained herein is, and remains the property of CleverDATA, LLC. 
 * The intellectual and technical concepts contained herein are proprietary to 
 * CleverDATA, LLC. Dissemination of this information or reproduction of this 
 * material is strictly forbidden unless prior written permission is obtained from 
 * CleverDATA, LLC.
 *
 * Unless required by applicable law or agreed to in writing, software is distributed 
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 */

package ru.cleverdata.dmpkit.gradle.plugin.release

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

    private static final String PLUGIN_NAME = 'cleverdata-dmpkit-gradle-release'

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
