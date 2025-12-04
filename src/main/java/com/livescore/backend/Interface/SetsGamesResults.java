package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SetsGamesResults extends JpaRepository<com.livescore.backend.Entity.SetsGamesResults,Integer> {
}
