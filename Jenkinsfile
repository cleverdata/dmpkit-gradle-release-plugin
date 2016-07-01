#!groovy

/***********************************************************************************************************************
*                                      IMPORTS                                                                         *
***********************************************************************************************************************/
import hudson.plugins.git.BranchSpec
import hudson.EnvVars

/***********************************************************************************************************************
*                                      VARIABLES                                                                       *
***********************************************************************************************************************/
// whether to skip empty stages or not
// can be useful to skip release stages if current branch is not going to be released
boolean skipEmptyStages = true

String teamName = 'cleverdata'
String repoName = 'cleverdata-dmpkit-gradle-release-plugin'
String remoteName = 'origin'

String gitBranchName = env.BRANCH_NAME
String gitBranchShortName = ''
String gitBranchRef = ''
boolean releaseBranch = false
List<String> releaseBranches = ["${remoteName}/develop*"]

Exception error = null
currentBuild.result = 'SUCCESS'

/***********************************************************************************************************************
*                                      UTILITIES                                                                       *
***********************************************************************************************************************/
def isReleaseBranch = {
    boolean matches = false
    EnvVars envVars = env.getEnvironment()
    for (String specStr : releaseBranches) {
        BranchSpec spec = new BranchSpec(specStr)
        if (spec.matches(gitBranchRef, envVars)) {
            matches = true
            break
        }
        if (spec.matches(gitBranchName, envVars)) {
            matches = true
            break
        }
        if (spec.matches(gitBranchShortName, envVars)) {
            matches = true
            break
        }
    }
    matches
}

def readOutput = { String cmd ->
    String output = ''
    File file = File.createTempFile('cleverdata_jenkins', null)
    try {
        sh "${cmd} > '${file.absolutePath}'"
        output = readFile(file.absolutePath).trim()
    } finally {
        file.delete()
    }
    output
}

def releaseStage = { String name ->
    if (releaseBranch || !skipEmptyStages) {
        stage name
    }
}

/***********************************************************************************************************************
*                                      STAGES                                                                          *
***********************************************************************************************************************/
def stageCheckout = {
    stage 'Checkout'
    checkout([
        $class: 'GitSCM',
        branches: scm.branches,
        browser: [
            $class: 'BitbucketWeb',
            repoUrl: "https://bitbucket.org/${teamName}/${repoName}"
        ],
        doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
        extensions: scm.extensions + [
            [$class: 'CleanBeforeCheckout'],
            // to prevent working on detached HEAD and
            // to be able to detect branch name by means of 'git rev-parse --abbrev-ref HEAD
            [$class: 'LocalBranch'],
            [$class: 'PruneStaleBranch']
        ],
        submoduleCfg: [],
        userRemoteConfigs: scm.userRemoteConfigs,
        poll: false
    ])

    // SCM-specific variables such as SVN_REVISION are currently unavailable inside a Pipeline script
    // (by means of "env" variable)
    // https://github.com/jenkinsci/pipeline-examples/blob/master/pipeline-examples/gitcommit/gitcommit.groovy
    gitBranchRef = readOutput('git symbolic-ref HEAD')
    gitBranchShortName = readOutput('git symbolic-ref --short HEAD')
    if (gitBranchName == null || gitBranchShortName.equals(gitBranchName)) { // make branch name contain remote name
        gitBranchName = "${remoteName}/${gitBranchShortName}"
    }
    releaseBranch = isReleaseBranch()
}

def stageReleasePrepare = {
    releaseStage 'Release Prepare'
    if (releaseBranch) {
        sh './gradlew --console=plain --stacktrace dmpkitReleaseInit'
    }
}

def stageUnitTest = {
    stage 'Unit Test'
    sh './gradlew --console=plain --stacktrace test'
    step([$class: 'JUnitResultArchiver', testResults: '**/target/test-results/TEST-*.xml'])
}

def stageIntegrationTest = {
    stage 'Integration Test'
    sh './gradlew --console=plain --stacktrace itest'
    step([$class: 'JUnitResultArchiver', testResults: '**/target/itest-results/TEST-*.xml'])
}

def stageCodeQuality = {
    stage 'Code Quality'
    sh './gradlew --console=plain --stacktrace --exclude-task test --exclude-task itest check'
    step([
        $class: 'TasksPublisher',
        thresholdLimit: 'low',
        canRunOnFailed: false,
        usePreviousBuildAsReference: false,
        useStableBuildAsReference: false,
        useDeltaValues: false,
        shouldDetectModules: false,
        dontComputeNew: true,
        doNotResolveRelativePaths: false,
        ignoreCase: true,
        asRegexp: false,
        pattern: '**/*.gradle,**/*.groovy,**/*.java,**/*.properties,**/*.py,**/*.scala,**/*.sbt,**/*.sh,**/*.xml,**/*.yaml,**/*.yml',
        high: 'TODO, FIXME, XXX',
        normal: 'DEPRECATED',
        low: '',
        excludePattern: ''
    ])
    step([
        $class: 'hudson.plugins.jacoco.JacocoPublisher',
        execPattern: '**/jacoco/*.exec',
        classPattern: '**/target/classes/main',
        sourcePattern: '**/src/main/java',
        exclusionPattern: '**/*Test.class,**/*IT.class',
        minimumInstructionCoverage: '0',
        minimumBranchCoverage: '0',
        minimumComplexityCoverage: '0',
        minimumLineCoverage: '0',
        minimumMethodCoverage: '0',
        minimumClassCoverage: '0',
        maximumInstructionCoverage: '0',
        maximumBranchCoverage: '0',
        maximumComplexityCoverage: '0',
        maximumLineCoverage: '0',
        maximumMethodCoverage: '0',
        maximumClassCoverage: '0',
        changeBuildStatus: false,
    ])
    step([
        $class: 'AnalysisPublisher',
        dontComputeNew: true,
        canRunOnFailed: false,
        doNotResolveRelativePaths: true,
        usePreviousBuildAsReference: false,
        useStableBuildAsReference: false,
        useDeltaValues: false,
        thresholdLimit: 'low',
        shouldDetectModules: false,
        isDryDeactivated: false,
        isCheckStyleDeactivated: false,
        isFindBugsDeactivated: false,
        isPmdDeactivated: false,
        isOpenTasksDeactivated: false,
        isWarningsDeactivated: false,
    ])
}

def stageBuild = {
    stage 'Build'
    sh './gradlew --console=plain --stacktrace jar'
}

def stageReleasePerform = {
    releaseStage 'Release Perform'
    if (releaseBranch) {
        sh './gradlew --console=plain --stacktrace dmpkitReleaseFinish'
    }
}

def stageReleasePublish = {
    releaseStage 'Release Publish'
    if (releaseBranch) {
        sh './gradlew --console=plain --stacktrace publish'
    }
}

def stageReleaseCleanup = {
    releaseStage 'Release Cleanup'
    if (releaseBranch) {
        sh './gradlew --console=plain --stacktrace dmpkitReleaseCleanup'
    }
}

def buildCompleted = {
    step([
        $class: 'hudson.plugins.chucknorris.CordellWalkerRecorder'
    ])

    // Send e-mail notifications for failed or unstable builds.
    // currentBuild.result must be non-null for this step to work.
    step([
        $class: 'Mailer',
        notifyEveryUnstableBuild: true,
        recipients: emailextrecipients([
            [$class: 'CulpritsRecipientProvider'],
            [$class: 'RequesterRecipientProvider'],
            [$class: 'DevelopersRecipientProvider']
        ])
    ])
}

/***********************************************************************************************************************
*                                      PIPELINES                                                                       *
***********************************************************************************************************************/
def pipeline = {
    stageCheckout()
    withEnv(["BRANCH_NAME=${gitBranchShortName}", "GIT_BRANCH=${gitBranchShortName}"]) {
        try {
            stageReleasePrepare()
            stageUnitTest()
            stageIntegrationTest()
            stageCodeQuality()
            stageBuild()
            stageReleasePerform()
            stageReleasePublish()
        } finally {
            stageReleaseCleanup()
        }
    }
}

/***********************************************************************************************************************
*                                      JOBS                                                                            *
***********************************************************************************************************************/
node('cleverdata && linux && java8') {
    withEnv(["JAVA_HOME=${tool name: '1.8', type: 'hudson.model.JDK'}"]) {
        try {
            pipeline()
        } catch (e) {
            error = e
            currentBuild.result = 'FAILURE'
        } finally {
            buildCompleted()
            if (error) {
                throw error // must re-throw exception to propagate error
            }
        }
    }
}
