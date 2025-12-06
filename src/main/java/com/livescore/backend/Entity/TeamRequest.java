package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class TeamRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;


    @ManyToOne
    @JoinColumn(name = "player_account_id", nullable = false)
    private Account playerAccount;


    @Column(nullable = false)
    private String status;
}
