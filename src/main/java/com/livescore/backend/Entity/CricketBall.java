package com.livescore.backend.Entity;

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

    private Integer runs;          // OFF-BAT runs
    private Integer extra;         // extra runs
    private String extraType;      // wide/no-ball/leg-bye/etc
    private String dismissalType;  // bowled/runout/lbw/etc

    private Boolean legalDelivery = true; // NEW
    private Boolean isFour = false;       // NEW
    private Boolean isSix = false;        // NEW

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
