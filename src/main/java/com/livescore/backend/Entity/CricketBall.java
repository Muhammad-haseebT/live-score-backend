package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    private Integer  overNumber;
    private Integer  ballNumber;
    private Integer  runs;
    private Integer  extra;
    private String extraType;
    private String dismissalType;
    private String comment;

    // optional media - one-way JSON
    @ManyToOne
    @JoinColumn(name = "media_id")
    @JsonIgnore
    private Media media;
}