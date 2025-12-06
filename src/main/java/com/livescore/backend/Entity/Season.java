package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
public class Season {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private String name;


    private LocalDate createdAt;


    @PrePersist
    void prePersist() {
        createdAt = LocalDate.now();
    }


    @ManyToOne
    @JoinColumn(name = "account_id")
    @JsonBackReference
    private Account account;


    @ManyToMany
    @JoinTable(name = "season_sports",
            joinColumns = @JoinColumn(name = "season_id"),
            inverseJoinColumns = @JoinColumn(name = "sports_id"))
    private List<Sports> sportsOffered;


    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Tournament> tournaments;
}