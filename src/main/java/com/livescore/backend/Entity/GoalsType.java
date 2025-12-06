package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class GoalsType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "match_id")
    @JsonBackReference
    private Match match;


    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;


    @ManyToOne
    @JoinColumn(name = "assist_id")
    private Player assist;


    private int minute;
    private int goal;
    private int foul;
    private int yellow;
    private int red;
}
