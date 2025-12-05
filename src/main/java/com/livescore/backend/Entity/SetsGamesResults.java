package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class SetsGamesResults {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int sgrid;

    @ManyToOne
    @JoinColumn(name = "match_id")
    Match match;

    @ManyToOne
    @JoinColumn(name = "winnerTeam_id")
    Team winnerTeam;

    @ManyToOne
    @JoinColumn(name = "loserTeam_id")
    Team loserTeam;

    int pts;
}
