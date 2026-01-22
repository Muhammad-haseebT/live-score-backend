package com.livescore.backend.Service;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class CacheEvictionService {

    private final CacheManager cacheManager;

    public CacheEvictionService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictTournament(Long tournamentId) {
        if (tournamentId == null) return;
        if (cacheManager.getCache("tournamentStats") != null) {
            cacheManager.getCache("tournamentStats").evict(tournamentId);
        }
    }

    public void evictTournamentAwards(Long tournamentId) {
        if (tournamentId == null) return;
        if (cacheManager.getCache("tournamentAwards") != null) {
            cacheManager.getCache("tournamentAwards").evict(tournamentId);
        }
    }

    public void evictPlayerStats(Long tournamentId, Long playerId) {
        if (tournamentId == null || playerId == null) return;
        String key = tournamentId + ":" + playerId;
        if (cacheManager.getCache("playerStats") != null) {
            cacheManager.getCache("playerStats").evict(key);
        }
    }

    public void evictTournamentPlayerStats(Long tournamentId, Long playerId) {
        if (tournamentId == null || playerId == null) return;
        String key = tournamentId + ":" + playerId;
        if (cacheManager.getCache("tournamentPlayerStats") != null) {
            cacheManager.getCache("tournamentPlayerStats").evict(key);
        }
    }
}
