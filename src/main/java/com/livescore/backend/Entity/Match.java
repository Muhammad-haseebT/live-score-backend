package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Data
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    Tournament tournament;

    @ManyToOne
    @JoinColumn(name = "team1_id")
    Team team1;

    @ManyToOne
    @JoinColumn(name = "team2_id")
    Team team2;

    @ManyToOne
    @JoinColumn(name = "scorer_id")
    Account scorer;

    String status;
    String venue;
    LocalDate date;
    LocalTime time;

    @ManyToOne
    @JoinColumn(name = "tossWinner_id")
    Team team;

    String decision;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    Team winner;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    List<Sets> sets;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    List<SetsGamesResults> setsGamesResults;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    List<Board> boards;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    List<GoalsType> goals;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    List<CricketInnings> cricketInnings;


    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    List<Media> mediaList;
}
