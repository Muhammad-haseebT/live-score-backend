package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamInterface extends JpaRepository<Team,Long> {

    List<Team> findByTournamentId(Long tournamentId);

    Object findByTournamentIdAndStatus(Long tournamentId, String status);

    Object findByStatus(String status);
}
