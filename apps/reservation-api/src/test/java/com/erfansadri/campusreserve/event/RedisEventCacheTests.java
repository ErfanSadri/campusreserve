package com.erfansadri.campusreserve.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisEventCacheTests {

    @Mock
    private RedisTemplate<String, EventResponse> redisTemplate;

    @Mock
    private ValueOperations<String, EventResponse> valueOperations;

    @InjectMocks
    private RedisEventCache eventCache;

    @Test
    void storesEventUsingNamespacedKeyAndFiveMinuteTtl() {
        EventResponse event = sampleResponse();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        eventCache.put(event);

        verify(valueOperations).set(
                "event:26",
                event,
                Duration.ofMinutes(5));
    }

    @Test
    void returnsCachedEventWhenPresent() {
        EventResponse event = sampleResponse();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("event:26")).thenReturn(event);

        assertThat(eventCache.get(26L)).contains(event);
    }

    @Test
    void evictsEventUsingNamespacedKey() {
        eventCache.evict(26L);

        verify(redisTemplate).delete("event:26");
    }

    private EventResponse sampleResponse() {
        return new EventResponse(
                26L,
                "Cached Event",
                null,
                "UCSD",
                OffsetDateTime.parse("2026-09-28T18:00:00-07:00"),
                OffsetDateTime.parse("2026-09-20T09:00:00-07:00"),
                10,
                10,
                OffsetDateTime.parse("2026-08-09T14:07:38-07:00"));
    }
}
