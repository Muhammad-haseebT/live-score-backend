package com.livescore.backend.DTO;

import lombok.Data;

// BallDTO.java
@Data
public class BallDTO {
    public Long id;
    public Integer overNumber;
    public Integer ballNumber;
    public Long batsmanId;
    public Long bowlerId;
    public Integer runs;
    public Integer extra;
    public String extraType;
    public String dismissalType;
    public Long fielderId;
    public Long mediaId;
}
