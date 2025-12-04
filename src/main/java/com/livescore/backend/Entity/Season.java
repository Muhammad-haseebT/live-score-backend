package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Season {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int sid;
    String name;
    @ManyToOne
    @JoinColumn(name = "aid")
    Account account;
    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL)
    List<Tournament> tournaments;
}
