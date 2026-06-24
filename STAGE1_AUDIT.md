# Stage 1 — Project Audit Report

> [!NOTE]
> This is a **read-only audit**. No files were created or modified. This report documents everything that exists in the project today and maps it to what we'll need for the Gradle migration in Stage 2.

---

## 1. pom.xml — Full Analysis

**File:** [pom.xml](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/pom.xml)

### 1A. Parent (Spring Boot Starter Parent)

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
    <relativePath/>
</parent>
```

| Field | Value | What It Does |
|---|---|---|
| `groupId` | `org.springframework.boot` | Identifies the Spring Boot organization |
| `artifactId` | `spring-boot-starter-parent` | A special parent POM that pre-configures defaults (plugin versions, dependency versions, Java version, etc.) |
| `version` | `3.2.5` | The Spring Boot version. All starter dependencies inherit this version automatically |
| `<relativePath/>` | empty | Tells Maven "don't look for the parent in a parent directory — download it from Maven Central" |

### 1B. Project Identity (GAV Coordinates)

```xml
<groupId>com.dast.demo</groupId>
<artifactId>search-backend</artifactId>
<version>0.0.1-SNAPSHOT</version>
<name>search-backend</name>
<description>Simple Spring Boot backend for DAST learning</description>
```

| Field | Value | Purpose |
|---|---|---|
| `groupId` | `com.dast.demo` | Like a namespace — your organization/package prefix |
| `artifactId` | `search-backend` | The project name — used as the JAR filename |
| `version` | `0.0.1-SNAPSHOT` | `-SNAPSHOT` means "this is a development version, not a release" |
| `name` | `search-backend` | Human-readable name (used in IDE and reports) |
| `description` | `Simple Spring Boot backend...` | Documentation only — no build effect |

### 1C. Properties

```xml
<properties>
    <java.version>17</java.version>
</properties>
```

| Property | Value | Purpose |
|---|---|---|
| `java.version` | `17` | Tells the Spring Boot parent POM to compile with Java 17. This sets `maven.compiler.source` and `maven.compiler.target` under the hood |

### 1D. Dependencies (4 total)

| # | GroupId | ArtifactId | Scope | What It Provides |
|---|---|---|---|---|
| 1 | `org.springframework.boot` | `spring-boot-starter-web` | compile (default) | Embedded Tomcat + Spring MVC + Jackson (JSON) |
| 2 | `org.springframework.boot` | `spring-boot-starter-jdbc` | compile (default) | JDBC auto-configuration + JdbcTemplate + DataSource management |
| 3 | `com.h2database` | `h2` | `runtime` | In-memory SQL database — only needed at runtime, not compile time |
| 4 | `org.springframework.boot` | `spring-boot-starter-test` | `test` | JUnit 5 + Mockito + Spring test utilities — only used during `mvn test` |

> [!IMPORTANT]
> No version numbers are specified on any dependency. They all inherit versions from the `spring-boot-starter-parent` BOM (Bill of Materials). This is a key Maven feature we must replicate in Gradle using the `io.spring.dependency-management` plugin.

### 1E. Plugins (1 total)

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

| Plugin | Purpose |
|---|---|
| `spring-boot-maven-plugin` | Enables `mvn spring-boot:run` and creates an executable "fat JAR" (includes all dependencies inside a single `.jar` file) |

---

## 2. Maven → Gradle Mapping Table

This is the core reference for Stage 2. Every Maven concept on the left becomes the Gradle equivalent on the right.

| Maven Concept | Maven Syntax | Gradle Equivalent | Gradle Syntax |
|---|---|---|---|
| **Parent POM** | `<parent>spring-boot-starter-parent:3.2.5</parent>` | Boot plugin + dependency management plugin | `id 'org.springframework.boot' version '3.2.5'` + `id 'io.spring.dependency-management' version '1.1.4'` |
| **groupId** | `<groupId>com.dast.demo</groupId>` | `group` property | `group = 'com.dast.demo'` |
| **artifactId** | `<artifactId>search-backend</artifactId>` | `rootProject.name` in settings.gradle | `rootProject.name = 'search-backend'` |
| **version** | `<version>0.0.1-SNAPSHOT</version>` | `version` property | `version = '0.0.1-SNAPSHOT'` |
| **java.version** | `<properties><java.version>17</java.version></properties>` | Java toolchain or sourceCompatibility | `sourceCompatibility = '17'` |
| **Compile dependency** | `<dependency>` (no scope) | `implementation` | `implementation 'group:artifact'` |
| **Runtime dependency** | `<scope>runtime</scope>` | `runtimeOnly` | `runtimeOnly 'group:artifact'` |
| **Test dependency** | `<scope>test</scope>` | `testImplementation` | `testImplementation 'group:artifact'` |
| **spring-boot-starter-web** | `<artifactId>spring-boot-starter-web</artifactId>` | Same artifact string | `implementation 'org.springframework.boot:spring-boot-starter-web'` |
| **spring-boot-starter-jdbc** | `<artifactId>spring-boot-starter-jdbc</artifactId>` | Same artifact string | `implementation 'org.springframework.boot:spring-boot-starter-jdbc'` |
| **h2** | `<artifactId>h2</artifactId><scope>runtime</scope>` | runtimeOnly scope | `runtimeOnly 'com.h2database:h2'` |
| **spring-boot-starter-test** | `<artifactId>spring-boot-starter-test</artifactId><scope>test</scope>` | testImplementation scope | `testImplementation 'org.springframework.boot:spring-boot-starter-test'` |
| **spring-boot-maven-plugin** | `<plugin>` in `<build>` | Boot Gradle plugin | `id 'org.springframework.boot' version '3.2.5'` (the plugin IS the equivalent) |
| **mvn spring-boot:run** | Maven goal | Gradle task | `gradlew.bat bootRun` |
| **mvn package** | Maven goal | Gradle task | `gradlew.bat bootJar` |
| **mvn test** | Maven goal | Gradle task | `gradlew.bat test` |
| **mvn clean** | Maven goal | Gradle task | `gradlew.bat clean` |
| **pom.xml** | Single build file | Two build files | `build.gradle` (build config) + `settings.gradle` (project identity) |
| **repositories (implicit)** | Maven Central is default | Must be explicit | `repositories { mavenCentral() }` |
| **Version omission (BOM)** | Parent POM provides versions | dependency-management plugin | Versions auto-managed via `io.spring.dependency-management` |

---

## 3. File Inventory — `/backend/src/`

### 3A. Java Source Files (`src/main/java/com/dast/demo/`)

| File | Package | Purpose | Key Annotations | Vulnerabilities |
|---|---|---|---|---|
| [SearchBackendApplication.java](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/SearchBackendApplication.java) | `com.dast.demo` | **Main class** — application entry point | `@SpringBootApplication` | None |
| [LoginController.java](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/controller/LoginController.java) | `com.dast.demo.controller` | POST `/api/login` — authenticates against H2 | `@RestController`, `@PostMapping`, `@CrossOrigin(origins="*")` | **SQL Injection (CWE-89)** — string concatenation into SQL on line 130-131 |
| [XssController.java](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/controller/XssController.java) | `com.dast.demo.controller` | GET `/api/greet` and GET `/api/echo` — returns raw HTML | `@RestController`, `@GetMapping`, `produces = TEXT_HTML_VALUE` | **Reflected XSS (CWE-79)** — unsanitized user input in HTML on line 121 and 152 |
| [SearchController.java](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/controller/SearchController.java) | `com.dast.demo.controller` | GET `/api/search` — returns JSON with echoed query | `@RestController`, `@GetMapping`, `@CrossOrigin(origins="*")` | **Potential XSS in JSON context** — no sanitization, but JSON `Content-Type` mitigates browser execution |
| [LoginRequest.java](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/model/LoginRequest.java) | `com.dast.demo.model` | POJO for login JSON body (username, password) | None | None (not currently used — login uses `@RequestParam` not `@RequestBody`) |
| [SearchResponse.java](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/model/SearchResponse.java) | `com.dast.demo.model` | POJO for search response JSON (`{"result": "..."}`) | None | None |

### 3B. Resource Files (`src/main/resources/`)

| File | Purpose | Key Settings |
|---|---|---|
| [application.properties](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/resources/application.properties) | Spring Boot configuration | Port 8080, H2 in-memory DB (`jdbc:h2:mem:dastdb`), H2 console enabled at `/h2-console`, JSON pretty-print ON |
| [schema.sql](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/resources/schema.sql) | Creates the `users` table on startup | `CREATE TABLE IF NOT EXISTS users (id, username, password, email, role)` |
| [data.sql](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/resources/data.sql) | Seeds 4 test users on startup | admin/password123, alice/alice2024, bob/bob_secret, charlie/ch4rli3! — **plaintext passwords (CWE-256)** |

### 3C. Other Project Files

| File | Location | Purpose |
|---|---|---|
| [README.md](file:///c:/Users/nandu/OneDrive/Desktop/DAST/README.md) | Project root | Project documentation with module guide |
| `2026-06-22-ZAP-Report-.pdf` | Project root | Previous ZAP scan result (PDF) |
| `2026-06-23-ZAP-Report-.html` | Project root | Previous ZAP scan result (HTML) |
| `backend/target/` | Build output | Maven build artifacts (will be replaced by `backend/build/` with Gradle) |

---

## 4. Main Class Identification

✅ **Main class found:** [SearchBackendApplication.java](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/SearchBackendApplication.java)

```java
@SpringBootApplication          // ← THIS is the annotation
public class SearchBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(SearchBackendApplication.class, args);
    }
}
```

**Package:** `com.dast.demo`
**What `@SpringBootApplication` does:** Combines `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`
**What `@ComponentScan` means here:** Spring scans `com.dast.demo` and all sub-packages (`com.dast.demo.controller`, `com.dast.demo.model`) to find beans annotated with `@RestController`, `@Component`, etc.

---

## 5. All Endpoints Discovered

| Method | Path | Controller | Response Type | Vulnerable? |
|---|---|---|---|---|
| POST | `/api/login` | LoginController | `application/json` | ✅ **SQL Injection** — `Statement` with string concat |
| GET | `/api/greet?name=` | XssController | `text/html` | ✅ **Reflected XSS** — raw name in HTML |
| GET | `/api/echo?input=` | XssController | `text/html` | ✅ **Reflected XSS** — raw input in HTML |
| GET | `/api/search?query=` | SearchController | `application/json` | ⚠️ XSS in JSON context (low risk — browser won't execute) |
| GET | `/h2-console` | (built-in H2) | `text/html` | ⚠️ Database console exposed |

**Total: 5 endpoints** (4 custom + 1 H2 console)

---

## 6. Vulnerability Summary

| CWE | Name | Where | Exact Line | Exploitable by ZAP? |
|---|---|---|---|---|
| CWE-89 | SQL Injection | [LoginController.java:130-131](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/controller/LoginController.java#L130-L131) | `"SELECT * FROM users WHERE username='" + username + "'..."` | ✅ Yes — real H2 DB throws SQL errors |
| CWE-79 | Reflected XSS | [XssController.java:121](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/controller/XssController.java#L121) | `"<span class='name'>" + name + "</span>"` | ✅ Yes — `text/html` response |
| CWE-79 | Reflected XSS | [XssController.java:152](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/controller/XssController.java#L152) | `"<div>" + input + "</div>"` | ✅ Yes — `text/html` response |
| CWE-256 | Plaintext Passwords | [data.sql:12-16](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/resources/data.sql#L12-L16) | `'password123'`, `'alice2024'`, etc. | ⚠️ Informational only |
| CWE-209 | Info Disclosure | [LoginController.java:171](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/src/main/java/com/dast/demo/controller/LoginController.java#L171) | `"SQL Error: " + e.getMessage()` | ✅ Yes — error-based SQLi detection |
| — | CORS Wildcard | All controllers | `@CrossOrigin(origins = "*")` | ⚠️ ZAP flags this as medium risk |

---

## 7. Observations for Subsequent Stages

> [!TIP]
> **LoginRequest.java is unused.** The `LoginController` uses `@RequestParam` (form-encoded) not `@RequestBody` (JSON). `LoginRequest.java` exists but is never referenced. We'll keep it — it doesn't cause harm and shows two approaches.

> [!IMPORTANT]
> **No test directory exists.** There is no `src/test/` directory. The `spring-boot-starter-test` dependency exists in pom.xml but no tests have been written. Stage 4's "Test Backend" stage will need either a placeholder test or we skip test reporting.

> [!WARNING]
> **`target/` directory exists from Maven builds.** After Gradle migration, build output goes to `build/` instead. The old `target/` should be cleaned up and added to `.gitignore`.

### Key Decisions for Stage 2

1. **`rootProject.name`** in `settings.gradle` should be `'search-backend'` (matching the `artifactId`)
2. **No version numbers needed** in `dependencies {}` — the Spring dependency management plugin handles this (just like the Maven parent POM)
3. **Gradle wrapper** must be generated first — Jenkins uses `gradlew.bat` so Gradle doesn't need to be installed globally
4. **`target/` cleanup** — add `target/` to `.gitignore` and delete it after confirming Gradle build works
