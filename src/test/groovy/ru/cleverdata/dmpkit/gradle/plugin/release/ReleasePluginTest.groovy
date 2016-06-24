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
    public void apply_should_addTaskToProject(String task) throws Exception {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'cleverdata-dmpkit-gradle-release'

        assertThat(project.tasks.findByName(task), notNullValue())
    }

    @Test
    public void apply_should_notAddUnexpectedTasks() throws Exception {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'cleverdata-dmpkit-gradle-release'

        assertThat(project.tasks.findAll { it.name.startsWith('dmpkit') }, hasSize(tasks().length))
    }

    @Test
    @SuppressWarnings("GroovyAssignabilityCheck")
    public void apply_should_registerExtension() throws Exception {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'cleverdata-dmpkit-gradle-release'

        def extension = project.dmpkitRelease
        assertThat(extension, notNullValue())
        assertThat(extension.snapshotSuffix, equalTo('-SNAPSHOT'))
        assertThat(extension.envBuildNumber, nullValue())
        assertThat(extension.defaultBranch, equalTo('master'))
        assertThat(extension.dryRun, equalTo(false))
        assertThat(extension.scmDir, nullValue())
    }

}
