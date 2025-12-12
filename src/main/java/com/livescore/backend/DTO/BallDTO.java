package com.livescore.backend.DTO;

import lombok.Data;

// BallDTO.java
@Data
public class BallDTO {
    public Long id;
    public int overNumber;
    public int ballNumber;
    public Long batsmanId;
    public Long bowlerId;
    public int runs;
    public int extra;
    public String extraType;
    public String dismissalType;
    public Long fielderId;
    public Long mediaId;
}
