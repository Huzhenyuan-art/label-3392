package com.example.lab3392.service;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
    private final CacheManager cacheManager;
    private static final String[] PRODUCT_CACHE_NAMES = {
            "productPagesV5", "productPagesV4", "productPagesV3", "productPagesV2", "productPages"
    };
    private static final String[] NOTIFICATION_CACHE_NAMES = {
            "notificationPagesV1", "notificationUnreadV1"
    };

    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictAllProductCaches() {
        for (String cacheName : PRODUCT_CACHE_NAMES) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    public void evictAllNotificationCaches() {
        for (String cacheName : NOTIFICATION_CACHE_NAMES) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    public void evictNotificationCachesForProduct(Long productId) {
        evictAllNotificationCaches();
    }
}
