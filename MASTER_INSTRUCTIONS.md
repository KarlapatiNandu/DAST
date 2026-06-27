# DAST Learning Lab — Master Instructions

> [!CAUTION]
> **READ THIS FILE FIRST in every new chat session.**
> This file contains the complete project instructions across all 7 stages.
> Each stage is implemented in a separate chat. Before doing ANY work,
> read this file, `ProjectV1.md` (for progress tracking), and the
> audit/report for the PREVIOUS stage.

---

## Global Rules (Apply to EVERY stage)

### 1. Teaching-First Approach
- The user is **new to Spring Boot, Gradle, Jenkins, and CI pipelines** but knows Java.
- **Every file you create must include line-by-line comments explaining WHY each line exists.**
- Treat every file as a teaching document for a beginner.

### 2. Platform & Environment
- **Operating System:** Windows (no Docker, no Linux commands)
- **Backend:** Spring Boot app in `/backend` directory
- **Frontend:** React + Vite in `/frontend` (no changes needed to frontend build)
- **Build tool:** Gradle (migrated from Maven in Stage 2)
- **ZAP:** Already installed on the Windows machine
- **Jenkins:** Will be installed on the same Windows machine
- **No Docker** involved anywhere in this project

### 3. Project Structure
```
DAST/
├── backend/                          ← Spring Boot (Java 17)
│   ├── build.gradle                  ← Gradle build file (created in Stage 2)
│   ├── settings.gradle               ← Gradle settings (created in Stage 2)
│   ├── gradlew.bat                   ← Gradle wrapper for Windows
│   └── src/main/java/com/dast/demo/
│       ├── SearchBackendApplication.java  ← Main class (@SpringBootApplication)
│       ├── config/
│       │   └── OpenApiConfig.java         ← OpenAPI config (Stage 2B)
│       ├── controller/
│       │   ├── LoginController.java       ← POST /api/login (SQLi - CWE-89)
│       │   ├── XssController.java         ← GET /api/greet, /api/echo (XSS - CWE-79)
│       │   ├── SearchController.java      ← GET /api/search (XSS in JSON)
│       │   └── secure/                    ← Fixed versions (Stage 6)
│       │       ├── SecureLoginController.java
│       │       ├── SecureXssController.java
│       │       └── SecureSearchController.java
│       ├── model/
│       │   ├── LoginRequest.java          ← POJO (unused currently)
│       │   └── SearchResponse.java        ← POJO for search JSON
│       └── resources/
│           ├── application.properties     ← Port 8080, H2 config
│           ├── schema.sql                 ← CREATE TABLE users
│           └── data.sql                   ← 4 test users (plaintext passwords)
├── frontend/                         ← React + Vite (no changes needed)
├── zap/
│   └── zap-automation.yaml           ← ZAP automation config (Stage 5)
├── Jenkinsfile                       ← CI pipeline (Stage 4)
├── JENKINS_SETUP.md                  ← Jenkins install guide (Stage 3)
├── INTERVIEW_PREP.md                 ← Q&A document (Stage 7)
├── zap_testing_guide.md              ← ZAP manual + concepts (Stage 5)
├── MASTER_INSTRUCTIONS.md            ← THIS FILE — read first every session
├── ProjectV1.md                      ← Progress tracker — update every session
├── STAGE1_AUDIT.md                   ← Stage 1 output
├── STAGE2_AUDIT.md                   ← Stage 2 output (created after Stage 2)
└── ...                               ← Additional stage audit files
```

### 4. Vulnerabilities in the App
| CWE | Vulnerability | Controller | Line(s) |
|---|---|---|---|
| CWE-89 | SQL Injection | LoginController.java | 130-131 (string concat into SQL) |
| CWE-79 | Reflected XSS | XssController.java | 121 (greet), 152 (echo) — raw HTML |
| CWE-79 | XSS (JSON) | SearchController.java | 76 — JSON mitigates execution |
| CWE-256 | Plaintext Passwords | data.sql | 12-16 |
| CWE-209 | Info Disclosure | LoginController.java | 171 (SQL error in response) |
| — | CORS Wildcard | All controllers | `@CrossOrigin(origins = "*")` |

### 5. Endpoints
| Method | Path | Controller | Response Type |
|---|---|---|---|
| POST | `/api/login` | LoginController | application/json |
| GET | `/api/greet?name=` | XssController | text/html |
| GET | `/api/echo?input=` | XssController | text/html |
| GET | `/api/search?query=` | SearchController | application/json |
| GET | `/h2-console` | (built-in H2) | text/html |

### 5B. Jenkins + ZAP Automation Intent

The Jenkins pipeline must reproduce the useful part of the original manual ZAP
workflow:

Manual workflow:
1. Start the backend.
2. Use Manual Explore to make ZAP aware of every API endpoint.
3. Send example requests to each endpoint so they appear in ZAP's site tree.
4. Run Active Scan against those discovered endpoints.
5. Generate and save the HTML report.

Automated Jenkins workflow:
1. Build Backend.
2. Start Backend on `http://localhost:8080`.
3. Import OpenAPI from `http://localhost:8080/v3/api-docs`.
4. Optionally run Spider only as an extra crawl/safety step.
5. Run Active Scan against the OpenAPI-discovered endpoints.
6. Generate one HTML report from the ZAP Automation Framework.
7. Publish that HTML report in Jenkins with the HTML Publisher plugin.

Important: OpenAPI import is mandatory. Spider is optional. A REST API often
does not expose normal HTML links, so spidering alone cannot be trusted to find
all endpoints. The OpenAPI import is the automated replacement for manually
exploring and touching each endpoint in ZAP.

### 6. Session Workflow
At the start of every new chat:
1. Read `MASTER_INSTRUCTIONS.md` (this file)
2. Read `ProjectV1.md` to see what stages are complete
3. Read the audit/report of the previous stage (e.g., `STAGE1_AUDIT.md` before starting Stage 2)
4. Do the work for the current stage
5. Create `STAGE{N}_AUDIT.md` in the project root with the stage report
6. Update `ProjectV1.md` to mark the stage complete

---

## Stage Definitions

---

### STAGE 1 — PROJECT AUDIT (no code changes) ✅ COMPLETE

- Read the existing pom.xml carefully
- List every dependency, plugin, and configuration in pom.xml
- Output a plain mapping table: Maven concept → Gradle equivalent
- List every file in /backend/src to understand what exists
- Identify the main class annotated with @SpringBootApplication
- Output: audit report only, no file changes

**Deliverable:** `STAGE1_AUDIT.md`

---

### STAGE 2 — GRADLE MIGRATION

Replace Maven with Gradle. Create these files in /backend/:

**1. settings.gradle**
- Set rootProject.name to match the artifactId in pom.xml (`search-backend`)
- Comment explaining what settings.gradle controls vs build.gradle

**2. build.gradle**
- Apply plugins: java, org.springframework.boot, io.spring.dependency-management
- Set group, version, java sourceCompatibility to match pom.xml values
- Add repositories block (mavenCentral())
- Add dependencies block with exact same dependencies as pom.xml:
  * spring-boot-starter-web
  * spring-boot-starter-jdbc
  * h2 (runtimeOnly)
  * spring-boot-starter-test (testImplementation)
- Add a bootRun task configuration
- Every line must have a comment comparing it to the pom.xml equivalent
  Example: `// Gradle equivalent of <artifactId>spring-boot-starter-web</artifactId>`

**3. Generate Gradle wrapper files:**
- Provide the exact command to run in the /backend/ directory
  to generate gradlew, gradlew.bat, and gradle/wrapper/ folder
- Explain what the wrapper is and why it matters for Jenkins
  (Jenkins uses gradlew.bat so it doesn't need Gradle installed globally)

**4. Verify instruction:**
- Provide the exact command to test the build works:
  `cd backend && gradlew.bat build`
- Explain what output to expect if it succeeds
- List the 3 most common errors and how to fix each

**5. After confirming build works:**
- Provide command to delete pom.xml and explain why keeping both would cause conflicts

**Deliverable:** `STAGE2_AUDIT.md`

---

### STAGE 2B — OPENAPI / SWAGGER INTEGRATION

Add Springdoc OpenAPI to the backend for automatic endpoint discovery.
This enables ZAP to find all endpoints from the spec instead of
hardcoded URLs in the Jenkinsfile.

**1. Add to build.gradle dependencies block:**
```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
```
- Explain: what Springdoc does (reads Spring annotations, generates spec)
- Explain: why version 2.x is required for Spring Boot 3.x
  (version 1.x only works with Spring Boot 2.x — common mistake)

**2. Add to application.properties:**
```properties
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
```
- Explain each property and what it controls

**3. Add @Operation and @Parameter annotations to all three
vulnerable controllers (do NOT modify logic, only add documentation):**

In LoginController:
```java
@Operation(summary = "Vulnerable login endpoint",
           description = "Demonstrates SQL Injection - CWE-89")
@Parameter(name = "username", description = "Username - injectable")
@Parameter(name = "password", description = "Password field")
```

In XssController (greet endpoint):
```java
@Operation(summary = "Vulnerable greeting endpoint",
           description = "Demonstrates Reflected XSS - CWE-79")
@Parameter(name = "name", description = "Name field - injectable")
```

In SearchController:
```java
@Operation(summary = "Vulnerable search endpoint",
           description = "Demonstrates Reflected XSS in JSON context")
@Parameter(name = "query", description = "Search query - injectable")
```

- Explain: @Operation describes the endpoint
- Explain: @Parameter describes each input field
- Explain: ZAP uses these descriptions to understand what data to inject

**4. Add OpenAPI config class:**
Create `/backend/src/main/java/com/dast/demo/config/OpenApiConfig.java`

This class uses @OpenAPIDefinition to set:
- title: "DAST Learning Lab API"
- description: "Intentionally vulnerable API for security testing"
- version: "1.0"
- Add a Server entry pointing to http://localhost:8080

Explain: ZAP needs the server URL in the spec to know where to send attacks.
Without it, ZAP reads the spec but doesn't know what host to target.

**5. Verification step:**
- Run: `gradlew.bat bootRun`
- Open: http://localhost:8080/swagger-ui.html
- Open: http://localhost:8080/v3/api-docs
- Explain what to look for in each:
  swagger-ui.html → visual list of all endpoints with try-it-out buttons
  v3/api-docs → raw JSON that ZAP will consume
- Confirm all 5 endpoints appear:
  GET /api/search, POST /api/login, GET /api/greet, GET /api/echo,
  any H2 console endpoints

**6. Update ZAP Automation Framework config (will be used in Stage 5):**
Provide the exact yaml snippet that tells ZAP to import from OpenAPI:
```yaml
- type: openapi
  parameters:
    apiUrl: http://localhost:8080/v3/api-docs
    targetUrl: http://localhost:8080
```
Explain: this replaces manually listing every URL in the Jenkinsfile.
Every new endpoint added to the Spring Boot app automatically gets
scanned on the next pipeline run without touching the Jenkinsfile.

**7. Interview talking point:**
Add a section explaining how to answer:
"How does your pipeline scale as the API grows?"
Answer should reference: Springdoc reads @RestController annotations →
generates spec → ZAP imports spec → new endpoints auto-scanned.
No manual Jenkinsfile updates needed.

**Deliverable:** `STAGE2B_AUDIT.md`

---

### STAGE 3 — JENKINS INSTALLATION AND CONFIGURATION

Do not write any Jenkinsfile yet. First explain setup.

Create `/JENKINS_SETUP.md` with these sections:

**1. Installing Jenkins on Windows**
- Download jenkins.war (not the Windows installer) from jenkins.io
- Exact command: `java -jar jenkins.war --httpPort=9090`
  (use 9090 not 8080 because Spring Boot uses 8080)
- URL: http://localhost:9090
- Initial admin password location on Windows:
  `%USERPROFILE%\.jenkins\secrets\initialAdminPassword`
- Plugins: "Install suggested plugins" then additionally:
  * HTML Publisher Plugin (for ZAP reports)
  * Gradle Plugin
- Explain Jenkins agent vs controller (single machine = same thing)

**2. Configuring Gradle in Jenkins**
- Jenkins → Manage Jenkins → Tools → Gradle installations
- Why Jenkins needs to know where Gradle is
- Alternative: use the wrapper (gradlew.bat) — recommend this and explain why

**3. Configuring ZAP path in Jenkins**
- Jenkins → Manage Jenkins → System → Global properties → Environment variables
- Add: `ZAP_PATH = C:\Program Files\OWASP\Zed Attack Proxy`
  (user must verify this path)
- Why store here vs hardcoding in Jenkinsfile

**4. Creating a Pipeline job in Jenkins UI**
- Jenkins → New Item → Pipeline (not Freestyle)
- Explain Freestyle vs Pipeline jobs
- "Pipeline script from SCM" vs "Pipeline script" —
  recommend "Pipeline script" first, explain what SCM means

**Deliverable:** `STAGE3_AUDIT.md`

---

### STAGE 4 — JENKINSFILE (Windows-specific)

Create `/Jenkinsfile` at the project root.

**Critical Windows rules:**
- Use `bat('command')` not `sh('command')` for all shell commands
- Use double backslash `\\` for Windows paths inside strings
- Background processes: `bat('start /b command')`
- Port check: `bat('netstat -ano | findstr :8080')`

**Required pipeline intent:**
- Jenkins must not rely on ZAP Quick Scan or URL-only spider discovery.
- Jenkins must build and start the Spring Boot backend first.
- Jenkins must run ZAP with the Automation Framework plan in
  `/zap/zap-automation.yaml`.
- The ZAP plan must import `/v3/api-docs` before Active Scan so ZAP attacks the
  same API surface that was previously discovered manually through ZAP.
- The HTML report generated by the ZAP plan must be published in Jenkins.

**Pipeline structure:**
```groovy
pipeline {
  agent any

  environment {
    ZAP_PATH = "${env.ZAP_PATH}"
    BACKEND_URL = 'http://localhost:8080'
    ZAP_PORT = '8090'
    REPORT_DIR = "${env.WORKSPACE}\\zap-reports"
  }

  stages {
    stage('Checkout') { /* confirm workspace */ }
    stage('Build Backend') { /* gradlew.bat build -x test */ }
    stage('Test Backend') { /* gradlew.bat test + junit publish */ }
    stage('Start Backend') { /* start /b java -jar, port wait loop */ }
    stage('ZAP Automation Scan') {
      /*
       * Run:
       *   "%ZAP_PATH%\\zap.bat" -cmd -addonupdate -autorun "%WORKSPACE%\\zap\\zap-automation.yaml"
       *
       * The YAML plan handles:
       *   1. OpenAPI import from /v3/api-docs
       *   2. Optional spider
       *   3. Active Scan
       *   4. HTML report generation
       */
    }
    stage('Publish ZAP Report') { /* publishHTML for zap-automation-report.html + CSP fix */ }
    stage('Stop Backend') { /* kill java process on port 8080 */ }
  }

  post {
    always { /* stop backend safety net, archive artifacts */ }
    success { echo 'Pipeline completed.' }
    failure { echo 'Pipeline failed.' }
  }
}
```

Every stage must have detailed comments explaining:
- What the stage does
- Why it exists
- Windows-specific gotchas
- What errors to expect and how to fix them

**ZAP stage requirements:**
- Create `%REPORT_DIR%` before running ZAP.
- Run ZAP from the Jenkins workspace so relative report paths in the YAML plan
  resolve under the workspace.
- Treat ZAP exit code `0` as success.
- Treat ZAP exit code `1` as "scan completed with findings" and keep publishing
  the report.
- Fail only on real execution errors such as ZAP not starting, the backend being
  unreachable, or the YAML plan being invalid.
- Publish exactly the HTML report generated by the Automation Framework plan.

**Deliverable:** `STAGE4_AUDIT.md`

---

### STAGE 5 — ZAP AUTOMATION FRAMEWORK CONFIG

**1. Create `/zap/zap-automation.yaml`**
- env section: target URL and context definition for `http://localhost:8080`
- jobs section in this exact intent order:
  1. `openapi` job imports `http://localhost:8080/v3/api-docs`
  2. `passiveScan-wait` job waits for passive findings from imported traffic
  3. optional `spider` job runs only as an extra crawl/safety step
  4. `activeScan` job attacks the endpoints imported from OpenAPI
  5. `report` job generates the HTML report for Jenkins
- The OpenAPI job is required because it replaces the manual ZAP workflow where
  the user clicked through or manually tested endpoints to populate ZAP's site
  tree.
- The spider job is optional because spidering alone cannot reliably discover
  REST endpoints that are not linked from an HTML page.
- The report job must write `zap-automation-report.html` into the Jenkins
  `zap-reports` directory.
- Rules: prioritize 40018 (SQL Injection) and 40012/40014 (XSS)
- Explain why these rule IDs match what ZAP should find in this project

**2. Create `/zap_testing_guide.md` covering:**
1. DAST vs SAST: one concrete analogy
2. Manual ZAP workflow (paste URL → manual explore → launch browser →
   send payloads → active scan → report) — explain each step technically
3. What ZAP finds in THIS app — reference actual controller code
4. How Jenkins automates every manual step:
   - backend start replaces manually running the app
   - OpenAPI import replaces manually discovering/touching endpoints
   - optional spider adds extra crawl coverage
   - activeScan replaces clicking Active Scan in the GUI
   - report + publishHTML replaces exporting and opening the HTML report manually
5. How to read ZAP alerts: High/Medium/Low/Informational with examples

**Important success criterion:**
The automated Jenkins report should cover the same API endpoints as the manual
ZAP report. If the report only contains a quick scan of `/`, Swagger UI, or a
small crawled subset, the stage does not meet the project intent.

**Deliverable:** `STAGE5_AUDIT.md`

---

### STAGE 6 — SECURE CONTROLLERS

Create new controllers in `backend/src/main/java/com/dast/demo/controller/secure/`

**1. SecureLoginController → /api/secure/login**
Fix: PreparedStatement with ? placeholders

**2. SecureXssController → /api/secure/greet**
Fix: HtmlUtils.htmlEscape() + Content-Security-Policy response header

**3. SecureSearchController → /api/secure/search**
Fix: return JSON (explain why JSON is inherently safer than HTML for XSS)

**For every secure controller:**
- Keep the vulnerable version running alongside it
- Add a comment block at the top:
```
VULNERABLE VERSION: [filename] at [endpoint]
WHAT WAS WRONG: [exact line]
THIS FIX WORKS BECAUSE: [technical explanation]
WHAT ATTACKER COULD DO WITHOUT FIX: [concrete example]
```

**Deliverable:** `STAGE6_AUDIT.md`

---

### STAGE 7 — INTERVIEW PREP DOCUMENT

Create `/INTERVIEW_PREP.md` with Q&A format.

**Questions to cover (answers tied to actual project code):**
1. Walk me through this project (2 paragraphs max)
2. What is DAST? SAST? How do they differ? Which did you use and why?
3. What is CI/CD? Where does security testing fit in a CI pipeline?
4. Explain SQL Injection using YOUR LoginController code
5. Explain Reflected XSS using YOUR XssController code
6. What is CWE-89, CWE-79, CWE-256? What is OWASP A03:2021?
7. Why did ZAP not detect SQLi before the H2 database was added?
8. What is @CrossOrigin(origins="*") and what attack does it enable?
9. What is a PreparedStatement? Write it from memory
10. What does HtmlUtils.htmlEscape() do to `<script>alert(1)</script>`?
11. Explain your Jenkins pipeline stage by stage
12. What is the difference between ZAP baseline scan and active scan?
13. What Spring Boot annotations did you use? Explain each one
14. What is Maven/Gradle? Why did you switch from Maven to Gradle?
15. What would you add to make this production-ready?
    (Spring Security, HTTPS, parameterized queries, CSP headers,
     secrets vault, rate limiting, input validation layer)
16. What is the H2 database? Why use it here instead of MySQL/PostgreSQL?
17. If ZAP reports a false positive, how do you handle it in the pipeline?

Each answer: 3-5 sentences maximum. Reference specific files and
line concepts from the project. Interview-length, not textbook-length.

**Deliverable:** `STAGE7_AUDIT.md`

---

## How to Start a New Stage Chat

Copy and paste this into your new chat:

```
Read the following files from my DAST project before doing anything:
1. c:\Users\nandu\OneDrive\Desktop\DAST\MASTER_INSTRUCTIONS.md
2. c:\Users\nandu\OneDrive\Desktop\DAST\ProjectV1.md

Then execute the next incomplete stage. Follow all global rules 
(line-by-line comments, Windows-only, teaching approach).
Create STAGE{N}_AUDIT.md in the project root when done.
Update ProjectV1.md to mark the stage complete.
```


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
//     5. Run ZAP Automation Framework scan (OpenAPI-driven)
//        (imports endpoints, passive scan, active scan, report — all in one)
//     6. Publish ZAP's HTML report in the Jenkins UI
//     7. Stop the backend server (cleanup)
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
//   2. Use double backslash \\ for paths in ALL Groovy strings:
//      bat('dir C:\\Users\\nandu')  or  bat '''dir C:\\Users\\nandu'''
//      Groovy treats \ as an escape character in EVERY string type
//      (single-quoted, double-quoted, AND triple-quoted). So \\ → one \.
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
        BACKEND_URL = 'http://localhost:8080'

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
    //   Checkout → Build → Test → Start App → ZAP Automation Framework
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
                    bat '.\\gradlew.bat test'
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

REM ── Find the executable JAR (ignore *-plain.jar) ─────────────────
set "JAR_PATH="

for /r backend\\build\\libs %%f in (*.jar) do (
    set "FILE=%%~nxf"

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

REM ── Wait for backend health check ────────────────────────────────
echo Waiting for backend health check at %BACKEND_URL%/v3/api-docs...

set ATTEMPTS=0
set MAX_ATTEMPTS=12

:check_health
set /a ATTEMPTS+=1

curl -s -o nul -w "%%{http_code}" %BACKEND_URL%/v3/api-docs | findstr "200" >nul
if not errorlevel 1 (
    echo Backend is UP and healthy! HTTP Status: 200 (attempt !ATTEMPTS! of !MAX_ATTEMPTS!)
    goto :port_ready
)

if !ATTEMPTS! GEQ !MAX_ATTEMPTS! (
    echo ERROR: Backend health check failed after 60 seconds.
    echo ========================================================
    echo                 BACKEND STARTUP LOGS
    echo ========================================================
    type "%WORKSPACE%\\backend_log.txt"
    echo ========================================================
    exit /b 1
)

echo Attempt !ATTEMPTS! of !MAX_ATTEMPTS!: Backend not ready, waiting 5 seconds...
timeout /t 5 /nobreak >nul
goto :check_health

:port_ready
echo Backend is running and ready for scanning.
'''
            }
        }


        // =====================================================================
        // STAGE 5: ZAP SCAN (AUTOMATION FRAMEWORK) — OpenAPI-driven scanning
        // =====================================================================
        // WHY THIS STAGE EXISTS:
        //   This single stage replaces the old "ZAP Baseline Scan" and
        //   "ZAP Active Scan" stages. It uses the ZAP Automation Framework
        //   to run a complete security scan driven by our OpenAPI spec.
        //
        // WHY WE SWITCHED FROM -quickurl TO -autorun:
        //   PROBLEM: The old approach used -quickurl which expects to crawl
        //   a WEBSITE starting from a homepage (HTTP 200). Our app is a REST
        //   API — the root URL (/) returns 404 because there's no homepage.
        //   ZAP could never discover any endpoints to scan.
        //
        //   SOLUTION: The Automation Framework reads our OpenAPI spec at
        //   /v3/api-docs and imports ALL endpoints automatically. No crawling
        //   needed — ZAP knows exactly where every endpoint is.
        //
        // WHAT THE YAML PLAN DOES (see zap/zap-automation.yaml):
        //   Job 1: openapi          → Import endpoints from /v3/api-docs
        //   Job 2: passiveScan-wait → Wait for passive analysis to complete
        //   Job 3: activeScan       → Send attack payloads to every endpoint
        //   Job 4: report           → Generate HTML report
        //
        // HOW THIS SCALES (THE KEY INTERVIEW TALKING POINT):
        //   When a developer adds a new @RestController, Springdoc auto-adds
        //   it to the OpenAPI spec. On the next build, ZAP reads the updated
        //   spec and scans the new endpoint. ZERO changes needed to this
        //   Jenkinsfile or the zap-automation.yaml file.
        //
        // WHAT WE EXPECT ZAP TO FIND:
        //   | Endpoint          | Expected Finding        | Why                          |
        //   |-------------------|-------------------------|------------------------------|
        //   | POST /api/login   | SQL Injection (CWE-89)  | String concat in SQL query   |
        //   | GET /api/greet    | Reflected XSS (CWE-79)  | Raw HTML output with name    |
        //   | GET /api/echo     | Reflected XSS (CWE-79)  | Raw HTML output with input   |
        //   | GET /api/search   | XSS (lower risk)        | JSON response (safer format) |
        //
        // PASSIVE vs ACTIVE SCANNING (both happen inside the YAML plan):
        //   | Aspect      | Passive (passiveScan-wait) | Active (activeScan)           |
        //   |-------------|----------------------------|-------------------------------|
        //   | Speed       | Fast (seconds)             | Slow (minutes to hours)       |
        //   | Risk        | Safe (read-only)           | Can modify data/crash app     |
        //   | Finds       | Missing headers, info leak | SQLi, XSS, command injection  |
        //   | Production? | Safe to run                | NEVER run against production! |
        stage('ZAP Scan (Automation Framework)') {
            steps {
                echo '=== Stage 5: ZAP Scan — OpenAPI-driven Automation Framework ==='

                // ── Create the report output directory ───────────────────────
                // mkdir with "2>nul" suppresses the error if the directory
                // already exists (otherwise bat() would report an error).
                // The YAML plan's report job writes to this directory.
                bat 'if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%"'

                // ── Run ZAP with the Automation Framework ────────────────────
                // ZAP command breakdown:
                //   "%ZAP_PATH%\\zap.bat"  = the ZAP launcher script
                //   -cmd                    = headless mode (no GUI)
                //   -addonupdate            = update all add-ons to latest versions
                //   -autorun <yaml-file>    = run the Automation Framework plan
                //
                // WHAT -autorun DOES vs -quickurl:
                //   -quickurl: ZAP tries to crawl a website from a single URL.
                //              Fails for REST APIs because there's nothing to crawl.
                //   -autorun:  ZAP reads a YAML plan file that defines exactly
                //              what jobs to run. The plan imports endpoints from
                //              OpenAPI, runs passive + active scans, and generates
                //              a report — all in one command.
                //
                // WHY -addonupdate?
                //   ZAP's vulnerability detection rules are updated frequently.
                //   -addonupdate ensures we have the latest scan rules.
                //   Without it, ZAP might miss vulnerabilities that newer
                //   rules would catch.
                //
                // EXIT CODE HANDLING:
                //   ZAP's Automation Framework exit codes:
                //     0 = plan ran with no problems
                //     1 = plan failed with an error
                //     2 = plan ran but there were warnings (e.g., vulnerabilities found)
                //
                //   We EXPECT exit code 2 because our app has intentional
                //   vulnerabilities. "exit /b 0" prevents Jenkins from treating
                //   ZAP findings as a pipeline failure. The vulnerabilities are
                //   reported in the HTML report — they don't need to fail the build.
                //
                //   In a production pipeline, you might WANT exit code 2 to fail
                //   the build (so vulnerabilities block deployment). For our
                //   learning lab, we always want to see the full report.
                bat '''
                    @echo off
                    echo ============================================================
                    echo   ZAP Automation Framework — OpenAPI-Driven Security Scan
                    echo ============================================================
                    echo.
                    echo Plan file: zap/zap-automation.yaml
                    echo Target:    %BACKEND_URL%
                    echo OpenAPI:   %BACKEND_URL%/v3/api-docs
                    echo.
                    echo Jobs that will execute:
                    echo   1. openapi          - Import endpoints from OpenAPI spec
                    echo   2. passiveScan-wait  - Wait for passive analysis
                    echo   3. activeScan        - Send attack payloads
                    echo   4. report            - Generate HTML report
                    echo.
                    echo This may take 5-20 minutes depending on the number of endpoints.
                    echo.

                    pushd "%ZAP_PATH%"

call zap.bat -cmd ^
    -addonupdate ^
    -autorun "%WORKSPACE%\\zap\\zap-automation.yaml"

REM ── Capture ZAP's exit code before changing directories ──
set ZAP_EXIT=%ERRORLEVEL%

popd

echo.
echo ============================================================
echo   ZAP scan finished with exit code: %ZAP_EXIT%
echo   (0=clean, 1=error, 2=warnings/findings)
echo ============================================================

REM ── Fail only on actual execution errors ──
if %ZAP_EXIT% EQU 1 (
    echo ERROR: ZAP Automation Framework plan failed!
    exit /b 1
)

echo ZAP scan complete. Report saved to %REPORT_DIR%
exit /b 0
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
                echo '=== Stage 6: Publish ZAP Report — Making report viewable in Jenkins ==='

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

                // ── Publish the Automation Framework scan report ─────────────
                // publishHTML creates a clickable tab on the Jenkins build page.
                //
                // The Automation Framework generates a SINGLE combined report
                // that includes BOTH passive and active scan findings. This
                // replaces the old approach of publishing two separate reports
                // (baseline + active).
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
                //
                // NOTE: The filename "zap-automation-report.html" must match
                // the "reportFile" parameter in zap/zap-automation.yaml.
                // ZAP appends ".html" to the reportFile value automatically.
                publishHTML(target: [
                    reportDir: 'zap-reports',
                    reportFiles: 'zap-automation-report.html',
                    reportName: 'ZAP Security Scan Report',
                    keepAll: true,
                    allowMissing: true,
                    alwaysLinkToLastBuild: true
                ])

                echo 'ZAP report published. Check the build page for the report tab.'
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
                echo '=== Stage 7: Stop Backend — Killing Spring Boot process ==='

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
    post {
        always {
            echo '=== Post: Always — Running cleanup tasks ==='

            // ── Safety net: stop backend if it's still running ───────────────
            // This duplicates Stage 8's logic, but that's intentional.
            // If Stage 8 was skipped (because an earlier stage failed),
            // this ensures the backend is still cleaned up.
            bat '''
                @echo off
                REM ── Safety net: kill any Java process on port 8080 ──────
                REM This runs even if previous stages failed, ensuring no
                REM zombie processes are left behind.
                for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING 2^>nul') do (
                    echo [Cleanup] Killing leftover process on port 8080: PID %%p
                    taskkill /PID %%p /F 2>nul
                )
            '''

            // ── Archive the backend log file ─────────────────────────────────
            // archiveArtifacts saves files from the workspace as downloadable
            // attachments on the build page. The backend log is useful for
            // debugging startup failures.
            //
            // allowEmptyArchive: true → don't fail if the log doesn't exist
            //   (it won't exist if the Start Backend stage was never reached)
            archiveArtifacts artifacts: 'backend_log.txt',
                             allowEmptyArchive: true

            // ── Archive ZAP reports as build artifacts ────────────────────────
            // These are downloadable from the build page, separate from the
            // HTML Publisher tabs. Useful for sharing reports or archiving.
            archiveArtifacts artifacts: 'zap-reports/**/*',
                             allowEmptyArchive: true
        }

        success {
            echo '''
╔══════════════════════════════════════════════════════════╗
║              PIPELINE COMPLETED SUCCESSFULLY             ║
╠══════════════════════════════════════════════════════════╣
║  ✅ Backend built and tested                             ║
║  ✅ ZAP Automation Framework scan completed              ║
║     (OpenAPI import → passive scan → active scan)        ║
║  ✅ Report published to Jenkins UI                       ║
║                                                          ║
║  Check the build page for:                               ║
║    📊 "Test Results" tab (JUnit)                         ║
║    🔒 "ZAP Security Scan Report" tab                     ║
╚══════════════════════════════════════════════════════════╝
'''
        }

        failure {
            echo '''
╔══════════════════════════════════════════════════════════╗
║                  PIPELINE FAILED                         ║
╠══════════════════════════════════════════════════════════╣
║  ❌ Check the Console Output above to find which stage   ║
║     failed and what the error message says.              ║
║                                                          ║
║  COMMON FIXES:                                           ║
║  • "gradlew.bat not found" → Checkout stage didn't copy  ║
║    files. Check the xcopy source path.                   ║
║  • "Port 8080 already in use" → Run:                     ║
║    netstat -ano | findstr :8080                          ║
║    taskkill /PID <pid> /F                                ║
║  • "ZAP not found" → Check ZAP_PATH in Jenkins global   ║
║    environment variables.                                ║
║  • ZAP exit code 1 → Check zap-automation.yaml syntax.   ║
║    Verify the OpenAPI spec URL is correct.               ║
║  • Build errors → Check Java compilation output.         ║
╚══════════════════════════════════════════════════════════╝
'''
        }
    }
}
