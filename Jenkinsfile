/*
 * Declarative Jenkins pipeline for the sample-app Maven project in this
 * repository.
 *
 * Stages: Checkout -> Build -> Unit Tests -> Static Analysis -> Package
 *         -> Publish Report, with a post block for cleanup/notification
 *         regardless of outcome.
 *
 * Verified headlessly with jenkins/jenkinsfile-runner against plugins.txt
 * in this repo (see README.md for the exact command and CI wiring).
 */
pipeline {
    agent any

    // No `tools { maven ...; jdk ... }` block: that requires named tool
    // installations to be pre-configured on the Jenkins controller (Manage
    // Jenkins > Global Tool Configuration, or JCasC), which this
    // self-contained repo has no way to guarantee on an arbitrary agent.
    // Instead the sample app ships its own Maven Wrapper (sample-app/mvnw),
    // so every stage below builds with the exact pinned Maven version
    // regardless of what is (or isn't) installed on the agent - only a JDK
    // needs to be on PATH, which every Jenkins agent has by definition.

    options {
        // Keep a bounded build history and guard against a stuck stage
        // hanging the executor forever.
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 20, unit: 'MINUTES')
        timestamps()
    }

    parameters {
        booleanParam(
            name: 'SKIP_STATIC_ANALYSIS',
            defaultValue: false,
            description: 'Skip the Static Analysis (Checkstyle) stage for this run'
        )
    }

    environment {
        APP_DIR   = 'sample-app'
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "Checking out ${env.JOB_NAME} #${env.BUILD_NUMBER}"
                checkout scm
            }
        }

        stage('Build') {
            steps {
                dir(env.APP_DIR) {
                    sh './mvnw -B -ntp clean compile'
                }
            }
        }

        stage('Unit Tests') {
            steps {
                dir(env.APP_DIR) {
                    sh './mvnw -B -ntp test'
                }
            }
            post {
                always {
                    junit "${env.APP_DIR}/target/surefire-reports/*.xml"
                }
            }
        }

        stage('Static Analysis') {
            when {
                expression { return !params.SKIP_STATIC_ANALYSIS }
            }
            steps {
                dir(env.APP_DIR) {
                    sh './mvnw -B -ntp checkstyle:check'
                }
            }
        }

        stage('Package') {
            steps {
                dir(env.APP_DIR) {
                    sh './mvnw -B -ntp package -DskipTests'
                }
                archiveArtifacts artifacts: "${env.APP_DIR}/target/*.jar", fingerprint: true
            }
        }

        stage('Publish Report') {
            steps {
                dir(env.APP_DIR) {
                    sh './mvnw -B -ntp surefire-report:report'
                    // maven-surefire-report-plugin writes to target/reports/
                    // or target/site/ depending on which site skin Maven
                    // resolves at build time (both are legitimate outcomes
                    // of the same goal on different environments) - copy
                    // whichever one exists to a fixed, predictable path so
                    // publishHTML below always finds it.
                    sh '''
                        mkdir -p target/surefire-html-report
                        if [ -f target/reports/surefire.html ]; then
                            cp target/reports/surefire.html target/surefire-html-report/index.html
                        elif [ -f target/site/surefire-report.html ]; then
                            cp target/site/surefire-report.html target/surefire-html-report/index.html
                        else
                            echo "No surefire HTML report found under target/reports or target/site" >&2
                            exit 1
                        fi
                    '''
                }
            }
            post {
                always {
                    publishHTML(target: [
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: "${env.APP_DIR}/target/surefire-html-report",
                        reportFiles: 'index.html',
                        reportName: 'Surefire HTML Report'
                    ])
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline finished with status: ${currentBuild.currentResult}"
        }
        success {
            echo 'Build, tests, static analysis, and packaging all succeeded.'
        }
        failure {
            echo 'Pipeline failed - see the stage logs and JUnit/Checkstyle reports above.'
        }
        cleanup {
            cleanWs()
        }
    }
}
