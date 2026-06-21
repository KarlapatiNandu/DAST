package com.dast.demo.model;

/**
 * =========================================================
 * LoginRequest — Request body model for the login endpoint
 * =========================================================
 *
 * Jackson automatically deserializes the incoming JSON body
 * into this POJO when the controller uses @RequestBody.
 *
 * Incoming JSON example:
 *   { "username": "admin", "password": "secret" }
 *
 * Maps to:
 *   loginRequest.getUsername() → "admin"
 *   loginRequest.getPassword() → "secret"
 */
public class LoginRequest {

    private String username;
    private String password;

    // Default no-arg constructor required by Jackson for deserialization
    public LoginRequest() {}

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
