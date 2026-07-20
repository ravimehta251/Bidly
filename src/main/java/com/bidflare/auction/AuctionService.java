package com.bidflare.auction;

import com.bidflare.auction.dto.AuctionResponse;
import com.bidflare.auction.dto.CreateAuctionRequest;
import com.bidflare.user.User;
import com.bidflare.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    public AuctionService(AuctionRepository auctionRepository, UserRepository userRepository) {
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AuctionResponse create(CreateAuctionRequest request, String sellerEmail) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.endTime().isBefore(request.startTime()) || request.endTime().isEqual(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }

        Auction auction = new Auction();
        auction.setTitle(request.title());
        auction.setDescription(request.description());
        auction.setSeller(seller);
        auction.setStartingPrice(request.startingPrice());
        auction.setMinIncrement(request.minIncrement() != null ? request.minIncrement() : BigDecimal.ONE);
        auction.setCurrentPrice(request.startingPrice());
        auction.setStartTime(request.startTime());
        auction.setEndTime(request.endTime());
        auction.setStatus(AuctionStatus.SCHEDULED);

        return AuctionResponse.from(auctionRepository.save(auction));
    }

    @Transactional(readOnly = true)
    public Page<AuctionResponse> list(AuctionStatus status, Pageable pageable) {
        if (status != null) {
            return auctionRepository.findByStatus(status, pageable).map(AuctionResponse::from);
        }
        return auctionRepository.findAll(pageable).map(AuctionResponse::from);
    }

    @Transactional(readOnly = true)
    public AuctionResponse getById(Long id) {
        return auctionRepository.findById(id)
                .map(AuctionResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
    }

    @Transactional
    public void delete(Long id, String requesterEmail) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
        if (!auction.getSeller().getEmail().equals(requesterEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the seller can delete this auction");
        }
        auctionRepository.delete(auction);
    }
}
