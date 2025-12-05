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
    int id;
    String name;


    LocalDate createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDate.now();
    }

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "account_id")
    Account account;


    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL)
    @JsonManagedReference
    List<Tournament> tournaments;


}