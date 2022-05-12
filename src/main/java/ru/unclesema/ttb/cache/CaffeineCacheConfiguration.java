package ru.unclesema.ttb.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CaffeineCacheConfiguration extends CachingConfigurerSupport {
    @Bean
    public CacheManager cache15s() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine
                .newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(15, TimeUnit.SECONDS));
        return caffeineCacheManager;
    }

    @Bean
    public CacheManager cache5s() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine
                .newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(5, TimeUnit.SECONDS));
        return caffeineCacheManager;
    }

    @Bean
    @Primary
    public CacheManager cacheForever() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine
                .newBuilder()
                .maximumSize(1000)
        );
        return caffeineCacheManager;
    }
}
