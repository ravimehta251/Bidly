package com.bidflare.messaging;

import com.bidflare.auction.AuctionClosedEvent;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuctionEventSubscriber {
    private static final Logger log = LoggerFactory.getLogger(AuctionEventSubscriber.class);
    private static final String TOPIC = "auction-closed";
    private final RedissonClient redissonClient;
    private final SimpMessagingTemplate messagingTemplate;

    public AuctionEventSubscriber(RedissonClient redissonClient, SimpMessagingTemplate messagingTemplate) {
        this.redissonClient = redissonClient;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void subscribe() {
        RTopic topic = redissonClient.getTopic(TOPIC, JsonJacksonCodec.INSTANCE);
        topic.addListener(AuctionClosedEvent.class, (channel, event) -> {
            log.debug("Received auction close event for {}", event.auctionId());
            messagingTemplate.convertAndSend("/topic/auctions/" + event.auctionId() + "/closed", event);
        });
    }
}
