package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@ToString(exclude = {"team"})
@EqualsAndHashCode(exclude = {"team"})
public class PtsTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "tournament_id")
    @JsonIgnore // avoid large nested objects
    private Tournament tournament;

    @OneToOne
    @JoinColumn(name = "team_id")
    @JsonBackReference("team-pointsTableEntry")
    private Team team;

    private int played;
    private int wins;
    private int losses;
    private int draws;
    private int points;
    private double nrr;
    @PrePersist
    public void prePersist() {
        this.played = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.points = 0;
        this.nrr = 0.0;
    }


    public PtsTable(Team team, Tournament tournament) {
        this.team = team;
        this.tournament = tournament;
    }
}
