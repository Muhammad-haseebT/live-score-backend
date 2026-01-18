package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInterface extends JpaRepository<Team,Long> {

    List<Team> findByTournamentId(Long tournamentId);

    Object findByTournamentIdAndStatus(Long tournamentId, String status);

    Object findByStatus(String status);

    @Query("SELECT t FROM Team t WHERE t.creator.id = :aid AND t.tournament.id = :tid")
    Optional<Team> findByTournamentIdAndPlayerId(@Param("tid") Long tid,
                                                 @Param("aid") Long aid);

}
