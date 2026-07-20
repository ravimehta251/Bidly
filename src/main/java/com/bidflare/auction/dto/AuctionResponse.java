package com.bidflare.auction.dto;

import com.bidflare.auction.Auction;
import com.bidflare.auction.AuctionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AuctionResponse(
    Long id,
    String title,
    String description,
    Long sellerId,
    String sellerDisplayName,
    BigDecimal startingPrice,
    BigDecimal minIncrement,
    BigDecimal currentPrice,
    Long currentWinnerId,
    String currentWinnerDisplayName,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    AuctionStatus status,
    Long version,
    OffsetDateTime createdAt
) {
    public static AuctionResponse from(Auction a) {
        return new AuctionResponse(
            a.getId(),
            a.getTitle(),
            a.getDescription(),
            a.getSeller() != null ? a.getSeller().getId() : null,
            a.getSeller() != null ? a.getSeller().getDisplayName() : null,
            a.getStartingPrice(),
            a.getMinIncrement(),
            a.getCurrentPrice(),
            a.getCurrentWinner() != null ? a.getCurrentWinner().getId() : null,
            a.getCurrentWinner() != null ? a.getCurrentWinner().getDisplayName() : null,
            a.getStartTime(),
            a.getEndTime(),
            a.getStatus(),
            a.getVersion(),
            a.getCreatedAt()
        );
    }
}
