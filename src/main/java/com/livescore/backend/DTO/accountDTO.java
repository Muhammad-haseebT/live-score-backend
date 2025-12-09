package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class accountDTO {
    private Long id;
    private String username;
    private String password;
    private String name;
    private String role;
}
