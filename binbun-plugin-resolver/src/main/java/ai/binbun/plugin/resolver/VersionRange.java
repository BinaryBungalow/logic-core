package ai.binbun.plugin.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionRange {
    private static final Pattern RANGE_PATTERN = Pattern.compile("(>=|<=|>|<|=)?\\s*(\\d+)\\.(\\d+)\\.(\\d+)");

    private final List<Constraint> constraints = new ArrayList<>();

    public static VersionRange parse(String range) {
        VersionRange vr = new VersionRange();
        if (range == null || range.isBlank() || range.equals("*")) {
            return vr;
        }
        Matcher m = RANGE_PATTERN.matcher(range);
        while (m.find()) {
            String op = m.group(1) == null ? "=" : m.group(1);
            SemVer ver = new SemVer(
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(4))
            );
            vr.constraints.add(new Constraint(op, ver));
        }
        return vr;
    }

    public boolean matches(SemVer version) {
        for (Constraint c : constraints) {
            if (!c.matches(version)) return false;
        }
        return true;
    }

    private record Constraint(String op, SemVer version) {
        boolean matches(SemVer v) {
            int cmp = v.compareTo(version);
            return switch (op) {
                case ">=" -> cmp >= 0;
                case "<=" -> cmp <= 0;
                case ">" -> cmp > 0;
                case "<" -> cmp < 0;
                default -> cmp == 0;
            };
        }
    }
}
