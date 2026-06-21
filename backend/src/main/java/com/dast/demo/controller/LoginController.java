package com.dast.demo.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * =========================================================
 * LoginController — Demonstrates SQL Injection Vulnerability
 * =========================================================
 *
 * VULNERABILITY: SQL Injection (CWE-89)
 * SEVERITY:      High
 * OWASP:         A03:2021 – Injection
 *
 * ── What is SQL Injection? ────────────────────────────────
 * SQL Injection happens when user-supplied input is concatenated
 * directly into a SQL query string instead of being treated as
 * data (via parameterized queries / prepared statements).
 *
 * An attacker can break out of the intended query and craft
 * their own SQL logic — bypassing authentication, reading
 * arbitrary data, or even dropping tables.
 *
 * ── How to exploit this endpoint ─────────────────────────
 * Normal request:
 *   username = admin
 *   password = secret
 *   → Query: SELECT * FROM users WHERE username='admin' AND password='secret'
 *
 * Injection attack:
 *   username = admin' --
 *   password = anything
 *   → Query: SELECT * FROM users WHERE username='admin' --' AND password='anything'
 *             ↑ The -- comments out the password check entirely!
 *             An attacker logs in as 'admin' with any (or no) password.
 *
 * ── Why ZAP Finds This ───────────────────────────────────
 * ZAP's Active Scanner injects SQL metacharacters (', --, OR 1=1, etc.)
 * and looks for:
 *   - Different response bodies depending on the payload
 *   - Error messages containing SQL keywords
 *   - Boolean-based differences (true vs false conditions)
 *
 * IMPORTANT: ZAP only fuzzes parameters it can see — URL query params
 * and form-encoded fields. JSON bodies (@RequestBody) are NOT auto-fuzzed.
 * This endpoint uses @RequestParam (form fields) so ZAP detects the SQLi.
 *
 * This endpoint exposes the constructed query in the response,
 * making the injection trivially visible.
 *
 * ── The Secure Fix ───────────────────────────────────────
 * Use PreparedStatement (parameterized queries):
 *   String sql = "SELECT * FROM users WHERE username=? AND password=?";
 *   PreparedStatement ps = conn.prepareStatement(sql);
 *   ps.setString(1, username);
 *   ps.setString(2, password);
 * The ? placeholders are filled in by the JDBC driver as data,
 * not as SQL code — injection is impossible.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
// ^^ CORS wildcard: intentionally permissive for this DAST demo
public class LoginController {

    // ── Hardcoded credentials (simulated "database") ──────
    // In a real app these would be in a database with hashed passwords.
    // Hardcoding credentials is itself a vulnerability (CWE-798),
    // but here it just gives us a known valid login to test with.
    private static final String VALID_USER = "admin";
    private static final String VALID_PASS = "password123";

    /**
     * POST /api/login
     *
     * Accepts form-encoded body: username=...&password=...
     * Content-Type: application/x-www-form-urlencoded
     *
     * ⚠️  INTENTIONALLY VULNERABLE — DO NOT USE IN PRODUCTION ⚠️
     *
     * Using @RequestParam (form fields) instead of @RequestBody (JSON)
     * is essential for ZAP detection. ZAP's active scanner automatically
     * fuzzes URL parameters and form fields — it does NOT fuzz JSON bodies
     * by default. This makes the SQLi visible to ZAP's scanner.
     *
     * @param username  from the form field "username"
     * @param password  from the form field "password"
     * @return          a JSON map with: success, message, query (for demo)
     */
    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestParam(value = "username", defaultValue = "") String username,
            // ↑ ZAP will fuzz this parameter with SQL injection payloads
            @RequestParam(value = "password", defaultValue = "") String password
            // ↑ ZAP will fuzz this parameter too
    ) {

        // ══════════════════════════════════════════════════════
        // ⚠️  VULNERABILITY: String concatenation into SQL query
        //
        // A real app would execute this against a database.
        // Here we simulate it — the query is constructed exactly
        // as a vulnerable JDBC/ODBC call would build it.
        //
        // If username = admin' --
        // the query becomes:
        //   SELECT * FROM users WHERE username='admin' --' AND password='...'
        // which bypasses the password check entirely.
        // ══════════════════════════════════════════════════════
        String simulatedQuery =
            "SELECT * FROM users WHERE username='" + username +
            "' AND password='" + password + "'";

        // Simulate auth: check against our hardcoded "database"
        boolean isAuthenticated =
            VALID_USER.equals(username) && VALID_PASS.equals(password);

        // Build response map (Jackson will convert this to JSON)
        Map<String, Object> response = new HashMap<>();
        response.put("success", isAuthenticated);
        response.put("message", isAuthenticated
                ? "Login successful! Welcome, " + username + "."
                : "Login failed. Invalid credentials.");
        // ⚠️  Exposing the query in the response is an additional
        //     vulnerability (information disclosure) — intentional here
        //     so you can see exactly what gets injected.
        response.put("query", simulatedQuery);

        return response;
    }
}
