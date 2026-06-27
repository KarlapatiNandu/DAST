# Stage 5 - ZAP Automation Framework Config Audit Report

> Stage 5 updates the DAST workflow so Jenkins runs a ZAP Automation Framework plan instead of older `-quickurl` scans.
> The new workflow imports the Springdoc OpenAPI spec, spiders the local target, waits for passive analysis, runs active scan rules, and publishes one HTML report.

---

## 1. Files Created

| File | Purpose |
|---|---|
| `zap_testing_guide.md` | Beginner-friendly guide for DAST, manual ZAP usage, Jenkins automation, and alert severity |

---

## 2. Files Modified

| File | Change |
|---|---|
| `zap/zap-automation.yaml` | Rebuilt the ZAP Automation Framework plan with OpenAPI import, spider, passive wait, active scan policy, explicit SQLi/XSS rules, and report generation |
| `Jenkinsfile` | Updated the workflow to call `zap.bat -cmd -addonupdate -autorun "%ZAP_PLAN%"` and publish `zap-automation-report.html` |
| `ProjectV1.md` | Marked Stage 5 items complete and recorded the workflow update |

---

## 3. Workflow Change Summary

### Before Stage 5

The Jenkinsfile used `-quickurl` scans.
That approach starts from a single URL and depends on crawling.
It is weak for REST APIs because `http://localhost:8080/` has no real homepage and may return 404.

### After Stage 5

The Jenkinsfile now runs:

```bat
call "%ZAP_PATH%\\zap.bat" -cmd -addonupdate -autorun "%ZAP_PLAN%"
```

The YAML plan controls the full security workflow:

1. `openapi` imports `http://localhost:8080/v3/api-docs`.
2. `spider` crawls in-scope URLs under `http://localhost:8080`.
3. `passiveScan-wait` waits for passive analysis to finish.
4. `activeScan` sends attack payloads using the configured policy.
5. `report` writes `zap-reports/zap-automation-report.html`.

This is more scalable because Springdoc updates `/v3/api-docs` whenever Spring controllers change.
ZAP reads that updated spec on the next Jenkins run, so new API endpoints are scanned without editing the Jenkinsfile.

---

## 4. Rule IDs Enabled

| Rule ID | ZAP Rule | Project endpoint |
|---|---|---|
| `40018` | SQL Injection | `POST /api/login` in `LoginController.java` |
| `40012` | Cross Site Scripting - Reflected | `GET /api/greet` and `GET /api/echo` in `XssController.java` |
| `40014` | Cross Site Scripting - Persistent | Enabled for XSS coverage if stored-input endpoints are added later |

---

## 5. Expected Findings

| Endpoint | Expected issue | Why |
|---|---|---|
| `POST /api/login` | SQL Injection / CWE-89 | User input is concatenated into SQL and executed with `Statement` |
| `GET /api/greet` | Reflected XSS / CWE-79 | `name` is inserted into `text/html` without escaping |
| `GET /api/echo` | Reflected XSS / CWE-79 | `input` is inserted into `text/html` without escaping |
| `GET /api/search` | Reflected input in JSON | `query` is reflected in JSON, which is safer than HTML but still useful for teaching |

---

## 6. Verification Notes

I reviewed the workflow files for the required Stage 5 structure.
The Jenkinsfile now has one ZAP scan stage that runs the Automation Framework plan.
The YAML now includes the requested spider, active scan, report, and explicit SQLi/XSS rule configuration.

I did not run the Jenkins pipeline from this session because it requires the local Jenkins and ZAP Windows installation.
To verify manually, run the Jenkins job and confirm the build publishes a `ZAP Automation Report` tab.

---

## 7. Manual Verification Command

From a Windows terminal with the backend already running:

```bat
cd C:\Users\nandu\OneDrive\Desktop\DAST
"%ZAP_PATH%\zap.bat" -cmd -addonupdate -autorun "zap\zap-automation.yaml"
```

Expected output:

- ZAP imports `http://localhost:8080/v3/api-docs`.
- ZAP runs spider and active scan jobs.
- ZAP writes `zap-reports\zap-automation-report.html`.
- The report includes findings for the intentionally vulnerable endpoints.
