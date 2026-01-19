package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.PlayerRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRequestInterface extends JpaRepository<PlayerRequest,Long> {
        @Query("SELECT pr.player FROM PlayerRequest pr WHERE pr.team.id = :teamId AND pr.status = 'APPROVED' AND pr.player.isDeleted = false")
        List<Player> findApprovedPlayersByTeamId(@Param("teamId") Long teamId);
    @Query("""
SELECT pr
FROM PlayerRequest pr
WHERE pr.player.id = :playerId
AND pr.tournament.id = :tournamentId
AND pr.status = 'APPROVED'
AND pr.player.isDeleted = false
""")
    PlayerRequest findExistingRequest(Long playerId, Long tournamentId);


    @Query("SELECT pr FROM PlayerRequest pr WHERE pr.player.id = :playerId AND pr.player.isDeleted = false")
    List<PlayerRequest> findbyPlayer_Id(@Param("playerId") Long playerId);

    Optional<PlayerRequest> findByPlayer_IdAndTeam_Id(Long playerId, Long teamId);


    List<PlayerRequest> findByTeam_Id(Long id);
}




