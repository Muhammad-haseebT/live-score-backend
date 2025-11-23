package com.livescore.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int Aid;

    @Column(unique = true)
    String arid;

    String password;
    String role;

    // One Account can have many Organizations
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    List<Organization> organizations;
}
