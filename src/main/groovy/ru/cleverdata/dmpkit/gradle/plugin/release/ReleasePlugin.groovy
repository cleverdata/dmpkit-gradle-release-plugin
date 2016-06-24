package ru.cleverdata.dmpkit.gradle.plugin.release

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskExecutionException
import org.ajoberstar.grgit.Grgit

public class ReleasePlugin implements Plugin<Project> {

    private static final String EXTENSION = 'dmpkitRelease';
    private static final String GRADLE_PROPS = 'gradle.properties';

    @SuppressWarnings("GroovyUnusedDeclaration")
    public static class ReleasePluginExtension {
        // settings that can be set from within a build script
        public String snapshotSuffix = '-SNAPSHOT'
        public String envBuildNumber
        public String envDefaultBranch
        public String defaultBranch = 'master'
        public boolean dryRun
        public String scmDir

        // props, detected during invocation of the plugin
        public Closure<String> currentBranch
        public Closure<String> currentVersion
        public Closure<String> releaseVersion
        public Closure<String> releaseBranch
        public Closure<String> releaseTag
        public Closure<String> revision

        protected Grgit scm
    }

    @Override
    @SuppressWarnings(["GroovyUnusedAssignment", "GroovyAssignabilityCheck"])
    public void apply(Project project) {
        ReleasePluginExtension pluginExt = project.extensions.create(EXTENSION, ReleasePluginExtension)
        afterEvaluate(project)

        Task dmpkitPrintCurrentVersion = project.task('dmpkitPrintCurrentVersion', {
            description = 'Print current version of the project'
        }) << {
            println project[EXTENSION].currentVersion()
        }

        Task dmpkitPrintReleaseVersion = project.task('dmpkitPrintReleaseVersion', {
            description = 'Print release version of the project'
        }) << {
            println project[EXTENSION].releaseVersion()
        }

        Task dmpkitPrintDefaultBranch = project.task('dmpkitPrintDefaultBranch', {
            description = 'Print default branch of the project'
        }) << {
            println project[EXTENSION].defaultBranch
        }

        Task dmpkitPrintCurrentBranch = project.task('dmpkitPrintCurrentBranch', {
            description = 'Print release branch of the project'
        }) << {
            println project[EXTENSION].currentBranch()
        }

        Task dmpkitPrintReleaseBranch = project.task('dmpkitPrintReleaseBranch', {
            description = 'Print release branch of the project'
        }) << {
            println project[EXTENSION].releaseBranch()
        }

        Task dmpkitPrintReleaseTag = project.task('dmpkitPrintReleaseTag', {
            description = 'Print release tag of the project'
        }) << {
            println project[EXTENSION].releaseTag()
        }

        Task dmpkitPrintRevision = project.task('dmpkitPrintRevision', {
            description = 'Print revision of the project'
        }) << {
            println project[EXTENSION].revision()
        }

        Task dmpkitCreateReleaseBranch = project.task('dmpkitCreateReleaseBranch', {
            description = 'Create release branch of the project'
        }) << {
            Grgit scm = project[EXTENSION].scm
            scm.checkout(branch: project[EXTENSION].releaseBranch(), createBranch: true)
        }

        Task dmpkitUpdateVersion = project.task('dmpkitUpdateVersion', {
            description = 'Update version of the project to release version'
        }) << { Task task ->
            ensureReleaseBranch(task)

            String releaseVersion = project[EXTENSION].releaseVersion()
            project.ant.replaceregexp(
                file: GRADLE_PROPS,
                match: '(^\\s*version\\s*[=:]).*',
                replace: "\\1${releaseVersion}",
                flags: 'g',
                byline: true
            )
            project.allprojects {
                version = releaseVersion
            }
        }

        Task dmpkitCommitVersion = project.task('dmpkitCommitVersion', {
            description = 'Update version of the project to release version'
        }) << { Task task ->
            ensureReleaseBranch(task)

            Grgit scm = project[EXTENSION].scm
            scm.add(patterns: [GRADLE_PROPS])
            scm.commit(message: "release :: ${project.name} :: ${project[EXTENSION].releaseVersion()}")
        }

        Task dmpkitTagReleaseBranch = project.task('dmpkitTagReleaseBranch', {
            description = 'Tag the release branch'
        }) << { Task task ->
            ensureReleaseBranch(task)

            Grgit scm = project[EXTENSION].scm
            scm.tag.add(
                name: project[EXTENSION].releaseTag(),
                message: "release :: ${project.name} :: ${project[EXTENSION].releaseVersion()}"
            )
        }

        Task dmpkitPushReleaseTag = project.task('dmpkitPushReleaseTag', {
            description = 'Pushes release tag to the remote repository'
        }) << { Task task ->
            ensureReleaseBranch(task)

            Grgit scm = project[EXTENSION].scm
            scm.push(
                tags: true,
                // be able to check external project properties passed by means of -Prelease.dryRun=true
                dryRun: project[EXTENSION].dryRun || String.valueOf(project.properties['release.dryRun']).toBoolean()
            )
        }

        Task dmpkitCheckoutDefaultBranch = project.task('dmpkitCheckoutDefaultBranch', {
            description = 'Check out the default branch'
        }) << {
            project[EXTENSION].scm.checkout(branch: project[EXTENSION].defaultBranch)
        }

        Task dmpkitRemoveReleaseBranch = project.task('dmpkitRemoveReleaseBranch', {
            description = 'Remove release branch'
        }) << {
            project[EXTENSION].scm.branch.remove(names: [project[EXTENSION].releaseBranch()], force: true)
        }

        Task dmpkitReleaseInit = project.task('dmpkitReleaseInit', {
            description = 'Initialize release of the project - creates branches, changes versions, commits changes, etc.'

            List<Task> subtasks = [ dmpkitCreateReleaseBranch, dmpkitUpdateVersion, dmpkitCommitVersion ]
            // order tasks, so that every subsequent task will run after the previous task
            subtasks.collate(2, 1, false).each { it[1].mustRunAfter(it[0]) }

            dependsOn = subtasks
        })

        Task dmpkitReleaseCleanup = project.task('dmpkitReleaseCleanup', {
            description = 'Remove release branch'

            List<Task> subtasks = [ dmpkitCheckoutDefaultBranch, dmpkitRemoveReleaseBranch ]
            // order tasks, so that every subsequent task will run after the previous task
            subtasks.collate(2, 1, false).each { it[1].mustRunAfter(it[0]) }

            dependsOn = subtasks
        })

        Task dmpkitReleaseFinish = project.task('dmpkitReleaseFinish', {
            description = 'Finish release of the project by tagging the release branch and pushing it to the remove repository'

            List<Task> subtasks = [ dmpkitTagReleaseBranch, dmpkitPushReleaseTag ]
            // order tasks, so that every subsequent task will run after the previous task
            subtasks.collate(2, 1, false).each { it[1].mustRunAfter(it[0]) }

            dependsOn = subtasks
        })
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    private static void afterEvaluate(Project project) {
        ReleasePluginExtension pluginExt = project[EXTENSION] as ReleasePluginExtension
        project.afterEvaluate {
            try {
                pluginExt.scmDir = Objects.toString(pluginExt.scmDir, project.rootDir.absolutePath)
            } catch (e) {
                project.logger.debug(e.getMessage(), e)
            }
            try {
                pluginExt.scm = Grgit.open(dir: pluginExt.scmDir)
            } catch (e) {
                project.logger.debug(e.getMessage(), e)
            }

            if (pluginExt.envDefaultBranch != null) {
                pluginExt.defaultBranch = System.env[pluginExt.envDefaultBranch]
            }

            pluginExt.currentBranch = {
                Grgit scm = project[EXTENSION].scm
                scm != null ? scm.branch.current.name : '' // by default empty to prevent failures if not under scm
            }
            pluginExt.currentVersion = {
                getCurrentVersion(project) ?: project.version.toString()
            }
            pluginExt.releaseVersion = {
                getReleaseVersion(project, project[EXTENSION].currentVersion())
            }
            pluginExt.releaseBranch = {
                "release/${project.name}-${project[EXTENSION].releaseVersion()}"
            }
            pluginExt.releaseTag = {
                "${project.name}-${project[EXTENSION].releaseVersion()}"
            }
            pluginExt.revision = {
                try {
                    project[EXTENSION].scm.head().id
                } catch (e) {
                    project.logger.debug(e.getMessage(), e)
                    ''
                }
            }
        }
    }

    private static String getCurrentVersion(Project project) {
        Properties props = new Properties()

        File propsFile = project.rootProject.file(GRADLE_PROPS)
        if (propsFile.exists()) {
            propsFile.withInputStream {
                props.load(it)
            }
        }

        props.version
    }

    private static String getReleaseVersion(Project project, String currentVersion) {
        String snapshotSuffix = project[EXTENSION].snapshotSuffix

        String ver = currentVersion
        if (ver.endsWith(snapshotSuffix)) {
            ver = ver.substring(0, ver.lastIndexOf(snapshotSuffix))

            String envBuildNumber = project[EXTENSION].envBuildNumber
            if (envBuildNumber != null) {
                String buildID = System.env[envBuildNumber]
                if (buildID != null) {
                    ver += "-${buildID}"
                }
            }
        }
        ver
    }

    private static void ensureReleaseBranch(Task task) {
        String currBranch = task.project[EXTENSION].currentBranch()
        String releaseBranch = task.project[EXTENSION].releaseBranch()
        if (!currBranch.equals(releaseBranch)) {
            throw new TaskExecutionException(task, new IllegalStateException(
                "Not on the release branch: [current: ${currBranch}, expected: ${releaseBranch}]"))
        }
    }

}
