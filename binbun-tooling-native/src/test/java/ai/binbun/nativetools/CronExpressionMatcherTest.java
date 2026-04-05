package ai.binbun.nativetools;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CronExpressionMatcherTest {
    private final CronExpressionMatcher matcher = new CronExpressionMatcher();

    @Test
    void matchesStartupOnce() {
        Instant now = Instant.parse("2026-04-03T18:00:00Z");
        assertTrue(matcher.matches("@startup", now, null));
        assertFalse(matcher.matches("@startup", now, now));
    }

    @Test
    void matchesEverySeconds() {
        Instant now = Instant.parse("2026-04-03T18:01:00Z");
        Instant last = Instant.parse("2026-04-03T18:00:00Z");
        assertTrue(matcher.matches("@every-seconds:30", now, last));
    }

    @Test
    void matchesFiveFieldCron() {
        Instant now = Instant.parse("2026-04-03T18:15:00Z");
        assertTrue(matcher.matches("15 18 * * 5", now, null));
        assertTrue(matcher.matches("*/15 18 * * 5", now, null));
        assertFalse(matcher.matches("16 18 * * 5", now, null));
    }
}
