package com.bidflare.auction;

import java.math.BigDecimal;

public record AuctionClosedEvent(
    Long auctionId,
    String winnerDisplayName,
    BigDecimal finalPrice
) {}
