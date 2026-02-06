package com.livescore.backend.Interface.Cricket;

import com.livescore.backend.Entity.PlayerInnings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerInningsInterface extends JpaRepository<PlayerInnings, Long> {
    PlayerInnings findByInnings_IdAndPlayer_Id(Long inningsId, Long bowlerId);
}
