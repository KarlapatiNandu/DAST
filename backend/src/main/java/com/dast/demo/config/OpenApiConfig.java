package com.dast.demo.config;

// =============================================================================
// OpenApiConfig.java — OpenAPI 3.0 Metadata Configuration (Stage 2B)
// =============================================================================
//
// PURPOSE:
//   This class configures the metadata that appears in the generated OpenAPI
//   specification (the JSON at /v3/api-docs) and in the Swagger UI header.
//
// WHY A SEPARATE CONFIG CLASS?
//   Springdoc auto-generates the spec from your @RestController annotations,
//   but it doesn't know your API's title, description, or which server to
//   target. This class provides that information.
//
// WHY ZAP NEEDS THE SERVER URL:
//   The OpenAPI spec includes a "servers" array listing the base URLs.
//   When ZAP imports the spec, it reads the server URL to know WHERE to
//   send its attack requests. Without it, ZAP would see the endpoints
//   but wouldn't know the host/port to target.
//
//   Example in the generated JSON:
//     "servers": [{ "url": "http://localhost:8080", "description": "..." }]
//
// ANNOTATIONS USED:
//   @Configuration — Tells Spring: "scan this class for configuration beans."
//     This is how Spring finds this class during startup.
//     Without it, Spring ignores this file entirely.
//
//   @OpenAPIDefinition — A Springdoc annotation that sets metadata for the
//     entire API specification. It's equivalent to manually writing the
//     "info" and "servers" sections of an OpenAPI 3.0 YAML/JSON file.
// =============================================================================

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

// @Configuration
//   Marks this class as a Spring configuration class.
//   Spring scans for @Configuration classes during startup and processes
//   any annotations or @Bean methods they contain.
//   Without this, the @OpenAPIDefinition annotation would be ignored.
@Configuration

// @OpenAPIDefinition
//   Sets the top-level metadata for the generated OpenAPI specification.
//   This annotation is read by Springdoc when it generates /v3/api-docs.
//
//   info = @Info(...)
//     Sets the "info" object in the OpenAPI spec:
//       - title: displayed at the top of Swagger UI
//       - description: shown below the title
//       - version: the API version (not the app version)
//
//   servers = { @Server(...) }
//     Sets the "servers" array in the OpenAPI spec.
//     ZAP reads this to know where to send attack requests.
//     Without it, ZAP would import the spec but have no target URL.
//
//     url = "http://localhost:8080"
//       The base URL of our Spring Boot app.
//       All endpoint paths (like /api/login) are appended to this.
//       ZAP constructs full URLs like: http://localhost:8080/api/login
//
//     description = "Local development server"
//       A human-readable label. In production, you might have multiple
//       servers (dev, staging, prod) with different URLs.
@OpenAPIDefinition(
    info = @Info(
        title = "DAST Learning Lab API",
        // ↑ This title appears at the top of the Swagger UI page
        //   and in the "info.title" field of the JSON spec.

        description = "Intentionally vulnerable API for security testing. "
                    + "This API contains deliberate vulnerabilities (SQL Injection, "
                    + "Reflected XSS) for DAST learning purposes. "
                    + "DO NOT deploy to production.",
        // ↑ Explains the purpose of this API. The warning about production
        //   is important — this API is MEANT to be vulnerable.

        version = "1.0"
        // ↑ API specification version. This is NOT the same as the
        //   project version (0.0.1-SNAPSHOT in build.gradle).
        //   It's the version of the API contract/documentation.
    ),
    servers = {
        @Server(
            url = "http://localhost:8080",
            // ↑ CRITICAL FOR ZAP:
            //   When ZAP imports the OpenAPI spec, it reads this URL to know
            //   where to send its attack requests. Without this server entry,
            //   ZAP would parse the spec but wouldn't know the target host.
            //
            //   The full attack URL is constructed as:
            //     server.url + endpoint.path
            //     http://localhost:8080 + /api/login = http://localhost:8080/api/login

            description = "Local development server"
            // ↑ Human-readable label for this server environment.
            //   If you had staging/production servers, you'd add more
            //   @Server entries with different URLs and descriptions.
        )
    }
)
public class OpenApiConfig {
    // This class has no methods or fields.
    // Its only purpose is to carry the @OpenAPIDefinition annotation.
    // Spring processes the annotation during startup and passes the
    // metadata to Springdoc, which includes it in the generated spec.
    //
    // Think of this class as a "configuration container" — it exists
    // solely to hold annotations that configure library behavior.
}
