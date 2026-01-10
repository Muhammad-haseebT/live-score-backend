package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonBackReference("tournament-stats")
    private Tournament tournament;

    @ManyToOne
    @JoinColumn(name = "player_id")
    @JsonIgnore
    private Player player;

    @ManyToOne
    @JoinColumn(name = "sport_type_id")
    @JsonIgnore
    private Sports sportType;

    // Cricket fields
    private Integer runs;
    private Integer wickets;
    private Integer highest;
    private Integer notOut;
    private Integer strikeRate; // you can compute this when needed

    // new fields for precision
    private Integer ballsFaced;      // batsman
    private Integer ballsBowled;     // bowler (legal deliveries)
    private Integer runsConceded;    // bowler
    private Integer fours;
    private Integer sixes;

    private Integer points;

    // football fields...
    private Integer goals;
    private Integer assists;
    private Integer fouls;
    private Integer yellowCards;
    private Integer redCards;

    @PrePersist
    public void prePersist() {
        if (runs == null) runs = 0;
        if (wickets == null) wickets = 0;
        if (strikeRate == null) strikeRate = 0;
        if (highest == null) highest = 0;
        if (notOut == null) notOut = 0;
        if (points == null) points = 0;
        if (goals == null) goals = 0;
        if (assists == null) assists = 0;
        if (fouls == null) fouls = 0;
        if (yellowCards == null) yellowCards = 0;
        if (redCards == null) redCards = 0;
        if (ballsFaced == null) ballsFaced = 0;
        if (ballsBowled == null) ballsBowled = 0;
        if (runsConceded == null) runsConceded = 0;
        if (fours == null) fours = 0;
        if (sixes == null) sixes = 0;
    }
}


