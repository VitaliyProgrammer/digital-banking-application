package io.github.vitaliyprogrammer.banking.shared.ratelimit;

import io.github.vitaliyprogrammer.banking.shared.AttemptCounter;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisAttemptCounter implements AttemptCounter {

    /**
     * INCR and PEXPIRE in one Lua script, executed atomically by Redis. As two separate calls,
     * a crash between them would leave a counter without TTL, blocking the user forever.
     */
    private static final RedisScript<Long> INCREMENT_WITH_WINDOW = RedisScript.of("""
            local attempts = redis.call('INCR', KEYS[1])
            if attempts == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return attempts
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public long increment(String key, Duration window) {

        Long attempts = redisTemplate.execute(INCREMENT_WITH_WINDOW, List.of(key), String.valueOf(window.toMillis()));

        if (attempts == null) {
            throw new IllegalStateException("Redis returned no result for attempt counter " + key);
        }

        return attempts;
    }

    @Override
    public void reset(String key) {

        redisTemplate.delete(key);
    }
}
