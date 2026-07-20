package com.bidflare.bid.dto;

import com.bidflare.bid.Bid;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BidResponse(
    Long id,
    Long auctionId,
    Long bidderId,
    String bidderDisplayName,
    BigDecimal amount,
    String idempotencyKey,
    OffsetDateTime createdAt
) {
    public static BidResponse from(Bid bid) {
        return new BidResponse(
            bid.getId(),
            bid.getAuction() != null ? bid.getAuction().getId() : null,
            bid.getBidder() != null ? bid.getBidder().getId() : null,
            bid.getBidder() != null ? bid.getBidder().getDisplayName() : null,
            bid.getAmount(),
            bid.getIdempotencyKey(),
            bid.getCreatedAt()
        );
    }
}
