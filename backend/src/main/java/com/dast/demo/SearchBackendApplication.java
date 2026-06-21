package com.dast.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * =========================================================
 * MODULE 1: APPLICATION ENTRY POINT
 * =========================================================
 *
 * @SpringBootApplication is a "convenience annotation" that combines three annotations:
 *
 *   1. @Configuration     — marks this class as a source of Spring bean definitions
 *   2. @EnableAutoConfiguration — tells Spring Boot to automatically configure
 *                                 your app based on the JARs on your classpath.
 *                                 (e.g., sees spring-web on classpath → sets up Tomcat + MVC)
 *   3. @ComponentScan     — tells Spring to scan this package (and sub-packages)
 *                           for @Component, @Service, @RestController, etc.
 *
 * Think of this as the "main class" that wires everything together.
 */
@SpringBootApplication
public class SearchBackendApplication {

    public static void main(String[] args) {
        // SpringApplication.run() does the heavy lifting:
        //   - Starts the embedded Tomcat server
        //   - Loads all your beans and configurations
        //   - Makes your app ready to handle HTTP requests
        SpringApplication.run(SearchBackendApplication.class, args);
    }
}
