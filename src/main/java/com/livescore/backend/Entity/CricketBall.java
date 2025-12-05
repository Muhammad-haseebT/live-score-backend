package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class CricketBall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne
    @JoinColumn(name = "innings_id")
    CricketInnings innings;

    @ManyToOne
    @JoinColumn(name = "media_id")
    Media media;

    String comment;

    int overNumber;
    int ballNumber;

    @ManyToOne
    @JoinColumn(name = "batsman_id")
    Player batsman;

    @ManyToOne
    @JoinColumn(name = "bowler_id")
    Player bowler;

    int runs;
    int extra;
    String extraType;
    String dismissleType;

    @ManyToOne
    @JoinColumn(name = "fielder_id")
    Player fielder;
}
