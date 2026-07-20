package com.bidflare.messaging;

import com.bidflare.auction.AuctionClosedEvent;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.stereotype.Component;

@Component
public class AuctionEventPublisher {
    private static final String TOPIC = "auction-closed";
    private final RTopic topic;

    public AuctionEventPublisher(RedissonClient redissonClient) {
        this.topic = redissonClient.getTopic(TOPIC, JsonJacksonCodec.INSTANCE);
    }

    public void publish(AuctionClosedEvent event) {
        topic.publish(event);
    }
}
