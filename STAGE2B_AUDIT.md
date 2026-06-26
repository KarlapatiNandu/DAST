# Stage 2B — OpenAPI / Swagger Integration Audit Report

> [!NOTE]
> This stage added Springdoc OpenAPI to the Spring Boot backend.
> All endpoints are now documented in an auto-generated OpenAPI 3.0 spec.
> ZAP can import this spec to auto-discover every endpoint without hardcoded URLs.

---

## 1. Files Created

| # | File | Purpose |
|---|---|---|
| 1 | [OpenApiConfig.java](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/config/OpenApiConfig.java) | API metadata + server URL for ZAP targeting |

## 2. Files Modified

| # | File | Change |
|---|---|---|
| 1 | [build.gradle](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/build.gradle) | Added `springdoc-openapi-starter-webmvc-ui:2.3.0` dependency |
| 2 | [application.properties](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/resources/application.properties) | Added Springdoc configuration (api-docs path, swagger-ui path, enabled) |
| 3 | [LoginController.java](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/controller/LoginController.java) | Added `@Operation` + `@Parameter` annotations (no logic change) |
| 4 | [XssController.java](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/controller/XssController.java) | Added `@Operation` + `@Parameter` to both `greet()` and `echo()` (no logic change) |
| 5 | [SearchController.java](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/controller/SearchController.java) | Added `@Operation` + `@Parameter` annotations (no logic change) |

## 3. Files Deleted (Deferred from Stage 2)

| # | File | Reason |
|---|---|---|
| 1 | `backend/pom.xml` | Maven build file — replaced by `build.gradle`. Keeping both causes confusion. |
| 2 | `backend/target/` | Maven's build output — Gradle uses `build/` instead. Already clean (didn't exist). |

---

## 4. Build Verification

### Command Run
```powershell
cd c:\Users\nandu\OneDrive\Desktop\DAST\backend
.\gradlew.bat build
```

### Result: ✅ BUILD SUCCESSFUL
```
> Task :compileJava
> Task :processResources
> Task :classes
> Task :resolveMainClassName
> Task :bootJar
> Task :jar
> Task :assemble
> Task :compileTestJava NO-SOURCE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test NO-SOURCE
> Task :check UP-TO-DATE
> Task :build

BUILD SUCCESSFUL in 5s
5 actionable tasks: 5 executed
```

All 5 tasks executed (full recompile) — confirms the new Springdoc dependency resolved correctly and all `@Operation`/`@Parameter` annotations compiled without errors.

---

## 5. What Was Added to Each Controller

### LoginController.java — `POST /api/login`

```java
@Operation(
    summary = "Vulnerable login endpoint",
    description = "Demonstrates SQL Injection - CWE-89. ..."
)
@PostMapping("/login")
public Map<String, Object> login(
    @Parameter(description = "Username - injectable via SQL concatenation")
    @RequestParam(value = "username", defaultValue = "") String username,
    @Parameter(description = "Password field - also injectable")
    @RequestParam(value = "password", defaultValue = "") String password
) {
```

**No logic changes.** The `@Operation` and `@Parameter` annotations only add metadata to the OpenAPI spec. The SQL injection vulnerability is untouched.

### XssController.java — `GET /api/greet` + `GET /api/echo`

```java
// greet endpoint
@Operation(
    summary = "Vulnerable greeting endpoint",
    description = "Demonstrates Reflected XSS - CWE-79. ..."
)
@GetMapping(value = "/greet", produces = MediaType.TEXT_HTML_VALUE)
public String greet(
    @Parameter(description = "Name field - injectable via unsanitized HTML embedding")
    @RequestParam(...) String name
) {

// echo endpoint
@Operation(
    summary = "Vulnerable echo endpoint",
    description = "Second Reflected XSS example - CWE-79. ..."
)
@GetMapping(value = "/echo", produces = MediaType.TEXT_HTML_VALUE)
public String echo(
    @Parameter(description = "Input text - echoed directly into HTML without escaping")
    @RequestParam(...) String input
) {
```

**No logic changes.** Both XSS vulnerabilities are untouched.

### SearchController.java — `GET /api/search`

```java
@Operation(
    summary = "Vulnerable search endpoint",
    description = "Demonstrates Reflected XSS in JSON context. ..."
)
@GetMapping("/search")
public SearchResponse search(
    @Parameter(description = "Search query - injectable, reflected in JSON response")
    @RequestParam(...) String query
) {
```

**No logic changes.** The JSON reflection behavior is untouched.

---

## 6. OpenApiConfig.java Explained

```java
@Configuration                          // Spring scans this class on startup
@OpenAPIDefinition(
    info = @Info(
        title = "DAST Learning Lab API",
        description = "Intentionally vulnerable API for security testing...",
        version = "1.0"
    ),
    servers = {
        @Server(
            url = "http://localhost:8080",     // ← ZAP reads this!
            description = "Local development server"
        )
    }
)
public class OpenApiConfig { }          // Empty class — annotations do all the work
```

### Why the Server URL Matters for ZAP

When ZAP imports the OpenAPI spec from `/v3/api-docs`, it reads the `servers` array to know **where to send attack requests**. Without a server entry:

```
ZAP reads:  POST /api/login  ← knows the PATH
ZAP asks:   "but WHERE is /api/login hosted?"  ← no server URL = can't attack
```

With the server entry:
```
ZAP reads:  POST /api/login  ← knows the PATH
ZAP reads:  servers[0].url = http://localhost:8080  ← knows the HOST
ZAP builds: http://localhost:8080/api/login  ← full attack URL ✅
```

---

## 7. Springdoc Version Compatibility

> [!WARNING]
> **Common mistake:** Using Springdoc 1.x with Spring Boot 3.x.

| Springdoc Version | Works with Spring Boot | Package Namespace |
|---|---|---|
| 1.x (`springdoc-openapi-ui`) | 2.x only | `javax.servlet.*` |
| **2.x** (`springdoc-openapi-starter-webmvc-ui`) | **3.x only** | `jakarta.servlet.*` |

Spring Boot 3.x migrated from `javax.*` to `jakarta.*` (Jakarta EE). Springdoc 1.x imports `javax.servlet.http.HttpServletRequest` which doesn't exist in Spring Boot 3.x → `ClassNotFoundException`.

**Our project:** Spring Boot 3.2.5 → Springdoc 2.3.0 ✅

---

## 8. Verification Steps (Manual)

After running `.\gradlew.bat bootRun`:

| Step | URL | What to Look For |
|---|---|---|
| 1 | http://localhost:8080/swagger-ui.html | Visual API explorer with all endpoints listed |
| 2 | http://localhost:8080/v3/api-docs | Raw OpenAPI 3.0 JSON spec |
| 3 | Expand `POST /api/login` in Swagger UI | See `@Operation` summary + `@Parameter` descriptions |
| 4 | Expand `GET /api/greet` in Swagger UI | See the XSS vulnerability description |
| 5 | Expand `GET /api/search` in Swagger UI | See the JSON XSS description |
| 6 | Click "Try it out" on any endpoint | Test the endpoint directly from the browser |

### Expected Endpoints in Swagger UI

| Method | Path | Controller | Description |
|---|---|---|---|
| POST | `/api/login` | LoginController | Vulnerable login endpoint (SQLi) |
| GET | `/api/greet` | XssController | Vulnerable greeting endpoint (XSS) |
| GET | `/api/echo` | XssController | Vulnerable echo endpoint (XSS) |
| GET | `/api/search` | SearchController | Vulnerable search endpoint (XSS in JSON) |

> [!NOTE]
> The H2 Console (`/h2-console`) will NOT appear in Swagger UI because it's a Spring Boot built-in servlet, not a `@RestController` endpoint. Springdoc only scans Spring MVC controllers.

---

## 9. ZAP Automation Framework YAML Snippet

This snippet tells ZAP to import the OpenAPI spec instead of hardcoding URLs:

```yaml
# Add this to your ZAP Automation Framework config (Stage 5)
# in the "jobs" array:
- type: openapi
  parameters:
    # URL where the OpenAPI 3.0 JSON spec is served
    # ZAP fetches this, parses all endpoints, and adds them to its scan target list
    apiUrl: http://localhost:8080/v3/api-docs

    # The base URL to send attack requests to
    # This should match the server URL in OpenApiConfig.java
    targetUrl: http://localhost:8080
```

### What This Replaces

**Without OpenAPI (manual URL list in Jenkinsfile):**
```groovy
// You'd have to maintain this list manually:
bat("zap.bat -cmd -quickurl http://localhost:8080/api/login")
bat("zap.bat -cmd -quickurl http://localhost:8080/api/greet")
bat("zap.bat -cmd -quickurl http://localhost:8080/api/echo")
bat("zap.bat -cmd -quickurl http://localhost:8080/api/search")
// Add a new controller → must update Jenkinsfile → easy to forget!
```

**With OpenAPI (auto-discovery):**
```yaml
- type: openapi
  parameters:
    apiUrl: http://localhost:8080/v3/api-docs
    targetUrl: http://localhost:8080
# Add a new controller → ZAP finds it automatically → zero Jenkinsfile changes!
```

---

## 10. Interview Talking Point

### Question: "How does your pipeline scale as the API grows?"

**Answer (3-5 sentences):**

> Our pipeline scales automatically because we use Springdoc OpenAPI for endpoint discovery. When a developer adds a new `@RestController` with `@GetMapping` or `@PostMapping`, Springdoc automatically reads those annotations and adds the new endpoint to the OpenAPI spec at `/v3/api-docs`. ZAP's Automation Framework imports that spec on every pipeline run, so the new endpoint is scanned without anyone touching the Jenkinsfile. This means the pipeline adapts to API growth with zero manual configuration — the developer's code change IS the security test configuration. In a team setting, this eliminates the risk of "we added an endpoint but forgot to add it to the security scan."

### The Flow

```
Developer adds @RestController with @PostMapping("/api/orders")
        ↓
Springdoc reads the annotation on startup
        ↓
/v3/api-docs now includes POST /api/orders
        ↓
Jenkins pipeline runs → ZAP imports /v3/api-docs
        ↓
ZAP sees POST /api/orders → scans it automatically
        ↓
No Jenkinsfile update needed! ✅
```

---

## 11. Common Errors & Fixes

### Error 1: "ClassNotFoundException: javax.servlet.http.HttpServletRequest"
```
java.lang.ClassNotFoundException: javax.servlet.http.HttpServletRequest
```
**Cause:** You're using Springdoc 1.x with Spring Boot 3.x.
**Fix:** Use `springdoc-openapi-starter-webmvc-ui:2.3.0` (not `springdoc-openapi-ui`).

### Error 2: Swagger UI returns 404
```
Whitelabel Error Page - 404
```
**Cause:** The Swagger UI path is wrong, or the dependency wasn't downloaded.
**Fix:** Check that `springdoc.swagger-ui.path=/swagger-ui.html` is in `application.properties`. Run `gradlew.bat clean build` to force re-download.

### Error 3: "Failed to clean up stale outputs"
```
Execution failed for task ':compileJava'.
> Failed to clean up stale outputs
```
**Cause:** File locks from a previous Gradle daemon or IDE. Common on Windows with OneDrive.
**Fix:** Run `Remove-Item -Path build -Recurse -Force` then `.\gradlew.bat build`.

---

## 12. Next Steps (Stage 3)

- [ ] Create `JENKINS_SETUP.md` at the project root
  - [ ] Section 1: Installing Jenkins on Windows (jenkins.war, port 9090)
  - [ ] Section 2: Configuring Gradle in Jenkins (recommend wrapper)
  - [ ] Section 3: Configuring ZAP path as Jenkins env variable
  - [ ] Section 4: Creating a Pipeline job in Jenkins UI
