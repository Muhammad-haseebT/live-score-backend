package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class SetsGamesResults {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer sgrid;

    @ManyToOne
    @JoinColumn(name = "mtid")
    Match match;

    @ManyToOne
    @JoinColumn(name = "winnerteamid")
    Team winnerTeam;

    @ManyToOne
    @JoinColumn(name = "loserteamid")
    Team loserTeam;

    Integer pts;
}
