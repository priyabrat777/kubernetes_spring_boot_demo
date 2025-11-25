package com.demo.k8s.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * REST controller for cache management operations.
 * 
 * Provides comprehensive cache management endpoints including:
 * - Cache statistics and monitoring
 * - Cache key listing and searching
 * - Cache clearing and eviction
 * - Redis server information
 * - TTL management
 * 
 * All endpoints follow REST API best practices with proper HTTP methods,
 * status codes, and error handling.
 * 
 * @author Kubernetes Spring Boot Demo
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private static final Logger logger = LoggerFactory.getLogger(CacheController.class);

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Get comprehensive cache statistics.
     * 
     * Returns statistics for all caches including:
     * - Cache names
     * - Number of keys in each cache
     * - Redis connection status
     * 
     * @return CacheStatsResponse with statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        logger.debug("Fetching cache statistics");

        Map<String, Object> stats = new HashMap<>();
        Map<String, Integer> cacheSizes = new HashMap<>();

        try {
            Collection<String> cacheNames = cacheManager.getCacheNames();
            stats.put("cacheCount", cacheNames.size());
            stats.put("cacheNames", cacheNames);

            // Get size for each cache
            for (String cacheName : cacheNames) {
                try {
                    Set<String> keys = redisTemplate.keys("k8sdemo:" + cacheName + ":*");
                    int size = keys != null ? keys.size() : 0;
                    cacheSizes.put(cacheName, size);
                } catch (Exception e) {
                    logger.warn("Could not get size for cache '{}': {}", cacheName, e.getMessage());
                    cacheSizes.put(cacheName, -1);
                }
            }

            stats.put("cacheSizes", cacheSizes);
            stats.put("redisConnected", true);
            stats.put("timestamp", System.currentTimeMillis());

            logger.info("Retrieved cache statistics: {} caches", cacheNames.size());
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Error fetching cache statistics: {}", e.getMessage(), e);
            stats.put("error", "Failed to fetch cache statistics: " + e.getMessage());
            stats.put("redisConnected", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(stats);
        }
    }

    /**
     * List all cache keys across all caches.
     * 
     * @return Map of cache name to list of keys
     */
    @GetMapping("/keys")
    public ResponseEntity<Map<String, Object>> getAllCacheKeys() {
        logger.debug("Fetching all cache keys");

        Map<String, Object> response = new HashMap<>();
        Map<String, Set<String>> cacheKeys = new HashMap<>();

        try {
            Collection<String> cacheNames = cacheManager.getCacheNames();

            for (String cacheName : cacheNames) {
                Set<String> keys = redisTemplate.keys("k8sdemo:" + cacheName + ":*");
                if (keys != null && !keys.isEmpty()) {
                    // Remove prefix for cleaner display
                    Set<String> cleanKeys = new HashSet<>();
                    for (String key : keys) {
                        String cleanKey = key.replace("k8sdemo:" + cacheName + ":", "");
                        cleanKeys.add(cleanKey);
                    }
                    cacheKeys.put(cacheName, cleanKeys);
                }
            }

            response.put("cacheKeys", cacheKeys);
            response.put("totalKeys", cacheKeys.values().stream().mapToInt(Set::size).sum());

            logger.info("Retrieved cache keys for {} caches", cacheKeys.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching cache keys: {}", e.getMessage(), e);
            response.put("error", "Failed to fetch cache keys: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Search cache keys by pattern.
     * 
     * @param pattern Search pattern (e.g., "*user*")
     * @return Matching keys
     */
    @GetMapping("/keys/{pattern}")
    public ResponseEntity<Map<String, Object>> searchCacheKeys(@PathVariable String pattern) {
        logger.debug("Searching cache keys with pattern: {}", pattern);

        if (pattern == null || pattern.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Pattern must not be empty");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> response = new HashMap<>();

        try {
            Set<String> matchingKeys = redisTemplate.keys("k8sdemo:*" + pattern + "*");

            if (matchingKeys != null) {
                // Group by cache name
                Map<String, Set<String>> groupedKeys = new HashMap<>();
                for (String key : matchingKeys) {
                    String[] parts = key.split(":");
                    if (parts.length >= 3) {
                        String cacheName = parts[1];
                        String cleanKey = key.substring(("k8sdemo:" + cacheName + ":").length());
                        groupedKeys.computeIfAbsent(cacheName, k -> new HashSet<>()).add(cleanKey);
                    }
                }
                response.put("matchingKeys", groupedKeys);
                response.put("totalMatches", matchingKeys.size());
            } else {
                response.put("matchingKeys", new HashMap<>());
                response.put("totalMatches", 0);
            }

            response.put("pattern", pattern);
            logger.info("Found {} keys matching pattern '{}'", response.get("totalMatches"), pattern);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error searching cache keys: {}", e.getMessage(), e);
            response.put("error", "Failed to search cache keys: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Clear all caches.
     * 
     * @return 204 No Content on success
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearAllCaches() {
        logger.info("Clearing all caches");

        try {
            Collection<String> cacheNames = cacheManager.getCacheNames();
            int clearedCount = 0;

            for (String cacheName : cacheNames) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    clearedCount++;
                    logger.debug("Cleared cache: {}", cacheName);
                }
            }

            logger.info("Successfully cleared {} caches", clearedCount);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            logger.error("Error clearing all caches: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Clear a specific cache.
     * 
     * @param cacheName Name of the cache to clear
     * @return 204 No Content on success, 404 if cache not found
     */
    @DeleteMapping("/clear/{cacheName}")
    public ResponseEntity<Map<String, String>> clearCache(@PathVariable String cacheName) {
        logger.info("Clearing cache: {}", cacheName);

        try {
            Cache cache = cacheManager.getCache(cacheName);

            if (cache == null) {
                logger.warn("Cache '{}' not found", cacheName);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Cache not found: " + cacheName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            cache.clear();
            logger.info("Successfully cleared cache: {}", cacheName);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            logger.error("Error clearing cache '{}': {}", cacheName, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to clear cache: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Evict a specific cache entry.
     * 
     * @param cacheName Name of the cache
     * @param key       Cache key to evict
     * @return 204 No Content on success, 404 if cache or key not found
     */
    @DeleteMapping("/evict/{cacheName}/{key}")
    public ResponseEntity<Map<String, String>> evictCacheEntry(
            @PathVariable String cacheName,
            @PathVariable String key) {

        logger.info("Evicting cache entry: cache='{}', key='{}'", cacheName, key);

        if (key == null || key.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Key must not be empty");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            Cache cache = cacheManager.getCache(cacheName);

            if (cache == null) {
                logger.warn("Cache '{}' not found", cacheName);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Cache not found: " + cacheName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            cache.evict(key);
            logger.info("Successfully evicted key '{}' from cache '{}'", key, cacheName);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            logger.error("Error evicting cache entry: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to evict cache entry: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get Redis server information.
     * 
     * @return Redis server info including connection status and total keys
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getRedisInfo() {
        logger.debug("Fetching Redis server information");

        Map<String, Object> info = new HashMap<>();

        try {
            // Test Redis connection
            String pingResponse = redisTemplate.getConnectionFactory().getConnection().ping();

            info.put("connected", true);
            info.put("pingResponse", pingResponse);
            info.put("message", "Redis is connected and operational");
            info.put("timestamp", System.currentTimeMillis());

            // Get database size using keys count
            Set<String> allKeys = redisTemplate.keys("k8sdemo:*");
            int totalKeys = allKeys != null ? allKeys.size() : 0;
            info.put("totalKeys", totalKeys);

            logger.info("Redis server is connected, total keys: {}", totalKeys);
            return ResponseEntity.ok(info);

        } catch (Exception e) {
            logger.error("Error fetching Redis info: {}", e.getMessage(), e);
            info.put("connected", false);
            info.put("error", "Failed to connect to Redis: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(info);
        }
    }

    /**
     * Update TTL for a specific cache entry.
     * 
     * @param cacheName  Name of the cache
     * @param key        Cache key
     * @param ttlSeconds TTL in seconds
     * @return Updated TTL information
     */
    @PutMapping("/ttl/{cacheName}/{key}")
    public ResponseEntity<Map<String, Object>> updateCacheTtl(
            @PathVariable String cacheName,
            @PathVariable String key,
            @RequestBody Map<String, Integer> request) {

        Integer ttlSeconds = request.get("ttl");

        if (ttlSeconds == null || ttlSeconds <= 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "TTL must be a positive integer");
            return ResponseEntity.badRequest().body(error);
        }

        logger.info("Updating TTL for cache='{}', key='{}' to {} seconds", cacheName, key, ttlSeconds);

        try {
            String redisKey = "k8sdemo:" + cacheName + "::" + key;
            Boolean result = redisTemplate.expire(redisKey, ttlSeconds, TimeUnit.SECONDS);

            Map<String, Object> response = new HashMap<>();

            if (Boolean.TRUE.equals(result)) {
                response.put("success", true);
                response.put("cacheName", cacheName);
                response.put("key", key);
                response.put("ttl", ttlSeconds);
                response.put("message", "TTL updated successfully");

                logger.info("Successfully updated TTL for key '{}' in cache '{}'", key, cacheName);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Key not found or TTL update failed");
                logger.warn("Failed to update TTL for key '{}' in cache '{}'", key, cacheName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            logger.error("Error updating TTL: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to update TTL: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
