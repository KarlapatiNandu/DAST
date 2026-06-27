// =============================================================================
// Jenkinsfile - DAST Learning Lab CI/CD workflow for Windows.
// =============================================================================
// This file teaches the full pipeline from build to DAST scan.
// Jenkins reads this file and runs each stage in order.
// The workflow now uses ZAP Automation Framework instead of quickurl scans.
// The ZAP plan lives in zap/zap-automation.yaml.
// The ZAP plan imports /v3/api-docs so new Spring endpoints are scanned automatically.

pipeline {
    // Run on any Jenkins agent available to this single-machine Windows lab.
    agent any

    // Keep all repeated paths and URLs in one place for easier learning and editing.
    environment {
        // ZAP_PATH comes from Jenkins global environment variables configured in Stage 3.
        ZAP_PATH = "${env.ZAP_PATH}"

        // BACKEND_URL is the base URL of the Spring Boot app started by this pipeline.
        BACKEND_URL = 'http://localhost:8080'

        // OPENAPI_URL is the Springdoc JSON specification that ZAP imports.
        OPENAPI_URL = 'http://localhost:8080/v3/api-docs'

        // ZAP_PORT is reserved for ZAP if it needs to expose its local proxy/API.
        ZAP_PORT = '8090'

        // REPORT_DIR is where the ZAP Automation Framework report is written.
        REPORT_DIR = "${env.WORKSPACE}\\zap-reports"

        // ZAP_PLAN is the YAML workflow consumed by zap.bat -autorun.
        ZAP_PLAN = "${env.WORKSPACE}\\zap\\zap-automation.yaml"
    }

    // Stages are the visible boxes in Jenkins Blue Ocean / classic stage view.
    stages {
        // Copy the local project into Jenkins workspace when using "Pipeline script" mode.
        stage('Checkout') {
            steps {
                // Print a clear stage marker for beginners reading Console Output.
                echo 'Stage 1: copying project files into the Jenkins workspace.'

                // Create an xcopy exclude list so generated folders are not copied.
                bat '''
@echo off
REM This file tells xcopy which generated folders to skip.
(
  echo \\.git\\
  echo \\.gradle\\
  echo \\build\\
  echo \\node_modules\\
  echo \\target\\
) > "%WORKSPACE%\\xcopy_excludes.txt"
'''

                // Copy source files from the project folder to Jenkins workspace.
                bat '''
@echo off
REM /E copies subfolders, /I assumes the target is a folder, /H includes hidden files.
REM /Y overwrites old files, /Q keeps output shorter for readable Jenkins logs.
xcopy "C:\\Users\\nandu\\OneDrive\\Desktop\\DAST\\*" "%WORKSPACE%\\" /E /I /H /Y /Q /EXCLUDE:%WORKSPACE%\\xcopy_excludes.txt
echo Checkout complete.
'''
            }
        }

        // Compile and package the Spring Boot backend with the Gradle wrapper.
        stage('Build Backend') {
            steps {
                // Explain why this stage exists before running the command.
                echo 'Stage 2: building the backend JAR with Gradle.'

                // dir('backend') is the Jenkins-safe equivalent of cd backend.
                dir('backend') {
                    // gradlew.bat keeps Jenkins independent from a globally installed Gradle.
                    bat '.\\gradlew.bat build -x test'
                }
            }
        }

        // Run tests separately so Jenkins can publish a test report.
        stage('Test Backend') {
            steps {
                // Explain that test failures should stop the pipeline.
                echo 'Stage 3: running backend tests and publishing JUnit XML.'

                // Run the Gradle test task inside the backend folder.
                dir('backend') {
                    // The test task writes XML files under build/test-results/test.
                    bat '.\\gradlew.bat test'
                }
            }

            // Stage-level post runs even when tests fail.
            post {
                // Always publish whatever JUnit XML files Gradle produced.
                always {
                    // Jenkins reads these XML files to create the Test Results tab.
                    junit allowEmptyResults: true, testResults: 'backend/build/test-results/test/*.xml'
                }
            }
        }

        // Start the packaged Spring Boot app so ZAP has a live target.
        stage('Start Backend') {
            steps {
                // Explain that DAST requires a running application, not just source code.
                echo 'Stage 4: starting Spring Boot on port 8080.'

                // Start Java in the background and wait until /v3/api-docs responds.
                bat '''
@echo off
REM The JAR was created by the Build Backend stage.
set "APP_JAR=%WORKSPACE%\\backend\\build\\libs\\search-backend-0.0.1-SNAPSHOT.jar"

REM Fail early if the expected JAR is missing.
if not exist "%APP_JAR%" (
  echo ERROR: Backend JAR not found at %APP_JAR%.
  exit /b 1
)

REM Start the app in the background and write logs to backend_log.txt.
start "DAST Backend" /b cmd /c "java -jar ""%APP_JAR%"" > ""%WORKSPACE%\\backend_log.txt"" 2>&1"

REM Wait up to 60 seconds for Springdoc's OpenAPI endpoint to become ready.
for /L %%i in (1,1,12) do (
  curl -f -s "%OPENAPI_URL%" > nul 2>&1
  if not errorlevel 1 (
    echo Backend is ready and OpenAPI is available.
    exit /b 0
  )
  echo Waiting for backend startup... attempt %%i of 12.
  ping -n 6 127.0.0.1 > nul
)

REM If the loop finishes, the app did not become ready in time.
echo ERROR: Backend did not expose %OPENAPI_URL% within 60 seconds.
type "%WORKSPACE%\\backend_log.txt"
exit /b 1
'''
            }
        }

        // Run the ZAP Automation Framework plan.
        stage('ZAP Automation Scan') {
            steps {
                // Explain the new workflow before running it.
                echo 'Stage 5: running OpenAPI-driven ZAP Automation Framework scan.'

                // Ensure the report folder exists before ZAP tries to write into it.
                bat '''
@echo off
REM Create the report directory used by zap/zap-automation.yaml.
if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%"

REM Confirm the ZAP plan was copied into the Jenkins workspace.
if not exist "%ZAP_PLAN%" (
  echo ERROR: ZAP Automation plan not found at %ZAP_PLAN%.
  exit /b 1
)

REM Confirm the ZAP JAR exists before trying to scan.
REM We call java -jar directly (bypassing zap.bat) because zap.bat uses
REM a relative path to the JAR that breaks when run from the Jenkins workspace.
if not exist "%ZAP_PATH%\\zap-2.17.0.jar" (
  echo ERROR: ZAP JAR not found at %ZAP_PATH%\\zap-2.17.0.jar.
  exit /b 1
)

REM Remove any old report so a failed ZAP run cannot pass by finding stale output.
if exist "%REPORT_DIR%\\zap-automation-report.html" del /f /q "%REPORT_DIR%\\zap-automation-report.html"

REM Run ZAP headlessly with the Automation Framework plan.
REM -cmd means no GUI, -addonupdate updates ZAP rules, -autorun executes YAML.
REM Call java -jar directly with the absolute path to the ZAP JAR.
REM zap.bat uses a relative path (java -jar zap-2.17.0.jar) which only works
REM when the working directory is the ZAP folder. Jenkins runs bat() from the
REM workspace, so we bypass zap.bat and invoke the JAR ourselves.
java -Xmx512m -jar "%ZAP_PATH%\\zap-2.17.0.jar" -cmd -addonupdate -autorun "%ZAP_PLAN%"

REM Store ZAP's exit code so we can decide whether to fail the stage.
set ZAP_EXIT=%ERRORLEVEL%
echo ZAP exited with code %ZAP_EXIT%.

REM A successful ZAP run must create the expected report.
if not exist "%REPORT_DIR%\\zap-automation-report.html" (
  echo ERROR: ZAP did not create %REPORT_DIR%\\zap-automation-report.html.
  exit /b 1
)

REM ZAP may return 1 when findings cross a warning threshold; in this lab, findings are expected.
if %ZAP_EXIT% LEQ 1 exit /b 0

REM Exit codes above 1 usually mean ZAP itself failed to run.
exit /b %ZAP_EXIT%
'''
            }
        }

        // Publish the generated ZAP report inside Jenkins.
        stage('Publish ZAP Report') {
            steps {
                // Explain why HTML Publisher is needed.
                echo 'Stage 6: publishing the ZAP Automation HTML report.'

                // Jenkins CSP can block the CSS/JS inside generated HTML reports.
                script {
                    // Empty CSP allows the local ZAP report to render correctly in Jenkins.
                    System.setProperty('hudson.model.DirectoryBrowserSupport.CSP', '')
                }

                // HTML Publisher creates a clickable report tab on the build page.
                publishHTML(target: [
                    // The report directory is relative to the Jenkins workspace.
                    reportDir: 'zap-reports',
                    // The YAML report job writes this exact file name.
                    reportFiles: 'zap-automation-report.html',
                    // This is the visible label in Jenkins.
                    reportName: 'ZAP Automation Report',
                    // Keep reports from older builds for comparison.
                    keepAll: true,
                    // Do not fail publishing if ZAP failed before creating the report.
                    allowMissing: true,
                    // The sidebar link should point to the newest build.
                    alwaysLinkToLastBuild: true
                ])
            }
        }

        // Stop the backend so the next pipeline run can reuse port 8080.
        stage('Stop Backend') {
            steps {
                // Explain that cleanup prevents "port already in use" errors.
                echo 'Stage 7: stopping the backend process on port 8080.'

                // Find the PID listening on port 8080 and force-kill it.
                bat '''
@echo off
REM netstat lists connections, findstr filters port 8080 and LISTENING rows.
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
  echo Killing backend process with PID %%p.
  taskkill /PID %%p /F 2>nul
)
exit /b 0
'''
            }
        }
    }

    // Post actions are the safety net for failed builds.
    post {
        // Always run cleanup and archive useful output.
        always {
            // Kill the backend again in case an earlier stage failed before Stop Backend.
            bat '''
@echo off
REM Safety cleanup: ignore errors because the backend may already be stopped.
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING 2^>nul') do (
  echo Cleanup killing leftover backend PID %%p.
  taskkill /PID %%p /F 2>nul
)
exit /b 0
'''

            // Save backend logs so startup errors can be inspected after the build.
            archiveArtifacts artifacts: 'backend_log.txt', allowEmptyArchive: true

            // Save all generated ZAP files as downloadable Jenkins artifacts.
            archiveArtifacts artifacts: 'zap-reports/**/*', allowEmptyArchive: true
        }

        // Print a short success message for beginners reading the final console lines.
        success {
            // The app built, tests ran, ZAP scanned, and the report was published.
            echo 'Pipeline completed successfully. Open the ZAP Automation Report tab for findings.'
        }

        // Print the requested failure message when any stage fails.
        failure {
            echo 'Pipeline Failed'
        }
    }
}
