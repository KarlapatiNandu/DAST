# DAST Learning Lab â€” Master Instructions

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
â”œâ”€â”€ backend/                          â† Spring Boot (Java 17)
â”‚   â”œâ”€â”€ build.gradle                  â† Gradle build file (created in Stage 2)
â”‚   â”œâ”€â”€ settings.gradle               â† Gradle settings (created in Stage 2)
â”‚   â”œâ”€â”€ gradlew.bat                   â† Gradle wrapper for Windows
â”‚   â””â”€â”€ src/main/java/com/dast/demo/
â”‚       â”œâ”€â”€ SearchBackendApplication.java  â† Main class (@SpringBootApplication)
â”‚       â”œâ”€â”€ config/
â”‚       â”‚   â””â”€â”€ OpenApiConfig.java         â† OpenAPI config (Stage 2B)
â”‚       â”œâ”€â”€ controller/
â”‚       â”‚   â”œâ”€â”€ LoginController.java       â† POST /api/login (SQLi - CWE-89)
â”‚       â”‚   â”œâ”€â”€ XssController.java         â† GET /api/greet, /api/echo (XSS - CWE-79)
â”‚       â”‚   â”œâ”€â”€ SearchController.java      â† GET /api/search (XSS in JSON)
â”‚       â”‚   â””â”€â”€ secure/                    â† Fixed versions (Stage 6)
â”‚       â”‚       â”œâ”€â”€ SecureLoginController.java
â”‚       â”‚       â”œâ”€â”€ SecureXssController.java
â”‚       â”‚       â””â”€â”€ SecureSearchController.java
â”‚       â”œâ”€â”€ model/
â”‚       â”‚   â”œâ”€â”€ LoginRequest.java          â† POJO (unused currently)
â”‚       â”‚   â””â”€â”€ SearchResponse.java        â† POJO for search JSON
â”‚       â””â”€â”€ resources/
â”‚           â”œâ”€â”€ application.properties     â† Port 8080, H2 config
â”‚           â”œâ”€â”€ schema.sql                 â† CREATE TABLE users
â”‚           â””â”€â”€ data.sql                   â† 4 test users (plaintext passwords)
â”œâ”€â”€ frontend/                         â† React + Vite (no changes needed)
â”œâ”€â”€ zap/
â”‚   â””â”€â”€ zap-automation.yaml           â† ZAP automation config (Stage 5)
â”œâ”€â”€ Jenkinsfile                       â† CI pipeline (Stage 4)
â”œâ”€â”€ JENKINS_SETUP.md                  â† Jenkins install guide (Stage 3)
â”œâ”€â”€ INTERVIEW_PREP.md                 â† Q&A document (Stage 7)
â”œâ”€â”€ zap_testing_guide.md              â† ZAP manual + concepts (Stage 5)
â”œâ”€â”€ MASTER_INSTRUCTIONS.md            â† THIS FILE â€” read first every session
â”œâ”€â”€ ProjectV1.md                      â† Progress tracker â€” update every session
â”œâ”€â”€ STAGE1_AUDIT.md                   â† Stage 1 output
â”œâ”€â”€ STAGE2_AUDIT.md                   â† Stage 2 output (created after Stage 2)
â””â”€â”€ ...                               â† Additional stage audit files
```

### 4. Vulnerabilities in the App
| CWE | Vulnerability | Controller | Line(s) |
|---|---|---|---|
| CWE-89 | SQL Injection | LoginController.java | 130-131 (string concat into SQL) |
| CWE-79 | Reflected XSS | XssController.java | 121 (greet), 152 (echo) â€” raw HTML |
| CWE-79 | XSS (JSON) | SearchController.java | 76 â€” JSON mitigates execution |
| CWE-256 | Plaintext Passwords | data.sql | 12-16 |
| CWE-209 | Info Disclosure | LoginController.java | 171 (SQL error in response) |
| â€” | CORS Wildcard | All controllers | `@CrossOrigin(origins = "*")` |

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

### STAGE 1 â€” PROJECT AUDIT (no code changes) âœ… COMPLETE

- Read the existing pom.xml carefully
- List every dependency, plugin, and configuration in pom.xml
- Output a plain mapping table: Maven concept â†’ Gradle equivalent
- List every file in /backend/src to understand what exists
- Identify the main class annotated with @SpringBootApplication
- Output: audit report only, no file changes

**Deliverable:** `STAGE1_AUDIT.md`

---

### STAGE 2 â€” GRADLE MIGRATION

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

### STAGE 2B â€” OPENAPI / SWAGGER INTEGRATION

Add Springdoc OpenAPI to the backend for automatic endpoint discovery.
This enables ZAP to find all endpoints from the spec instead of
hardcoded URLs in the Jenkinsfile.

**1. Add to build.gradle dependencies block:**
```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
```
- Explain: what Springdoc does (reads Spring annotations, generates spec)
- Explain: why version 2.x is required for Spring Boot 3.x
  (version 1.x only works with Spring Boot 2.x â€” common mistake)

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
  swagger-ui.html â†’ visual list of all endpoints with try-it-out buttons
  v3/api-docs â†’ raw JSON that ZAP will consume
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
Answer should reference: Springdoc reads @RestController annotations â†’
generates spec â†’ ZAP imports spec â†’ new endpoints auto-scanned.
No manual Jenkinsfile updates needed.

**Deliverable:** `STAGE2B_AUDIT.md`

---

### STAGE 3 â€” JENKINS INSTALLATION AND CONFIGURATION

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
- Jenkins â†’ Manage Jenkins â†’ Tools â†’ Gradle installations
- Why Jenkins needs to know where Gradle is
- Alternative: use the wrapper (gradlew.bat) â€” recommend this and explain why

**3. Configuring ZAP path in Jenkins**
- Jenkins â†’ Manage Jenkins â†’ System â†’ Global properties â†’ Environment variables
- Add: `ZAP_PATH = C:\Program Files\OWASP\Zed Attack Proxy`
  (user must verify this path)
- Why store here vs hardcoding in Jenkinsfile

**4. Creating a Pipeline job in Jenkins UI**
- Jenkins â†’ New Item â†’ Pipeline (not Freestyle)
- Explain Freestyle vs Pipeline jobs
- "Pipeline script from SCM" vs "Pipeline script" â€”
  recommend "Pipeline script" first, explain what SCM means

**Deliverable:** `STAGE3_AUDIT.md`

---

### STAGE 4 â€” JENKINSFILE (Windows-specific)

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

### STAGE 5 â€” ZAP AUTOMATION FRAMEWORK CONFIG

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
2. Manual ZAP workflow (paste URL â†’ manual explore â†’ launch browser â†’
   send payloads â†’ active scan â†’ report) â€” explain each step technically
3. What ZAP finds in THIS app â€” reference actual controller code
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

### STAGE 6 â€” SECURE CONTROLLERS

Create new controllers in `backend/src/main/java/com/dast/demo/controller/secure/`

**1. SecureLoginController â†’ /api/secure/login**
Fix: PreparedStatement with ? placeholders

**2. SecureXssController â†’ /api/secure/greet**
Fix: HtmlUtils.htmlEscape() + Content-Security-Policy response header

**3. SecureSearchController â†’ /api/secure/search**
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

### STAGE 7 â€” INTERVIEW PREP DOCUMENT

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
