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
    Integer mtid;

    @ManyToOne
    @JoinColumn(name = "tid")
    Tournament tournament;

    @ManyToOne
    @JoinColumn(name = "team1id")
    Team team1;

    @ManyToOne
    @JoinColumn(name = "team2id")
    Team team2;

    @ManyToOne
    @JoinColumn(name = "scorerid")
    Account scorer;

    String status;
    String venue;
    LocalDate date;
    LocalTime time;

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
    List<CricketStats> cricketStats;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    List<Media> mediaList;
}
