package com.example.UploadFilePractice.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Collections;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig() //
                .prefixCacheNameWith(this.getClass().getPackageName() + ".") //
                .entryTtl(Duration.ofMinutes(2)) //
                .disableCachingNullValues();

        return RedisCacheManager.builder(jedisConnectionFactory()) //
                .cacheDefaults(config) //
                .build();
    }

    @Bean
    public LettuceConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration("myapp.redis.cache.windows.net", 6379);
        configuration.setPassword("Ss38ZEEa6KA6DKAc1Wx1ary5MbQxxgZGVAzCaFhlLxE="); // Set your Redis password here
        return new LettuceConnectionFactory(configuration);
    }

}
