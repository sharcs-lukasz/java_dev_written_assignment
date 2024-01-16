package org.example.DeadlineEngine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Synchronized implementation of DeadlineEngine interface.
 * Manages an active set of deadlines to be raised whenever they expire.
 */
public class SynchronizedDeadlineEngine implements DeadlineEngine {

    private final PriorityQueue<Deadline> queue;
    private final HashMap<Long, Deadline> map;

    SynchronizedDeadlineEngine() {
        this.queue = new PriorityQueue<>();
        this.map = new HashMap<>();
    }

    /**
     * Request a new deadline be added to the engine.  The deadline is in millis offset from
     * unix epoch. https://en.wikipedia.org/wiki/Unix_time
     * The engine will raise an event whenever a deadline (usually now in millis) supplied in the poll method
     * exceeds the request deadline.
     * @param deadlineMs Deadline as unix epoch (in milliseconds).
     * @return Returns -1 if deadlineMs param is negative or zero. Otherwise, returns an identifier for the scheduled deadline.
     */
    @Override
    public synchronized long schedule(long deadlineMs) {
        if (deadlineMs <= 0L) {
            return -1L;
        }
        final Deadline deadline = new Deadline(deadlineMs);
        queue.add(deadline);
        map.put(deadline.id, deadline);
        return deadline.id;
    }

    /**
     * Remove the scheduled event using the identifier returned when the deadline was scheduled.
     * @param requestId Identifier of the scheduled deadline to be cancelled.
     * @return Returns true if canceled and false otherwise.
     */
    @Override
    public synchronized boolean cancel(long requestId) {
        final Deadline deadline = map.get(requestId);
        if (null == deadline) {
            return false;
        }
        map.remove(requestId);
        return queue.remove(deadline);
    }

    /**
     * Supplies a deadline in millis to check against scheduled deadlines.  If any deadlines are triggered the
     * supplied handler is called with the identifier of the expired deadline.
     * To avoid a system flood and manage how many expired events we can handle we also pass in the maximum number of
     * expired deadlines to fire.  Those expired deadlines that wernt raised will be available in the next poll.
     * There is no need for the triggered deadlines to fire in order.
     * @param nowMs time in millis since epoch to check deadlines against.
     * @param handler to call with identifier of expired deadlines.
     * @param maxPoll count of maximum number of expired deadlines to process.
     * @return number of expired deadlines that fired successfully.
     */
    @Override
    public int poll(long nowMs, Consumer<Long> handler, int maxPoll) {
        if (nowMs <= 0 || maxPoll <= 0) {
            return 0;
        }
        int counter = 0;
        while (counter < maxPoll) {
            synchronized (this) {
                if (queue.size() <= 0) {
                    break;
                }
                final Deadline deadline = queue.peek();
                if (deadline.timeout > nowMs) {
                    break;
                }
                if (null != handler) {
                    handler.accept(deadline.id);
                }
                queue.poll();
                map.remove(deadline.id);
                ++counter;
            }
        }
        return counter;
    }

    /**
     * The number of registered deadlines.
     * @return the number of registered deadlines.
     */
    @Override
    public synchronized int size() {
        return queue.size();
    }

    /**
     * Returns string representation of the scheduled deadlines. Enclosed in square brackets and comma separated.
     * @return String representation of the scheduled deadlines sorted from nearest to farthest.
     */
    @Override
    public String toString() {
        final Deadline[] array;
        synchronized (this) {
             array = queue.toArray(new Deadline[0]);
        }
        Arrays.sort(array);
        return Arrays.stream(array).map(dl -> String.valueOf(dl.timeout)).collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Internal class representing a deadline object with a timeout and an identifier.
     */
    static class Deadline implements Comparable<Deadline> {
        private static long nextId = 0L;
        final private long id;
        final private long timeout;

        Deadline(long deadlineMs) {
            this.id = ++nextId;
            this.timeout = deadlineMs;
        }

        @Override
        public int compareTo(Deadline deadline) {
            if (this.timeout < deadline.timeout) {
                return -1;
            } else if (this.timeout > deadline.timeout) {
                return 1;
            }
            return this.id < deadline.id ? -1 : (this.id > deadline.id ? 1 : 0);
        }

        @Override
        public boolean equals(Object obj) {
            if (null == obj) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }
            Deadline dl = (Deadline) obj;
            if (this.id == dl.id && this.timeout == dl.timeout) {
                return true;
            }
            return false;
        }
    }
}
