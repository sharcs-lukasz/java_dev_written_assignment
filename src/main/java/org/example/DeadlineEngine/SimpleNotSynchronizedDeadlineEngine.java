package org.example.DeadlineEngine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SimpleNotSynchronizedDeadlineEngine implements DeadlineEngine {

    private final PriorityQueue<Deadline> queue;
    private final HashMap<Long, Deadline> map;

    SimpleNotSynchronizedDeadlineEngine() {
        this.queue = new PriorityQueue<>();
        this.map = new HashMap<>();
    }

    @Override
    public long schedule(long deadlineMs) {
        final Deadline deadline = new Deadline(deadlineMs);
        queue.add(deadline);
        map.put(deadline.id, deadline);
        return deadline.id;
    }

    @Override
    public boolean cancel(long requestId) {
        Deadline deadline = map.get(requestId);
        if (null == deadline) {
            return false;
        }
        map.remove(requestId);
        return queue.remove(deadline);
    }

    @Override
    public int poll(long nowMs, Consumer<Long> handler, int maxPoll) {
        if (nowMs <= 0 || maxPoll <= 0) {
            return 0;
        }
        int counter = 0;
        while (counter < maxPoll && queue.size() > 0) {
            Deadline deadline = queue.peek();
            if (deadline.timeout > nowMs) {
                break;
            }
            if (null != handler) {
                handler.accept(deadline.id);
            }
            queue.poll();
            ++counter;
        }
        return counter;
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public String toString() {
        Deadline[] array = queue.toArray(new Deadline[0]);
        Arrays.sort(array);
        return Arrays.stream(array).map(dl -> String.valueOf(dl.timeout)).collect(Collectors.joining(", ", "[", "]"));
    }

    static class Deadline implements Comparable<Deadline> {
        private static long nextId = 0L;
        private final long id;
        private final long timeout;
        Deadline (long deadlineMs) {
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
