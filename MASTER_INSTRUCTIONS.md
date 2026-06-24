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

### STAGE 2 — GRADLE MIGRATION ✅ COMPLETE

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
    stage('ZAP Baseline Scan') { /* passive scan, -cmd mode */ }
    stage('ZAP Active Scan') { /* attack mode, per-endpoint */ }
    stage('Publish ZAP Report') { /* publishHTML + CSP fix */ }
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

**Deliverable:** `STAGE4_AUDIT.md`

---

### STAGE 5 — ZAP AUTOMATION FRAMEWORK CONFIG

**1. Create `/zap/zap-automation.yaml`**
- env section: target URLs, context definition
- jobs section: spider job, activeScan job, report job
- rules: enable 40018 (SQLi) and 40012/40014 (XSS)
- Explain why these rule IDs match what ZAP finds in this project

**2. Create `/zap_testing_guide.md` covering:**
1. DAST vs SAST: one concrete analogy
2. Manual ZAP workflow (paste URL → manual explore → launch browser →
   send payloads → active scan → report) — explain each step technically
3. What ZAP finds in THIS app — reference actual controller code
4. How the Jenkinsfile automates every manual step
5. How to read ZAP alerts: High/Medium/Low/Informational with examples

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
