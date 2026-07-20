package com.bidflare.auction;

import com.bidflare.AbstractIntegrationTest;
import com.bidflare.auth.dto.AuthResponse;
import com.bidflare.auth.dto.RegisterRequest;
import com.bidflare.auction.dto.AuctionResponse;
import com.bidflare.auction.dto.CreateAuctionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionLifecycleTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String getToken(String email, String name) {
        RegisterRequest reg = new RegisterRequest(email, "password123", name);
        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
                "/api/auth/register", reg, AuthResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().accessToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void createAuction_shouldReturnScheduledStatus() {
        String token = getToken("seller1@test.com", "Seller1");

        CreateAuctionRequest req = new CreateAuctionRequest(
                "Test Widget",
                "A fine widget",
                new BigDecimal("10.00"),
                new BigDecimal("1.00"),
                OffsetDateTime.now().plusHours(1),
                OffsetDateTime.now().plusHours(2)
        );

        HttpEntity<CreateAuctionRequest> entity = new HttpEntity<>(req, authHeaders(token));
        ResponseEntity<AuctionResponse> resp = restTemplate.postForEntity(
                "/api/auctions", entity, AuctionResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().status()).isEqualTo(AuctionStatus.SCHEDULED);
        assertThat(resp.getBody().currentPrice()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void listAuctions_shouldReturnPagedResults() {
        String token = getToken("seller2@test.com", "Seller2");
        HttpHeaders headers = authHeaders(token);

        CreateAuctionRequest req = new CreateAuctionRequest(
                "Widget 2", null,
                new BigDecimal("5.00"), new BigDecimal("0.50"),
                OffsetDateTime.now().plusMinutes(30),
                OffsetDateTime.now().plusHours(3)
        );
        restTemplate.postForEntity("/api/auctions", new HttpEntity<>(req, headers), AuctionResponse.class);

        ResponseEntity<String> listResp = restTemplate.exchange(
                "/api/auctions", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).contains("content");
    }

    @Test
    void getAuction_byId_shouldReturn200() {
        String token = getToken("seller3@test.com", "Seller3");
        HttpHeaders headers = authHeaders(token);

        CreateAuctionRequest req = new CreateAuctionRequest(
                "Widget 3", null,
                new BigDecimal("20.00"), null,
                OffsetDateTime.now().plusHours(1),
                OffsetDateTime.now().plusHours(4)
        );
        ResponseEntity<AuctionResponse> created = restTemplate.postForEntity(
                "/api/auctions", new HttpEntity<>(req, headers), AuctionResponse.class);
        Long id = created.getBody().id();

        ResponseEntity<AuctionResponse> resp = restTemplate.exchange(
                "/api/auctions/" + id, HttpMethod.GET, new HttpEntity<>(headers), AuctionResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().id()).isEqualTo(id);
        assertThat(resp.getBody().title()).isEqualTo("Widget 3");
    }

    @Test
    void getAuction_notFound_shouldReturn404() {
        String token = getToken("seller4@test.com", "Seller4");
        HttpHeaders headers = authHeaders(token);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/auctions/999999", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
