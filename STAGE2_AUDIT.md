# Stage 2 — Gradle Migration Audit Report

> [!NOTE]
> This stage replaced Maven (`pom.xml`) with Gradle (`build.gradle` + `settings.gradle`).
> All code compiles and packages successfully. The build produces an executable JAR.

---

## 1. Files Created

| # | File | Purpose |
|---|---|---|
| 1 | [settings.gradle](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/settings.gradle) | Project identity — sets `rootProject.name = 'search-backend'` |
| 2 | [build.gradle](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/build.gradle) | Build configuration — plugins, dependencies, tasks |
| 3 | [gradlew](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/gradlew) | Gradle wrapper script (Linux/Mac) |
| 4 | [gradlew.bat](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/gradlew.bat) | Gradle wrapper script (Windows) — this is what Jenkins uses |
| 5 | [gradle-wrapper.jar](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/gradle/wrapper/gradle-wrapper.jar) | Wrapper bootstrap JAR (downloads Gradle if not cached) |
| 6 | [gradle-wrapper.properties](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/gradle/wrapper/gradle-wrapper.properties) | Wrapper config — specifies Gradle 8.14 |

## 2. Files Modified

| # | File | Change |
|---|---|---|
| 1 | [.gitignore](file:///c:/Users/nandu/OneDrive/Desktop/DAST/.gitignore) | Added `backend/build/` and `backend/.gradle/` ignore patterns |

## 3. Files to Delete (After User Confirms Build Works)

| # | File | Reason |
|---|---|---|
| 1 | [pom.xml](file:///c:/Users/nandu/OneDrive/Desktop/DAST/backend/pom.xml) | Keeping both Maven and Gradle configs causes confusion — only one build tool should be active |
| 2 | `backend/target/` (directory) | Maven's build output directory — Gradle uses `backend/build/` instead |

> [!WARNING]
> **Do NOT delete `pom.xml` yet.** Wait until you've verified the app runs correctly with `gradlew.bat bootRun` (tested in Stage 2B). The pom.xml serves as a reference until then.

---

## 4. Build Verification

### Command Run
```powershell
cd c:\Users\nandu\OneDrive\Desktop\DAST\backend
.\gradlew.bat build
```

### Result: ✅ BUILD SUCCESSFUL
```
> Task :compileJava UP-TO-DATE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE
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

BUILD SUCCESSFUL in 9s
5 actionable tasks: 3 executed, 2 up-to-date
```

### Build Output
| File | Size | Purpose |
|---|---|---|
| `build/libs/search-backend-0.0.1-SNAPSHOT.jar` | ~23 MB | Executable "fat JAR" with all dependencies embedded |
| `build/libs/search-backend-0.0.1-SNAPSHOT-plain.jar` | ~9 KB | Plain JAR without dependencies (not used) |

### What "NO-SOURCE" Means
`compileTestJava NO-SOURCE` and `test NO-SOURCE` mean there are no test files in `src/test/`. This is expected — Stage 1 noted that no tests exist. Gradle skips these tasks gracefully.

---

## 5. Compatibility Issues Encountered & Resolved

### Issue 1: `sourceCompatibility` removed in Gradle 9.x

**Error:**
```
Could not set unknown property 'sourceCompatibility' for root project 'search-backend'
```

**Cause:** The top-level `sourceCompatibility = '17'` property was removed in Gradle 9.x. Although we target Gradle 8.14, the local Gradle 9.5.1 (used to generate the wrapper) evaluates `build.gradle` during wrapper generation.

**Fix:** Replaced with the Java toolchain API:
```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}
```

---

### Issue 2: Java 24 class file version not supported by Gradle 8.7/8.13

**Error:**
```
Unsupported class file major version 68
```

**Cause:** Java 24 produces `.class` files with major version 68. Gradle 8.7 supports up to version 66 (Java 22). Gradle 8.13 supports up to version 67 (Java 23). Neither can parse Java 24 class files.

**Fix:** Upgraded wrapper to **Gradle 8.14**, the first 8.x release with Java 24 support.

---

### Issue 3: Spring Boot 3.2.5 plugin incompatible with Gradle 9.x

**Error (when using Gradle 9.5.1 wrapper):**
```
'java.util.Set org.gradle.api.artifacts.LenientConfiguration.getArtifacts(org.gradle.api.specs.Spec)'
```

**Cause:** The Spring Boot 3.2.5 Gradle plugin uses Gradle internal APIs that were removed in Gradle 9.x.

**Fix:** Stayed on Gradle 8.14 instead of 9.x. Spring Boot 3.4+ would be needed for Gradle 9.x compatibility.

---

### Issue 4: Spring Boot plugin can't scan Java 24 class files for main class

**Error:**
```
Execution failed for task ':resolveMainClassName'.
> Unsupported class file major version 68
```

**Cause:** The Spring Boot plugin scans compiled `.class` files using ASM (a bytecode library) to find `@SpringBootApplication`. The ASM version in Spring Boot 3.2.5 doesn't support Java 24's class file format.

**Fix:** Explicitly configured the main class in `build.gradle`:
```groovy
springBoot {
    mainClass = 'com.dast.demo.SearchBackendApplication'
}
```
This bypasses the bytecode scanning entirely.

---

## 6. Gradle Version Decision Matrix

| Gradle Version | Java 24? | Spring Boot 3.2.5? | Chosen? |
|---|---|---|---|
| 8.7 | ❌ Max Java 22 | ✅ Compatible | ❌ |
| 8.13 | ❌ Max Java 23 | ✅ Compatible | ❌ |
| **8.14** | **✅ Java 24 support** | **✅ Compatible** | **✅ Selected** |
| 9.5.1 | ✅ Java 24 support | ❌ Uses removed APIs | ❌ |

---

## 7. build.gradle Structure Summary

```
build.gradle
├── plugins { }           ← java + spring-boot + dependency-management
├── group / version       ← project identity (from pom.xml)
├── java { toolchain }    ← Java 24 (from pom.xml's java.version)
├── repositories { }      ← mavenCentral()
├── dependencies { }      ← 4 deps (same as pom.xml)
├── tasks.named('test')   ← JUnit 5 configuration
├── springBoot { }        ← explicit main class (Java 24 workaround)
└── bootRun { }           ← dev server configuration
```

---

## 8. Gradle Command Reference

| What You Want | Maven Command | Gradle Command |
|---|---|---|
| Compile code | `mvn compile` | `gradlew.bat compileJava` |
| Run tests | `mvn test` | `gradlew.bat test` |
| Build everything | `mvn package` | `gradlew.bat build` |
| Create executable JAR | `mvn package` | `gradlew.bat bootJar` |
| Start the app | `mvn spring-boot:run` | `gradlew.bat bootRun` |
| Clean build output | `mvn clean` | `gradlew.bat clean` |
| Clean + build | `mvn clean package` | `gradlew.bat clean build` |

---

## 9. How the Wrapper Works (For Jenkins)

```
┌──────────────────────────────────────────────────┐
│  Developer/Jenkins runs: gradlew.bat build       │
│                                                  │
│  1. gradlew.bat reads gradle-wrapper.properties  │
│  2. Finds: distributionUrl = gradle-8.14-bin.zip │
│  3. Checks: is Gradle 8.14 already cached?       │
│     YES → use cached copy                        │
│     NO  → download from services.gradle.org      │
│  4. Launches Gradle 8.14 with build.gradle       │
│  5. Build runs identically everywhere            │
└──────────────────────────────────────────────────┘
```

**Why this matters for Jenkins:**
Jenkins runs `gradlew.bat build` just like you do locally. Because the wrapper pins the exact Gradle version, the build is **reproducible** — same behavior on your laptop, on Jenkins, on a teammate's machine.

---

## 10. Common Errors & Fixes

### Error 1: "gradlew.bat is not recognized"
```
gradlew.bat : The term 'gradlew.bat' is not recognized
```
**Fix:** You must be inside the `/backend` directory. Run:
```powershell
cd c:\Users\nandu\OneDrive\Desktop\DAST\backend
.\gradlew.bat build
```

### Error 2: "JAVA_HOME is not set"
```
ERROR: JAVA_HOME is not set and no 'java' command could be found
```
**Fix:** Set JAVA_HOME to your JDK installation:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```

### Error 3: "Could not resolve dependencies"
```
Could not resolve all dependencies for configuration ':runtimeClasspath'
```
**Fix:** Check your internet connection. Gradle needs to download from Maven Central on the first build. If behind a proxy, configure it in `~/.gradle/gradle.properties`.

---

## 11. Next Steps (Stage 2B)

- [ ] Add `springdoc-openapi-starter-webmvc-ui:2.3.0` dependency to `build.gradle`
- [ ] Add Springdoc properties to `application.properties`
- [ ] Add `@Operation` and `@Parameter` annotations to controllers
- [ ] Create `OpenApiConfig.java`
- [ ] Verify with `gradlew.bat bootRun` → check Swagger UI
- [ ] Delete `pom.xml` after confirming everything works
- [ ] Delete `target/` directory
