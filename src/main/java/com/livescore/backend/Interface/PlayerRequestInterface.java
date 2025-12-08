package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.PlayerRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRequestInterface extends JpaRepository<PlayerRequest,Long> {
        @Query("SELECT pr.player FROM PlayerRequest pr WHERE pr.team.id = :teamId AND pr.status = 'approved'")
        List<Player> findApprovedPlayersByTeamId(@Param("teamId") Long teamId);

    }



