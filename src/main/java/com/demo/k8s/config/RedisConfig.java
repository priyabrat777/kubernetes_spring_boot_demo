package com.demo.k8s.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * Redis configuration class for enterprise-grade caching.
 * 
 * This configuration provides:
 * - Connection pooling with Jedis client
 * - JSON serialization for human-readable cache entries
 * - Configurable TTL for cache entries
 * - Graceful error handling for cache failures
 * - Custom cache manager with optimized settings
 * 
 * @author Kubernetes Spring Boot Demo
 * @version 1.0.0
 */
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.cache.redis.time-to-live:600000}")
    private long cacheTtl;

    /**
     * Configure Jedis connection pool with enterprise-grade settings.
     * 
     * Pool Configuration:
     * - Max total connections: 20
     * - Max idle connections: 10
     * - Min idle connections: 5
     * - Test on borrow: true (validate connections before use)
     * - Test while idle: true (validate idle connections)
     * 
     * @return JedisPoolConfig with optimized settings
     */
    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(5);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(60));
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setMaxWait(Duration.ofSeconds(2));

        logger.info("Configured Jedis connection pool: maxTotal={}, maxIdle={}, minIdle={}",
                poolConfig.getMaxTotal(), poolConfig.getMaxIdle(), poolConfig.getMinIdle());

        return poolConfig;
    }

    /**
     * Create Redis connection factory with Jedis client.
     * 
     * @return JedisConnectionFactory configured with pool settings
     */
    @Bean
    public JedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);

        JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
                .usePooling()
                .poolConfig(jedisPoolConfig())
                .build();

        JedisConnectionFactory factory = new JedisConnectionFactory(redisConfig, clientConfig);

        logger.info("Configured Redis connection factory: host={}, port={}", redisHost, redisPort);

        return factory;
    }

    /**
     * Create Redis template for custom cache operations.
     * 
     * Serialization Strategy:
     * - Keys: StringRedisSerializer (simple string keys)
     * - Values: GenericJackson2JsonRedisSerializer (JSON for human-readable
     * storage)
     * - Hash keys/values: Same as above
     * 
     * @param connectionFactory Redis connection factory
     * @return RedisTemplate configured with JSON serialization
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Create ObjectMapper with type information for polymorphic deserialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Set serializers
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.setEnableTransactionSupport(false);
        template.afterPropertiesSet();

        logger.info("Configured RedisTemplate with JSON serialization");

        return template;
    }

    /**
     * Create cache manager with custom configuration.
     * 
     * Cache Configuration:
     * - Default TTL: Configurable (default 10 minutes)
     * - Null value caching: Disabled (prevent cache pollution)
     * - Key prefix: Enabled with "k8sdemo:" prefix
     * - Serialization: JSON for values, String for keys
     * 
     * @param connectionFactory Redis connection factory
     * @return RedisCacheManager with custom configuration
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        // Create ObjectMapper for cache serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMillis(cacheTtl))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .prefixCacheNameWith("k8sdemo:");

        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory())
                .cacheDefaults(cacheConfig)
                .transactionAware()
                .build();

        logger.info("Configured RedisCacheManager with TTL={}ms, null caching disabled", cacheTtl);

        return cacheManager;
    }

    /**
     * Custom error handler for cache operations.
     * 
     * This handler ensures graceful degradation when Redis is unavailable:
     * - Logs errors but doesn't throw exceptions
     * - Allows application to continue without cache
     * - Implements cache-aside pattern for resilience
     * 
     * @return CacheErrorHandler for graceful error handling
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache,
                    Object key) {
                logger.error("Cache GET error for cache '{}' and key '{}': {}",
                        cache.getName(), key, exception.getMessage());
                logger.debug("Cache GET error details", exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache,
                    Object key, Object value) {
                logger.error("Cache PUT error for cache '{}' and key '{}': {}",
                        cache.getName(), key, exception.getMessage());
                logger.debug("Cache PUT error details", exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache,
                    Object key) {
                logger.error("Cache EVICT error for cache '{}' and key '{}': {}",
                        cache.getName(), key, exception.getMessage());
                logger.debug("Cache EVICT error details", exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                logger.error("Cache CLEAR error for cache '{}': {}",
                        cache.getName(), exception.getMessage());
                logger.debug("Cache CLEAR error details", exception);
            }
        };
    }
}
