package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class CricketBall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int ballid;

    @ManyToOne
    @JoinColumn(name = "inningsid")
    CricketInnings innings;

    int overnumber;
    int ballnumber;

    @ManyToOne
    @JoinColumn(name = "batsmanid")
    Player batsman;

    @ManyToOne
    @JoinColumn(name = "bowlerid")
    Player bowler;

    int runs;
    int extra;
    String extratype;
    String dismissletype;

    @ManyToOne
    @JoinColumn(name = "fielderid")
    Player fielder;
}
