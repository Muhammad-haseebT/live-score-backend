package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
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
    @JsonBackReference("innings-balls")
    private CricketInnings innings;

    @ManyToOne
    @JoinColumn(name = "match_id")
    @JsonBackReference("match-balls")
    private Match match;

    @ManyToOne
    @JoinColumn(name = "batsman_id")
    @JsonIgnore
    private Player batsman;

    @ManyToOne
    @JoinColumn(name = "nonStraiker_id")
    @JsonIgnore
    private Player nonStriker;


    @ManyToOne
    @JoinColumn(name = "bowler_id")
    @JsonIgnore
    private Player bowler;

    @ManyToOne
    @JoinColumn(name = "fielder_id")
    @JsonIgnore
    private Player fielder;

    @ManyToOne
    @JoinColumn(name = "out_batsman_id")
    @JsonIgnore
    private Player outBatsman;

    private Integer overNumber;
    private Integer ballNumber;

    private Integer runs;
    private Integer extra;
    private String extraType;
    private String dismissalType;

    private Boolean legalDelivery = true;
    private Boolean isFour = false;
    private Boolean isSix = false;

    private String comment;

    @ManyToOne
    @JoinColumn(name = "out_player_id")
    @JsonIgnore
    public Player outPlayer;

    @ManyToOne
    @JoinColumn(name = "media_id")
    @JsonIgnore
    private Media media;


}
