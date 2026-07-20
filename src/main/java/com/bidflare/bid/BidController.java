package com.bidflare.bid;

import com.bidflare.bid.dto.BidResponse;
import com.bidflare.bid.dto.PlaceBidRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auctions/{auctionId}/bids")
@Tag(name = "Bids", description = "Bid placement and history")
@SecurityRequirement(name = "bearerAuth")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place a bid on an active auction")
    public BidResponse placeBid(
            @PathVariable Long auctionId,
            @Valid @RequestBody PlaceBidRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return bidService.placeBid(auctionId, request, userDetails.getUsername());
    }

    @GetMapping
    @Operation(summary = "Get bid history for an auction (sorted by amount descending)")
    public Page<BidResponse> getBids(
            @PathVariable Long auctionId,
            @PageableDefault(size = 20) Pageable pageable) {
        return bidService.getBidsForAuction(auctionId, pageable);
    }
}
