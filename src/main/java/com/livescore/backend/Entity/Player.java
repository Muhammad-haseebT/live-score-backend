package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;


    @Column(nullable = false)
    private String name;


    @Column(nullable = false)
    private String playerRole;


    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    @JsonBackReference
    private List<PlayerTeam> playerTeams;
}
