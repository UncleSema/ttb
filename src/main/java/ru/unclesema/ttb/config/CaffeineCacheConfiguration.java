package ru.unclesema.ttb.config;

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
    public CacheManager cache10s() {
        return caffeineCacheManager(10, TimeUnit.SECONDS);
    }

    @Bean
    public CacheManager cache5s() {
        return caffeineCacheManager(5, TimeUnit.SECONDS);
    }

    @Bean
    @Primary
    public CacheManager cache1d() {
        return caffeineCacheManager(1, TimeUnit.DAYS);
    }

    private CaffeineCacheManager caffeineCacheManager(long duration, TimeUnit unit) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine
                .newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(duration, unit)
        );
        return caffeineCacheManager;
    }
}
