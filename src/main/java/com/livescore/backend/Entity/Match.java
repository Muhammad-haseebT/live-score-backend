package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    private Long id;


    @ManyToOne
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;


    @ManyToOne
    @JoinColumn(name = "team1_id")
    private Team team1;


    @ManyToOne
    @JoinColumn(name = "team2_id")
    private Team team2;


    @ManyToOne
    @JoinColumn(name = "scorer_id")
    private Account scorer;


    private String status; // UPCOMING / LIVE / FINISHED
    private String venue;
    private LocalDate date;
    private LocalTime time;


    @ManyToOne
    @JoinColumn(name = "toss_winner_id")
    private Team tossWinner;


    private String decision;


    @ManyToOne
    @JoinColumn(name = "winner_team_id")
    private Team winnerTeam;


    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<CricketInnings> cricketInnings;


    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<GoalsType> footballEvents;


    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<MatchSets> matchSets;


    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Board> boards;


    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Media> mediaList;
}