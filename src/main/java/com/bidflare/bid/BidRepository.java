package com.bidflare.bid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    Page<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId, Pageable pageable);

    Optional<Bid> findByIdempotencyKey(String idempotencyKey);

    long countByAuctionId(Long auctionId);
}
