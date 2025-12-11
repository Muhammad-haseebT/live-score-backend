package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerInterface extends JpaRepository<Player,Long> {

    @Query("SELECT p FROM Player p JOIN p.playerRequests pr WHERE pr.team.id = :teamId AND pr.status = 'APPROVE'")
    List<Player> findPlayersByTeamId(@Param("teamId") Long teamId);


}
