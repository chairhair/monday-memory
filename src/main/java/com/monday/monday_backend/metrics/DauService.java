package com.monday.monday_backend.metrics;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DauService {
    private final StringRedisTemplate redis;

    public DauService(StringRedisTemplate redis) { this.redis = redis; }

    private String key(LocalDate date) { return "dau:" + date; }

    public void markActiveUser(String userId) {
        redis.execute((RedisCallback<Object>) (connection) -> {
            byte[] k = redis.getStringSerializer().serialize(key(LocalDate.now()));
            byte[] v = redis.getStringSerializer().serialize(userId);
            connection.execute("PFADD", k, v);
            return null;
        });
    }

    public long todayCount() {
        return cardinality(LocalDate.now());
    }

    public long cardinality(LocalDate date) {
        return redis.execute((RedisCallback<Long>) (connection) -> {
            byte[] k = redis.getStringSerializer().serialize(key(date));
            byte[] resp = (byte[]) connection.execute("PFCOUNT", k);
            return Long.parseLong(new String(resp));
        });
    }
}
