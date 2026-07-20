package com.bidflare.auction;

import com.bidflare.auction.dto.AuctionResponse;
import com.bidflare.auction.dto.CreateAuctionRequest;
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
@RequestMapping("/api/auctions")
@Tag(name = "Auctions", description = "Auction management and listing")
@SecurityRequirement(name = "bearerAuth")
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new auction")
    public AuctionResponse create(
            @Valid @RequestBody CreateAuctionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return auctionService.create(request, userDetails.getUsername());
    }

    @GetMapping
    @Operation(summary = "List auctions with optional status filter and pagination")
    public Page<AuctionResponse> list(
            @RequestParam(required = false) AuctionStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return auctionService.list(status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get auction details by ID")
    public AuctionResponse getById(@PathVariable Long id) {
        return auctionService.getById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an auction (only creator allowed)")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        auctionService.delete(id, userDetails.getUsername());
    }
}
