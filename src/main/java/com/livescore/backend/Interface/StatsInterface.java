package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Account;
import com.livescore.backend.Entity.Stats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatsInterface extends JpaRepository<Stats,Integer> {
}
