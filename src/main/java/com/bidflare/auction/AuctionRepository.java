package com.bidflare.auction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    Page<Auction> findByStatus(AuctionStatus status, Pageable pageable);

    Page<Auction> findAll(Pageable pageable);

    // Fetch SCHEDULED auctions whose startTime has passed
    @Query("SELECT a FROM Auction a WHERE a.status = 'SCHEDULED' AND a.startTime <= :now")
    List<Auction> findScheduledReadyToStart(@Param("now") OffsetDateTime now);

    // Fetch LIVE auctions whose endTime has passed
    @Query("SELECT a FROM Auction a WHERE a.status = 'LIVE' AND a.endTime <= :now")
    List<Auction> findLiveReadyToEnd(@Param("now") OffsetDateTime now);
}
