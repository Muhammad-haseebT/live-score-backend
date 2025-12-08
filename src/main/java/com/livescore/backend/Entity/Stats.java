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
    private Sports sportType;


    // Cricket fields
    private Integer runs;
    private Integer wickets;
    private Integer strikeRate;
    private Integer highest;
    private Integer notOut;

    private Integer points;


    private Integer goals;
    private Integer assists;
    private Integer fouls;
    private Integer yellowCards;
    private Integer redCards;
    @PrePersist
    public void prePersist() {
        runs=0;
        wickets=0;
        strikeRate=0;
        highest=0;
        notOut=0;
        points=0;
        goals=0;
        assists=0;
        fouls=0;
        yellowCards=0;
        redCards=0;

    }

}

