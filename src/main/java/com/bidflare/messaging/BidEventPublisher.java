package com.bidflare.messaging;

import com.bidflare.bid.BidPlacedEvent;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.stereotype.Component;

@Component
public class BidEventPublisher {

    private static final String TOPIC = "auction-updates";

    private final RTopic topic;

    public BidEventPublisher(RedissonClient redissonClient) {
        this.topic = redissonClient.getTopic(TOPIC, JsonJacksonCodec.INSTANCE);
    }

    public void publish(BidPlacedEvent event) {
        topic.publish(event);
    }
}
