package com.dast.demo.controller;

// ── Springdoc OpenAPI imports (Stage 2B) ─────────────────────────────────
// These annotations describe the endpoint for the OpenAPI specification.
// Springdoc reads them and includes the information in /v3/api-docs.
// @Operation = describes WHAT the endpoint does (summary + description)
// @Parameter = describes each INPUT field (name, description, where it comes from)
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
 * ── Previous Version vs This Version ──────────────────────
 * The old version "simulated" SQL — it built the query string
 * but never executed it. ZAP couldn't detect it because:
 *   - No SQL error messages when injecting bad syntax
 *   - No behavioral differences (boolean-based)
 *   - No timing differences
 *
 * THIS version uses a REAL H2 database with REAL JDBC calls.
 * When ZAP injects a single quote ('), H2 throws a real
 * SQL syntax error, and ZAP recognizes the error message
 * → confirmed SQL injection.
 *
 * ── How to exploit this endpoint ─────────────────────────
 * Normal request:
 *   username = admin
 *   password = password123
 *   → Query: SELECT * FROM users WHERE username='admin' AND password='password123'
 *   → Returns 1 row → login success
 *
 * Injection attack:
 *   username = admin' --
 *   password = anything
 *   → Query: SELECT * FROM users WHERE username='admin' --' AND password='anything'
 *             ↑ The -- comments out the password check entirely!
 *             An attacker logs in as 'admin' with any (or no) password.
 *
 * Another injection (bypass without knowing a username):
 *   username = ' OR '1'='1
 *   password = ' OR '1'='1
 *   → Query: SELECT * FROM users WHERE username='' OR '1'='1' AND password='' OR '1'='1'
 *   → Returns ALL rows → login success
 *
 * ── Why ZAP Now Finds This ────────────────────────────────
 * ZAP's Active Scanner injects SQL metacharacters and detects:
 *   1. ERROR-BASED: Injecting ' causes H2 to throw a real
 *      JdbcSQLSyntaxErrorException. ZAP sees SQL keywords
 *      in the error response → confirmed SQLi.
 *   2. BOOLEAN-BASED: ZAP sends OR 1=1 (returns rows) vs
 *      OR 1=2 (returns nothing). Different responses = SQLi.
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

    // ── DataSource is auto-configured by Spring Boot ──────
    // Spring sees H2 on the classpath + our application.properties
    // and automatically creates a DataSource bean pointing to
    // our in-memory H2 database.
    @Autowired
    private DataSource dataSource;

    /**
     * POST /api/login
     *
     * Accepts form-encoded body: username=...&password=...
     * Content-Type: application/x-www-form-urlencoded
     *
     * ⚠️  INTENTIONALLY VULNERABLE — DO NOT USE IN PRODUCTION ⚠️
     *
     * This endpoint executes a REAL SQL query against an H2 database
     * using string concatenation (Statement, NOT PreparedStatement).
     * This makes it vulnerable to SQL injection that ZAP can detect.
     *
     * @param username  from the form field "username"
     * @param password  from the form field "password"
     * @return          a JSON map with: success, message, query
     */
    // ── OpenAPI Annotations (Stage 2B) ────────────────────────────────────
    // @Operation tells Springdoc: "Here's what this endpoint does."
    //   summary  = short one-line description (shown in Swagger UI endpoint list)
    //   description = longer explanation (shown when you expand the endpoint)
    //
    // These annotations do NOT change behavior — they only add documentation
    // to the generated OpenAPI spec (/v3/api-docs) and Swagger UI.
    @Operation(
        summary = "Vulnerable login endpoint",
        description = "Demonstrates SQL Injection - CWE-89. "
                    + "This endpoint concatenates user input directly into SQL queries "
                    + "using java.sql.Statement instead of PreparedStatement. "
                    + "ZAP's active scanner will inject SQL metacharacters and detect "
                    + "error-based and boolean-based SQL injection."
    )
    @PostMapping("/login")
    public Map<String, Object> login(
            // @Parameter tells Springdoc: "Here's what this input field is."
            //   description = explains the parameter's purpose
            //   ZAP reads this to understand what kind of data to inject.
            @Parameter(description = "Username - injectable via SQL concatenation")
            @RequestParam(value = "username", defaultValue = "") String username,
            // ↑ ZAP will fuzz this parameter with SQL injection payloads
            @Parameter(description = "Password field - also injectable")
            @RequestParam(value = "password", defaultValue = "") String password
            // ↑ ZAP will fuzz this parameter too
    ) {

        // ══════════════════════════════════════════════════════
        // ⚠️  VULNERABILITY: String concatenation into SQL query
        //
        // We use Statement (not PreparedStatement) and concatenate
        // user input directly into the SQL string.
        //
        // When ZAP sends: username = admin'
        // The query becomes:
        //   SELECT * FROM users WHERE username='admin'' AND password=''
        // H2 throws: JdbcSQLSyntaxErrorException
        // ZAP sees "Syntax error" in the response → confirmed SQLi!
        // ══════════════════════════════════════════════════════
        String sql = "SELECT * FROM users WHERE username='" + username
                   + "' AND password='" + password + "'";

        Map<String, Object> response = new HashMap<>();
        response.put("query", sql);  // Show the query (info disclosure — intentional)

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // ↑ This ACTUALLY EXECUTES the SQL against H2!
            // If the query is malformed (injection), H2 throws an exception.
            // If the injection is valid SQL (e.g., OR 1=1), it returns rows.

            if (rs.next()) {
                // At least one row matched → "authenticated"
                String foundUser = rs.getString("username");
                String foundRole = rs.getString("role");
                response.put("success", true);
                response.put("message", "Login successful! Welcome, " + foundUser
                           + " (role: " + foundRole + ").");
            } else {
                // No rows matched
                response.put("success", false);
                response.put("message", "Login failed. Invalid credentials.");
            }

        } catch (SQLException e) {
            // ══════════════════════════════════════════════════
            // ⚠️  VULNERABILITY: Exposing SQL error messages
            //
            // Returning the raw SQL exception message to the client
            // is an information disclosure vulnerability (CWE-209).
            // ZAP uses these error messages to CONFIRM SQL injection:
            //   - "Syntax error in SQL statement"
            //   - "Column ... not found"
            //   - H2-specific error codes
            //
            // In production, NEVER expose database errors to users.
            // Return a generic "Something went wrong" instead.
            // ══════════════════════════════════════════════════
            response.put("success", false);
            response.put("message", "SQL Error: " + e.getMessage());
            response.put("errorClass", e.getClass().getSimpleName());
        }

        return response;
    }
}
