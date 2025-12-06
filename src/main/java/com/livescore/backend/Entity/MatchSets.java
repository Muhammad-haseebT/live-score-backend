package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class MatchSets {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "match_id")
    @JsonBackReference
    private Match match;


    private int setNumber;
    private int team1Score;
    private int team2Score;


    @ManyToOne
    @JoinColumn(name = "winner_team_id")
    private Team winnerTeam;


    @OneToMany(mappedBy = "matchSet", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<SetPoint> points;
}