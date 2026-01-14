package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {
        "seasonsCreated",
        "tournamentsCreated",
        "scoredMatches",
        "player"})
//@Where(clause = "is_deleted = false")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;


    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Account -> Season (one-to-many)
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Season> seasonsCreated;

    // Account -> Tournament (one-to-many)
    @OneToMany(mappedBy = "organizer", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Tournament> tournamentsCreated;

    // Account -> Match (as scorer) (one-to-many)
    @OneToMany(mappedBy = "scorer", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Match> scoredMatches;

    // Soft delete method
    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    // Restore method (optional - agar restore karna ho)
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }
}