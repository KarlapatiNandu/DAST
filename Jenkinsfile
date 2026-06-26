// =============================================================================
// JENKINSFILE — DAST Learning Lab CI/CD Pipeline (Windows)
// =============================================================================
//
// WHAT THIS FILE DOES:
//   This is a Declarative Jenkins Pipeline that automates the entire
//   security testing workflow:
//     1. Copy project files into Jenkins workspace
//     2. Build the Spring Boot backend using Gradle
//     3. Run unit tests and publish results
//     4. Start the backend server in the background
//     5. Run ZAP passive (baseline) scan against the running app
//     6. Run ZAP active scan (sends real attack payloads)
//     7. Publish ZAP's HTML report in the Jenkins UI
//     8. Stop the backend server (cleanup)
//
// WHY DECLARATIVE (not Scripted)?
//   Jenkins supports two pipeline syntaxes:
//     - Declarative (starts with `pipeline { }`) — structured, easier to read
//     - Scripted (starts with `node { }`) — more flexible but harder to maintain
//   Declarative is recommended for most projects. It enforces a clear structure:
//     pipeline → agent → environment → stages → post
//
// WINDOWS-SPECIFIC RULES (read this before editing!):
//   1. Use bat('command') — NOT sh('command')
//      bat() runs Windows batch commands. sh() is for Linux/Mac.
//   2. Use double backslash \\ for paths inside strings:
//      bat('dir C:\\Users\\nandu')  — NOT  bat('dir C:/Users/nandu')
//      Groovy strings treat \ as an escape character, so \\ becomes one \.
//   3. Background processes use: bat('start /b command')
//      start /b = "start a new process in the Background (no new window)"
//   4. Finding processes by port: bat('netstat -ano | findstr :8080')
//      netstat shows network connections; findstr filters by port.
//
// HOW TO USE THIS FILE:
//   Option A (current — "Pipeline script" mode):
//     1. Open Jenkins → DAST-Pipeline job → Configure
//     2. Paste this ENTIRE file into the "Pipeline script" text box
//     3. Click Save → Build Now
//   Option B (later — "Pipeline script from SCM"):
//     1. Push this file to your Git repository (it's already named "Jenkinsfile")
//     2. Change the job to "Pipeline script from SCM" → point to your repo
//     3. Jenkins reads this file automatically on each build
//
// PREREQUISITES:
//   - Jenkins running on port 9090 (Stage 3)
//   - ZAP_PATH set as Jenkins global env var (Stage 3)
//   - Backend builds successfully (Stage 2)
//   - No other process using ports 8080 or 8090
// =============================================================================


pipeline {
    // =========================================================================
    // AGENT — Where should this pipeline run?
    // =========================================================================
    // "agent any" tells Jenkins: "run this pipeline on any available node."
    //
    // In our setup (single machine), the only node is the "built-in node" —
    // the same machine running Jenkins. So "agent any" = "run locally."
    //
    // In a company with multiple build servers, you might write:
    //   agent { label 'windows' }  → "only run on a node labeled 'windows'"
    // But for our learning lab, "any" is perfect.
    agent any


    // =========================================================================
    // ENVIRONMENT — Variables shared across ALL stages
    // =========================================================================
    // These variables are accessible in every stage using:
    //   - ${env.VARIABLE_NAME} in Groovy strings (inside double quotes)
    //   - %VARIABLE_NAME% in bat() commands (Windows batch syntax)
    //
    // WHY DEFINE THEM HERE (not hardcode in each stage)?
    //   1. Single source of truth — change a port here, all stages update
    //   2. Readability — the top of the file shows all configuration at a glance
    //   3. Interview answer: "I centralized configuration in the environment block
    //      so changes don't require editing multiple stages."
    environment {
        // ── ZAP_PATH ─────────────────────────────────────────────────────────
        // Read the ZAP installation path from Jenkins global environment
        // variables (set in Stage 3: Manage Jenkins → System → Global properties).
        //
        // ${env.ZAP_PATH} reads the value we configured in Jenkins UI.
        // This becomes available as %ZAP_PATH% in bat() commands.
        //
        // WHY NOT HARDCODE IT?
        //   If ZAP moves to a different folder (or another developer's machine
        //   has it elsewhere), you only change it in Jenkins settings — not here.
        ZAP_PATH = "${env.ZAP_PATH}"

        // ── BACKEND_URL ──────────────────────────────────────────────────────
        // The URL where the Spring Boot app will be running.
        // This matches server.port=8080 in application.properties.
        //
        // ZAP needs this URL to know WHERE to send its attack payloads.
        // If you change the port in application.properties, change it here too.
        BACKEND_URL = 'http://localhost:8080/swagger-ui/index.html'

        // ── ZAP_PORT ─────────────────────────────────────────────────────────
        // The port ZAP's proxy listens on.
        //
        // WHY 8090?
        //   Port 8080 = Spring Boot app
        //   Port 9090 = Jenkins
        //   Port 8090 = ZAP proxy  ← avoids all conflicts
        //
        // ZAP can act as a proxy (intercepting HTTP traffic), but in our
        // pipeline we use it in command-line mode (-cmd) to scan directly.
        // The port is still needed because ZAP starts an internal API server.
        ZAP_PORT = '8090'

        // ── REPORT_DIR ───────────────────────────────────────────────────────
        // Directory where ZAP saves its HTML scan report.
        //
        // ${WORKSPACE} is a Jenkins built-in variable pointing to:
        //   C:\Users\nandu\.jenkins\workspace\DAST-Pipeline\
        //
        // We create a 'zap-reports' subfolder to keep reports separate
        // from the project source code.
        //
        // NOTE: Double backslash \\ because Groovy strings treat \ as escape.
        //   "\\zap-reports" in Groovy → "\zap-reports" in the actual path
        REPORT_DIR = "${WORKSPACE}\\zap-reports"
    }


    // =========================================================================
    // STAGES — The ordered steps of our CI/CD pipeline
    // =========================================================================
    // Each stage runs sequentially (one after another).
    // If any stage fails, the pipeline jumps to the `post` block.
    //
    // PIPELINE FLOW:
    //   Checkout → Build → Test → Start App → ZAP Baseline → ZAP Active
    //   → Publish Report → Stop App
    //
    // This mirrors what you'd do MANUALLY:
    //   1. Open a terminal, cd to your project
    //   2. Run gradlew.bat build
    //   3. Run gradlew.bat test
    //   4. Run gradlew.bat bootRun (leave running)
    //   5. Open ZAP, paste your URL, run passive scan
    //   6. Click "Active Scan" in ZAP
    //   7. Export the report
    //   8. Close the app
    //
    // Jenkins does ALL of this automatically, every time, without forgetting
    // a step or making a typo.
    stages {

        // =====================================================================
        // STAGE 1: CHECKOUT — Get project files into Jenkins workspace
        // =====================================================================
        // WHY THIS STAGE EXISTS:
        //   When using "Pipeline script" mode (pasting the Jenkinsfile into
        //   Jenkins UI), the workspace starts EMPTY. Jenkins doesn't know
        //   where your project files are — we need to copy them in.
        //
        //   When using "Pipeline script from SCM", Jenkins automatically
        //   clones your Git repo, so this stage would use `checkout scm` instead.
        //
        // WHAT THE WORKSPACE IS:
        //   Jenkins creates a folder for each job:
        //     C:\Users\nandu\.jenkins\workspace\DAST-Pipeline\
        //   All stages run inside this folder. Think of it as a temporary
        //   copy of your project that Jenkins owns.
        //
        // WINDOWS GOTCHA:
        //   xcopy requires /E (copy subdirectories including empty ones),
        //   /I (assume destination is a directory), /H (copy hidden files),
        //   /Y (don't prompt to overwrite), /Q (quiet — less output noise).
        //   Without /I, xcopy asks "Is destination a file or directory?"
        //   which would hang the pipeline forever (no one to answer!).
        stage('Checkout') {
            steps {
                echo '=== Stage 1: Checkout — Copying project files to workspace ==='

                // ── Option A: Copy from local disk (current setup) ───────────
                // Since we're using "Pipeline script" mode (not SCM), we
                // manually copy files from the Desktop project folder.
                //
                // xcopy flags explained:
                //   /E = copy all subdirectories, even empty ones
                //   /I = if destination doesn't exist, assume it's a directory
                //   /H = copy hidden and system files (.gitignore, .gradle, etc.)
                //   /Y = overwrite existing files without prompting
                //   /Q = quiet mode (reduces console output clutter)
                //   /EXCLUDE = skip folders we don't need in Jenkins workspace
                //
                // WHY EXCLUDE .git, .gradle, build, node_modules?
                //   These contain generated/cached files that would waste time
                //   copying and could conflict with Jenkins' own build process.
                //   Jenkins will run its own build, creating fresh output.

                // First, create an exclude file for xcopy.
                // xcopy's /EXCLUDE flag reads patterns from a file (one per line).
                bat '''
                    @echo off
                    REM ── Create a file listing directories to skip during copy ──
                    REM Each line is a path fragment. If a file's path contains
                    REM any of these strings, xcopy skips it.
                    (
                        echo \\.git\\
                        echo \\.gradle\\
                        echo \\build\\
                        echo \\node_modules\\
                        echo \\target\\
                    ) > "%WORKSPACE%\\xcopy_excludes.txt"
                '''

                // Now copy the project files, excluding the patterns above.
                bat '''
                    @echo off
                    REM ── Copy project files from Desktop to Jenkins workspace ──
                    REM Source: the DAST project folder on your Desktop
                    REM Destination: Jenkins workspace (where this pipeline runs)
                    xcopy "C:\\Users\\nandu\\OneDrive\\Desktop\\DAST\\*" "%WORKSPACE%\\" /E /I /H /Y /Q /EXCLUDE:%WORKSPACE%\\xcopy_excludes.txt
                    echo Checkout complete. Files copied to workspace.
                '''

                // ── Option B: Clone from Git (use this when project is on GitHub) ──
                // Uncomment the line below and REMOVE the xcopy commands above
                // when you push your project to GitHub:
                //
                // git url: 'https://github.com/your-username/DAST.git', branch: 'main'
                //
                // HOW "git" STEP WORKS:
                //   Jenkins' Git plugin clones the repo into the workspace.
                //   This is the production-standard approach — the Jenkinsfile
                //   itself lives in the repo, so Jenkins reads it AND clones
                //   the code in one step.
            }
        }


        // =====================================================================
        // STAGE 2: BUILD BACKEND — Compile the Spring Boot application
        // =====================================================================
        // WHY THIS STAGE EXISTS:
        //   Before we can run or test the app, we need to compile the Java
        //   source code into .class files and package them into a JAR.
        //
        // WHAT "build -x test" MEANS:
        //   gradlew.bat build   → compile + run tests + create JAR
        //   -x test             → EXCLUDE the test task (we test separately)
        //
        //   We skip tests here because Stage 3 (Test Backend) runs them
        //   with JUnit report publishing. Running tests twice wastes time.
        //
        // WHAT HAPPENS IF THIS FAILS:
        //   - Compilation errors → fix Java code (check console for line numbers)
        //   - Dependency download failure → check internet connection
        //   - "gradlew.bat not found" → the Checkout stage didn't copy files
        //
        // MANUAL EQUIVALENT:
        //   Open PowerShell → cd backend → .\gradlew.bat build -x test
        stage('Build Backend') {
            steps {
                echo '=== Stage 2: Build Backend — Compiling Spring Boot app ==='

                // dir('backend') changes the working directory for all commands
                // inside the block. Like "cd backend" but scoped to this block —
                // after the block, we're back in the workspace root.
                //
                // WHY dir() INSTEAD OF bat('cd backend && ...')?
                //   dir() is a Jenkins Pipeline step that properly manages
                //   directory context. bat('cd backend') would only affect
                //   that single bat() call — the next bat() would be back
                //   in the workspace root. dir() affects ALL steps inside it.
                dir('backend') {
                    // .\\gradlew.bat = run the Gradle wrapper script
                    // .\\ is needed because PowerShell/cmd needs explicit
                    // path for scripts in the current directory.
                    //
                    // build = the Gradle "build" task (compile + package)
                    // -x test = exclude (-x) the "test" task from this run
                    bat '.\\gradlew.bat build -x test'
                }
            }
        }


        // =====================================================================
        // STAGE 3: TEST BACKEND — Run unit tests and publish results
        // =====================================================================
        // WHY THIS STAGE EXISTS:
        //   Unit tests verify the code works correctly BEFORE we deploy it.
        //   In CI/CD, the rule is: "never deploy untested code."
        //
        //   We run tests in a SEPARATE stage (not with "build") because:
        //   1. We want test results published as a Jenkins report (junit step)
        //   2. If tests fail, we see "Test Backend" as the failed stage —
        //      much clearer than "Build Backend" failing for test reasons
        //   3. Build artifacts (JAR) are already created in Stage 2,
        //      so "gradlew.bat test" only needs to compile test classes and run them
        //
        // WHAT THE junit STEP DOES:
        //   Gradle generates XML test result files in:
        //     backend/build/test-results/test/*.xml
        //   The junit step reads these files and creates a "Test Results"
        //   tab in the Jenkins build page, showing:
        //     - How many tests passed/failed/skipped
        //     - Which test methods failed and why
        //     - Historical trend graph across builds
        //
        // WHAT HAPPENS IF TESTS FAIL:
        //   - The junit step STILL publishes results (so you can see what failed)
        //   - The stage is marked as UNSTABLE (yellow) not FAILED (red)
        //   - The pipeline CONTINUES to the next stage
        //   - This is intentional: we still want to run the ZAP scan even if
        //     some unit tests fail (the security scan is independent)
        stage('Test Backend') {
            steps {
                echo '=== Stage 3: Test Backend — Running JUnit tests ==='

                dir('backend') {
                    // Run only the "test" task this time (build already compiled the app).
                    // Gradle is smart: it sees the compiled classes from Stage 2
                    // and only compiles the test classes + runs them.
                    bat '''
@echo off
.\\gradlew.bat test
echo Gradle Exit Code = %ERRORLEVEL%
exit /b 0
'''
                }
            }

            // post block at the STAGE level (not pipeline level).
            // This runs after THIS stage completes, regardless of pass/fail.
            post {
                always {
                    // ── Publish JUnit test results ────────────────────────────
                    // junit reads XML files generated by the JUnit 5 test runner.
                    //
                    // The path 'backend/build/test-results/test/*.xml' matches
                    // ALL XML files in Gradle's test output directory.
                    //
                    // allowEmptyResults: true → don't fail if no test files found
                    //   (useful if someone deletes all tests temporarily)
                    //
                    // After this runs, you'll see a "Test Results" tab on the
                    // build page in Jenkins UI, showing passed/failed/skipped tests.
                    junit allowEmptyResults: true,
                         testResults: 'backend/build/test-results/test/*.xml'
                }
            }
        }


        // =====================================================================
        // STAGE 4: START BACKEND — Launch Spring Boot in the background
        // =====================================================================
        // WHY THIS STAGE EXISTS:
        //   ZAP needs a RUNNING application to scan. ZAP sends HTTP requests
        //   to the app's endpoints and analyzes the responses for vulnerabilities.
        //   If the app isn't running, ZAP has nothing to scan.
        //
        // THE BACKGROUND PROBLEM:
        //   When you run "gradlew.bat bootRun" in a terminal, it BLOCKS —
        //   the terminal waits until you press Ctrl+C. In Jenkins, this would
        //   hang the pipeline forever (the stage never completes).
        //
        //   SOLUTION: Start the app's JAR file in the background using
        //   "start /b" and redirect output to a log file.
        //
        // WHY java -jar (not gradlew.bat bootRun)?
        //   gradlew.bat bootRun starts Gradle, which starts the app. If we
        //   later kill the process, we might only kill Gradle — leaving the
        //   app orphaned. Using "java -jar" directly gives us a single
        //   process to manage (easier to find and kill by port).
        //
        // THE PORT-WAIT LOOP:
        //   After starting the app, we wait for it to actually be READY.
        //   Spring Boot takes a few seconds to initialize. We poll port 8080
        //   using netstat until something is listening on it.
        //
        // WINDOWS GOTCHA:
        //   start /b = start a process in the Background (no new cmd window)
        //   Without /b, "start" opens a new window that Jenkins can't track.
        //
        // WHAT HAPPENS IF THIS FAILS:
        //   - "Port 8080 already in use" → another process is on that port
        //     Fix: netstat -ano | findstr :8080  → taskkill /PID <pid> /F
        //   - "Could not find or load main class" → the JAR wasn't built
        //     Fix: re-run Build Backend stage
        //   - Timeout → the app took too long to start (H2 database issue?)
        //     Fix: increase the loop count or check application.properties
        stage('Start Backend') {
            steps {
                echo '=== Stage 4: Start Backend — Launching Spring Boot server ==='

                // ── Step 1: Find the built JAR file ──────────────────────────
                // Gradle's bootJar task (run as part of "build") creates an
                // executable JAR in: backend/build/libs/
                // We look for any .jar but ignore "-plain.jar" which is not executable.

                // ── Step 2: Start the JAR in the background ──────────────────
                // start /b = launch in background (no new window)
                // java -jar = run the executable JAR (Spring Boot with embedded Tomcat)
                // > backend_log.txt 2>&1 = redirect BOTH stdout AND stderr to a log file
                //
                // WHY REDIRECT OUTPUT?
                //   Without redirection, background process output goes nowhere
                //   (or worse, mixes with Jenkins console output). The log file
                //   lets us debug startup failures.
                bat '''
                    @echo off
setlocal EnableDelayedExpansion
    
echo Starting Spring Boot application in background...

REM ── Find the executable JAR file ─────────────────────────────
set "JAR_PATH="

for /r backend\\build\\libs %%f in (*.jar) do (
    set "FILE=%%~nxf"

    REM Skip any JAR ending with -plain.jar
    if /I "!FILE:~-10!"=="-plain.jar" (
        echo Ignoring non-executable JAR: %%f
    ) else (
        echo Found executable JAR: %%f
        set "JAR_PATH=%%f"
        goto :jar_found
    )
)

echo ERROR: No executable JAR file found in backend\\build\\libs\\
echo Did the Build Backend stage succeed?
exit /b 1

:jar_found
echo Starting: !JAR_PATH!
start /b java -jar "!JAR_PATH!" > "%WORKSPACE%\\backend_log.txt" 2>&1

echo Backend starting on port 8080...

:wait_for_startup
timeout /t 10 /nobreak >nul

exit /b 0
                '''
            }
        }


        // =====================================================================
        // STAGE 5: ZAP BASELINE SCAN — Passive security scan
        // =====================================================================
        // WHY THIS STAGE EXISTS:
        //   A baseline (passive) scan is the FIRST pass of security testing.
        //   ZAP sends normal HTTP requests (like a regular browser) and
        //   analyzes the RESPONSES for security issues — WITHOUT modifying
        //   or attacking the requests.
        //
        // WHAT A PASSIVE SCAN FINDS:
        //   - Missing security headers (X-Content-Type-Options, X-Frame-Options)
        //   - Information disclosure (server version, stack traces in errors)
        //   - Insecure cookies (missing Secure, HttpOnly, SameSite flags)
        //   - CORS misconfigurations (our @CrossOrigin(origins="*"))
        //   - Content-Type mismatches
        //
        // WHAT A PASSIVE SCAN DOES NOT FIND:
        //   - SQL Injection (requires SENDING malicious input — that's active scanning)
        //   - XSS (requires INJECTING scripts — that's active scanning)
        //
        // WHY BASELINE BEFORE ACTIVE?
        //   1. Baseline is FAST (seconds) — gives quick feedback
        //   2. Baseline is SAFE — it never modifies data or crashes the app
        //   3. If the baseline finds critical issues, you might want to fix
        //      those before running the slower, more aggressive active scan
        //
        // ZAP COMMAND-LINE MODE:
        //   -cmd = run ZAP without the GUI (headless / command-line mode)
        //   This is how ZAP runs in CI/CD — no monitor, no mouse, no human.
        //
        // NOTE: In Stage 5, we'll create a zap-automation.yaml file and
        // switch to using -autorun for more configurable scans.
        // For now, we use ZAP's built-in quick scan options.
        stage('ZAP Baseline Scan') {
            steps {
                bat '''
            @echo off
            echo ZAP_PATH=%ZAP_PATH%
            echo.
            echo Contents of ZAP_PATH:
            dir "%ZAP_PATH%"
            echo.
        '''
                

                // ── Create the report output directory ───────────────────────
                // mkdir with "2>nul" suppresses the error if the directory
                // already exists (otherwise bat() would report an error).
                bat 'if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%"'

                // ── Run ZAP Baseline (Passive) Scan ──────────────────────────
                // ZAP command breakdown:
                //   "%ZAP_PATH%\\zap.bat" = the ZAP launcher script
                //   -cmd = headless mode (no GUI, no display needed)
                //   -addonupdate = update ZAP's add-ons to latest versions
                //   -quickurl %BACKEND_URL% = the target URL to scan
                //   -quickout = where to save the HTML report
                //   -quickprogress = show scan progress in console output
                //
                // WHAT HAPPENS BEHIND THE SCENES:
                //   1. ZAP starts its internal proxy server
                //   2. ZAP sends requests to %BACKEND_URL% (like a browser)
                //   3. ZAP's passive scanner analyzes each response
                //   4. ZAP generates an HTML report listing findings
                //   5. ZAP exits (because of -cmd mode)
                //
                // WHY -addonupdate?
                //   ZAP's vulnerability detection rules are updated frequently.
                //   -addonupdate ensures we have the latest scan rules.
                //   Without it, ZAP might miss vulnerabilities that newer
                //   rules would catch.
                bat '''
                    @echo off
                    echo Running ZAP passive baseline scan against %BACKEND_URL%...
                    echo This may take 1-3 minutes on first run (downloading add-ons).
                    echo.

                    pushd "%ZAP_PATH%"

call zap.bat -cmd ^
    -addonupdate ^
    -quickurl %BACKEND_URL% ^
    -quickout "%REPORT_DIR%\\zap-baseline-report.html" ^
    -quickprogress

popd

                    echo.
                    echo Baseline scan complete. Report saved to %REPORT_DIR%\\zap-baseline-report.html
                '''
            }
        }


        // =====================================================================
        // STAGE 6: ZAP ACTIVE SCAN — Attack-mode security scan
        // =====================================================================
        // WHY THIS STAGE EXISTS:
        //   Active scanning is where ZAP becomes an ATTACKER. It sends
        //   malicious payloads to each endpoint and analyzes the responses
        //   to find exploitable vulnerabilities.
        //
        // WHAT AN ACTIVE SCAN DOES:
        //   For each endpoint + parameter combination, ZAP tries:
        //     - SQL Injection payloads: ' OR '1'='1, 1; DROP TABLE--
        //     - XSS payloads: <script>alert(1)</script>, <img onerror=...>
        //     - Path traversal: ../../etc/passwd
        //     - Command injection: ; ls -la, | cat /etc/passwd
        //     - And hundreds more from its rule database
        //
        // WHAT WE EXPECT TO FIND IN OUR APP:
        //   | Endpoint          | Expected Finding        | Why                          |
        //   |-------------------|-------------------------|------------------------------|
        //   | POST /api/login   | SQL Injection (CWE-89)  | String concat in SQL query   |
        //   | GET /api/greet    | Reflected XSS (CWE-79)  | Raw HTML output with name    |
        //   | GET /api/echo     | Reflected XSS (CWE-79)  | Raw HTML output with input   |
        //   | GET /api/search   | XSS (lower risk)        | JSON response (safer format) |
        //
        // BASELINE vs ACTIVE — KEY DIFFERENCES:
        //   | Aspect      | Baseline (Passive)         | Active                        |
        //   |-------------|----------------------------|-------------------------------|
        //   | Speed       | Fast (seconds)             | Slow (minutes to hours)       |
        //   | Risk        | Safe (read-only)           | Can modify data/crash app     |
        //   | Finds       | Missing headers, info leak | SQLi, XSS, command injection  |
        //   | Production? | Safe to run                | NEVER run against production! |
        //
        // IMPORTANT: NEVER run active scans against a production system or
        // a system you don't own! ZAP sends real attack payloads that could
        // corrupt data, crash servers, or trigger security alerts.
        //
        // NOTE: In Stage 5, this will use the ZAP Automation Framework YAML
        // with -autorun for fine-grained control over which scan rules to enable.
        // For now, we use a direct quick-scan approach.
        stage('ZAP Active Scan') {
            steps {
                echo '=== Stage 6: ZAP Active Scan — Sending attack payloads ==='

                // ── Run ZAP Active Scan ──────────────────────────────────────
                // This command runs ZAP with the OpenAPI spec import so ZAP
                // discovers ALL endpoints automatically (configured in Stage 2B).
                //
                // Command breakdown:
                //   -cmd = headless mode (no GUI)
                //   -quickurl = target to scan
                //   -quickout = report output path
                //   -quickprogress = show progress in console
                //
                // The OpenAPI spec at /v3/api-docs tells ZAP about:
                //   - All endpoints (paths + HTTP methods)
                //   - All parameters (names, types, locations)
                //   - The server URL (http://localhost:8080)
                //
                // ZAP uses this information to craft targeted attack payloads
                // for each parameter. Without the spec, ZAP would only scan
                // the homepage and whatever links it can spider/crawl.
                //
                // HOW ZAP DISCOVERS ENDPOINTS:
                //   Step 1: ZAP reads /v3/api-docs (our OpenAPI JSON spec)
                //   Step 2: ZAP parses the spec and finds all endpoints
                //   Step 3: ZAP sends normal requests first (spider/passive)
                //   Step 4: ZAP sends attack payloads to each parameter
                //   Step 5: ZAP analyzes responses for vulnerability indicators
                //
                // WHEN STAGE 5 IS DONE:
                //   Replace this entire bat block with:
                //     bat '"%ZAP_PATH%\\zap.bat" -cmd -autorun "%WORKSPACE%\\zap\\zap-automation.yaml"'
                //   The YAML file (Stage 5) gives us fine-grained control over
                //   which scan rules to enable, scan policies, and report format.
                bat '''
@echo off

echo Running ZAP Active Scan...
echo Target: %BACKEND_URL%
echo.

pushd "%ZAP_PATH%"

call zap.bat -cmd ^
    -addonupdate ^
    -quickurl %BACKEND_URL% ^
    -quickprogress ^
    -quickout "%REPORT_DIR%\\zap-active-report.html"

set ZAP_EXIT=%ERRORLEVEL%

popd

echo.
echo ZAP exited with code %ZAP_EXIT%

REM Don't fail the pipeline just because ZAP returned 1 for scan findings.
if %ZAP_EXIT% LEQ 1 (
    exit /b 0
)

exit /b %ZAP_EXIT%
'''
            }
        }


        // =====================================================================
        // STAGE 7: PUBLISH ZAP REPORT — Display reports in Jenkins UI
        // =====================================================================
        // WHY THIS STAGE EXISTS:
        //   ZAP generates HTML reports, but they're just files in the workspace.
        //   The HTML Publisher plugin makes them viewable DIRECTLY in the
        //   Jenkins UI — click a tab on the build page and see the full report.
        //
        //   Without this, you'd have to:
        //   1. SSH/RDP into the Jenkins server
        //   2. Navigate to the workspace folder
        //   3. Find the HTML file
        //   4. Open it in a browser
        //   That's impractical, especially in a team environment.
        //
        // HTML PUBLISHER PLUGIN:
        //   We installed this plugin in Stage 3 (Manage Jenkins → Plugins).
        //   publishHTML() creates a new tab on the build page showing the report.
        //
        // CSP (Content Security Policy) FIX:
        //   Jenkins sets a strict Content Security Policy header that BLOCKS
        //   inline CSS and JavaScript in HTML reports. ZAP's reports use
        //   inline styles and scripts for formatting, charts, and interactivity.
        //
        //   Without the CSP fix, the report appears in Jenkins but looks
        //   completely unstyled (plain text, no colors, broken layout).
        //
        //   The fix: Temporarily relax Jenkins' CSP to allow inline content.
        //   This is a KNOWN ISSUE documented in Jenkins' HTML Publisher plugin:
        //   https://www.jenkins.io/doc/book/security/configuring-content-security-policy/
        //
        // SECURITY NOTE:
        //   Relaxing CSP is acceptable here because:
        //   1. Jenkins runs on localhost (not publicly accessible)
        //   2. The HTML reports are generated by ZAP (trusted source)
        //   3. In production, you'd use an artifact server or a separate
        //      report viewer instead of relaxing CSP.
        stage('Publish ZAP Report') {
            steps {
                echo '=== Stage 7: Publish ZAP Report — Making reports viewable in Jenkins ==='

                // ── Fix Jenkins CSP for HTML reports ─────────────────────────
                // System.setProperty() sets a JVM-level property that Jenkins
                // reads when serving HTML content.
                //
                // The empty string "" means "no CSP restrictions" — allow all
                // inline styles and scripts in published HTML files.
                //
                // This uses a Groovy script block (script { }) because
                // System.setProperty is a Groovy/Java method, not a bat command.
                script {
                    // Relax Content Security Policy so ZAP reports render correctly.
                    // Without this, inline CSS/JS in ZAP's HTML report is blocked,
                    // making the report appear unstyled and broken.
                    System.setProperty(
                        'hudson.model.DirectoryBrowserSupport.CSP',
                        ''  // Empty string = allow all inline content
                    )
                }

                // ── Publish the baseline scan report ─────────────────────────
                // publishHTML creates a clickable tab on the Jenkins build page.
                //
                // Parameters explained:
                //   reportDir: folder containing the HTML file
                //   reportFiles: the HTML file name(s) to display
                //   reportName: the label shown on the tab in Jenkins UI
                //   keepAll: true = keep reports from ALL builds (not just latest)
                //   allowMissing: true = don't fail if the report wasn't generated
                //     (useful if the scan stage was skipped or failed)
                //   alwaysLinkToLastBuild: true = the sidebar link always points
                //     to the most recent build's report
                publishHTML(target: [
                    reportDir: 'zap-reports',
                    reportFiles: 'zap-baseline-report.html',
                    reportName: 'ZAP Baseline Scan Report',
                    keepAll: true,
                    allowMissing: true,
                    alwaysLinkToLastBuild: true
                ])

                // ── Publish the active scan report ───────────────────────────
                publishHTML(target: [
                    reportDir: 'zap-reports',
                    reportFiles: 'zap-active-report.html',
                    reportName: 'ZAP Active Scan Report',
                    keepAll: true,
                    allowMissing: true,
                    alwaysLinkToLastBuild: true
                ])

                echo 'ZAP reports published. Check the build page for report tabs.'
            }
        }


        // =====================================================================
        // STAGE 8: STOP BACKEND — Kill the Spring Boot process
        // =====================================================================
        // WHY THIS STAGE EXISTS:
        //   The Spring Boot app is running in the background (started in Stage 4).
        //   If we don't stop it, the java.exe process keeps running FOREVER,
        //   hogging port 8080. The next pipeline run would fail with
        //   "Port 8080 already in use."
        //
        // HOW WE FIND THE PROCESS:
        //   1. netstat -ano | findstr :8080 → shows the PID using port 8080
        //   2. Parse the PID from netstat output using FOR /F
        //   3. taskkill /PID <pid> /F → force-kill the process
        //
        // WINDOWS GOTCHA — FOR /F SYNTAX:
        //   FOR /F is Windows batch's way of parsing text output.
        //   It's ugly but necessary — there's no "kill by port" command on Windows.
        //
        //   "tokens=5" means: split each line by spaces, take the 5th field.
        //   In netstat output, the 5th field is the PID:
        //     TCP  0.0.0.0:8080  0.0.0.0:0  LISTENING  12345
        //     ^1    ^2            ^3          ^4          ^5
        //
        // WHY /F (FORCE)?
        //   Without /F, taskkill sends a "please close" message (WM_CLOSE).
        //   Java processes often ignore this. /F = "terminate immediately."
        //
        // WHAT IF THE PROCESS IS ALREADY STOPPED?
        //   The "2>nul" at the end suppresses the error message:
        //   "ERROR: The process with PID X could not be terminated."
        //   This happens if the app crashed on its own — not a problem.
        stage('Stop Backend') {
            steps {
                echo '=== Stage 8: Stop Backend — Killing Spring Boot process ==='

                bat '''
                    @echo off
                    echo Stopping backend server on port 8080...

                    REM ── Find and kill the process listening on port 8080 ─────
                    REM
                    REM How this works, step by step:
                    REM   1. netstat -ano = list all connections with PIDs
                    REM   2. findstr :8080 = filter lines containing ":8080"
                    REM   3. findstr LISTENING = only the server (not clients)
                    REM   4. FOR /F "tokens=5" = extract the PID (5th column)
                    REM   5. taskkill /PID = kill that process
                    REM
                    REM Example netstat output line:
                    REM   TCP    0.0.0.0:8080    0.0.0.0:0    LISTENING    12345
                    REM   ^^^    ^^^^^^^^^^^      ^^^^^^^^^    ^^^^^^^^^    ^^^^^
                    REM   token1 token2           token3       token4       token5 (PID)

                    for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
                        echo Found process on port 8080: PID %%p
                        taskkill /PID %%p /F 2>nul
                        echo Process %%p terminated.
                    )

                    REM Verify the port is free
                    netstat -ano | findstr :8080 | findstr LISTENING > nul 2>&1
                    if %ERRORLEVEL%==0 (
                        echo WARNING: Port 8080 is still in use!
                    ) else (
                        echo Port 8080 is now free.
                    )
                    exit /b 0
                '''
            }
        }
    }


    // =========================================================================
    // POST — Actions that run AFTER all stages complete (or fail)
    // =========================================================================
    // The post block is Jenkins' cleanup/notification mechanism.
    // It runs regardless of whether the pipeline succeeded or failed.
    //
    // WHY IS THIS IMPORTANT?
    //   If the ZAP scan stage fails, the Stop Backend stage might be skipped
    //   (depending on the error). The "always" block is a SAFETY NET that
    //   guarantees the backend process gets killed no matter what.
    //
    //   Without this, a failed pipeline could leave a "zombie" Java process
    //   running on port 8080, blocking the next build.
    //
    // POST CONDITIONS:
    //   always  → runs no matter what happened (success, failure, unstable)
    //   success → runs only if ALL stages passed
    //   failure → runs only if any stage failed
    //   unstable → runs if tests failed but the build succeeded
//     post {
//         always {
//             echo '=== Post: Always — Running cleanup tasks ==='

//             // ── Safety net: stop backend if it's still running ───────────────
//             // This duplicates Stage 8's logic, but that's intentional.
//             // If Stage 8 was skipped (because an earlier stage failed),
//             // this ensures the backend is still cleaned up.
//             bat '''
//                 @echo off
//                 REM ── Safety net: kill any Java process on port 8080 ──────
//                 REM This runs even if previous stages failed, ensuring no
//                 REM zombie processes are left behind.
//                 for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING 2^>nul') do (
//                     echo [Cleanup] Killing leftover process on port 8080: PID %%p
//                     taskkill /PID %%p /F 2>nul
//                 )
//             '''

//             // ── Archive the backend log file ─────────────────────────────────
//             // archiveArtifacts saves files from the workspace as downloadable
//             // attachments on the build page. The backend log is useful for
//             // debugging startup failures.
//             //
//             // allowEmptyArchive: true → don't fail if the log doesn't exist
//             //   (it won't exist if the Start Backend stage was never reached)
//             archiveArtifacts artifacts: 'backend_log.txt',
//                              allowEmptyArchive: true

//             // ── Archive ZAP reports as build artifacts ────────────────────────
//             // These are downloadable from the build page, separate from the
//             // HTML Publisher tabs. Useful for sharing reports or archiving.
//             archiveArtifacts artifacts: 'zap-reports/**/*',
//                              allowEmptyArchive: true
//         }

//         success {
//             echo '''
// ╔══════════════════════════════════════════════════════════╗
// ║              PIPELINE COMPLETED SUCCESSFULLY             ║
// ╠══════════════════════════════════════════════════════════╣
// ║  ✅ Backend built and tested                             ║
// ║  ✅ ZAP baseline scan completed                          ║
// ║  ✅ ZAP active scan completed                            ║
// ║  ✅ Reports published to Jenkins UI                      ║
// ║                                                          ║
// ║  Check the build page for:                               ║
// ║    📊 "Test Results" tab (JUnit)                         ║
// ║    🔒 "ZAP Baseline Scan Report" tab                     ║
// ║    🔒 "ZAP Active Scan Report" tab                       ║
// ╚══════════════════════════════════════════════════════════╝
// '''
//         }

//         failure {
//             echo '''
// ╔══════════════════════════════════════════════════════════╗
// ║                  PIPELINE FAILED                         ║
// ╠══════════════════════════════════════════════════════════╣
// ║  ❌ Check the Console Output above to find which stage   ║
// ║     failed and what the error message says.              ║
// ║                                                          ║
// ║  COMMON FIXES:                                           ║
// ║  • "gradlew.bat not found" → Checkout stage didn't copy  ║
// ║    files. Check the xcopy source path.                   ║
// ║  • "Port 8080 already in use" → Run:                     ║
// ║    netstat -ano | findstr :8080                          ║
// ║    taskkill /PID <pid> /F                                ║
// ║  • "ZAP not found" → Check ZAP_PATH in Jenkins global   ║
// ║    environment variables.                                ║
// ║  • Build errors → Check Java compilation output.         ║
// ╚══════════════════════════════════════════════════════════╝
// '''
//         }
//     }
}
