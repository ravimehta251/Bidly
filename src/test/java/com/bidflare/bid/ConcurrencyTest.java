package com.bidflare.bid;

import com.bidflare.AbstractIntegrationTest;
import com.bidflare.auction.Auction;
import com.bidflare.auction.AuctionRepository;
import com.bidflare.auction.AuctionStatus;
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
import com.bidflare.auth.dto.AuthResponse;
import com.bidflare.auth.dto.RegisterRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrencyTest extends AbstractIntegrationTest {

    private static final int NUM_USERS = 200;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BidRepository bidRepository;

    private Auction liveAuction;
    private List<String> tokens;
    private BigDecimal expectedWinAmount;

    @BeforeEach
    void setup() {
        // Create seller
        User seller = userRepository.findByEmail("concurrency-seller@test.com")
                .orElseGet(() -> userRepository.save(
                        new User("concurrency-seller@test.com",
                                passwordEncoder.encode("pass1234"), "ConcurrencySeller", UserRole.SELLER)));

        // Create LIVE auction
        Auction auction = new Auction();
        auction.setSeller(seller);
        auction.setTitle("Concurrency Test Auction " + System.currentTimeMillis());
        auction.setStartingPrice(new BigDecimal("100.00"));
        auction.setMinIncrement(new BigDecimal("1.00"));
        auction.setCurrentPrice(new BigDecimal("100.00"));
        auction.setStartTime(OffsetDateTime.now().minusMinutes(5));
        auction.setEndTime(OffsetDateTime.now().plusHours(1));
        auction.setStatus(AuctionStatus.LIVE);
        liveAuction = auctionRepository.save(auction);

        // Create 200 users and get their tokens
        tokens = new ArrayList<>(NUM_USERS);
        for (int i = 0; i < NUM_USERS; i++) {
            String email = "concurrency-bidder-" + System.currentTimeMillis() + "-" + i + "@test.com";
            RegisterRequest reg = new RegisterRequest(email, "password123", "Bidder" + i);
            ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
                    "/api/auth/register", reg, AuthResponse.class);
            tokens.add(resp.getBody().accessToken());
        }

        // The highest bid will be from user at index 199: 100 + 1*increment per user = user i bids (101 + i)
        // User 199 bids 300.00 which is the highest
        expectedWinAmount = new BigDecimal("300.00"); // 101 + 199 = 300
    }

    @Test
    void concurrentBids_exactlyOneWinner_finalPriceIsHighest() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_USERS);
        ExecutorService executor = Executors.newFixedThreadPool(NUM_USERS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < NUM_USERS; i++) {
            final int userIndex = i;
            final String token = tokens.get(i);
            // Each user bids a unique amount: 101, 102, ..., 300
            final BigDecimal amount = new BigDecimal(101 + userIndex);

            executor.submit(() -> {
                try {
                    startLatch.await();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(token);
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    PlaceBidRequest req = new PlaceBidRequest(amount, null);
                    ResponseEntity<BidResponse> resp = restTemplate.postForEntity(
                            "/api/auctions/" + liveAuction.getId() + "/bids",
                            new HttpEntity<>(req, headers), BidResponse.class);

                    if (resp.getStatusCode() == HttpStatus.CREATED) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(120, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).as("All bids should complete within 120 seconds").isTrue();
        assertThat(exceptions).as("No unexpected exceptions should occur").isEmpty();

        // Reload auction from DB
        Auction finalAuction = auctionRepository.findById(liveAuction.getId()).orElseThrow();

        // Final price should equal the highest valid bid placed
        assertThat(finalAuction.getCurrentPrice()).isEqualByComparingTo(expectedWinAmount);

        // There should be exactly one current winner
        assertThat(finalAuction.getCurrentWinner()).isNotNull();

        // Count bids — each unique user bid once, all bids should succeed
        // (each amount is strictly increasing, so only bids that beat the current price succeed
        //  sequentially. But since bids are concurrent, the exact number depends on ordering.)
        long bidCount = bidRepository.countByAuctionId(liveAuction.getId());
        assertThat(bidCount).isGreaterThan(0).isLessThanOrEqualTo(NUM_USERS);

        // The total successes + errors should be NUM_USERS
        assertThat(successCount.get() + errorCount.get()).isEqualTo(NUM_USERS);
    }
}
