package com.dast.demo.controller;

// ── Springdoc OpenAPI imports (Stage 2B) ─────────────────────────────────
// @Operation = describes WHAT the endpoint does
// @Parameter = describes each INPUT field
// These do NOT change behavior — they only add documentation to the spec.
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * =========================================================
 * XssController — Demonstrates Reflected XSS Vulnerability
 * =========================================================
 *
 * VULNERABILITY: Reflected Cross-Site Scripting (CWE-79)
 * SEVERITY:      High
 * OWASP:         A03:2021 – Injection
 *
 * ── What is Reflected XSS? ────────────────────────────────
 * Reflected XSS occurs when user-supplied input is included
 * directly in the server's HTML response WITHOUT sanitization
 * or encoding. An attacker can craft a URL containing malicious
 * JavaScript — anyone who clicks that link executes the script
 * in their browser, in the context of the vulnerable site.
 *
 * ── Why ZAP Detects This ──────────────────────────────────
 * ZAP's active scanner injects payloads like:
 *   <script>alert(1)</script>
 *   "><img src=x onerror=alert(1)>
 *   javascript:alert(1)
 *
 * It then checks whether the EXACT payload appears unescaped
 * in the HTML response. Because this endpoint returns raw HTML
 * (Content-Type: text/html) with the user input directly
 * concatenated in, ZAP immediately confirms the XSS.
 *
 * KEY INSIGHT: ZAP needs the response to be text/html.
 * If the response is application/json, browsers won't execute
 * embedded scripts, so ZAP won't flag it as XSS.
 *
 * ── How to exploit this endpoint ──────────────────────────
 * Normal request:
 *   GET /api/greet?name=John
 *   → Returns: <h1>Hello, John!</h1>
 *
 * XSS attack:
 *   GET /api/greet?name=<script>alert('XSS')</script>
 *   → Returns: <h1>Hello, <script>alert('XSS')</script>!</h1>
 *   The browser executes the script tag!
 *
 * ── The Secure Fix ────────────────────────────────────────
 * 1. HTML-encode all user input before embedding in HTML:
 *      import org.springframework.web.util.HtmlUtils;
 *      String safe = HtmlUtils.htmlEscape(name);
 *    This converts < to &lt;, > to &gt;, etc.
 *
 * 2. Or better yet: return JSON (application/json) and let
 *    the frontend framework (React, Angular) handle rendering.
 *    Frameworks auto-escape by default.
 *
 * 3. Set Content-Security-Policy headers to block inline scripts.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
// ^^ CORS wildcard: intentionally permissive for this DAST demo
public class XssController {

    /**
     * GET /api/greet?name=...
     *
     * Returns an HTML page that greets the user by name.
     *
     * ⚠️  INTENTIONALLY VULNERABLE — DO NOT USE IN PRODUCTION ⚠️
     *
     * The name parameter is embedded directly into the HTML response
     * without any sanitization or encoding. This creates a textbook
     * Reflected XSS vulnerability.
     *
     * produces = "text/html" tells Spring to set the Content-Type
     * header to text/html, which is CRITICAL for ZAP detection.
     * If the response were application/json, the browser wouldn't
     * parse it as HTML, and ZAP wouldn't flag XSS.
     *
     * @param name  the user-supplied name to greet
     * @return      raw HTML with the name embedded (unsanitized!)
     */
    // ── OpenAPI Annotations (Stage 2B) ────────────────────────────────────
    // @Operation documents this endpoint in the OpenAPI spec / Swagger UI.
    // ZAP reads the spec and knows to test this endpoint with XSS payloads.
    @Operation(
        summary = "Vulnerable greeting endpoint",
        description = "Demonstrates Reflected XSS - CWE-79. "
                    + "User input is embedded directly into an HTML response "
                    + "without sanitization. The response Content-Type is text/html, "
                    + "which means browsers will execute any injected scripts."
    )
    @GetMapping(value = "/greet", produces = MediaType.TEXT_HTML_VALUE)
    public String greet(
            @Parameter(description = "Name field - injectable via unsanitized HTML embedding")
            @RequestParam(value = "name", required = false, defaultValue = "World") String name
            // ↑ ZAP will fuzz this parameter with XSS payloads
    ) {
        // ══════════════════════════════════════════════════════
        // ⚠️  VULNERABILITY: Direct string concatenation into HTML
        //
        // The 'name' variable is inserted into the HTML page without
        // any escaping. If name contains <script>...</script>, the
        // browser will execute it as JavaScript.
        //
        // This is the simplest possible Reflected XSS — exactly
        // what OWASP ZAP's active scanner is designed to find.
        // ══════════════════════════════════════════════════════
        return "<!DOCTYPE html>"
             + "<html lang='en'>"
             + "<head>"
             + "<meta charset='UTF-8'>"
             + "<title>Greeting Page</title>"
             + "<style>"
             + "  body { font-family: 'Inter', system-ui, sans-serif; "
             + "         background: #0a0e1a; color: #e2e8f0; "
             + "         display: flex; justify-content: center; align-items: center; "
             + "         min-height: 100vh; margin: 0; }"
             + "  .card { background: #111827; border: 1px solid rgba(99,136,255,0.15); "
             + "          border-radius: 16px; padding: 40px 48px; "
             + "          box-shadow: 0 4px 24px rgba(0,0,0,0.4); text-align: center; }"
             + "  h1 { font-size: 2rem; margin-bottom: 12px; }"
             + "  .name { color: #818cf8; font-weight: 700; }"
             + "  p { color: #94a3b8; font-size: 14px; margin-top: 16px; }"
             + "  code { background: rgba(99,102,241,0.1); color: #818cf8; "
             + "         padding: 2px 8px; border-radius: 4px; font-size: 13px; }"
             + "</style>"
             + "</head>"
             + "<body>"
             + "<div class='card'>"
             + "  <h1>Hello, <span class='name'>" + name + "</span>!</h1>"
             // ↑↑↑ THIS IS THE VULNERABILITY ↑↑↑
             // 'name' is inserted raw — no HtmlUtils.htmlEscape()
             // If name = <script>alert(1)</script>, it executes!
             + "  <p>This page is <strong>intentionally vulnerable</strong> to "
             + "     Reflected XSS (CWE-79).</p>"
             + "  <p>Try: <code>/api/greet?name=&lt;script&gt;alert(1)&lt;/script&gt;</code></p>"
             + "</div>"
             + "</body>"
             + "</html>";
    }

    /**
     * GET /api/echo?input=...
     *
     * A second XSS-vulnerable endpoint that echoes user input
     * directly into an HTML response. This gives ZAP multiple
     * attack surfaces to scan.
     *
     * ⚠️  INTENTIONALLY VULNERABLE — DO NOT USE IN PRODUCTION ⚠️
     */
    // ── OpenAPI Annotations (Stage 2B) ────────────────────────────────────
    @Operation(
        summary = "Vulnerable echo endpoint",
        description = "Second Reflected XSS example - CWE-79. "
                    + "Echoes user input directly into HTML without escaping. "
                    + "Provides ZAP with an additional XSS attack surface."
    )
    @GetMapping(value = "/echo", produces = MediaType.TEXT_HTML_VALUE)
    public String echo(
            @Parameter(description = "Input text - echoed directly into HTML without escaping")
            @RequestParam(value = "input", required = false, defaultValue = "") String input
    ) {
        // Same vulnerability pattern — raw user input in HTML
        return "<!DOCTYPE html>"
             + "<html lang='en'>"
             + "<head><meta charset='UTF-8'><title>Echo</title></head>"
             + "<body>"
             + "<h2>You entered:</h2>"
             + "<div>" + input + "</div>"
             // ↑ Direct injection point — no escaping
             + "</body>"
             + "</html>";
    }
}
