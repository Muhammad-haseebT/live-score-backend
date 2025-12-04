package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class GoalsType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int gid;

    @ManyToOne
    @JoinColumn(name = "mtid")
    Match match;

    @ManyToOne
    @JoinColumn(name = "playerid")
    Player player;

    @ManyToOne
    @JoinColumn(name = "assistid")
    Player assist;

    String game;
    int yellow;
    int red;
    int foul;
    int score;
}
