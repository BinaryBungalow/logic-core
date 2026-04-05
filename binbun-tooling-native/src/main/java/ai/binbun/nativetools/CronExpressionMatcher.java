package ai.binbun.nativetools;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public final class CronExpressionMatcher {
    public boolean matches(String expression, Instant now, Instant lastRunAt) {
        if (expression == null || expression.isBlank()) {
            return false;
        }
        if ("@startup".equals(expression)) {
            return lastRunAt == null;
        }
        if (expression.startsWith("@every-seconds:")) {
            long seconds = Long.parseLong(expression.substring("@every-seconds:".length()));
            return lastRunAt == null || now.getEpochSecond() - lastRunAt.getEpochSecond() >= seconds;
        }

        String[] parts = expression.trim().split("\\s+");
        if (parts.length != 5) {
            return false;
        }
        ZonedDateTime dt = ZonedDateTime.ofInstant(now, ZoneOffset.UTC);
        return matchesField(parts[0], dt.getMinute(), 0, 59)
                && matchesField(parts[1], dt.getHour(), 0, 23)
                && matchesField(parts[2], dt.getDayOfMonth(), 1, 31)
                && matchesField(parts[3], dt.getMonthValue(), 1, 12)
                && matchesField(parts[4], dt.getDayOfWeek().getValue() % 7, 0, 6)
                && (lastRunAt == null || lastRunAt.getEpochSecond() / 60 != now.getEpochSecond() / 60);
    }

    private boolean matchesField(String expr, int value, int min, int max) {
        for (String token : expr.split(",")) {
            if (matchesToken(token.trim(), value, min, max)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesToken(String token, int value, int min, int max) {
        if (token.equals("*")) {
            return true;
        }
        if (token.contains("/")) {
            String[] split = token.split("/");
            List<Integer> base = expandBase(split[0], min, max);
            int step = Integer.parseInt(split[1]);
            for (int candidate : base) {
                if ((candidate - base.get(0)) % step == 0 && candidate == value) {
                    return true;
                }
            }
            return false;
        }
        if (token.contains("-")) {
            String[] split = token.split("-");
            int start = Integer.parseInt(split[0]);
            int end = Integer.parseInt(split[1]);
            return value >= start && value <= end;
        }
        return Integer.parseInt(token) == value;
    }

    private List<Integer> expandBase(String token, int min, int max) {
        if (token.equals("*")) {
            List<Integer> values = new ArrayList<>();
            for (int i = min; i <= max; i++) {
                values.add(i);
            }
            return values;
        }
        if (token.contains("-")) {
            String[] split = token.split("-");
            int start = Integer.parseInt(split[0]);
            int end = Integer.parseInt(split[1]);
            List<Integer> values = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                values.add(i);
            }
            return values;
        }
        return List.of(Integer.parseInt(token));
    }
}
