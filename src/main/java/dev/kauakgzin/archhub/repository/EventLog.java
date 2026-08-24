package dev.kauakgzin.archhub.repository;

import dev.kauakgzin.archhub.domain.EventType;
import dev.kauakgzin.archhub.domain.SystemEvent;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded, thread-safe activity feed of everything that happened to
 * connected systems (registrations, removals, up/down transitions).
 * Oldest entries are dropped once {@link #capacity} is exceeded.
 */
@Repository
public class EventLog {

    private final int capacity;
    private final Deque<SystemEvent> events = new ArrayDeque<>();
    private final AtomicLong sequence = new AtomicLong();

    public EventLog() {
        this(200);
    }

    public EventLog(int capacity) {
        this.capacity = capacity;
    }

    public synchronized SystemEvent append(String systemId, EventType type, String message, Instant occurredAt) {
        SystemEvent event = new SystemEvent(sequence.incrementAndGet(), systemId, type, message, occurredAt);
        events.addFirst(event);
        while (events.size() > capacity) {
            events.removeLast();
        }
        return event;
    }

    public synchronized List<SystemEvent> recent(int limit) {
        List<SystemEvent> result = new ArrayList<>(Math.min(limit, events.size()));
        for (SystemEvent event : events) {
            if (result.size() >= limit) {
                break;
            }
            result.add(event);
        }
        return result;
    }
}
