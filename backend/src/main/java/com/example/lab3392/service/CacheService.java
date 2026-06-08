package com.example.lab3392.service;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
    private final CacheManager cacheManager;
    private static final String[] PRODUCT_CACHE_NAMES = {
            "productPagesV4", "productPagesV3", "productPagesV2", "productPages"
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
}
