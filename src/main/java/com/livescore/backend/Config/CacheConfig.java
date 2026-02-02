package com.livescore.backend.Config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching  // ✅ Add this annotation
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // ✅ Add "matchState" alongside "tournamentAwards"
        return new ConcurrentMapCacheManager("tournamentAwards", "matchState","inningsState","tournamentStats");
    }
}
