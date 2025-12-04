package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Sets {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer stid;

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
