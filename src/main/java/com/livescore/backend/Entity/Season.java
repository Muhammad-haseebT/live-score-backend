package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
// Season.java
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

    // Season -> Account (many-to-one)
    @ManyToOne
    @JoinColumn(name = "account_id")
    @JsonBackReference("account-seasons")
    private Account account;

    // ManyToMany with Sports - break recursion by not using managed/backrefs here.
    @ManyToMany
    @JoinTable(name = "season_sports",
            joinColumns = @JoinColumn(name = "season_id"),
            inverseJoinColumns = @JoinColumn(name = "sports_id"))
    private List<Sports> sportsOffered;

    // Season -> Tournament (one-to-many)
    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL)
    @JsonManagedReference("season-tournaments")
    private List<Tournament> tournaments;


}
