package com.livescore.backend.Cricket;

import com.livescore.backend.Entity.CricketBall;

public class CricketRules {

    /**
     * Whether this ball counts as a ball faced by the batsman.
     * Wides are NOT faced by batsman. Everything else is.
     */
    public static boolean isBallFaced(CricketBall ball) {
        if (ball == null) return false;
        String extraType = ball.getExtraType();
        if (extraType != null && extraType.equalsIgnoreCase("wide")) {
            return false;
        }
        return true;
    }

    /**
     * How many runs are conceded by the bowler on this ball.
     *
     * Bowler is charged for:
     * - All runs off the bat (normal runs, boundaries)
     * - Wide runs (1 + any additional)
     * - No-ball penalty (1) + runs off the bat on no-ball
     *
     * Bowler is NOT charged for:
     * - Byes
     * - Leg byes
     */
    public static int runsConcededThisBall(CricketBall ball) {
        if (ball == null) return 0;

        String extraType = ball.getExtraType();
        int runs = ball.getRuns() != null ? ball.getRuns() : 0;
        int extra = ball.getExtra() != null ? ball.getExtra() : 0;

        if (extraType == null || extraType.isEmpty()) {
            // normal delivery — bowler concedes the runs
            return runs;
        }

        String et = extraType.toLowerCase();

        if (et.equals("wide")) {
            // wide: 1 penalty + any additional runs (overthrows etc.)
            return runs + 1;
        }

        if (et.equals("noball")) {
            // no-ball: 1 penalty + runs scored off bat
            return runs + 1;
        }

        if (et.equals("bye") || et.equals("legbye")) {
            // byes and leg byes: bowler concedes nothing
            return 0;
        }

        // default fallback
        return runs;
    }

    /**
     * Whether this dismissal type is credited to the bowler.
     * Runouts, retired, and mankad are NOT bowler's wickets.
     */
    public static boolean isBowlerCreditedWicket(String dismissalType) {
        if (dismissalType == null || dismissalType.isEmpty()) return false;

        String dt = dismissalType.toLowerCase().trim();

        switch (dt) {
            case "bowled":
            case "caught":
            case "hitwicket":
            case "stumped":
            case "lbw":
            case "overthefence":
            case "onehandonebounce":
                return true;

            case "runout":
            case "retired":
            case "mankad":
                return false;

            default:
                return false;
        }
    }

    /**
     * Whether this delivery is a legal delivery (counts towards the over).
     * Wides and no-balls are NOT legal deliveries.
     */
    public static boolean isLegalDelivery(CricketBall ball) {
        if (ball == null) return false;
        return Boolean.TRUE.equals(ball.getLegalDelivery());
    }

    /**
     * Whether this is a dot ball (legal delivery, 0 runs conceded, no wicket).
     */
    public static boolean isDotBall(CricketBall ball) {
        if (ball == null) return false;
        return isLegalDelivery(ball)
                && runsConcededThisBall(ball) == 0
                && ball.getDismissalType() == null;
    }
}