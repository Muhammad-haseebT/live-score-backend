package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class CricketInnings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int inningsid;

    @ManyToOne
    @JoinColumn(name = "mtid")
    Match match;

    int extras;

    @ManyToOne
    @JoinColumn(name = "batsmanid")
    Player batsman;

    @ManyToOne
    @JoinColumn(name = "bowlerid")
    Player bowler;

    int runs;
    int wicket;
    Double over;
    String stats;
    @OneToMany(mappedBy = "innings", cascade = CascadeType.ALL)
    List<CricketBall> balls;
}
