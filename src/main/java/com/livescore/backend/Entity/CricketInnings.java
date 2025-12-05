package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class CricketInnings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne
    @JoinColumn(name = "match_id")
    Match match;

    @ManyToOne
    @JoinColumn(name = "team_id")
    Team team;

}
