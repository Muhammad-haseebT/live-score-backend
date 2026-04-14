package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class PlayerFullStatsDTO {

    private Long   playerId;
    private String playerName;
    private int    matchesPlayed;
    private int    pomCount;

    // ── Sport identifier — frontend checks this ───────────────────
    // "cricket", "futsal", etc.
    private String sport;

    // ── CRICKET — Batting ────────────────────────────────────────
    private int    totalRuns;
    private int    ballsFaced;
    private double strikeRate;
    private double battingAvg;
    private int    highest;
    private int    fours;
    private int    sixes;
    private int    notOuts;
    private int    fifties;
    private int    hundreds;

    // ── CRICKET — Bowling ────────────────────────────────────────
    private int    wickets;
    private int    ballsBowled;
    private int    runsConceded;
    private double economy;
    private double bowlingAverage;
    private double bowlingStrikeRate;
    private int    maidens;
    private int    dotBalls;
    private int    threeWicketHauls;
    private int    fiveWicketHauls;

    // ── CRICKET — Fielding ───────────────────────────────────────
    private int    catches;
    private int    stumpings;
    private int    runouts;

    // ── FUTSAL ──────────────────────────────────────────────────
    private int    goals;
    private int    assists;
    private int    ownGoals;
    private int    futsalFouls;
    private int    yellowCards;
    private int    redCards;
}
