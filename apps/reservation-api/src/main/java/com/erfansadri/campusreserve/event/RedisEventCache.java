package com.erfansadri.campusreserve.event;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisEventCache implements EventCache {

    private static final Duration EVENT_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, EventResponse> redisTemplate;

    public RedisEventCache(
            RedisTemplate<String, EventResponse> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<EventResponse> get(Long eventId) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key(eventId)));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void put(EventResponse event) {
        try {
            redisTemplate.opsForValue().set(key(event.id()), event, EVENT_TTL);
        } catch (RuntimeException exception) {
            // PostgreSQL remains available when Redis cannot be reached.
        }
    }

    @Override
    public void evict(Long eventId) {
        try {
            redisTemplate.delete(key(eventId));
        } catch (RuntimeException exception) {
            // A stale cache entry is preferable to failing a reservation write.
        }
    }

    private String key(Long eventId) {
        return "event:" + eventId;
    }
}
