# Stage 4 — Jenkinsfile (Windows-Specific) Audit Report

> [!NOTE]
> This stage creates the core CI/CD pipeline — a Jenkinsfile that automates
> the entire workflow from building the app to running security scans.
> Every command uses `bat()` (Windows batch) and every line is commented
> with teaching explanations.

---

## 1. Files Created

| # | File | Purpose |
|---|---|---|
| 1 | [Jenkinsfile](file:///c:/Users/nandu/OneDrive/Desktop/DAST/Jenkinsfile) | Declarative Jenkins Pipeline — automates build → test → scan → report |

## 2. Files Modified

None — the Jenkinsfile is a new file at the project root.

## 3. Files Deleted

None.

---

## 4. Pipeline Overview

### Pipeline Structure

```
pipeline {
    agent any
    environment { ZAP_PATH, BACKEND_URL, ZAP_PORT, REPORT_DIR }
    stages {
        1. Checkout         → Copy project files to Jenkins workspace
        2. Build Backend    → gradlew.bat build -x test
        3. Test Backend     → gradlew.bat test + JUnit publishing
        4. Start Backend    → start /b java -jar ... + port-wait loop
        5. ZAP Baseline     → Passive scan (headers, info disclosure)
        6. ZAP Active Scan  → Attack scan (SQLi, XSS payloads)
        7. Publish Report   → HTML Publisher plugin + CSP fix
        8. Stop Backend     → Find & kill process on port 8080
    }
    post {
        always  → Safety-net cleanup + archive artifacts
        success → Success message
        failure → Failure message + troubleshooting hints
    }
}
```

### Environment Variables

| Variable | Value | Purpose |
|---|---|---|
| `ZAP_PATH` | `${env.ZAP_PATH}` | ZAP install path (from Jenkins global env vars, set in Stage 3) |
| `BACKEND_URL` | `http://localhost:8080` | Where Spring Boot listens |
| `ZAP_PORT` | `8090` | ZAP proxy port (avoids conflict with 8080 and 9090) |
| `REPORT_DIR` | `${WORKSPACE}\\zap-reports` | Where ZAP HTML reports are saved |

---

## 5. Stage-by-Stage Details

### Stage 1: Checkout
- **What:** Copies project files from `C:\Users\nandu\OneDrive\Desktop\DAST\` to Jenkins workspace using `xcopy`
- **Why:** "Pipeline script" mode starts with an empty workspace (no SCM clone)
- **Windows gotcha:** Uses `/E /I /H /Y /Q` flags and an exclude file to skip `.git`, `.gradle`, `build`, `node_modules`, `target`
- **Future:** When project is on GitHub, replace with `git url: '...'` step

### Stage 2: Build Backend
- **What:** Runs `gradlew.bat build -x test` inside the `backend/` directory
- **Why:** Compiles Java code + packages into executable JAR (skips tests — they run separately)
- **Uses:** `dir('backend')` block to scope the working directory
- **Output:** JAR at `backend/build/libs/search-backend-0.0.1-SNAPSHOT.jar`

### Stage 3: Test Backend
- **What:** Runs `gradlew.bat test` + publishes results with `junit` step
- **Why:** Runs tests separately so failures show as "Test Backend" (not "Build Backend")
- **JUnit publishing:** Reads `backend/build/test-results/test/*.xml` → creates "Test Results" tab in Jenkins
- **Behavior:** Stage marked UNSTABLE (yellow) if tests fail — pipeline continues

### Stage 4: Start Backend
- **What:** Starts the Spring Boot JAR in the background, waits for port 8080
- **Why:** ZAP needs a running application to scan
- **How:**
  1. `for /r` finds the JAR file in `backend/build/libs/`
  2. `start /b java -jar` launches it in the background
  3. Output redirected to `backend_log.txt` for debugging
  4. Port-wait loop: checks `netstat` every 5 seconds, up to 12 attempts (60s)
  5. Health check: `curl` hits `/v3/api-docs` to verify Spring MVC is ready
- **Windows gotcha:** `start /b` = background with no new window

### Stage 5: ZAP Baseline Scan
- **What:** Passive scan — ZAP sends normal requests and analyzes responses
- **Finds:** Missing security headers, info disclosure, CORS issues, insecure cookies
- **Does NOT find:** SQLi, XSS (those require active attack payloads)
- **Command:** `zap.bat -cmd -quickurl -quickout -quickprogress`
- **Output:** `zap-reports/zap-baseline-report.html`

### Stage 6: ZAP Active Scan
- **What:** Attack scan — ZAP sends malicious payloads to every parameter
- **Finds:** SQL Injection (CWE-89), Reflected XSS (CWE-79), path traversal, etc.
- **Expected findings in our app:**
  | Endpoint | Finding | Root Cause |
  |---|---|---|
  | POST /api/login | SQL Injection | String concatenation in SQL query |
  | GET /api/greet | Reflected XSS | Raw HTML output with user input |
  | GET /api/echo | Reflected XSS | Raw HTML output with user input |
  | GET /api/search | XSS (lower risk) | JSON response format (safer) |
- **Output:** `zap-reports/zap-active-report.html`
- **Stage 5 integration:** Will switch to `-autorun zap-automation.yaml` after Stage 5

### Stage 7: Publish ZAP Report
- **What:** Makes ZAP reports viewable in Jenkins UI using HTML Publisher plugin
- **CSP fix:** Sets `hudson.model.DirectoryBrowserSupport.CSP` to empty string
  - Without this, Jenkins blocks inline CSS/JS → report looks broken/unstyled
  - Safe for localhost; in production, use a separate report viewer
- **Creates two report tabs:**
  1. "ZAP Baseline Scan Report"
  2. "ZAP Active Scan Report"

### Stage 8: Stop Backend
- **What:** Finds and kills the Java process listening on port 8080
- **How:** `netstat -ano | findstr :8080 | findstr LISTENING` → parse PID → `taskkill /PID /F`
- **Windows gotcha:** `FOR /F "tokens=5"` extracts the 5th column (PID) from netstat output
- **Verification:** Checks port 8080 is free after kill

### Post Block
- **always:** Safety-net kill of port 8080 process + archive `backend_log.txt` and `zap-reports/`
- **success:** ASCII art success banner with checklist
- **failure:** ASCII art failure banner with common fixes

---

## 6. Teaching Elements Included

| Concept | Where It's Taught |
|---|---|
| Declarative vs Scripted pipelines | File header comment |
| bat() vs sh() | File header + every stage |
| Double backslash escaping in Groovy | File header + REPORT_DIR |
| dir() vs bat('cd ...') | Build Backend stage |
| start /b for background processes | Start Backend stage |
| FOR /F parsing in Windows batch | Stop Backend stage |
| JUnit test result publishing | Test Backend stage |
| Content Security Policy (CSP) | Publish Report stage |
| Passive vs Active scanning | Baseline Scan + Active Scan stages |
| What ZAP finds in each endpoint | Active Scan stage (table) |
| archiveArtifacts vs publishHTML | Post block comments |
| Why centralize environment variables | Environment block |
| What ${WORKSPACE} resolves to | REPORT_DIR comment |
| Why java -jar instead of bootRun | Start Backend stage |

---

## 7. How to Run the Pipeline

### First Time Setup
1. **Jenkins is running** on port 9090 (verify: http://localhost:9090)
2. **Copy this Jenkinsfile** content into the DAST-Pipeline job:
   - Jenkins → DAST-Pipeline → Configure → Pipeline section
   - Paste the entire Jenkinsfile content into the Script text box
   - Click Save

### Running the Pipeline
1. Click **"Build Now"** in the left sidebar
2. Click the **build number** (e.g., #1) → **"Console Output"** to watch live
3. After completion, check:
   - **"Test Results"** tab → JUnit test results
   - **"ZAP Baseline Scan Report"** tab → passive scan findings
   - **"ZAP Active Scan Report"** tab → active scan findings

### Expected Behavior
- **Stages 1-4** (Checkout, Build, Test, Start) should pass on first run
- **Stages 5-6** (ZAP Scans) depend on ZAP being configured — may need path verification
- **Stage 7** (Publish Report) works even if reports are missing (`allowMissing: true`)
- **Stage 8** (Stop Backend) always runs to clean up

---

## 8. Common Errors and Fixes

| Error | Cause | Fix |
|---|---|---|
| `'gradlew.bat' is not recognized` | Checkout didn't copy files | Verify xcopy source path matches your project location |
| `Port 8080 already in use` | Previous run left a zombie process | `netstat -ano \| findstr :8080` → `taskkill /PID <pid> /F` |
| `ZAP not found` or `zap.bat not recognized` | ZAP_PATH env var wrong | Verify in Jenkins: Manage Jenkins → System → Environment variables |
| JUnit report empty | No test XML files | Check `backend/build/test-results/test/` exists |
| ZAP report unstyled/broken | CSP blocking inline CSS | The Publish Report stage handles this — check CSP fix ran |
| `Timeout waiting for port 8080` | App failed to start | Check `backend_log.txt` artifact for startup errors |

---

## 9. Connection to Stage 5

The Jenkinsfile includes TODO comments marking where the ZAP Automation Framework YAML (Stage 5) will plug in:

**Current approach (Stage 4):**
```groovy
bat '"%ZAP_PATH%\\zap.bat" -cmd -quickurl %BACKEND_URL% -quickout ...'
```

**After Stage 5:**
```groovy
bat '"%ZAP_PATH%\\zap.bat" -cmd -autorun "%WORKSPACE%\\zap\\zap-automation.yaml"'
```

The `-autorun` approach gives fine-grained control over:
- Which scan rules to enable (40018 for SQLi, 40012/40014 for XSS)
- Scan policies (how aggressive, how many payloads)
- Report format and content
- Spider configuration for endpoint discovery

---

## 10. Next Steps (Stage 5)

- [ ] Create `/zap/zap-automation.yaml` with spider, activeScan, and report jobs
- [ ] Create `/zap_testing_guide.md` with DAST concepts and manual ZAP workflow
- [ ] Update Jenkinsfile to use `-autorun` instead of `-quickurl`
