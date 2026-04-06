package com.livescore.backend.Service;

import com.livescore.backend.Entity.FavouritePlayer;
import com.livescore.backend.Interface.FavouritePlayerInterface;
import com.livescore.backend.Interface.MatchInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FavouritePlayerService {

    private final FavouritePlayerInterface fpRepo;
    private final MatchInterface matchRepo;

    public String submitVote(Long matchId, Long accountId, Long playerId) {


        FavouritePlayer fp = fpRepo.findByMatchId(matchId)
                .orElseGet(() -> {
                    FavouritePlayer newFp = new FavouritePlayer();
                    newFp.setMatch(matchRepo.findById(matchId)
                            .orElseThrow(() -> new RuntimeException("Match not found")));
                    return newFp;
                });

        // Duplicate check
        if (fp.getVotedAccountIds().contains(accountId)) {
            throw new RuntimeException("Aap pehle hi vote kar chuke hain!");
        }

        // Account add karo
        fp.getVotedAccountIds().add(accountId);

        // Count +1
        fp.getPlayerVoteCounts().merge(playerId, 1, Integer::sum);

        fpRepo.save(fp);
        return "Vote submit ho gaya!";
    }

    public FavouritePlayer getResults(Long matchId) {
        return fpRepo.findByMatchId(matchId)
                .orElseThrow(() -> new RuntimeException("Koi votes nahi abhi tak!"));
    }
    public boolean checkVote(Long matchId, Long accountId) {
        FavouritePlayer fp = fpRepo.findByMatchId(matchId).orElse(null);
        if (fp == null) {
            return false;
        }else return fp.getVotedAccountIds().contains(accountId);

    }
}