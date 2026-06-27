# ngrok Setup Guide — Exposing Local Jenkins to GitHub Webhooks

> [!IMPORTANT]
> **Why ngrok?** Your Jenkins runs on `localhost:9090`. GitHub's servers
> are on the internet and cannot send webhook HTTP requests to `localhost`.
> ngrok creates a secure tunnel that gives your local machine a temporary
> public URL. GitHub POSTs to that URL, ngrok forwards the request to
> `localhost:9090`, and Jenkins receives the webhook.

> [!NOTE]
> **What is ngrok?** ngrok is a tunneling tool that creates a public URL
> (e.g., `https://abcd1234.ngrok-free.app`) and forwards all traffic to
> a local port on your machine. Think of it as a "reverse proxy as a
> service" — it bridges the gap between the public internet and your
> private local network.

---

## How the Tunnel Works

```
GitHub Webhook POST
        │
        ▼
https://abcd1234.ngrok-free.app/generic-webhook-trigger/invoke?token=DAST-ZAP-SCAN
        │
        ▼
ngrok cloud server (receives the HTTPS request)
        │
        ▼
Encrypted tunnel to your machine
        │
        ▼
localhost:9090/generic-webhook-trigger/invoke?token=DAST-ZAP-SCAN
        │
        ▼
Jenkins Generic Webhook Trigger plugin processes the request
```

---

## Step 1: Create an ngrok Account

1. Go to: https://ngrok.com/
2. Click **Sign Up** (free tier is sufficient for this project)
3. Verify your email address
4. After signing in, you'll land on the **ngrok Dashboard**

> [!TIP]
> The free tier gives you:
> - 1 ngrok agent (one tunnel at a time)
> - A random subdomain that changes each restart
> - 20,000 requests per month (more than enough for learning)

---

## Step 2: Install ngrok on Windows

### Option A: Download from the website (recommended for beginners)

1. Go to: https://ngrok.com/download
2. Under **Windows**, click **Download**
3. You'll get a `.zip` file containing `ngrok.exe`
4. Extract `ngrok.exe` to a folder you'll remember:
   ```
   C:\Users\nandu\ngrok\ngrok.exe
   ```
   (Create the `ngrok` folder if it doesn't exist.)

### Option B: Install via Chocolatey (if you have Chocolatey)

```powershell
# Chocolatey is a package manager for Windows (like apt for Linux)
choco install ngrok -y
```

### Option C: Install via winget

```powershell
# winget is the Windows Package Manager (built into Windows 11)
winget install ngrok.ngrok
```

### Verify installation

Open a **new** PowerShell window and run:

```powershell
# If you used Option A (manual download), use the full path:
C:\Users\nandu\ngrok\ngrok.exe version

# If you used Option B or C (package manager), ngrok is on PATH:
ngrok version
```

Expected output:
```
ngrok version 3.x.x
```

> [!WARNING]
> If you used Option A, ngrok is NOT on your system PATH.
> You'll need to use the full path `C:\Users\nandu\ngrok\ngrok.exe`
> in every command, OR add the folder to your PATH:
> ```powershell
> # Add ngrok to your PATH for this session only
> $env:PATH += ";C:\Users\nandu\ngrok"
>
> # To add permanently: System Properties → Environment Variables → Path → New
> ```

---

## Step 3: Authenticate ngrok

ngrok requires an authentication token to create tunnels (even on the
free tier). You get this token from your ngrok dashboard.

1. Go to: https://dashboard.ngrok.com/get-started/your-authtoken
2. Copy your **Authtoken** (it looks like a long random string)
3. Run this command to save it:

```powershell
# Replace <YOUR_AUTHTOKEN> with the token you copied from the dashboard.
# This saves the token to a config file so you don't have to pass it every time.
ngrok config add-authtoken <YOUR_AUTHTOKEN>
```

Expected output:
```
Authtoken saved to configuration file: C:\Users\nandu\AppData\Local\ngrok\ngrok.yml
```

> [!NOTE]
> **Where is the config saved?** ngrok stores its config at:
> `C:\Users\nandu\AppData\Local\ngrok\ngrok.yml`
> This file contains your authtoken. Don't commit it to Git.

---

## Step 4: Start the Tunnel

> [!IMPORTANT]
> **Jenkins must be running first!** Make sure Jenkins is started on
> port 9090 before running ngrok. ngrok just forwards traffic — if
> Jenkins isn't listening, the forwarded requests will fail.

### Start Jenkins (if not already running):
```powershell
java -jar C:\Users\nandu\jenkins\jenkins.war --httpPort=9090
```

### Start the ngrok tunnel:

Open a **second** PowerShell window (keep Jenkins running in the first):

```powershell
# Create a tunnel that forwards public HTTPS traffic to your local Jenkins
ngrok http 9090
```

### What you'll see:

ngrok will display a status screen like this:

```
ngrok                                                      (Ctrl+C to quit)

Session Status      online
Account             your-email@example.com (Plan: Free)
Version             3.x.x
Region              India (in)
Latency             -
Web Interface       http://127.0.0.1:4040
Forwarding          https://abcd1234.ngrok-free.app -> http://localhost:9090

Connections         ttl     opn     rt1     rt5     p50     p90
                    0       0       0.00    0.00    0.00    0.00
```

**Key information:**
- **Forwarding URL**: `https://abcd1234.ngrok-free.app` — this is your
  public URL. Copy this — you'll need it for the GitHub webhook.
- **Web Interface**: `http://127.0.0.1:4040` — ngrok's local dashboard
  where you can inspect every request that passes through the tunnel.
  Great for debugging webhook issues.
- **Session Status**: Must say `online`. If it says `reconnecting`,
  check your internet connection.

> [!CAUTION]
> **The URL changes every restart!** On the free tier, ngrok generates
> a new random URL each time you run `ngrok http 9090`. This means:
> 1. You must update the GitHub webhook URL every time you restart ngrok.
> 2. Keep ngrok running in this terminal window while testing.
> 3. If you close this window, the tunnel dies and GitHub can't reach Jenkins.

---

## Step 5: Verify the Tunnel Works

### Test 1: Open the public URL in your browser

1. Copy the Forwarding URL from ngrok (e.g., `https://abcd1234.ngrok-free.app`)
2. Open it in your browser
3. You may see an **ngrok interstitial page** saying "You are about to visit..."
   — click "Visit Site"
4. You should see the **Jenkins login page** (or dashboard if already logged in)

### Test 2: Check the ngrok Web Interface

1. Open `http://127.0.0.1:4040` in your browser
2. This shows all HTTP requests passing through the tunnel
3. After the GitHub webhook is configured, you'll see the POST requests
   here — very useful for debugging

### Test 3: Verify the webhook endpoint is reachable

Open PowerShell and run:

```powershell
# This should return a response from Jenkins (even if it's an error page,
# it proves the tunnel reaches Jenkins)
curl https://abcd1234.ngrok-free.app/generic-webhook-trigger/invoke
```

If you see any JSON response (even an error like "No token provided"),
the tunnel is working correctly.

---

## Step 6: Use the ngrok URL in GitHub Webhook

When configuring the GitHub webhook (Stage 4B, Step 5), use:

```
https://abcd1234.ngrok-free.app/generic-webhook-trigger/invoke?token=DAST-ZAP-SCAN
```

Replace `abcd1234.ngrok-free.app` with your actual ngrok Forwarding URL.

---

## Quick Reference — Commands You'll Use Often

| What | Command |
|---|---|
| Start Jenkins | `java -jar C:\Users\nandu\jenkins\jenkins.war --httpPort=9090` |
| Start ngrok tunnel | `ngrok http 9090` |
| Check ngrok status | Open `http://127.0.0.1:4040` in browser |
| Stop ngrok | Press `Ctrl+C` in the ngrok terminal |
| Get your authtoken | https://dashboard.ngrok.com/get-started/your-authtoken |

---

## Workflow Summary for PR Testing

Every time you want to test the PR trigger:

1. **Terminal 1**: Start Jenkins → `java -jar C:\Users\nandu\jenkins\jenkins.war --httpPort=9090`
2. **Terminal 2**: Start ngrok → `ngrok http 9090`
3. **Copy** the ngrok Forwarding URL
4. **Update** the GitHub webhook Payload URL (if the ngrok URL changed)
5. **Open a PR** on GitHub → Jenkins build triggers automatically
6. **Inspect** requests at `http://127.0.0.1:4040` if something goes wrong

---

## Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| `ERR_NGROK_108` | authtoken not configured | Run `ngrok config add-authtoken <TOKEN>` |
| `ERR_NGROK_120` | Tunnel already running | Close the other ngrok terminal or run `taskkill /IM ngrok.exe /F` |
| Browser shows ngrok warning page | Free tier interstitial | Click "Visit Site" — this is normal |
| GitHub webhook red ❌ "timed out" | ngrok not running | Start ngrok with `ngrok http 9090` |
| GitHub webhook red ❌ "failed to connect" | ngrok URL changed | Copy the new URL from ngrok and update the webhook |
| Jenkins shows 403 Forbidden | CSRF protection blocking webhook | Jenkins → Manage Jenkins → Security → uncheck "Prevent Cross Site Request Forgery exploits" (only for learning!) |
| `502 Bad Gateway` from ngrok | Jenkins not running on 9090 | Start Jenkins first, then ngrok |
