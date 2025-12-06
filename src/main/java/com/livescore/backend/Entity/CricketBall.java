package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class CricketBall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "innings_id")
    @JsonBackReference
    private CricketInnings innings;


    @ManyToOne
    @JoinColumn(name = "batsman_id")
    private Player batsman;


    @ManyToOne
    @JoinColumn(name = "bowler_id")
    private Player bowler;


    @ManyToOne
    @JoinColumn(name = "fielder_id")
    private Player fielder;


    @Column
    private int overNumber;


    @Column
    private int ballNumber;


    @Column
    private int runs;


    @Column
    private int extra;


    @Column
    private String extraType;


    @Column
    private String dismissalType;


    @Column
    private String comment;


    @ManyToOne
    @JoinColumn(name = "media_id")
    private Media media;
}