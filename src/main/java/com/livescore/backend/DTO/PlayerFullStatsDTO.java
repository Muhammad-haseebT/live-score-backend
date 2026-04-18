package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class PlayerFullStatsDTO {

    private Long   playerId;
    private String playerName;
    private String sport; // "cricket" / "futsal" / "volleyball"

    // ── matchesPlayed — sport-specific ───────────────────────────
    private int    matchesPlayed;           // current sport ka count
    private int    cricketMatchesPlayed;
    private int    futsalMatchesPlayed;
    private int    volleyballMatchesPlayed; // ✅ NEW

    private int    pomCount;

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

    // ── FUTSAL / VOLLEYBALL ──────────────────────────────────────
    private int    goals;        // futsal: goals    | volleyball: points scored
    private int    assists;      // futsal: assists  | volleyball: aces
    private int    ownGoals;
    private int    futsalFouls;  // futsal: fouls    | volleyball: blocks
    private int    yellowCards;  // futsal: yellow   | volleyball: attack errors
    private int    redCards;     // futsal: red      | volleyball: service errors
}