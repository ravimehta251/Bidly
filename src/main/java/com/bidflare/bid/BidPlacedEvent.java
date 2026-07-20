package com.bidflare.bid;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BidPlacedEvent(
    Long auctionId,
    Long bidId,
    BigDecimal amount,
    String leaderDisplayName,
    OffsetDateTime endTime,
    OffsetDateTime ts
) {}
