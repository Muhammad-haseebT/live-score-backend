package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class ScoreDTO {
    private Long runs;
    private Long overs;
    private Long wickets;
    private Long balls;
    private String status;
    private Long target;
     // RUN, BOUNDARY, SIX, EXTRA, WICKET
    private String event;       // 1,2,3,4,6,WIDE,NO_BALL,LBW etc.
    private Long teamId;
    private Long matchId;
    private Long batsmanId;
    private Long bowlerId;
    private Long fielderId;
          // runs off bat
    private Integer extra;       // number of extra runs
    private String extraType;    // wide, no-ball, bye, leg-bye
    private String eventType;    // wide, no-ball, bye, leg-bye

    private String dismissalType; // catch, bowled, runout, stumped, lbw, hit-wicket, retired
    private Boolean isLegal;
    private Double runRate;
    private Long inningsId;
    private Long outPlayerId;
    private int runsOnThisBall;
    private int extrasThisBall;
    private boolean isFour;
    private boolean isSix;
}


