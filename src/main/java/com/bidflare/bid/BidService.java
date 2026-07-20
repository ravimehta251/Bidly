package com.bidflare.bid;

import com.bidflare.auction.Auction;
import com.bidflare.auction.AuctionRepository;
import com.bidflare.auction.AuctionStatus;
import com.bidflare.bid.dto.BidResponse;
import com.bidflare.bid.dto.PlaceBidRequest;
import com.bidflare.messaging.BidEventPublisher;
import com.bidflare.user.User;
import com.bidflare.user.UserRepository;
import jakarta.persistence.OptimisticLockException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class BidService {

    private static final Logger log = LoggerFactory.getLogger(BidService.class);
    private static final int MAX_RETRIES = 3;
    private static final long ANTI_SNIPE_WINDOW_SECONDS = 60L;
    private static final long ANTI_SNIPE_EXTENSION_SECONDS = 60L;

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final RedissonClient redissonClient;
    private final BidEventPublisher bidEventPublisher;
    // Self-reference so @Transactional proxy wraps the inner call
    private final BidTransactionHelper txHelper;

    public BidService(AuctionRepository auctionRepository,
                      BidRepository bidRepository,
                      UserRepository userRepository,
                      RedissonClient redissonClient,
                      BidEventPublisher bidEventPublisher,
                      BidTransactionHelper txHelper) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.userRepository = userRepository;
        this.redissonClient = redissonClient;
        this.bidEventPublisher = bidEventPublisher;
        this.txHelper = txHelper;
    }

    /**
     * Entry point: acquires the Redisson distributed lock, then delegates to the
     * transactional helper which Spring can proxy correctly.
     */
    public BidResponse placeBid(Long auctionId, PlaceBidRequest request, String bidderEmail) {
        // Fast-path idempotency check — before acquiring lock
        if (request.idempotencyKey() != null) {
            var existing = bidRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                return BidResponse.from(existing.get());
            }
        }

        RLock lock = redissonClient.getLock("lock:auction:" + auctionId);
        try {
            boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Could not acquire auction lock, please retry");
            }
            try {
                return doPlaceBidWithRetry(auctionId, request, bidderEmail, 0);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Interrupted while acquiring lock");
        }
    }

    /**
     * Retry loop around the single-transaction bid attempt.
     * Calls txHelper (a Spring-proxied bean) so @Transactional is honoured.
     */
    private BidResponse doPlaceBidWithRetry(Long auctionId, PlaceBidRequest request,
                                             String bidderEmail, int attempt) {
        try {
            return txHelper.executeBidInTransaction(auctionId, request, bidderEmail);
        } catch (OptimisticLockException e) {
            if (attempt < MAX_RETRIES) {
                log.warn("Optimistic lock conflict on auction {}, retry {}/{}", auctionId, attempt + 1, MAX_RETRIES);
                return doPlaceBidWithRetry(auctionId, request, bidderEmail, attempt + 1);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Auction is under heavy load, please retry");
        }
    }

    @Transactional(readOnly = true)
    public Page<BidResponse> getBidsForAuction(Long auctionId, Pageable pageable) {
        auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId, pageable)
                .map(BidResponse::from);
    }

    // ── Inner transaction helper ────────────────────────────────────────────
    // Extracted as a separate Spring bean so @Transactional is proxied correctly.
    @Service
    static class BidTransactionHelper {

        private final AuctionRepository auctionRepository;
        private final BidRepository bidRepository;
        private final UserRepository userRepository;
        private final BidEventPublisher bidEventPublisher;

        BidTransactionHelper(AuctionRepository auctionRepository,
                             BidRepository bidRepository,
                             UserRepository userRepository,
                             BidEventPublisher bidEventPublisher) {
            this.auctionRepository = auctionRepository;
            this.bidRepository = bidRepository;
            this.userRepository = userRepository;
            this.bidEventPublisher = bidEventPublisher;
        }

        /**
         * Executes the entire bid in a single database transaction.
         * Called from within the Redisson lock — no concurrent writes to this auction.
         * @Version retry is the second guard.
         */
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public BidResponse executeBidInTransaction(Long auctionId, PlaceBidRequest request,
                                                    String bidderEmail) {
                        Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));

            // Repeat the idempotency check inside the lock and transaction. This closes
            // the race where duplicate requests pass the fast-path check together.
            if (request.idempotencyKey() != null) {
                var existing = bidRepository.findByIdempotencyKey(request.idempotencyKey());
                if (existing.isPresent()) {
                    return BidResponse.from(existing.get());
                }
            }

            if (auction.getStatus() != AuctionStatus.LIVE) {

                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Auction is not live (status=" + auction.getStatus() + ")");
            }

            OffsetDateTime now = OffsetDateTime.now();
            if (now.isAfter(auction.getEndTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Auction has already ended");
            }

            BigDecimal minRequired = auction.getCurrentPrice().add(auction.getMinIncrement());
            if (request.amount().compareTo(minRequired) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Bid must be at least " + minRequired + " (current=" + auction.getCurrentPrice()
                                + " + increment=" + auction.getMinIncrement() + ")");
            }

            User bidder = userRepository.findByEmail(bidderEmail)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // Persist bid
            Bid bid = new Bid();
            bid.setAuction(auction);
            bid.setBidder(bidder);
            bid.setAmount(request.amount());
            bid.setIdempotencyKey(request.idempotencyKey());
            bid = bidRepository.save(bid);

            // Update auction price + winner
            auction.setCurrentPrice(request.amount());
            auction.setCurrentWinner(bidder);

            // Anti-snipe
            long secondsLeft = Duration.between(now, auction.getEndTime()).getSeconds();
            if (secondsLeft < ANTI_SNIPE_WINDOW_SECONDS) {
                auction.setEndTime(auction.getEndTime().plusSeconds(ANTI_SNIPE_EXTENSION_SECONDS));
                log.info("Anti-snipe triggered on auction {}: extended by {}s", auctionId, ANTI_SNIPE_EXTENSION_SECONDS);
            }

            Auction savedAuction = auctionRepository.save(auction);

            // Publish — happens after DB commit in REQUIRES_NEW (within lock, safe)
            BidPlacedEvent event = new BidPlacedEvent(
                    auctionId,
                    bid.getId(),
                    request.amount(),
                    bidder.getDisplayName(),
                    savedAuction.getEndTime(),
                    now
            );
            bidEventPublisher.publish(event);

            return BidResponse.from(bid);
        }
    }
}
