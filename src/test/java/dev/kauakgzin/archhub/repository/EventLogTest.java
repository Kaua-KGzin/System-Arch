package dev.kauakgzin.archhub.repository;

import dev.kauakgzin.archhub.domain.EventType;
import dev.kauakgzin.archhub.domain.SystemEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventLogTest {

    @Test
    void recentReturnsMostRecentFirst() {
        EventLog log = new EventLog();
        log.append("archmap", EventType.REGISTERED, "first", Instant.parse("2026-01-01T00:00:00Z"));
        log.append("archmap", EventType.WENT_DOWN, "second", Instant.parse("2026-01-01T00:01:00Z"));

        List<SystemEvent> recent = log.recent(10);

        assertThat(recent).extracting(SystemEvent::message).containsExactly("second", "first");
    }

    @Test
    void recentRespectsLimit() {
        EventLog log = new EventLog();
        for (int i = 0; i < 5; i++) {
            log.append("archmap", EventType.REGISTERED, "event-" + i, Instant.now());
        }

        assertThat(log.recent(2)).hasSize(2);
    }

    @Test
    void dropsOldestEventsBeyondCapacity() {
        EventLog log = new EventLog(3);
        for (int i = 0; i < 5; i++) {
            log.append("archmap", EventType.REGISTERED, "event-" + i, Instant.now());
        }

        assertThat(log.recent(10)).extracting(SystemEvent::message)
                .containsExactly("event-4", "event-3", "event-2");
    }

    @Test
    void assignsIncrementingSequenceNumbers() {
        EventLog log = new EventLog();
        SystemEvent first = log.append("archmap", EventType.REGISTERED, "a", Instant.now());
        SystemEvent second = log.append("archmap", EventType.REGISTERED, "b", Instant.now());

        assertThat(second.sequence()).isEqualTo(first.sequence() + 1);
    }
}
