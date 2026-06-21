# DAST Learning Lab — Spring Boot + React

A minimal full-stack application for learning **Spring Boot REST APIs** and **OWASP ZAP** security testing. Intentionally simple and deliberately left without security hardening so you can find and understand real vulnerabilities.

---

## Project Structure

```
DAST/
├── backend/                          ← Spring Boot (Java)
│   ├── pom.xml                       ← Maven build file (like package.json for Java)
│   └── src/main/java/com/dast/demo/
│       ├── SearchBackendApplication.java  ← MODULE 1: App entry point
│       ├── model/
│       │   └── SearchResponse.java        ← MODULE 2: Response data model
│       ├── controller/
│       │   └── SearchController.java      ← MODULE 3: REST endpoint
│       └── resources/
│           └── application.properties     ← MODULE 4: Config
│
└── frontend/                         ← React (JavaScript)
    ├── index.html                    ← HTML shell (just the <div id="root">)
    ├── vite.config.js                ← MODULE 7: Dev server config + proxy
    └── src/
        ├── main.jsx                  ← MODULE 5: React entry point
        ├── App.jsx                   ← MODULE 6: The entire UI + API call
        ├── App.css                   ← Styles
        └── index.css                 ← Global reset
```

---

## Prerequisites

| Tool | Version | Download |
|---|---|---|
| Java JDK | 17 or later | https://adoptium.net |
| Maven | 3.8+ | https://maven.apache.org (or use `mvnw` wrapper) |
| Node.js | 18+ | https://nodejs.org |
| OWASP ZAP | Latest | https://www.zaproxy.org/download/ |

Check your versions:
```powershell
java -version
mvn -version
node --version
```

---

## Running the App

### Step 1 — Start the Spring Boot Backend

```powershell
cd DAST\backend
mvn spring-boot:run
```

You should see:
```
Tomcat started on port 8080 (http)
Started SearchBackendApplication in X.XXX seconds
```

**Test it directly** (open in browser or use PowerShell):
```powershell
Invoke-WebRequest "http://localhost:8080/api/search?query=hello" | Select-Object -ExpandProperty Content
# Expected: {"result" : "you searched for: hello"}
```

---

### Step 2 — Start the React Frontend

Open a **second terminal**:

```powershell
cd DAST\frontend
npm run dev
```

You should see:
```
VITE ready in XXXms
➜  Local:   http://localhost:5173/
```

Open **http://localhost:5173** in your browser.

---

## How They Connect

```
Browser ──fetch──▶ http://localhost:8080/api/search?query=...
                            │
                   Spring Boot Controller
                            │
                   Returns: {"result": "you searched for: ..."}
                            │
Browser ◀──JSON──────────────
```

CORS is enabled with `@CrossOrigin(origins = "*")` so the browser on `:5173` can call the backend on `:8080`.

---

## Module Learning Guide

Read the source files in this order to understand how it all fits together:

| Order | File | What you learn |
|---|---|---|
| 1 | `SearchBackendApplication.java` | `@SpringBootApplication`, entry point |
| 2 | `pom.xml` | Maven dependencies, starter-web |
| 3 | `application.properties` | Server config, logging |
| 4 | `SearchResponse.java` | Java POJOs, Jackson JSON serialization |
| 5 | `SearchController.java` | `@RestController`, `@GetMapping`, `@RequestParam`, `@CrossOrigin` |
| 6 | `main.jsx` | React 18 entry point, `createRoot` |
| 7 | `App.jsx` | `useState`, `useCallback`, `fetch`, JSX, controlled inputs |
| 8 | `vite.config.js` | Vite dev server, proxy configuration |

---

## Testing with OWASP ZAP

See the full guide: **`zap_testing_guide.md`** (in the artifacts/docs folder)

**Quick start:**
1. Start both servers (above)
2. Open ZAP → Tools → Spider → `http://localhost:8080`
3. Right-click `/api/search` → Attack → Active Scan
4. View Alerts tab for findings

---

## What Vulnerabilities Will ZAP Find?

| Vulnerability | Severity | Root Cause in Our Code |
|---|---|---|
| Missing security headers | Medium | No Spring Security configured |
| CORS misconfiguration | Medium | `origins = "*"` in `@CrossOrigin` |
| Missing CSRF protection | Medium | No Spring Security |
| Reflected XSS potential | Low | Query echoed without sanitization |
| Server version disclosure | Info | Spring Boot's default error pages |

These are **intentional** so you can learn to identify and fix them.

---

## Ports Summary

| Service | Port | URL |
|---|---|---|
| Spring Boot | 8080 | http://localhost:8080 |
| React Dev Server | 5173 | http://localhost:5173 |
| ZAP Proxy | 8090 | Configure browser to use this |
