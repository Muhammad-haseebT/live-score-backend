package com.livescore.backend.Interface.Cricket;

import com.livescore.backend.Entity.CricketInnings;
import com.livescore.backend.Entity.MatchState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MatchStateInterface extends JpaRepository<MatchState, Long> {
    MatchState findByInnings_Id(Long inningsId);

    Long innings(CricketInnings innings);

    @Query("SELECT ms FROM MatchState ms WHERE ms.innings.team.id = :teamId and ms.innings.match.id=:matchId")
    MatchState findByTeam_Id(Long teamId, Long matchId);
}
