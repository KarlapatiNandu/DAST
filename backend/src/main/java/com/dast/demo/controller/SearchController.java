package com.dast.demo.controller;

import com.dast.demo.model.SearchResponse;

// ── Springdoc OpenAPI imports (Stage 2B) ─────────────────────────────────
// @Operation = describes WHAT the endpoint does
// @Parameter = describes each INPUT field
// These do NOT change behavior — they only add documentation to the spec.
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * =========================================================
 * MODULE 3: THE CONTROLLER (The Heart of the REST API)
 * =========================================================
 *
 * @RestController
 *   Tells Spring: "This class handles HTTP requests and returns data directly."
 *   It's a shortcut for @Controller + @ResponseBody.
 *   - @Controller  → registers this class as an MVC controller (Spring scans for it)
 *   - @ResponseBody → automatically converts your return value to JSON
 *                     (using Jackson, which is on the classpath via spring-boot-starter-web)
 *
 * Without @RestController, Spring would try to find a "view" (like an HTML template)
 * to render — not what we want for a REST API.
 *
 * @RequestMapping("/api")
 *   Sets the base URL prefix for ALL methods in this class.
 *   Any method endpoint is appended to this prefix.
 *   So a method mapped to "/search" becomes reachable at: /api/search
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
// ^^ @CrossOrigin(origins = "*")
//    Enables CORS (Cross-Origin Resource Sharing) for this controller.
//    By default, browsers block requests from a different origin
//    (e.g., React on http://localhost:5173 calling Java on http://localhost:8080).
//    origins = "*" means: allow requests from ANY origin (domain/port).
//    ⚠️  INTENTIONALLY PERMISSIVE for learning — NOT safe for production!
public class SearchController {

    /**
     * @GetMapping("/search")
     *   Maps HTTP GET requests to the path /api/search (base + this path).
     *   This method is called when a client does:
     *       GET http://localhost:8080/api/search?query=hello
     *
     *   Other HTTP method mappings (for reference):
     *   - @PostMapping    → POST requests
     *   - @PutMapping     → PUT requests
     *   - @DeleteMapping  → DELETE requests
     *   - @PatchMapping   → PATCH requests
     *
     * @param query
     *   @RequestParam — extracts a query parameter from the URL.
     *   URL: /api/search?query=hello  →  query = "hello"
     *
     *   Optional attributes:
     *   - required = true (default) → 400 Bad Request if missing
     *   - defaultValue = "..."      → fallback if not provided
     *
     * @return SearchResponse
     *   The returned object is automatically serialized to JSON by Jackson.
     *   new SearchResponse("you searched for: hello")
     *       → {"result": "you searched for: hello"}
     */
    // ── OpenAPI Annotations (Stage 2B) ────────────────────────────────────
    // @Operation documents this endpoint in the OpenAPI spec / Swagger UI.
    // Even though the response is JSON (which mitigates XSS execution in
    // browsers), ZAP still tests for reflected content in the response body.
    @Operation(
        summary = "Vulnerable search endpoint",
        description = "Demonstrates Reflected XSS in JSON context. "
                    + "User input is echoed in the JSON response without sanitization. "
                    + "While JSON Content-Type prevents browser script execution, "
                    + "the unsanitized reflection is still a finding ZAP reports."
    )
    @GetMapping("/search")
    public SearchResponse search(
            @Parameter(description = "Search query - injectable, reflected in JSON response")
            @RequestParam(value = "query", required = false, defaultValue = "") String query
            // ↑ value = "query"       : the URL parameter name (?query=...)
            // ↑ required = false      : no error if client doesn't send ?query=
            // ↑ defaultValue = ""     : use empty string if param is missing
    ) {
        // Build the response message
        // NOTE: No sanitization is done here — intentional for DAST demo!
        // ZAP will detect this as a potential reflected XSS / injection point.
        String responseMessage = "you searched for: " + query;

        // Wrap the message in our response object and return it.
        // Spring automatically converts this to JSON.
        return new SearchResponse(responseMessage);
    }
}
