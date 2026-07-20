package com.bidflare.messaging;

import com.bidflare.bid.BidPlacedEvent;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class BidEventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(BidEventSubscriber.class);
    private static final String TOPIC = "auction-updates";

    private final RedissonClient redissonClient;
    private final SimpMessagingTemplate messagingTemplate;

    public BidEventSubscriber(RedissonClient redissonClient,
                              SimpMessagingTemplate messagingTemplate) {
        this.redissonClient = redissonClient;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void subscribe() {
        RTopic topic = redissonClient.getTopic(TOPIC, JsonJacksonCodec.INSTANCE);
        topic.addListener(BidPlacedEvent.class, (channel, event) -> {
            log.debug("Received bid event for auction {}: amount={}", event.auctionId(), event.amount());
            messagingTemplate.convertAndSend("/topic/auctions/" + event.auctionId(), event);
        });
        log.info("Subscribed to Redis topic '{}'", TOPIC);
    }
}
