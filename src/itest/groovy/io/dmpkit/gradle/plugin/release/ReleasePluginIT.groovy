/*
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

import org.ajoberstar.grgit.Branch
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.arrayContainingInAnyOrder
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.matchesPattern
import static org.hamcrest.Matchers.not

class ReleasePluginIT {

    private Path tmpDir;

    @BeforeMethod
    void setUp(Method test) throws Exception {
        Path root = new File(this.class.getResource("${this.class.simpleName}.class").toURI()).parentFile.toPath()
        tmpDir = Files.createTempDirectory(root.toAbsolutePath(), test.name).toAbsolutePath()
    }

    @AfterMethod
    void tearDown() throws Exception {
        assertThat(tmpDir.deleteDir(), is(true))
    }

    @Test
    void dmpkitPrintCurrentVersion_should_printCurrentVersion() throws Exception {
        createGradleFiles(tmpDir)

        BuildResult result = GradleRunner.create()
            .withProjectDir(tmpDir.toFile())
            .withPluginClasspath()
            .withArguments('dmpkitPrintCurrentVersion', '--stacktrace', '--refresh-dependencies')
            .build();

        assertThat(result.task(':dmpkitPrintCurrentVersion').outcome, equalTo(TaskOutcome.SUCCESS));
        assertThat(result.output, containsString('3.2.1-SNAPSHOT'));
    }

    @Test
    void dmpkitPrintReleaseVersion_should_printReleaseVersion() throws Exception {
        createGradleFiles(tmpDir)

        BuildResult result = GradleRunner.create()
            .withProjectDir(tmpDir.toFile())
            .withPluginClasspath()
            .withArguments('dmpkitPrintReleaseVersion', '--stacktrace', '--refresh-dependencies')
            .build();

        assertThat(result.task(':dmpkitPrintReleaseVersion').outcome, equalTo(TaskOutcome.SUCCESS));
        assertThat(result.output, allOf(
            containsString('3.2.1'),
            not(containsString('3.2.1-SNAPSHOT'))
        ));
    }

    @Test
    void dmpkitPrintDefaultBranch_should_printDefaultBranch() throws Exception {
        createGradleFiles(tmpDir)

        String buildScript = """
            dmpkitRelease {
                defaultBranch = 'super-development-branch'
            }
        """
        Files.write(tmpDir.resolve('build.gradle'), buildScript.getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.APPEND)

        Grgit.init(dir: tmpDir.toString()).close()

        BuildResult result = GradleRunner.create()
            .withProjectDir(tmpDir.toFile())
            .withPluginClasspath()
            .withArguments('dmpkitPrintDefaultBranch', '--stacktrace', '--refresh-dependencies')
            .build();

        assertThat(result.task(':dmpkitPrintDefaultBranch').outcome, equalTo(TaskOutcome.SUCCESS));
        assertThat(result.output, containsString('super-development-branch'));
    }

    @Test
    void dmpkitPrintCurrentBranch_should_printCurrentBranch() throws Exception {
        createGradleFiles(tmpDir)

        Grgit scm = Grgit.init(dir: tmpDir.toString())
        try {
            scm.add(patterns: ['settings.gradle', 'build.gradle', 'gradle.properties'])
            scm.commit(message: 'Gradle files added')
            scm.checkout(branch: 'super-development-branch', createBranch: true)
        } finally {
            scm.close()
        }

        BuildResult result = GradleRunner.create()
            .withProjectDir(tmpDir.toFile())
            .withPluginClasspath()
            .withArguments('dmpkitPrintCurrentBranch', '--stacktrace', '--refresh-dependencies')
            .build();

        assertThat(result.task(':dmpkitPrintCurrentBranch').outcome, equalTo(TaskOutcome.SUCCESS));
        assertThat(result.output, containsString('super-development-branch'));
    }

    @Test
    void dmpkitPrintReleaseBranch_should_printReleaseBranch() throws Exception {
        createGradleFiles(tmpDir)

        Grgit.init(dir: tmpDir.toString()).close()

        BuildResult result = GradleRunner.create()
            .withProjectDir(tmpDir.toFile())
            .withPluginClasspath()
            .withArguments('dmpkitPrintReleaseBranch', '--stacktrace', '--refresh-dependencies')
            .build();

        assertThat(result.task(':dmpkitPrintReleaseBranch').outcome, equalTo(TaskOutcome.SUCCESS));
        assertThat(result.output, allOf(
            containsString('release/test-project-3.2.1'),
            not(containsString('release/test-project-3.2.1-SNAPSHOT'))
        ));
    }

    @Test
    void dmpkitPrintReleaseTag_should_printReleaseTag() throws Exception {
        createGradleFiles(tmpDir)

        Grgit.init(dir: tmpDir.toString()).close()

        BuildResult result = GradleRunner.create()
            .withProjectDir(tmpDir.toFile())
            .withPluginClasspath()
            .withArguments('dmpkitPrintReleaseTag', '--stacktrace', '--refresh-dependencies')
            .build();

        assertThat(result.task(':dmpkitPrintReleaseTag').outcome, equalTo(TaskOutcome.SUCCESS));
        assertThat(result.output, allOf(
            containsString('test-project-3.2.1'),
            not(containsString('test-project-3.2.1-SNAPSHOT'))
        ));
    }

    @Test
    void dmpkitPrintRevision_should_printRevision() throws Exception {
        createGradleFiles(tmpDir)

        Grgit scm = Grgit.init(dir: tmpDir.toString())
        Commit commit = null
        try {
            scm.add(patterns: ['gradle.properties'])
            commit = scm.commit(message: 'Props file added')
        } finally {
            scm.close()
        }

        BuildResult result = GradleRunner.create()
            .withProjectDir(tmpDir.toFile())
            .withPluginClasspath()
            .withArguments('dmpkitPrintRevision', '--stacktrace', '--refresh-dependencies')
            .build();

        assertThat(result.task(':dmpkitPrintRevision').outcome, equalTo(TaskOutcome.SUCCESS));
        assertThat(result.output, containsString(commit.id));
    }

    @Test
    @SuppressWarnings("GroovyAssignabilityCheck")
    void dmpkitCreateReleaseBranch_should_createReleaseBranch() throws Exception {
        createGradleFiles(tmpDir)

        Grgit scm = Grgit.init(dir: tmpDir.toString())
        try {
            scm.add(patterns: ['gradle.properties'])
            scm.commit(message: 'Props file added')

            BuildResult result = GradleRunner.create()
                .withProjectDir(tmpDir.toFile())
                .withPluginClasspath()
                .withArguments('dmpkitCreateReleaseBranch', '--stacktrace', '--refresh-dependencies')
                .build();

            assertThat(result.task(':dmpkitCreateReleaseBranch').outcome, equalTo(TaskOutcome.SUCCESS));

            assertThat(scm.branch.list().any { it.fullName.contains('release/test-project-3.2.1') }, is(true))
            assertThat(scm.branch.current.fullName, containsString('release/test-project-3.2.1'))
        } finally {
            scm.close()
        }
    }

    @Test
    void dmpkitUpdateVersion_should_updateVersionsFile() throws Exception {
        createGradleFiles(tmpDir)

        Grgit scm = Grgit.init(dir: tmpDir.toString())
        try {
            scm.add(patterns: ['gradle.properties'])
            scm.commit(message: 'Props file added')
            scm.checkout(branch: 'release/test-project-3.2.1', createBranch: true)
        } finally {
            scm.close()
        }

        BuildResult result = GradleRunner.create()
            .withProjectDir(tmpDir.toFile())
            .withPluginClasspath()
            .withArguments('dmpkitUpdateVersion', '--stacktrace', '--refresh-dependencies')
            .build();

        assertThat(result.task(':dmpkitUpdateVersion').outcome, equalTo(TaskOutcome.SUCCESS));

        result = GradleRunner.create()
            .withProjectDir(tmpDir.toFile())
            .withPluginClasspath()
            .withArguments('properties', '--stacktrace', '--refresh-dependencies')
            .build();

        assertThat(result.output, matchesPattern(~/(?s).*version\s*[:=]\s*3\.2\.1.*/));
        assertThat(tmpDir.resolve('gradle.properties').toFile().text.trim(), equalTo('version=3.2.1'))
    }

    @Test
    void dmpkitUpdateVersion_should_ensureReleaseBranch() throws Exception {
        createGradleFiles(tmpDir)

        Grgit.init(dir: tmpDir.toString()).close()

        BuildResult result = GradleRunner.create()
            .withProjectDir(tmpDir.toFile())
            .withPluginClasspath()
            .withArguments('dmpkitUpdateVersion', '--stacktrace', '--refresh-dependencies')
            .buildAndFail();

        assertThat(result.task(':dmpkitUpdateVersion').outcome, equalTo(TaskOutcome.FAILED));
        assertThat(result.output, matchesPattern(~/(?s).*IllegalStateException.*?Not on the release branch.*/));
    }

    @Test
    void dmpkitCommitVersion_should_commitReleaseVersion() throws Exception {
        createGradleFiles(tmpDir)

        String buildScript = """
            afterEvaluate {
                println "dmpkitRevision: \${dmpkitRelease.revision()}"
            }
        """
        Files.write(tmpDir.resolve('build.gradle'), buildScript.getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.APPEND)

        Grgit scm = Grgit.init(dir: tmpDir.toString())
        try {
            scm.add(patterns: ['settings.gradle', 'build.gradle'])
            Commit commit = scm.commit(message: 'Gradle files added')
            scm.checkout(branch: 'release/test-project-3.2.1', createBranch: true)

            BuildResult result = GradleRunner.create()
                .withProjectDir(tmpDir.toFile())
                .withPluginClasspath()
                .withArguments('dmpkitCommitVersion', '--stacktrace', '--refresh-dependencies')
                .build();
            assertThat(result.task(':dmpkitCommitVersion').outcome, equalTo(TaskOutcome.SUCCESS));

            assertThat(scm.head().fullMessage, equalTo('release :: test-project :: 3.2.1'))
            assertThat(result.output, matchesPattern(~/(?s).*dmpkitRevision\s*[:=]\s*${commit.id}.*/));
        } finally {
            scm.close()
        }
    }

    @Test
    void dmpkitCommitVersion_should_ensureReleaseBranch() throws Exception {
        createGradleFiles(tmpDir)

        Grgit.init(dir: tmpDir.toString()).close()

        BuildResult result = GradleRunner.create()
            .withProjectDir(tmpDir.toFile())
            .withPluginClasspath()
            .withArguments('dmpkitCommitVersion', '--stacktrace', '--refresh-dependencies')
            .buildAndFail();
        assertThat(result.task(':dmpkitCommitVersion').outcome, equalTo(TaskOutcome.FAILED));
        assertThat(result.output, matchesPattern(~/(?s).*IllegalStateException.*?Not on the release branch.*/));
    }

    @Test
    void dmpkitTagReleaseBranch_should_tagReleaseBranch() throws Exception {
        createGradleFiles(tmpDir)

        Grgit scm = Grgit.init(dir: tmpDir.toString())
        try {
            scm.add(patterns: ['settings.gradle', 'build.gradle', 'gradle.properties'])
            Commit commit = scm.commit(message: 'Gradle files added')
            scm.checkout(branch: 'release/test-project-3.2.1', createBranch: true)

            BuildResult result = GradleRunner.create()
                .withProjectDir(tmpDir.toFile())
                .withPluginClasspath()
                .withArguments('dmpkitTagReleaseBranch', '--stacktrace', '--refresh-dependencies')
                .build();
            assertThat(result.task(':dmpkitTagReleaseBranch').outcome, equalTo(TaskOutcome.SUCCESS));

            List<Tag> tags = scm.tag.list()
            assertThat(tags, hasSize(1))
            assertThat(tags[0].fullName, containsString('test-project-3.2.1'))
            assertThat(tags[0].fullMessage, equalTo('release :: test-project :: 3.2.1'))
            assertThat(tags[0].commit.id, equalTo(commit.id))
        } finally {
            scm.close()
        }
    }

    @Test
    void dmpkitTagReleaseBranch_should_ensureReleaseBranch() throws Exception {
        createGradleFiles(tmpDir)

        Grgit.init(dir: tmpDir.toString()).close()

        BuildResult result = GradleRunner.create()
            .withProjectDir(tmpDir.toFile())
            .withPluginClasspath()
            .withArguments('dmpkitTagReleaseBranch', '--stacktrace', '--refresh-dependencies')
            .buildAndFail();
        assertThat(result.task(':dmpkitTagReleaseBranch').outcome, equalTo(TaskOutcome.FAILED));
        assertThat(result.output, matchesPattern(~/(?s).*IllegalStateException.*?Not on the release branch.*/));
    }

    @Test
    void dmpkitPushReleaseTag_should_pushReleaseTag() throws Exception {
        Path local = tmpDir.resolve('local')
        local.toFile().mkdirs()

        Path remote = tmpDir.resolve('remote')
        remote.toFile().mkdirs()

        createGradleFiles(local)

        Grgit scm = Grgit.init(dir: local.toString())
        Commit commit = null
        try {
            scm.add(patterns: ['settings.gradle', 'build.gradle', 'gradle.properties'])
            commit = scm.commit(message: 'Gradle files added')
            scm.checkout(branch: 'release/test-project-3.2.1', createBranch: true)
            scm.tag.add(name: 'test-project-3.2.1', message: 'release :: test-project :: 3.2.1')
            scm.remote.add(name: 'origin', url: "${remote.toAbsolutePath()}")
        } finally {
            scm.close()
        }

        scm = Grgit.init(dir: remote.toString())
        try {
            BuildResult result = GradleRunner.create()
                .withProjectDir(local.toFile())
                .withPluginClasspath()
                .withArguments('dmpkitPushReleaseTag', '--stacktrace', '--refresh-dependencies')
                .build();
            assertThat(result.task(':dmpkitPushReleaseTag').outcome, equalTo(TaskOutcome.SUCCESS));

            List<Tag> tags = scm.tag.list()
            assertThat(tags, hasSize(1))
            assertThat(tags[0].fullName, containsString('test-project-3.2.1'))
            assertThat(tags[0].fullMessage, equalTo('release :: test-project :: 3.2.1'))

            scm.checkout(branch: 'tags/test-project-3.2.1')
            assertThat(scm.head().id, equalTo(commit.id))

            assertThat(remote.toFile().list(),
                arrayContainingInAnyOrder('settings.gradle', 'build.gradle', 'gradle.properties', '.git'))
        } finally {
            scm.close()
        }
    }

    @Test
    void dmpkitPushReleaseTag_should_pushOnlySingleReleaseTag() throws Exception {
        Path local = tmpDir.resolve('local')
        local.toFile().mkdirs()

        Path remote = tmpDir.resolve('remote')
        remote.toFile().mkdirs()

        createGradleFiles(local)

        Grgit scm = Grgit.init(dir: local.toString())
        Commit commit = null
        try {
            scm.add(patterns: ['settings.gradle', 'build.gradle', 'gradle.properties'])
            commit = scm.commit(message: 'Gradle files added')
            scm.checkout(branch: 'release/test-project-3.2.1', createBranch: true)
            scm.tag.add(name: 'test-project-3.2.0', message: 'release :: test-project :: 3.2.0')
            scm.tag.add(name: 'test-project-3.2.1', message: 'release :: test-project :: 3.2.1')
            scm.remote.add(name: 'origin', url: "${remote.toAbsolutePath()}")
        } finally {
            scm.close()
        }

        scm = Grgit.init(dir: remote.toString())
        try {
            BuildResult result = GradleRunner.create()
                .withProjectDir(local.toFile())
                .withPluginClasspath()
                .withArguments('dmpkitPushReleaseTag', '--stacktrace', '--refresh-dependencies')
                .build();
            assertThat(result.task(':dmpkitPushReleaseTag').outcome, equalTo(TaskOutcome.SUCCESS));

            List<Tag> tags = scm.tag.list()
            assertThat(tags, hasSize(1))
            assertThat(tags[0].fullName, containsString('test-project-3.2.1'))
            assertThat(tags[0].fullMessage, equalTo('release :: test-project :: 3.2.1'))

            scm.checkout(branch: 'tags/test-project-3.2.1')
            assertThat(scm.head().id, equalTo(commit.id))

            assertThat(remote.toFile().list(),
                arrayContainingInAnyOrder('settings.gradle', 'build.gradle', 'gradle.properties', '.git'))
        } finally {
            scm.close()
        }
    }

    @Test
    void dmpkitPushReleaseTag_should_ensureReleaseBranch() throws Exception {
        createGradleFiles(tmpDir)

        Grgit.init(dir: tmpDir.toString()).close()

        BuildResult result = GradleRunner.create()
            .withProjectDir(tmpDir.toFile())
            .withPluginClasspath()
            .withArguments('dmpkitPushReleaseTag', '--stacktrace', '--refresh-dependencies')
            .buildAndFail();
        assertThat(result.task(':dmpkitPushReleaseTag').outcome, equalTo(TaskOutcome.FAILED));
        assertThat(result.output, matchesPattern(~/(?s).*IllegalStateException.*?Not on the release branch.*/));
    }

    @Test
    void dmpkitCheckoutDefaultBranch_should_checkoutDefaultBranch() throws Exception {
        createGradleFiles(tmpDir)

        String buildScript = """
            dmpkitRelease {
                defaultBranch = 'develop'
            }
        """
        Files.write(tmpDir.resolve('build.gradle'), buildScript.getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.APPEND)

        Grgit scm = Grgit.init(dir: tmpDir.toString())
        try {
            scm.add(patterns: ['settings.gradle', 'build.gradle'])
            scm.commit(message: 'Gradle files added')

            scm.checkout(branch: 'develop', createBranch: true)
            scm.checkout(branch: 'release/test-project-3.2.1', createBranch: true)

            BuildResult result = GradleRunner.create()
                .withProjectDir(tmpDir.toFile())
                .withPluginClasspath()
                .withArguments('dmpkitCheckoutDefaultBranch', '--stacktrace', '--refresh-dependencies')
                .build();
            assertThat(result.task(':dmpkitCheckoutDefaultBranch').outcome, equalTo(TaskOutcome.SUCCESS));

            assertThat(scm.branch.current.name, equalTo('develop'))
        } finally {
            scm.close()
        }
    }

    @Test
    void dmpkitRemoveReleaseBranch_should_removeReleaseBranch() throws Exception {
        createGradleFiles(tmpDir)

        Grgit scm = Grgit.init(dir: tmpDir.toString())
        try {
            scm.add(patterns: ['settings.gradle', 'build.gradle', 'gradle.properties'])
            scm.commit(message: 'Gradle files added')
            scm.checkout(branch: 'release/test-project-3.2.1', createBranch: true)
            scm.checkout(branch: 'master')

            BuildResult result = GradleRunner.create()
                .withProjectDir(tmpDir.toFile())
                .withPluginClasspath()
                .withArguments('dmpkitRemoveReleaseBranch', '--stacktrace', '--refresh-dependencies')
                .build();
            assertThat(result.task(':dmpkitRemoveReleaseBranch').outcome, equalTo(TaskOutcome.SUCCESS));

            List<Branch> branches = scm.branch.list()
            assertThat(branches, hasSize(1))
            assertThat(branches[0].fullName, containsString('master'))
        } finally {
            scm.close()
        }
    }

    @Test
    void dmpkitReleaseCleanup_should_checkoutDefault_and_removeReleaseBranch() throws Exception {
        createGradleFiles(tmpDir)

        Grgit scm = Grgit.init(dir: tmpDir.toString())
        try {
            scm.add(patterns: ['settings.gradle', 'build.gradle', 'gradle.properties'])
            scm.commit(message: 'Gradle files added')
            scm.checkout(branch: 'release/test-project-3.2.1', createBranch: true)

            assertThat(scm.branch.current.name, equalTo('release/test-project-3.2.1'))

            BuildResult result = GradleRunner.create()
                .withProjectDir(tmpDir.toFile())
                .withPluginClasspath()
                .withArguments('dmpkitReleaseCleanup', '--stacktrace', '--refresh-dependencies')
                .build();
            assertThat(result.task(':dmpkitReleaseCleanup').outcome, equalTo(TaskOutcome.SUCCESS));

            List<Branch> branches = scm.branch.list()
            assertThat(branches, hasSize(1))
            assertThat(branches[0].fullName, containsString('master'))
        } finally {
            scm.close()
        }
    }

    @Test
    void dmpkitReleaseInit_should_createReleaseBranch_and_commitUpdatedVersion() throws Exception {
        createGradleFiles(tmpDir)

        Grgit scm = Grgit.init(dir: tmpDir.toString())
        try {
            scm.add(patterns: ['settings.gradle', 'build.gradle', 'gradle.properties'])
            scm.commit(message: 'Gradle files added')

            assertThat(scm.branch.current.name, equalTo('master'))

            BuildResult result = GradleRunner.create()
                .withProjectDir(tmpDir.toFile())
                .withPluginClasspath()
                .withArguments('dmpkitReleaseInit', '--stacktrace', '--refresh-dependencies')
                .build();
            assertThat(result.task(':dmpkitReleaseInit').outcome, equalTo(TaskOutcome.SUCCESS));

            assertThat(scm.branch.current.name, equalTo('release/test-project-3.2.1'))
            assertThat(scm.head().fullMessage, equalTo('release :: test-project :: 3.2.1'))
        } finally {
            scm.close()
        }
    }

    @Test
    @SuppressWarnings("GroovyAssignabilityCheck")
    void dmpkitReleaseFinish_should_pushTag() throws Exception {
        Path local = tmpDir.resolve('local')
        local.toFile().mkdirs()

        Path remote = tmpDir.resolve('remote')
        remote.toFile().mkdirs()

        createGradleFiles(local)

        Grgit scm = Grgit.init(dir: local.toString())
        Commit commit = null
        try {
            scm.add(patterns: ['settings.gradle', 'build.gradle', 'gradle.properties'])
            commit = scm.commit(message: 'Gradle files added')
            scm.checkout(branch: 'release/test-project-3.2.1', createBranch: true)
            scm.remote.add(name: 'origin', url: "${remote.toAbsolutePath()}")
        } finally {
            scm.close()
        }

        scm = Grgit.init(dir: remote.toString())
        try {
            BuildResult result = GradleRunner.create()
                .withProjectDir(local.toFile())
                .withPluginClasspath()
                .withArguments('dmpkitReleaseFinish', '--stacktrace', '--refresh-dependencies')
                .build();
            assertThat(result.task(':dmpkitReleaseFinish').outcome, equalTo(TaskOutcome.SUCCESS));

            List<Tag> tags = scm.tag.list()
            assertThat(tags, hasSize(1))
            assertThat(tags[0].fullName, containsString('test-project-3.2.1'))
            assertThat(tags[0].fullMessage, equalTo('release :: test-project :: 3.2.1'))

            scm.checkout(branch: 'tags/test-project-3.2.1')
            assertThat(scm.head().id, equalTo(commit.id))

            assertThat(remote.toFile().list(),
                arrayContainingInAnyOrder('settings.gradle', 'build.gradle', 'gradle.properties', '.git'))
        } finally {
            scm.close()
        }
    }

    private static void createGradleFiles(Path dir) throws Exception {
        String gradleProps = """
            version=3.2.1-SNAPSHOT
        """
        Files.write(dir.resolve('gradle.properties'), gradleProps.getBytes(StandardCharsets.UTF_8))

        String buildScript = """
            plugins {
                id 'dmpkit-gradle-release'
            }
        """
        Files.write(dir.resolve('build.gradle'), buildScript.getBytes(StandardCharsets.UTF_8))

        String settingsScript = """
            rootProject.name = 'test-project'
        """
        Files.write(dir.resolve('settings.gradle'), settingsScript.getBytes(StandardCharsets.UTF_8))
    }

}
