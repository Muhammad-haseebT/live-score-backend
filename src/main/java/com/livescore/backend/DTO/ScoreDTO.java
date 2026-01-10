package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class ScoreDTO {
    private int runs;
    private int overs;
    private int wickets;
    private int balls;
    private String status;
    private int target;
     // RUN, BOUNDARY, SIX, EXTRA, WICKET
    private String event;       // 1,2,3,4,6,WIDE,NO_BALL,LBWetc
    private Long teamId;
    private Long matchId;
    private Long batsmanId;
    private Long bowlerId;
    private Long fielderId;
          // runs off bat
    private int extra;       // number of extra runs
    private String extraType;    // wide, no-ball, bye, leg-bye
    private String eventType;    // wide, no-ball, bye, leg-bye, boundry

    private String dismissalType; // catch, bowled, runout, stumped, lbw, hit-wicket, retired
    private Boolean isLegal;

    private Long inningsId;
    private Long outPlayerId;
    private int runsOnThisBall;
    private int extrasThisBall;
    private boolean isFour;
    private boolean isSix;
    private boolean firstInnings=true;
    private String comment;
    private Long mediaId;


    private double crr;
    private double rrr;





}


