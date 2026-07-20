package com.bidflare.auction.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateAuctionRequest(
    @NotBlank @Size(max = 255) String title,
    String description,
    @NotNull @DecimalMin("0.01") BigDecimal startingPrice,
    @DecimalMin("0.01") BigDecimal minIncrement,
    @NotNull @Future OffsetDateTime startTime,
    @NotNull @Future OffsetDateTime endTime
) {}
