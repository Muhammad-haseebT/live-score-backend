package com.livescore.backend.Futsal;

import com.livescore.backend.Entity.Futsal.FutsalEvent;
import com.livescore.backend.Entity.Futsal.FutsalMatchState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Interface/Futsal/FutsalEventInterface.java
public interface FutsalEventInterface extends JpaRepository<FutsalEvent, Long> {

    // Last event dhundhne ke liye (undo ke liye)
    Optional<FutsalEvent> findTopByMatch_IdOrderByIdDesc(Long matchId);

    // Sare events ek match ke
    List<FutsalEvent> findByMatch_IdOrderByIdAsc(Long matchId);
}

