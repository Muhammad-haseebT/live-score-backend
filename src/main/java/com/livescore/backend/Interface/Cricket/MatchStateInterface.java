package com.livescore.backend.Interface.Cricket;

import com.livescore.backend.Entity.CricketInnings;
import com.livescore.backend.Entity.MatchState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchStateInterface extends JpaRepository<MatchState, Long> {
    MatchState findByInnings_Id(Long inningsId);

    Long innings(CricketInnings innings);
}
