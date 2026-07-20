package com.bidflare.bid;

import com.bidflare.AbstractIntegrationTest;
import com.bidflare.auction.Auction;
import com.bidflare.auction.AuctionRepository;
import com.bidflare.auction.AuctionStatus;
import com.bidflare.auth.dto.AuthResponse;
import com.bidflare.auth.dto.RegisterRequest;
import com.bidflare.bid.dto.BidResponse;
import com.bidflare.bid.dto.PlaceBidRequest;
import com.bidflare.user.User;
import com.bidflare.user.UserRepository;
import com.bidflare.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BidServiceTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Auction liveAuction;
    private String bidderToken;
    private Long bidderId;

    @BeforeEach
    void setup() {
        // Create a seller
        final User newSeller = new User("bid-seller@test.com", passwordEncoder.encode("pass1234"), "BidSeller", UserRole.SELLER);
        // Check if exists (tests may share context)
        User seller = userRepository.findByEmail("bid-seller@test.com").orElseGet(() -> userRepository.save(newSeller));

        // Create a live auction
        Auction auction = new Auction();
        auction.setSeller(seller);
        auction.setTitle("Live Auction");
        auction.setStartingPrice(new BigDecimal("100.00"));
        auction.setMinIncrement(new BigDecimal("5.00"));
        auction.setCurrentPrice(new BigDecimal("100.00"));
        auction.setStartTime(OffsetDateTime.now().minusMinutes(10));
        auction.setEndTime(OffsetDateTime.now().plusHours(2));
        auction.setStatus(AuctionStatus.LIVE);
        liveAuction = auctionRepository.save(auction);

        // Register a bidder
        RegisterRequest reg = new RegisterRequest(
                "bidder-" + System.currentTimeMillis() + "@test.com", "password123", "TestBidder");
        ResponseEntity<AuthResponse> authResp = restTemplate.postForEntity(
                "/api/auth/register", reg, AuthResponse.class);
        bidderToken = authResp.getBody().accessToken();

        // Get the bidder id
        bidderId = userRepository.findByEmail(reg.email()).map(User::getId).orElseThrow();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bidderToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void placeBid_validAmount_shouldSucceed() {
        PlaceBidRequest req = new PlaceBidRequest(new BigDecimal("110.00"), null);
        HttpEntity<PlaceBidRequest> entity = new HttpEntity<>(req, authHeaders());

        ResponseEntity<BidResponse> resp = restTemplate.postForEntity(
                "/api/auctions/" + liveAuction.getId() + "/bids", entity, BidResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().amount()).isEqualByComparingTo(new BigDecimal("110.00"));
        assertThat(resp.getBody().auctionId()).isEqualTo(liveAuction.getId());
    }

    @Test
    void placeBid_tooLow_shouldReturn400() {
        PlaceBidRequest req = new PlaceBidRequest(new BigDecimal("100.50"), null);
        HttpEntity<PlaceBidRequest> entity = new HttpEntity<>(req, authHeaders());

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/auctions/" + liveAuction.getId() + "/bids", entity, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getBids_shouldReturnBidList() {
        // Place a bid first
        PlaceBidRequest req = new PlaceBidRequest(new BigDecimal("115.00"), "idem-key-1");
        restTemplate.postForEntity(
                "/api/auctions/" + liveAuction.getId() + "/bids",
                new HttpEntity<>(req, authHeaders()), BidResponse.class);

        ResponseEntity<String> listResp = restTemplate.exchange(
                "/api/auctions/" + liveAuction.getId() + "/bids",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).contains("content");
    }

    @Test
    void placeBid_withIdempotencyKey_shouldReturnSameBid() {
        String idempotencyKey = "unique-key-" + System.currentTimeMillis();
        PlaceBidRequest req = new PlaceBidRequest(new BigDecimal("120.00"), idempotencyKey);

        ResponseEntity<BidResponse> resp1 = restTemplate.postForEntity(
                "/api/auctions/" + liveAuction.getId() + "/bids",
                new HttpEntity<>(req, authHeaders()), BidResponse.class);

        ResponseEntity<BidResponse> resp2 = restTemplate.postForEntity(
                "/api/auctions/" + liveAuction.getId() + "/bids",
                new HttpEntity<>(req, authHeaders()), BidResponse.class);

        assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp2.getBody().id()).isEqualTo(resp1.getBody().id());
    }

    @Test
    void placeBid_onNonexistentAuction_shouldReturn404() {
        PlaceBidRequest req = new PlaceBidRequest(new BigDecimal("200.00"), null);
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/auctions/999999/bids",
                new HttpEntity<>(req, authHeaders()), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
