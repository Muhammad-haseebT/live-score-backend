package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Stats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "tournament_id")
    @JsonBackReference
    private Tournament tournament;


    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;


    private int runs;
    private int wickets;
    private double strikeRate;
    private int highest;
    private int points;
    private int notOut;
}