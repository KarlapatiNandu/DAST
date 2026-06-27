package com.dast.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * =========================================================
 * ROOT HEALTH-CHECK CONTROLLER
 * =========================================================
 *
 * WHY THIS EXISTS:
 * Spring Boot does not map anything to "/" by default. When ZAP's spider
 * requests http://localhost:8080/, the server returns 404, which causes a
 * warning in the ZAP Automation Framework log:
 *   "Job spider error accessing URL http://localhost:8080 status code returned : 404 expected 200"
 *
 * This tiny controller returns a 200 OK at "/" so the spider's initial
 * check passes cleanly. It also doubles as a simple health-check endpoint
 * that confirms the backend is running.
 *
 * TEACHING NOTES:
 * - @RestController tells Spring this class handles HTTP requests and
 *   returns data directly (not a view/template name).
 * - @GetMapping("/") maps HTTP GET requests for "/" to the root() method.
 * - The method returns a plain String which Spring sends as text/plain.
 */
@RestController
public class RootController {

    // Respond to GET / with a simple status message.
    // This prevents ZAP spider from logging a 404 warning for the context root.
    @GetMapping("/")
    public String root() {
        return "DAST Learning Lab backend is running.";
    }
}
