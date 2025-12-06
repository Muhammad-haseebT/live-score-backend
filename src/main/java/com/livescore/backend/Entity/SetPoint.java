package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class SetPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "match_set_id")
    @JsonBackReference
    private MatchSets matchSet;


    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;


    @Column(nullable = false)
    private String pointType; // normal / ace / serviceError etc.
}