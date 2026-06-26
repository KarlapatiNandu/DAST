# Stage 3 — Jenkins Installation & Configuration Audit Report

> [!NOTE]
> This stage is **documentation only** — no code changes, no Jenkinsfile yet.
> We created a comprehensive Jenkins setup guide for Windows, covering
> installation, Gradle integration, ZAP path configuration, and pipeline job creation.

---

## 1. Files Created

| # | File | Purpose |
|---|---|---|
| 1 | [JENKINS_SETUP.md](file:///c:/Users/nandu/OneDrive/Desktop/DAST/JENKINS_SETUP.md) | Step-by-step Jenkins setup guide for Windows |

## 2. Files Modified

None — this stage is purely documentation.

## 3. Files Deleted

None.

---

## 4. What JENKINS_SETUP.md Covers

### Section 1: Installing Jenkins on Windows
- **Why `jenkins.war` over the Windows `.msi` installer**: The .war file runs as your user account (same permissions as your terminal), while the .msi installer runs as `NT AUTHORITY\SYSTEM` (can't find your Java/Gradle/ZAP).
- **Download and launch command**: `java -jar jenkins.war --httpPort=9090`
- **Port 9090**: Avoids conflict with Spring Boot on port 8080.
- **First-time setup**: Where to find the initial admin password (`%USERPROFILE%\.jenkins\secrets\initialAdminPassword`), how to install suggested plugins, and creating an admin user.
- **Additional plugins**: HTML Publisher (for ZAP reports) and Gradle (for build integration).
- **Agent vs Controller**: On a single machine, the controller IS the agent (built-in node). `agent any` in the Jenkinsfile means "run on whatever's available" — which is our local machine.

### Section 2: Configuring Gradle in Jenkins
- **Two options**: Global Gradle installation vs. Gradle wrapper (`gradlew.bat`).
- **Wrapper is recommended**: It locks the Gradle version in `gradle-wrapper.properties`, so everyone (developers, Jenkins) uses the same version.
- **Jenkinsfile implication**: We call `gradlew.bat` directly, not `gradle`, so Jenkins doesn't need a global Gradle install.

### Section 3: Configuring ZAP Path in Jenkins
- **Why an environment variable**: Avoids hardcoding the ZAP install path in the Jenkinsfile. Each machine can have ZAP in a different folder.
- **Where to set it**: Manage Jenkins → System → Global properties → Environment variables → `ZAP_PATH = C:\Program Files\OWASP\Zed Attack Proxy`
- **How the Jenkinsfile reads it**: `${env.ZAP_PATH}` in the `environment` block.
- **Why Jenkins env vars over Windows system env vars**: Scoped to Jenkins only, no restart needed, changeable from the UI.

### Section 4: Creating a Pipeline Job
- **Job type explained**: Freestyle vs Pipeline vs Multibranch Pipeline. We use Pipeline because our workflow has 8 stages with error handling and shared environment variables.
- **"Pipeline script" vs "Pipeline script from SCM"**: Start with "Pipeline script" (paste directly), move to SCM when code is on GitHub.
- **Test pipeline included**: A 3-stage Jenkinsfile that verifies Java, Gradle, and ZAP are accessible. This lets the user validate their Jenkins setup before Stage 4.
- **Common errors**: 4 error scenarios with exact fixes (Java not found, Gradle not found, ZAP path wrong, port conflict).

---

## 5. Teaching Elements Included

Every section follows the "teaching-first" approach:

| Concept | Where It's Taught |
|---|---|
| Why .war over .msi | Section 1.1 — comparison table + permission explanation |
| What port conflicts are | Section 1.3 — why 9090 not 8080 |
| What SCM means | Section 4.2 — "Source Code Management" = Git |
| Agent vs Controller | Section 1.6 — single-machine diagram |
| Gradle wrapper purpose | Section 2.2 — version consistency diagram |
| Jenkins env vars vs System env vars | Section 3.4 — scope comparison table |
| Freestyle vs Pipeline jobs | Section 4.1 — feature comparison table |

---

## 6. Prerequisites Verified

The guide includes a prerequisites checklist that the user should verify before proceeding:

| Requirement | Verification Command |
|---|---|
| Java 17+ | `java -version` |
| Gradle wrapper | `cd backend && .\gradlew.bat --version` |
| ZAP installed | `Test-Path "C:\Program Files\OWASP\Zed Attack Proxy\zap.bat"` |
| Spring Boot builds | `cd backend && .\gradlew.bat build` |
| Port 8080 free | `netstat -ano \| findstr :8080` |
| Port 9090 free | `netstat -ano \| findstr :9090` |

---

## 7. Test Pipeline Provided

A minimal 3-stage test pipeline is included in the guide so the user can verify their Jenkins setup works before creating the full Jenkinsfile in Stage 4:

```groovy
pipeline {
    agent any
    stages {
        stage('Check Java')    { steps { bat('java -version') } }
        stage('Check Gradle')  { steps { dir('backend') { bat('.\\gradlew.bat --version') } } }
        stage('Check ZAP Path') { steps { bat('echo ZAP is at: %ZAP_PATH%') } }
    }
}
```

This confirms:
- ✅ Jenkins can run `bat()` commands
- ✅ Java is on the PATH
- ✅ Gradle wrapper is accessible
- ✅ ZAP_PATH environment variable is set correctly

---

## 8. Next Steps (Stage 4)

- [ ] Create `Jenkinsfile` at the project root
  - [ ] `environment` block with ZAP_PATH, BACKEND_URL, ZAP_PORT, REPORT_DIR
  - [ ] Stage: Checkout
  - [ ] Stage: Build Backend (`gradlew.bat build -x test`)
  - [ ] Stage: Test Backend (`gradlew.bat test` + junit publishing)
  - [ ] Stage: Start Backend (background `start /b`, port-wait loop)
  - [ ] Stage: ZAP Baseline Scan (passive, `-cmd` mode)
  - [ ] Stage: ZAP Active Scan (attack mode, per-endpoint)
  - [ ] Stage: Publish ZAP Report (HTML Publisher plugin + CSP fix)
  - [ ] Stage: Stop Backend (find & kill Java process on port 8080)
  - [ ] `post` block: always/success/failure handlers
