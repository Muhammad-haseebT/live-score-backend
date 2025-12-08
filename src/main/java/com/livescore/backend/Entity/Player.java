package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;
// Player.java
@Entity
@Data
@JsonIgnoreProperties({"playerTeams"}) // avoid recursion if needed
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Player -> Account (many-to-one)  -- careful: this was previously wrong (used seasonsCreated)
    @ManyToOne
    @JoinColumn(name = "account_id")
    @JsonIgnore // keep account info out of nested JSON to avoid cycles; adjust as needed
    private Account account;
    //team foreign key
    @ManyToOne
    @JoinColumn(name = "team_id")
    @JsonBackReference("team-players") // keep team info out of nested JSON to avoid cycles; adjust as needed
    private Team team;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String playerRole;


    // Player -> PlayerTeam (one-to-many)
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    @JsonManagedReference("player-playerTeams")
    private List<PlayerRequest> playerRequests;

}

