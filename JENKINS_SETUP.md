# Jenkins Setup Guide — Windows (No Docker)

> [!IMPORTANT]
> This guide installs Jenkins on the **same Windows machine** that runs
> your Spring Boot backend and ZAP. No Docker, no Linux, no VMs.
> Every command is PowerShell-compatible.

> [!NOTE]
> **Why Jenkins?** Jenkins is a CI/CD automation server. It runs your
> pipeline (build → test → security scan → report) automatically every
> time code changes. Without Jenkins, you'd run each step manually in
> separate terminal windows — error-prone and forgettable.

---

## Prerequisites Checklist

Before you begin, verify these are installed:

| Requirement | How to Check | Expected Output |
|---|---|---|
| Java 17+ | `java -version` | `openjdk version "17.x.x"` or higher |
| Gradle wrapper | `cd backend && .\gradlew.bat --version` | Gradle 8.x |
| ZAP installed | Check `C:\Program Files\OWASP\Zed Attack Proxy` | Folder exists with `zap.bat` inside |
| Spring Boot app builds | `cd backend && .\gradlew.bat build` | `BUILD SUCCESSFUL` |
| Port 8080 free | `netstat -ano | findstr :8080` | No output (nothing using 8080) |
| Port 9090 free | `netstat -ano | findstr :9090` | No output (nothing using 9090) |

---

## Section 1: Installing Jenkins on Windows

### 1.1 Why jenkins.war Instead of the Windows Installer?

Jenkins offers two ways to install on Windows:

| Method | Pros | Cons |
|---|---|---|
| **Windows Installer (.msi)** | Auto-starts as a Windows service | Runs as SYSTEM user (permission issues), harder to debug, installs into `C:\Program Files` |
| **jenkins.war (recommended)** | You control when it starts/stops, runs as YOUR user, easy to debug | Must start manually each session |

**We use jenkins.war** because:
- It runs as **your user account**, so it has the same file permissions
  as when you run `gradlew.bat` or `zap.bat` manually.
  The .msi installer runs as `NT AUTHORITY\SYSTEM` which can't find your
  user-installed Java, Gradle, or ZAP — causing mysterious "command not found" errors.
- It's **one file** — easy to move, delete, or restart.
- You can **see all logs in real-time** in your terminal window.

### 1.2 Download Jenkins

1. Go to: https://www.jenkins.io/download/
2. Under **Generic Java Package (.war)**, click **Download**.
3. Save `jenkins.war` somewhere easy to find:
   ```
   C:\Users\nandu\jenkins\jenkins.war
   ```
   (Create the `jenkins` folder if it doesn't exist.)

> [!TIP]
> You can also download via PowerShell:
> ```powershell
> # Create a folder for Jenkins
> mkdir C:\Users\nandu\jenkins -Force
>
> # Download the latest LTS war file
> Invoke-WebRequest -Uri "https://get.jenkins.io/war-stable/latest/jenkins.war" `
>     -OutFile "C:\Users\nandu\jenkins\jenkins.war"
> ```

### 1.3 Start Jenkins

Open PowerShell and run:

```powershell
# Start Jenkins on port 9090
# ────────────────────────────────────────────────────────────────
# Why port 9090?
#   - Spring Boot uses port 8080 (configured in application.properties)
#   - Jenkins defaults to 8080 too — they'd conflict!
#   - Using 9090 keeps both running side-by-side without port clashes.
#
# Why "java -jar"?
#   - jenkins.war is a self-contained Java web application
#   - "java -jar" tells Java to run it directly — no Tomcat install needed
#   - Jenkins bundles its own embedded Jetty server inside the .war file
# ────────────────────────────────────────────────────────────────
java -jar C:\Users\nandu\jenkins\jenkins.war --httpPort=9090
```

> [!CAUTION]
> **Keep this terminal window open!** Jenkins runs in the foreground.
> Closing the terminal stops Jenkins. If you need to use the terminal
> for other things, open a **new** PowerShell window.

### 1.4 First-Time Setup

When Jenkins starts for the first time, it prints something like this
in your terminal:

```
*************************************************************
Jenkins initial setup is required. An admin user has been created
and a password generated. Please use the following password to
proceed to installation:

a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6

This may also be found at:
C:\Users\nandu\.jenkins\secrets\initialAdminPassword
*************************************************************
```

**Steps:**

1. **Open your browser** and go to: http://localhost:9090

2. **Paste the password** from the terminal (or read it from the file):
   ```powershell
   # Read the initial admin password from the file
   # ──────────────────────────────────────────────
   # Jenkins stores this at %USERPROFILE%\.jenkins\secrets\
   # %USERPROFILE% is Windows' way of saying "your home folder"
   # which is typically C:\Users\<your-username>
   # ──────────────────────────────────────────────
   Get-Content "$env:USERPROFILE\.jenkins\secrets\initialAdminPassword"
   ```

3. **Install suggested plugins** — click the left option:
   ```
   [Install suggested plugins]    [Select plugins to install]
            ↑                              ↑
       CLICK THIS                    (for advanced users)
   ```
   This installs ~20 common plugins including Git, Pipeline,
   Credentials, and Workspace Cleanup. Takes 2-5 minutes.

4. **Create your admin user** when prompted. Use something simple
   for local development:
   ```
   Username: admin
   Password: admin
   Full name: Admin
   Email: admin@local.dev
   ```

5. **Set the Jenkins URL** — keep the default: `http://localhost:9090/`

6. **Click "Start using Jenkins"** — you're in!

### 1.5 Install Additional Plugins

After the initial setup, install two more plugins we need:

1. Go to: **Manage Jenkins** → **Plugins** → **Available plugins**

2. Search for and install:

| Plugin | Why We Need It |
|---|---|
| **HTML Publisher** | Displays ZAP's HTML scan reports directly in the Jenkins UI. Without this, you'd have to find and open the report files manually. |
| **Gradle** | Lets Jenkins find and run Gradle builds. Even though we use the wrapper (`gradlew.bat`), this plugin adds Gradle-aware features like build caching hints. |

3. Click **"Install without restart"** for each.

> [!NOTE]
> **Plugin = Extension.** Jenkins by itself is bare-bones. Plugins add
> features like Git support, HTML report viewing, Slack notifications, etc.
> The "suggested plugins" step already installed Git and Pipeline support.
> We're adding HTML Publisher (for ZAP reports) and Gradle (for build integration).

### 1.6 Jenkins Agent vs Controller — Explained

You might see the terms "agent" and "controller" in Jenkins docs:

| Term | What It Means | In Our Setup |
|---|---|---|
| **Controller** | The main Jenkins process — manages the UI, schedules jobs, stores results | The `java -jar jenkins.war` process we just started |
| **Agent** | A separate machine (or process) that runs the actual build work | **We don't have one** — our controller does both jobs |

**On a single machine**, the controller IS the agent. Jenkins calls this
the **"built-in node"**. Our pipeline runs directly on the same machine
where Jenkins is installed — no remote agents, no SSH, no distributed builds.

**In a real company**, you'd have:
```
Controller (manages everything)
├── Agent 1 (Linux) — runs Linux builds
├── Agent 2 (Windows) — runs Windows builds
└── Agent 3 (macOS) — runs iOS builds
```

But for our learning lab, one machine does it all. That's why the
Jenkinsfile says `agent any` — "run this on whatever agent is available"
(which is just our local machine).

---

## Section 2: Configuring Gradle in Jenkins

### 2.1 Two Options for Running Gradle in Jenkins

| Option | How It Works | Recommended? |
|---|---|---|
| **Global Gradle installation** | Jenkins downloads and manages Gradle itself | ❌ No — adds complexity |
| **Gradle wrapper (`gradlew.bat`)** | Use the wrapper script already in your project | ✅ Yes — portable and version-locked |

### 2.2 Why the Wrapper Is Better

The Gradle wrapper (`gradlew.bat` + `gradle/wrapper/gradle-wrapper.jar`)
is a script that automatically downloads the exact Gradle version your
project needs. Here's why this matters:

```
Without wrapper:
  Developer A has Gradle 7.6 installed globally
  Developer B has Gradle 8.5 installed globally
  Jenkins server has Gradle 8.2 installed
  → Different Gradle versions → inconsistent builds → mysterious failures

With wrapper:
  gradlew.bat reads gradle/wrapper/gradle-wrapper.properties
  → sees: distributionUrl=...gradle-8.7-bin.zip
  → downloads Gradle 8.7 automatically (first time only)
  → EVERYONE uses the same version → consistent builds ✅
```

**In the Jenkinsfile**, we call `gradlew.bat` directly instead of `gradle`:
```groovy
// ✅ CORRECT — uses the project's wrapper (Gradle 8.7)
bat('.\\backend\\gradlew.bat build')

// ❌ WRONG — uses whatever Gradle Jenkins has installed (maybe wrong version)
bat('gradle build')
```

### 2.3 (Optional) Registering Gradle in Jenkins UI

Even though we use the wrapper, you can optionally tell Jenkins about Gradle.
This is only needed if you DON'T use the wrapper:

1. Go to: **Manage Jenkins** → **Tools**
2. Scroll to **Gradle installations**
3. Click **"Add Gradle"**
4. Options:
   - **Name:** `Gradle 8.7` (any label you want)
   - **Install automatically:** ✅ Check this box
   - **Version:** Select `Gradle 8.7` from the dropdown

> [!TIP]
> **Skip this step.** Our Jenkinsfile uses `gradlew.bat` directly, so
> Jenkins doesn't need a global Gradle installation. The wrapper handles
> everything. This section is here so you understand what the option does
> if you see it mentioned in tutorials.

---

## Section 3: Configuring ZAP Path in Jenkins

### 3.1 Why Store the ZAP Path as a Jenkins Environment Variable?

Our Jenkinsfile needs to know where ZAP is installed to call `zap.bat`.
We have two options:

```groovy
// Option A: Hardcode the path in the Jenkinsfile (BAD)
bat('"C:\\Program Files\\OWASP\\Zed Attack Proxy\\zap.bat" -cmd ...')
// Problem: If ZAP moves to a different folder (or another developer's
// machine has it elsewhere), you have to edit the Jenkinsfile.

// Option B: Use an environment variable set in Jenkins (GOOD)
bat("\"%ZAP_PATH%\\zap.bat\" -cmd ...")
// Benefit: Change the path in Jenkins settings → all pipelines update.
// No Jenkinsfile edits needed. Each developer's machine can have ZAP
// in a different folder and it still works.
```

### 3.2 Setting the ZAP_PATH Environment Variable in Jenkins

1. Go to: **Manage Jenkins** → **System** (under "System Configuration")

2. Scroll down to: **Global properties**

3. Check: ☑ **Environment variables**

4. Click: **Add**

5. Fill in:

| Field | Value |
|---|---|
| **Name** | `ZAP_PATH` |
| **Value** | `C:\Program Files\OWASP\Zed Attack Proxy` |

6. Click **Save** at the bottom.

> [!WARNING]
> **Verify your ZAP path first!** Open File Explorer and navigate to
> `C:\Program Files\OWASP\Zed Attack Proxy`. You should see `zap.bat`
> inside that folder. If ZAP is installed elsewhere (e.g., `C:\OWASP ZAP`),
> use THAT path instead.
>
> Quick PowerShell check:
> ```powershell
> # This should print "True" if ZAP is at the expected path
> Test-Path "C:\Program Files\OWASP\Zed Attack Proxy\zap.bat"
> ```

### 3.3 How the Jenkinsfile Uses This Variable

In the Jenkinsfile (Stage 4), we'll reference `ZAP_PATH` like this:

```groovy
pipeline {
    agent any

    environment {
        // ──────────────────────────────────────────────────────
        // Read the ZAP_PATH from Jenkins' global environment variables.
        // env.ZAP_PATH refers to the value we just set in Jenkins UI.
        // ──────────────────────────────────────────────────────
        ZAP_PATH = "${env.ZAP_PATH}"
    }

    stages {
        stage('ZAP Scan') {
            steps {
                // ──────────────────────────────────────────────
                // Use the ZAP_PATH variable to call zap.bat
                // %ZAP_PATH% is how Windows batch reads env vars
                // ──────────────────────────────────────────────
                bat('"%ZAP_PATH%\\zap.bat" -cmd -autorun zap-config.yaml')
            }
        }
    }
}
```

### 3.4 Why Not Use System Environment Variables Instead?

You could set `ZAP_PATH` in Windows System Environment Variables
(System → Advanced → Environment Variables). But storing it in Jenkins
is better because:

| Approach | Scope | Who Can Change It |
|---|---|---|
| Windows System env var | All programs on the machine | Must be admin, restart required |
| **Jenkins global env var** | Only Jenkins pipelines | Any Jenkins admin, no restart needed |

Jenkins env vars are **scoped to Jenkins only** — they don't pollute your
system environment. And you can change them from the Jenkins UI without
restarting anything.

---

## Section 4: Creating a Pipeline Job in Jenkins

### 4.1 Job Types in Jenkins

Jenkins offers several job types. Here's what they mean:

| Job Type | What It Does | Use When |
|---|---|---|
| **Freestyle project** | Simple: click buttons to configure build steps, one after another | You just need "run this command, then that command" |
| **Pipeline** | Complex: write a `Jenkinsfile` script that defines stages, conditions, error handling | You want a real CI/CD pipeline with multiple stages |
| **Multibranch Pipeline** | Like Pipeline, but automatically creates a pipeline for each Git branch | You have a Git repo with multiple branches |

**We use Pipeline** because:
- Our pipeline has 8 stages (build, test, start app, scan, report, stop)
- We need error handling (`post { always { ... } }`)
- We need environment variables shared across stages
- Freestyle can't do conditional logic or parallel stages

### 4.2 "Pipeline Script" vs "Pipeline Script from SCM"

When you create a Pipeline job, Jenkins asks where the Jenkinsfile lives:

| Option | What It Means | Use When |
|---|---|---|
| **Pipeline script** | Paste the Jenkinsfile content directly into the Jenkins UI text box | ✅ **Start here** — easiest for learning, no Git setup needed |
| **Pipeline script from SCM** | Jenkins reads the Jenkinsfile from a Git repository (GitHub, local repo, etc.) | Later — when you push your code to GitHub |

**"SCM" = Source Code Management** (usually Git). When you choose "from SCM",
Jenkins clones your repo and looks for a file called `Jenkinsfile` at the
root. This is the production standard — but for learning, pasting the
script directly is simpler.

### 4.3 Step-by-Step: Creating Your Pipeline Job

1. **Open Jenkins**: http://localhost:9090

2. **Click "New Item"** (left sidebar)

3. **Enter a name**: `DAST-Pipeline`
   ```
   Item name: [DAST-Pipeline          ]
   ```
   > Use hyphens, not spaces. Jenkins uses this name in URLs and file paths.

4. **Select "Pipeline"** (not "Freestyle project"):
   ```
   ○ Freestyle project
   ● Pipeline              ← SELECT THIS
   ○ Multi-configuration project
   ○ Folder
   ○ Multibranch Pipeline
   ```

5. **Click "OK"**

6. **Configure the job:**

   **General section:**
   - ☑ Check **"Do not allow concurrent builds"**
     ```
     Why? Our pipeline starts a Spring Boot server on port 8080 and
     ZAP on port 8090. If two builds run at the same time, the second
     one fails because the ports are already in use.
     ```

   **Pipeline section:**
   - **Definition:** Select `Pipeline script` (not "from SCM")
   - **Script:** This is where you'll paste the Jenkinsfile content
     (we'll create this in Stage 4). For now, paste this test script
     to verify Jenkins works:

   ```groovy
   // ──────────────────────────────────────────────────────────
   // Test Pipeline — verifies Jenkins can run basic commands
   // Replace this with the full Jenkinsfile in Stage 4
   // ──────────────────────────────────────────────────────────
   pipeline {
       // agent any = run on any available node (our local machine)
       agent any

       stages {
           // Stage 1: Verify Java is accessible
           stage('Check Java') {
               steps {
                   // bat() runs a Windows batch command
                   // (Linux pipelines use sh() instead)
                   bat('java -version')
               }
           }

           // Stage 2: Verify Gradle wrapper works
           stage('Check Gradle') {
               steps {
                   // dir() changes to a directory for the commands inside
                   dir('backend') {
                       bat('.\\gradlew.bat --version')
                   }
               }
           }

           // Stage 3: Verify ZAP path is set
           stage('Check ZAP Path') {
               steps {
                   // %ZAP_PATH% reads the Jenkins environment variable
                   // we set in Section 3
                   bat('echo ZAP is at: %ZAP_PATH%')
                   bat('if exist "%ZAP_PATH%\\zap.bat" (echo ZAP found!) else (echo ZAP NOT FOUND!)')
               }
           }
       }

       // post block runs after all stages complete (success or failure)
       post {
           success {
               echo 'All checks passed! Jenkins is configured correctly.'
           }
           failure {
               echo 'Something failed. Check the console output above.'
           }
       }
   }
   ```

7. **Click "Save"**

8. **Click "Build Now"** (left sidebar)

9. **Click the build number** (e.g., `#1`) → **"Console Output"** to see results

### 4.4 Expected Output from Test Pipeline

If everything is configured correctly, you should see:

```
Started by user admin
Running in Durability level: MAX_SURVIVABILITY
[Pipeline] Start of Pipeline
[Pipeline] node
Running on Built-In Node in workspace C:\Users\nandu\.jenkins\workspace\DAST-Pipeline

[Pipeline] stage (Check Java)
[Pipeline] bat
C:\Users\nandu\.jenkins\workspace\DAST-Pipeline> java -version
openjdk version "17.0.x" ...
                                          ← Java found ✅

[Pipeline] stage (Check Gradle)
[Pipeline] dir
C:\Users\nandu\.jenkins\workspace\DAST-Pipeline\backend> .\gradlew.bat --version
Gradle 8.7
                                          ← Gradle wrapper works ✅

[Pipeline] stage (Check ZAP Path)
[Pipeline] bat
ZAP is at: C:\Program Files\OWASP\Zed Attack Proxy
ZAP found!
                                          ← ZAP path is set ✅

[Pipeline] echo
All checks passed! Jenkins is configured correctly.

Finished: SUCCESS
```

### 4.5 Common Errors and Fixes

#### Error 1: "'java' is not recognized as an internal or external command"

```
'java' is not recognized as an internal or external command,
operable program or batch file.
```

**Cause:** Jenkins can't find Java. Even though YOU can run `java -version`
in your terminal, Jenkins might not have the same PATH.

**Fix:** Add `JAVA_HOME` as a Jenkins global environment variable:
1. **Manage Jenkins** → **System** → **Global properties** → **Environment variables**
2. Add:
   - Name: `JAVA_HOME`
   - Value: `C:\Program Files\Java\jdk-21`
     (find your actual path with: `where java` in PowerShell)
3. Also add to **PATH**: in the same environment variables section, add:
   - Name: `PATH`
   - Value: `%JAVA_HOME%\bin;%PATH%`

#### Error 2: "gradlew.bat is not recognized"

```
'.\gradlew.bat' is not recognized ...
```

**Cause:** Jenkins' workspace doesn't contain your project files yet.
When using "Pipeline script" (not SCM), Jenkins starts with an empty workspace.

**Fix:** The Jenkinsfile needs a Checkout stage that copies files.
For "Pipeline script" mode, you have two options:

```groovy
// Option A: Clone from Git (if your project is in a Git repo)
stage('Checkout') {
    steps {
        git url: 'https://github.com/your-username/DAST.git', branch: 'main'
    }
}

// Option B: Copy files manually (for local-only development)
// Before running the pipeline, copy your project to Jenkins' workspace:
// Copy-Item -Path "C:\Users\nandu\OneDrive\Desktop\DAST\*" `
//     -Destination "C:\Users\nandu\.jenkins\workspace\DAST-Pipeline\" `
//     -Recurse -Force
```

> [!TIP]
> When we create the real Jenkinsfile in Stage 4, we'll handle this
> properly with a Checkout stage. For now, the test pipeline just checks
> if tools are accessible.

#### Error 3: "ZAP NOT FOUND!" despite setting ZAP_PATH

```
ZAP is at: C:\Program Files\OWASP\Zed Attack Proxy
ZAP NOT FOUND!
```

**Cause:** The path in Jenkins doesn't match where ZAP is actually installed.

**Fix:**
```powershell
# Find where ZAP is actually installed
Get-ChildItem "C:\Program Files" -Filter "zap.bat" -Recurse -ErrorAction SilentlyContinue
Get-ChildItem "C:\Program Files (x86)" -Filter "zap.bat" -Recurse -ErrorAction SilentlyContinue

# Common locations:
# C:\Program Files\OWASP\Zed Attack Proxy\zap.bat
# C:\Program Files\ZAP\Zed Attack Proxy\zap.bat
# C:\OWASP ZAP\zap.bat
```
Update the `ZAP_PATH` in Jenkins to match the correct folder.

#### Error 4: Port 9090 already in use

```
java.net.BindException: Address already in use: bind
```

**Cause:** Another process (or a previous Jenkins instance) is using port 9090.

**Fix:**
```powershell
# Find what's using port 9090
netstat -ano | findstr :9090

# Output example:
#   TCP  0.0.0.0:9090  0.0.0.0:0  LISTENING  12345
#                                              ↑ PID

# Kill the process using that PID
taskkill /PID 12345 /F

# Now start Jenkins again
java -jar C:\Users\nandu\jenkins\jenkins.war --httpPort=9090
```

---

## Quick Reference Card

| Task | Command / URL |
|---|---|
| **Start Jenkins** | `java -jar C:\Users\nandu\jenkins\jenkins.war --httpPort=9090` |
| **Open Jenkins UI** | http://localhost:9090 |
| **Get initial password** | `Get-Content "$env:USERPROFILE\.jenkins\secrets\initialAdminPassword"` |
| **Jenkins home directory** | `C:\Users\nandu\.jenkins\` |
| **Jenkins workspace** | `C:\Users\nandu\.jenkins\workspace\DAST-Pipeline\` |
| **Stop Jenkins** | Close the terminal window (or Ctrl+C) |
| **Start Spring Boot** | `cd backend && .\gradlew.bat bootRun` (separate terminal) |
| **Check port 8080** | `netstat -ano \| findstr :8080` |
| **Check port 9090** | `netstat -ano \| findstr :9090` |

---

## What Happens Next (Stage 4)

In Stage 4, we'll create the real `Jenkinsfile` at the project root with:
- Build and test stages using `gradlew.bat`
- A stage that starts the Spring Boot app in the background
- ZAP baseline and active scan stages
- Report publishing with the HTML Publisher plugin
- Cleanup stages that stop the backend server

All using `bat()` commands (Windows batch) instead of `sh()` (Linux shell).

> [!NOTE]
> **Keep Jenkins running** when you start Stage 4. You'll paste the
> Jenkinsfile into the Pipeline job we created and run it for real.
