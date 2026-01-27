package com.livescore.backend.Interface;

import com.livescore.backend.Entity.CricketInnings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface CricketInningsInterface extends JpaRepository<CricketInnings, Long> {

    // Get all innings of a team inside a tournament
    @Query("SELECT ci FROM CricketInnings ci " +
            "WHERE ci.team.id = :teamId AND ci.match.tournament.id = :tournamentId")
    List<CricketInnings> findByTeamAndTournament(@Param("teamId") Long teamId, @Param("tournamentId") Long tournamentId);

    // Get opponent innings for same match
    @Query("SELECT ci FROM CricketInnings ci " +
            "WHERE ci.match.id = :matchId AND ci.team.id <> :teamId")
    CricketInnings findOpponentInnings(@Param("matchId") Long matchId, @Param("teamId") Long teamId);

    @Query(value = "SELECT * FROM cricket_innings ci WHERE ci.match_id = :matchId AND ci.no = :no ORDER BY ci.id DESC LIMIT 1", nativeQuery = true)
    CricketInnings findByMatchIdAndNo(@Param("matchId") Long matchId, @Param("no") int no);

    @Query("""
        SELECT ci FROM CricketInnings ci
        WHERE ci.match.id = :matchId
          AND ci.team.id = :teamId
    """)
    CricketInnings findByMatchAndTeam(
            @Param("matchId") Long matchId,
            @Param("teamId") Long teamId
    );

}
