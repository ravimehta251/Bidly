package com.bidflare.auth;

import com.bidflare.AbstractIntegrationTest;
import com.bidflare.auth.dto.AuthResponse;
import com.bidflare.auth.dto.LoginRequest;
import com.bidflare.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void register_shouldReturnToken() {
        RegisterRequest req = new RegisterRequest("alice@example.com", "password123", "Alice");
        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
                "/api/auth/register", req, AuthResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().accessToken()).isNotBlank();
        assertThat(resp.getBody().tokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_withValidCredentials_shouldReturnToken() {
        // Register first
        RegisterRequest reg = new RegisterRequest("bob@example.com", "password123", "Bob");
        restTemplate.postForEntity("/api/auth/register", reg, AuthResponse.class);

        // Login
        LoginRequest login = new LoginRequest("bob@example.com", "password123");
        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
                "/api/auth/login", login, AuthResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().accessToken()).isNotBlank();
    }

    @Test
    void login_withWrongPassword_shouldReturn401() {
        RegisterRequest reg = new RegisterRequest("carol@example.com", "password123", "Carol");
        restTemplate.postForEntity("/api/auth/register", reg, AuthResponse.class);

        LoginRequest login = new LoginRequest("carol@example.com", "wrongpassword");
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/auth/login", login, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_withoutToken_shouldReturn401() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/auctions", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void duplicateRegister_shouldReturn409() {
        RegisterRequest req = new RegisterRequest("dave@example.com", "password123", "Dave");
        restTemplate.postForEntity("/api/auth/register", req, AuthResponse.class);

        ResponseEntity<String> resp2 = restTemplate.postForEntity(
                "/api/auth/register", req, String.class);
        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
