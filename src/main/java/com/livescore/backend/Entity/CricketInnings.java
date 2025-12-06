package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data

public class CricketInnings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "match_id")
    @JsonBackReference
    private Match match;


    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;


    @OneToMany(mappedBy = "innings", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<CricketBall> balls;
}