package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentInterface extends JpaRepository<Tournament,Long> {
    boolean existsByNameAndSeasonId(String name, Long seasonId);
}
