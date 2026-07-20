package com.bidflare.auction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.bidflare.messaging.AuctionEventPublisher;


import java.time.OffsetDateTime;
import java.util.List;

@Component
public class AuctionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuctionScheduler.class);

        private final AuctionRepository auctionRepository;
    private final AuctionEventPublisher auctionEventPublisher;


    public AuctionScheduler(AuctionRepository auctionRepository,
                            AuctionEventPublisher auctionEventPublisher) {
        this.auctionRepository = auctionRepository;
        this.auctionEventPublisher = auctionEventPublisher;
    }


    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void tickAuctions() {
        OffsetDateTime now = OffsetDateTime.now();

        // SCHEDULED → LIVE
        List<Auction> toStart = auctionRepository.findScheduledReadyToStart(now);
        for (Auction auction : toStart) {
            auction.setStatus(AuctionStatus.LIVE);
            auctionRepository.save(auction);
            log.info("Auction {} transitioned SCHEDULED → LIVE", auction.getId());
        }

        // LIVE → ENDED
        List<Auction> toEnd = auctionRepository.findLiveReadyToEnd(now);
        for (Auction auction : toEnd) {
            auction.setStatus(AuctionStatus.ENDED);
            // winner is already tracked in currentWinner via bid flow
            auctionRepository.save(auction);
            log.info("Auction {} transitioned LIVE → ENDED, winner={}", auction.getId(),
                    auction.getCurrentWinner() != null ? auction.getCurrentWinner().getDisplayName() : "none");

                        // Publish through Redis so every backend instance can notify its local clients.


            String winnerName = auction.getCurrentWinner() != null
                    ? auction.getCurrentWinner().getDisplayName()
                    : null;
            AuctionClosedEvent event = new AuctionClosedEvent(
                    auction.getId(), winnerName, auction.getCurrentPrice());
                        auctionEventPublisher.publish(event);


        }
    }
}
