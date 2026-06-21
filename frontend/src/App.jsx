/*
 * =========================================================
 * MODULE 6: THE MAIN APP COMPONENT  (App.jsx)
 * =========================================================
 *
 * This is the only React component in this app.
 * It handles:
 *   1. State management (what the user typed, the API result)
 *   2. The API call to our Spring Boot backend
 *   3. Rendering the UI
 *
 * React Concepts Used:
 * ─────────────────────────────────────────────────────────
 *
 * useState(initialValue)
 *   Creates a "state variable" — a value React watches.
 *   When it changes, React automatically re-renders the component.
 *   Returns [currentValue, setterFunction].
 *
 *   Example:
 *     const [query, setQuery] = useState('')
 *     query      → the current text in the input box
 *     setQuery() → call this to update the text
 *
 * useCallback(fn, [deps])
 *   Memoizes a function so it isn't recreated on every render.
 *   Only recreates when values in the deps array change.
 *   Good practice for event handlers passed to child components.
 *
 * async/await + fetch()
 *   Modern way to make HTTP requests from the browser.
 *   fetch() returns a Promise. We "await" it to get the Response.
 *   We then call .json() to parse the JSON body (also a Promise).
 */

import { useState, useCallback } from 'react'
import './App.css'

// The base URL of our Spring Boot backend.
// In dev, React runs on :5173, Spring Boot runs on :8080.
// We need the full URL because they're different origins.
const API_BASE_URL = 'http://localhost:8080'

function App() {
  // ── ACTIVE TAB ───────────────────────────────────────────
  // Controls which panel is shown: 'search' or 'login'
  const [activeTab, setActiveTab] = useState('search')

  // ── SEARCH STATE ─────────────────────────────────────────
  const [query, setQuery] = useState('')
  const [result, setResult] = useState(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState(null)

  // ── LOGIN STATE (SQL Injection Demo) ─────────────────────
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loginResult, setLoginResult] = useState(null)   // { success, message, query }
  const [loginLoading, setLoginLoading] = useState(false)
  const [loginError, setLoginError] = useState(null)

  // ── API CALL: SEARCH ─────────────────────────────────────
  /**
   * handleSearch: called when the user clicks the Search button.
   *
   * Flow:
   *   1. Set loading state → UI shows "Searching..."
   *   2. Build the URL: /api/search?query=<userInput>
   *   3. await fetch() → waits for Spring Boot to respond
   *   4. await response.json() → parses the JSON body
   *   5. Update state with the result (triggers a re-render)
   *
   * NOTE: We're using template literals (backticks) to embed
   * the query variable directly into the URL string.
   * encodeURIComponent() encodes special characters in the query
   * so they're safe to include in a URL.
   * Example: "hello world" → "hello%20world"
   */
  const handleSearch = useCallback(async () => {
    // Don't search if the box is empty
    if (!query.trim()) return

    setIsLoading(true)
    setError(null)
    setResult(null)

    try {
      // Build the request URL — this is what ZAP will intercept!
      // ⚠️  Notice: we send the raw user input to the backend.
      //     No validation. This is intentional for the DAST demo.
      const url = `${API_BASE_URL}/api/search?query=${encodeURIComponent(query)}`

      // Make the GET request to Spring Boot
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          // Tell the server we accept JSON back
          'Accept': 'application/json',
        },
      })

      // Check if the HTTP status code indicates success (200–299)
      if (!response.ok) {
        throw new Error(`Server error: ${response.status} ${response.statusText}`)
      }

      // Parse the JSON response body
      // Our Spring Boot controller returns: {"result": "you searched for: ..."}
      const data = await response.json()

      // Update state with the "result" field from the JSON
      setResult(data.result)

    } catch (err) {
      // Network error or JSON parse error
      setError(err.message || 'Something went wrong. Is the backend running on port 8080?')
    } finally {
      // Always stop loading, whether it succeeded or failed
      setIsLoading(false)
    }
  }, [query]) // useCallback depends on "query" — recreate if query changes

  // ── KEYBOARD SUPPORT ─────────────────────────────────────
  // Allow pressing Enter in the input box to trigger search
  const handleKeyDown = (e) => {
    if (e.key === 'Enter') handleSearch()
  }

  // ── API CALL: LOGIN (SQL Injection demo) ─────────────────
  /**
   * handleLogin: POSTs username + password to /api/login.
   *
   * ⚠️  VULNERABILITY DEMO — SQL Injection (CWE-89)
   *
   * The backend concatenates these values directly into a SQL
   * query string WITHOUT parameterized queries / prepared statements.
   *
   * Try the injection payload in the Username field:
   *   admin' --
   * This turns the backend query into:
   *   SELECT * FROM users WHERE username='admin' --' AND password='...'
   * The -- comments out the password check → authentication bypass!
   *
   * The response includes the constructed query so you can see
   * exactly what was injected. ZAP's active scanner will also
   * detect this automatically.
   */
  const handleLogin = useCallback(async () => {
    if (!username.trim()) return

    setLoginLoading(true)
    setLoginError(null)
    setLoginResult(null)

    try {
      // POST to /api/login with a FORM-ENCODED body (not JSON).
      //
      // ⚠️  WHY FORM-ENCODED MATTERS FOR ZAP:
      //     ZAP's active scanner auto-fuzzes URL params and form fields.
      //     It does NOT automatically fuzz JSON bodies (@RequestBody).
      //     Using application/x-www-form-urlencoded makes the username
      //     and password parameters visible and fuzzable by ZAP.
      const formData = new URLSearchParams()
      formData.append('username', username)
      formData.append('password', password)

      const response = await fetch(`${API_BASE_URL}/api/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'Accept': 'application/json',
        },
        // Body looks like: username=admin&password=password123
        // ZAP will intercept this and fuzz each field individually
        body: formData.toString(),
      })

      if (!response.ok) {
        throw new Error(`Server error: ${response.status} ${response.statusText}`)
      }

      const data = await response.json()
      // data = { success: bool, message: string, query: string }
      setLoginResult(data)

    } catch (err) {
      setLoginError(err.message || 'Something went wrong. Is the backend running on port 8080?')
    } finally {
      setLoginLoading(false)
    }
  }, [username, password])

  const handleLoginKeyDown = (e) => {
    if (e.key === 'Enter') handleLogin()
  }

  // ── RENDER ───────────────────────────────────────────────
  // JSX: JavaScript XML — looks like HTML but it's actually JavaScript.
  // Each JSX element compiles to React.createElement() under the hood.
  return (
    <div className="app-container">
      {/* Header Section */}
      <header className="app-header">
        <div className="header-badge">DAST Learning Lab</div>
        <h1 className="app-title">Spring Boot Search API</h1>
        <p className="app-subtitle">
          A minimal full-stack app for learning Spring Boot &amp; OWASP ZAP
        </p>
      </header>

      {/* Tab Navigation */}
      <nav className="tab-nav" role="tablist">
        <button
          id="tab-search"
          className={`tab-btn ${activeTab === 'search' ? 'active' : ''}`}
          role="tab"
          aria-selected={activeTab === 'search'}
          onClick={() => setActiveTab('search')}
        >
          🔍 Search (Reflected XSS)
        </button>
        <button
          id="tab-login"
          className={`tab-btn ${activeTab === 'login' ? 'active' : ''}`}
          role="tab"
          aria-selected={activeTab === 'login'}
          onClick={() => setActiveTab('login')}
        >
          🔓 Login (SQL Injection)
        </button>
      </nav>

      {/* ── SEARCH PANEL ──────────────────────────────────── */}
      {activeTab === 'search' && (
        <main className="search-card" role="tabpanel" aria-labelledby="tab-search">
          <div className="search-section">
            <label htmlFor="search-input" className="search-label">
              Enter a search query
            </label>

            <div className="input-row">
              <input
                id="search-input"
                type="text"
                className="search-input"
                placeholder='e.g. hello world or <script>alert(1)</script>'
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={isLoading}
                autoComplete="off"
              />

              <button
                id="search-button"
                className={`search-button ${isLoading ? 'loading' : ''}`}
                onClick={handleSearch}
                disabled={isLoading || !query.trim()}
              >
                {isLoading ? (
                  <span className="spinner-wrapper">
                    <span className="spinner" />
                    Searching…
                  </span>
                ) : (
                  'Search'
                )}
              </button>
            </div>

            <p className="hint">
              💡 Tip: Try entering <code>&lt;script&gt;alert(1)&lt;/script&gt;</code> — ZAP uses payloads like this to test for XSS
            </p>
          </div>

          {/* Result / Error Display */}
          <div className="result-section">
            {error && (
              <div className="result-box error-box" role="alert">
                <span className="result-icon">⚠️</span>
                <div>
                  <strong>Error</strong>
                  <p>{error}</p>
                </div>
              </div>
            )}

            {result && !error && (
              <div className="result-box success-box" role="status">
                <span className="result-icon">✅</span>
                <div>
                  <strong>Backend Response</strong>
                  <p className="result-text">{result}</p>
                </div>
              </div>
            )}

            {!result && !error && !isLoading && (
              <div className="placeholder-text">
                Your response from the Spring Boot API will appear here
              </div>
            )}
          </div>
        </main>
      )}

      {/* ── LOGIN / SQL INJECTION PANEL ───────────────────── */}
      {activeTab === 'login' && (
        <main className="login-card" role="tabpanel" aria-labelledby="tab-login">

          {/* Vulnerability badge */}
          <div>
            <span className="vuln-badge">SQL Injection — CWE-89 — OWASP A03</span>
            <p className="login-heading">Vulnerable Login Form</p>
            <p className="login-description">
              The backend builds its SQL query by concatenating your input directly —
              no prepared statements, no parameterization.
              Submit the form, then check the <code>query</code> field in the
              response to see exactly what gets executed.
            </p>
          </div>

          {/* Injection tip */}
          <div className="inject-tip">
            💉 <strong>Try the bypass payload</strong> in the Username field:{' '}
            <code>admin&apos; --</code><br />
            This comments out the password check entirely, granting access as{' '}
            <code>admin</code> with any (or empty) password.
          </div>

          {/* Login form fields */}
          <div className="login-fields">
            <div>
              <label htmlFor="login-username" className="field-label">Username</label>
              <input
                id="login-username"
                type="text"
                className="login-input"
                placeholder="e.g.  admin  or  admin' --"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                onKeyDown={handleLoginKeyDown}
                disabled={loginLoading}
                autoComplete="off"
              />
            </div>
            <div>
              <label htmlFor="login-password" className="field-label">Password</label>
              <input
                id="login-password"
                type="text"
                className="login-input"
                placeholder="e.g.  password123  or anything when injecting"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onKeyDown={handleLoginKeyDown}
                disabled={loginLoading}
                autoComplete="off"
              />
            </div>

            <button
              id="login-button"
              className="login-button"
              onClick={handleLogin}
              disabled={loginLoading || !username.trim()}
            >
              {loginLoading ? (
                <span className="spinner-wrapper">
                  <span className="spinner" />
                  Logging in…
                </span>
              ) : (
                'Log In'
              )}
            </button>
          </div>

          {/* Login error */}
          {loginError && (
            <div className="login-result-box login-fail" role="alert">
              <span className="result-icon">⚠️</span>
              <div>
                <strong>Error</strong>
                <p className="login-message">{loginError}</p>
              </div>
            </div>
          )}

          {/* Login result */}
          {loginResult && !loginError && (
            <>
              <div
                className={`login-result-box ${loginResult.success ? 'login-success' : 'login-fail'}`}
                role="status"
              >
                <span className="result-icon">{loginResult.success ? '✅' : '❌'}</span>
                <div>
                  <strong>Auth Result</strong>
                  <p className="login-message">{loginResult.message}</p>
                </div>
              </div>

              {/* The constructed SQL query — core of the vulnerability demo */}
              {loginResult.query && (
                <div className="query-display">
                  <div className="query-display-label">⚠️ Constructed SQL Query (Backend)</div>
                  <div className="query-text">{loginResult.query}</div>
                </div>
              )}
            </>
          )}
        </main>
      )}

      {/* Info Panel */}
      <section className="info-panel">
        <div className="info-card">
          <h3>🖥️ Backend</h3>
          <code>GET /api/search?query=...</code>
          <p>Spring Boot on port <strong>8080</strong></p>
        </div>
        <div className="info-card">
          <h3>⚛️ Frontend</h3>
          <code>React + Vite</code>
          <p>Dev server on port <strong>5173</strong></p>
        </div>
        <div className="info-card">
          <h3>🔍 ZAP Proxy</h3>
          <code>localhost:8090</code>
          <p>Intercept &amp; attack traffic</p>
        </div>
      </section>

      <footer className="app-footer">
        DAST Learning Lab · Spring Boot + React · Intentionally Vulnerable for Testing
      </footer>
    </div>
  )
}

export default App
