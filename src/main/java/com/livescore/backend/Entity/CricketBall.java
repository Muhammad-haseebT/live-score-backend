package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class CricketBall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer ballid;

    @ManyToOne
    @JoinColumn(name = "inningsid")
    CricketInnings innings;

    Integer overnumber;
    Integer ballnumber;

    @ManyToOne
    @JoinColumn(name = "batsmanid")
    Player batsman;

    @ManyToOne
    @JoinColumn(name = "bowlerid")
    Player bowler;

    Integer runs;
    Integer extra;
    String extratype;
    String dismissletype;

    @ManyToOne
    @JoinColumn(name = "fielderid")
    Player fielder;
}
