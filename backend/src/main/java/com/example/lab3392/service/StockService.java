package com.example.lab3392.service;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    private static final String HOLD_KEY_PREFIX = "stock:hold:";
    private static final long HOLD_TTL_MINUTES = 35;

    private final StringRedisTemplate redisTemplate;

    private static final String HOLD_LUA =
            "local holdKey = KEYS[1]\n" +
            "local detailKey = KEYS[2]\n" +
            "local mysqlStock = tonumber(ARGV[1])\n" +
            "local quantity = tonumber(ARGV[2])\n" +
            "local ttl = tonumber(ARGV[3])\n" +
            "local currentHeld = tonumber(redis.call('GET', holdKey) or '0')\n" +
            "if currentHeld + quantity > mysqlStock then\n" +
            "  return -1\n" +
            "end\n" +
            "redis.call('INCRBY', holdKey, quantity)\n" +
            "redis.call('SET', detailKey, quantity)\n" +
            "redis.call('EXPIRE', detailKey, ttl)\n" +
            "redis.call('EXPIRE', holdKey, ttl + 300)\n" +
            "return 1\n";

    private static final String RELEASE_LUA =
            "local holdKey = KEYS[1]\n" +
            "local detailKey = KEYS[2]\n" +
            "local quantity = tonumber(ARGV[1])\n" +
            "local currentHeld = tonumber(redis.call('GET', holdKey) or '0')\n" +
            "local toRelease = math.min(currentHeld, quantity)\n" +
            "if toRelease > 0 then\n" +
            "  redis.call('INCRBY', holdKey, -toRelease)\n" +
            "  local remaining = tonumber(redis.call('GET', holdKey) or '0')\n" +
            "  if remaining <= 0 then\n" +
            "    redis.call('DEL', holdKey)\n" +
            "  end\n" +
            "end\n" +
            "redis.call('DEL', detailKey)\n" +
            "return toRelease\n";

    private final DefaultRedisScript<Long> holdScript;
    private final DefaultRedisScript<Long> releaseScript;

    public StockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.holdScript = new DefaultRedisScript<>(HOLD_LUA, Long.class);
        this.releaseScript = new DefaultRedisScript<>(RELEASE_LUA, Long.class);
    }

    public boolean holdStock(Long productId, int quantity, int mysqlStock, Long orderId) {
        String holdKey = HOLD_KEY_PREFIX + productId;
        String detailKey = HOLD_KEY_PREFIX + "order:" + orderId + ":" + productId;
        long ttlSeconds = HOLD_TTL_MINUTES * 60;

        Long result = redisTemplate.execute(
                holdScript,
                java.util.List.of(holdKey, detailKey),
                String.valueOf(mysqlStock),
                String.valueOf(quantity),
                String.valueOf(ttlSeconds)
        );
        return result != null && result == 1;
    }

    public void releaseStock(Long productId, int quantity, Long orderId) {
        String holdKey = HOLD_KEY_PREFIX + productId;
        String detailKey = HOLD_KEY_PREFIX + "order:" + orderId + ":" + productId;
        redisTemplate.execute(
                releaseScript,
                java.util.List.of(holdKey, detailKey),
                String.valueOf(quantity)
        );
    }

    public void clearHoldOnPayment(Long productId, int quantity, Long orderId) {
        String holdKey = HOLD_KEY_PREFIX + productId;
        String detailKey = HOLD_KEY_PREFIX + "order:" + orderId + ":" + productId;
        Long currentHeld = getHeldQuantity(productId);
        long toDecrement = Math.min(currentHeld, quantity);
        if (toDecrement > 0) {
            redisTemplate.opsForValue().increment(holdKey, -toDecrement);
            String remaining = redisTemplate.opsForValue().get(holdKey);
            if (remaining != null && Long.parseLong(remaining) <= 0) {
                redisTemplate.delete(holdKey);
            }
        }
        redisTemplate.delete(detailKey);
    }

    public long getHeldQuantity(Long productId) {
        String holdKey = HOLD_KEY_PREFIX + productId;
        String val = redisTemplate.opsForValue().get(holdKey);
        if (val == null) return 0;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int getAvailableStock(int mysqlStock, Long productId) {
        long held = getHeldQuantity(productId);
        return Math.max(0, mysqlStock - (int) held);
    }

    public void batchHoldStock(Map<Long, Integer> productQuantityMap, Map<Long, Integer> productStockMap, Long orderId) {
        for (Map.Entry<Long, Integer> entry : productQuantityMap.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            Integer mysqlStock = productStockMap.get(productId);
            if (mysqlStock == null) {
                throw new IllegalArgumentException("商品 ID " + productId + " 不存在");
            }
            boolean success = holdStock(productId, quantity, mysqlStock, orderId);
            if (!success) {
                for (Map.Entry<Long, Integer> rollback : productQuantityMap.entrySet()) {
                    if (rollback.getKey().equals(productId)) break;
                    releaseStock(rollback.getKey(), rollback.getValue(), orderId);
                }
                throw new IllegalArgumentException("商品 ID " + productId + " 库存不足，预占失败");
            }
        }
    }
}
