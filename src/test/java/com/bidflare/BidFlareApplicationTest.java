package com.bidflare;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: Spring context loads successfully.
 * Uses the "test" profile which spins up real Postgres + Redis via Testcontainers.
 */
@SpringBootTest
@ActiveProfiles("test")
class BidFlareApplicationTest {

    @Test
    void contextLoads() {
        // If the context fails to start, this test fails — that's the whole point.
    }
}
