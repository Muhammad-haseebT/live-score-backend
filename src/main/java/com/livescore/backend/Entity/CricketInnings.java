package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
// CricketInnings.java
@Entity
@Data
public class CricketInnings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int no;//1 or 2

    // CricketInnings -> Match (many-to-one)
    @ManyToOne
    @JoinColumn(name = "match_id")
    @JsonBackReference("match-innings")
    private Match match;

    // CricketInnings -> Team
    @ManyToOne
    @JoinColumn(name = "team_id")
    @JsonIgnore // no reverse list on Team for innings
    private Team team;

    // CricketInnings -> CricketBall (one-to-many)
    @OneToMany(mappedBy = "innings", cascade = CascadeType.ALL)
    @JsonManagedReference("innings-balls")
    private List<CricketBall> balls;
}
