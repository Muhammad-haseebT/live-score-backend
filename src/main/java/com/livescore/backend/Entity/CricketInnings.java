package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class CricketInnings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer inningsid;

    @ManyToOne
    @JoinColumn(name = "mtid")
    Match match;

    Integer extras;

    @ManyToOne
    @JoinColumn(name = "batsmanid")
    Player batsman;

    @ManyToOne
    @JoinColumn(name = "bowlerid")
    Player bowler;

    Integer runs;
    Integer wicket;
    Double over;
    String stats;
    @OneToMany(mappedBy = "innings", cascade = CascadeType.ALL)
    List<CricketBall> balls;
}
