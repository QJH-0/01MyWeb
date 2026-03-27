package com.myweb.backend.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class RateLimitFilterConfigTest {
    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Test
    void shouldLoadRateLimitPerMinuteFromApplicationTestYml() throws Exception {
        Field field = RateLimitFilter.class.getDeclaredField("rateLimitPerMinute");
        field.setAccessible(true);
        int value = (int) field.get(rateLimitFilter);

        assertEquals(120, value);
    }
}

