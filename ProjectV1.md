# DAST Learning Lab — Project Tracker v1.0

> **Purpose:** This document tracks every deliverable across all 7 stages.
> Update status as each item is completed. Use this as your session-to-session guide.

> [!IMPORTANT]
> **New Chat Session?** Read [MASTER_INSTRUCTIONS.md](file:///c:/Users/nandu/OneDrive/Desktop/DAST/MASTER_INSTRUCTIONS.md) first.
> It contains all global rules, stage definitions, and the complete original prompt.
> Copy the "How to Start a New Stage Chat" snippet from the bottom of that file.

---

## Session Quick-Start

Paste this into every new chat:
```
Read the following files from my DAST project before doing anything:
1. c:\Users\nandu\OneDrive\Desktop\DAST\MASTER_INSTRUCTIONS.md
2. c:\Users\nandu\OneDrive\Desktop\DAST\ProjectV1.md

Then execute the next incomplete stage. Follow all global rules
(line-by-line comments, Windows-only, teaching approach).
Create STAGE{N}_AUDIT.md in the project root when done.
Update ProjectV1.md to mark the stage complete.
```

---

## Overall Progress

| Stage | Name | Status |
|---|---|---|
| 1 | Project Audit | ✅ Complete |
| 2 | Gradle Migration | ✅ Complete |
| 2B | OpenAPI / Swagger Integration | ✅ Complete |
| 3 | Jenkins Installation & Configuration | ✅ Complete |
| 4 | Jenkinsfile (Windows) | ✅ Complete |
| 5 | ZAP Automation Framework Config | ✅ Complete |
| 6 | Secure Controllers | ⬜ Not Started |
| 7 | Interview Prep Document | ⬜ Not Started |

---

## Stage 1 — Project Audit ✅

- [x] Read and analyze pom.xml
- [x] List every dependency, plugin, and configuration
- [x] Create Maven → Gradle mapping table
- [x] List every file in `/backend/src`
- [x] Identify `@SpringBootApplication` main class (`SearchBackendApplication.java`)
- [x] Output audit report (no file changes)
- **Deliverable:** [STAGE1_AUDIT.md](file:///c:/Users/nandu/OneDrive/Desktop/DAST/STAGE1_AUDIT.md)

---

## Stage 2 — Gradle Migration ✅

- [x] Create `/backend/settings.gradle`
- [x] Create `/backend/build.gradle`
- [x] Generate Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/`)
- [x] Verify build works: `gradlew.bat build`
- [x] Delete `pom.xml` after confirming build (completed in Stage 2B)
- [x] Clean up `target/` directory (completed in Stage 2B — already clean)
- [x] Update `.gitignore` for Gradle

**Files to create:**
| File | Path |
|---|---|
| settings.gradle | `/backend/settings.gradle` |
| build.gradle | `/backend/build.gradle` |
| gradlew (generated) | `/backend/gradlew` |
| gradlew.bat (generated) | `/backend/gradlew.bat` |
| wrapper jar (generated) | `/backend/gradle/wrapper/gradle-wrapper.jar` |
| wrapper props (generated) | `/backend/gradle/wrapper/gradle-wrapper.properties` |

---

## Stage 2B — OpenAPI / Swagger Integration ✅

- [x] Add `springdoc-openapi-starter-webmvc-ui:2.3.0` to `build.gradle`
- [x] Add Springdoc properties to `application.properties`
- [x] Add `@Operation` + `@Parameter` annotations to `LoginController`
- [x] Add `@Operation` + `@Parameter` annotations to `XssController` (greet + echo)
- [x] Add `@Operation` + `@Parameter` annotations to `SearchController`
- [x] Create `OpenApiConfig.java` config class
- [ ] Verify Swagger UI at `http://localhost:8080/swagger-ui.html`
- [ ] Verify raw spec at `http://localhost:8080/v3/api-docs`
- [x] Provide ZAP Automation Framework YAML snippet for OpenAPI import
- [x] Write "How does your pipeline scale?" interview talking point
- **Deliverable:** [STAGE2B_AUDIT.md](file:///c:/Users/nandu/OneDrive/Desktop/DAST/STAGE2B_AUDIT.md)

**Files created/modified:**
| Action | File | Path |
|---|---|---|
| MODIFY | build.gradle | `/backend/build.gradle` |
| MODIFY | application.properties | `/backend/src/main/resources/application.properties` |
| MODIFY | LoginController.java | `/backend/src/main/java/com/dast/demo/controller/LoginController.java` |
| MODIFY | XssController.java | `/backend/src/main/java/com/dast/demo/controller/XssController.java` |
| MODIFY | SearchController.java | `/backend/src/main/java/com/dast/demo/controller/SearchController.java` |
| NEW | OpenApiConfig.java | `/backend/src/main/java/com/dast/demo/config/OpenApiConfig.java` |
| DELETE | pom.xml | `/backend/pom.xml` (deferred from Stage 2) |

---

## Stage 3 — Jenkins Installation & Configuration ✅

- [x] Create `JENKINS_SETUP.md` at project root
  - [x] Section 1: Installing Jenkins on Windows (jenkins.war, port 9090)
  - [x] Section 2: Configuring Gradle in Jenkins (recommend wrapper)
  - [x] Section 3: Configuring ZAP path as Jenkins env variable
  - [x] Section 4: Creating a Pipeline job in Jenkins UI
- **Deliverable:** [STAGE3_AUDIT.md](file:///c:/Users/nandu/OneDrive/Desktop/DAST/STAGE3_AUDIT.md)

**Files created:**
| File | Path |
|---|---|
| JENKINS_SETUP.md | `/JENKINS_SETUP.md` |

---

## Stage 4 — Jenkinsfile (Windows-specific) Needs Revision

- [x] Create `Jenkinsfile` at project root
  - [x] `environment` block with ZAP_PATH, BACKEND_URL, ZAP_PORT, REPORT_DIR
  - [x] Stage: Checkout
  - [x] Stage: Build Backend (`gradlew.bat build -x test`)
  - [x] Stage: Test Backend (`gradlew.bat test` + junit publishing)
  - [x] Stage: Start Backend (background `start /b`, port-wait loop)
  - [x] Stage: ZAP Automation Scan (Stage 5 update: `zap.bat -cmd -addonupdate -autorun "%ZAP_PLAN%"`)
  - [x] Stage: Publish ZAP Report (HTML Publisher plugin + CSP fix)
  - [x] Stage: Stop Backend (find & kill Java process on port 8080)
  - [x] `post` block: always/success/failure handlers
- **Deliverable:** [STAGE4_AUDIT.md](file:///c:/Users/nandu/OneDrive/Desktop/DAST/STAGE4_AUDIT.md)

**Files created:**
| File | Path |
|---|---|
| Jenkinsfile | `/Jenkinsfile` |

---

## Stage 5 — ZAP Automation Framework Config ✅

- [x] Create `/zap/zap-automation.yaml`
  - [x] `env` section: target URLs, context
  - [x] `jobs` section: OpenAPI import, spider, passiveScan-wait, activeScan, report
  - [x] Rules: 40018 (SQLi), 40012/40014 (XSS)
- [x] Update `/Jenkinsfile` workflow to run `zap.bat -cmd -addonupdate -autorun "%ZAP_PLAN%"`
- [x] Publish `zap-automation-report.html` as the Jenkins ZAP Automation Report
- [x] Create `/zap_testing_guide.md`
  - [x] DAST vs SAST analogy
  - [x] Manual ZAP workflow documentation
  - [x] What ZAP finds in THIS app (with controller references)
  - [x] How Jenkinsfile automates each manual step
  - [x] How to read ZAP alert severity levels
- **Deliverable:** [STAGE5_AUDIT.md](file:///c:/Users/nandu/OneDrive/Desktop/DAST/STAGE5_AUDIT.md)

**Files to create:**
| File | Path |
|---|---|
| zap-automation.yaml | `/zap/zap-automation.yaml` |
| zap_testing_guide.md | `/zap_testing_guide.md` |
| STAGE5_AUDIT.md | `/STAGE5_AUDIT.md` |

---

## Stage 6 — Secure Controllers ⬜

- [ ] Create `SecureLoginController.java` → `/api/secure/login` (PreparedStatement fix)
- [ ] Create `SecureXssController.java` → `/api/secure/greet` (HtmlUtils.htmlEscape + CSP header)
- [ ] Create `SecureSearchController.java` → `/api/secure/search` (explain JSON safety)
- [ ] Each file has vulnerability comment block at top

**Files to create:**
| File | Path |
|---|---|
| SecureLoginController.java | `/backend/src/main/java/com/dast/demo/controller/secure/SecureLoginController.java` |
| SecureXssController.java | `/backend/src/main/java/com/dast/demo/controller/secure/SecureXssController.java` |
| SecureSearchController.java | `/backend/src/main/java/com/dast/demo/controller/secure/SecureSearchController.java` |

---

## Stage 7 — Interview Prep Document ⬜

- [ ] Create `INTERVIEW_PREP.md` at project root
  - [ ] Q1: Walk me through this project
  - [ ] Q2: DAST vs SAST
  - [ ] Q3: CI/CD and security testing
  - [ ] Q4: SQL Injection (LoginController code)
  - [ ] Q5: Reflected XSS (XssController code)
  - [ ] Q6: CWE-89, CWE-79, CWE-256, OWASP A03:2021
  - [ ] Q7: Why ZAP didn't detect SQLi before H2
  - [ ] Q8: @CrossOrigin(origins="*")
  - [ ] Q9: PreparedStatement from memory
  - [ ] Q10: HtmlUtils.htmlEscape() behavior
  - [ ] Q11: Jenkins pipeline stage-by-stage
  - [ ] Q12: ZAP baseline vs active scan
  - [ ] Q13: Spring Boot annotations
  - [ ] Q14: Maven vs Gradle, why switch
  - [ ] Q15: Production-ready additions
  - [ ] Q16: H2 database purpose
  - [ ] Q17: Handling false positives

**Files to create:**
| File | Path |
|---|---|
| INTERVIEW_PREP.md | `/INTERVIEW_PREP.md` |

---

## Full File Manifest (All Stages Combined)

| # | Action | File | Stage |
|---|---|---|---|
| 1 | NEW | `/backend/settings.gradle` | 2 |
| 2 | NEW | `/backend/build.gradle` | 2 |
| 3 | GENERATE | `/backend/gradlew` + `gradlew.bat` + `gradle/wrapper/*` | 2 |
| 4 | DELETE | `/backend/pom.xml` | 2 |
| 5 | MODIFY | `/backend/build.gradle` (add springdoc dep) | 2B |
| 6 | MODIFY | `/backend/src/main/resources/application.properties` | 2B |
| 7 | MODIFY | `LoginController.java` (annotations only) | 2B |
| 8 | MODIFY | `XssController.java` (annotations only) | 2B |
| 9 | MODIFY | `SearchController.java` (annotations only) | 2B |
| 10 | NEW | `OpenApiConfig.java` | 2B |
| 11 | NEW | `/JENKINS_SETUP.md` | 3 |
| 12 | NEW | `/Jenkinsfile` | 4 |
| 13 | NEW | `/zap/zap-automation.yaml` | 5 |
| 14 | NEW | `/zap_testing_guide.md` | 5 |
| 15 | NEW | `SecureLoginController.java` | 6 |
| 16 | NEW | `SecureXssController.java` | 6 |
| 17 | NEW | `SecureSearchController.java` | 6 |
| 18 | NEW | `/INTERVIEW_PREP.md` | 7 |

**Total: 18 file operations across 7 stages**
