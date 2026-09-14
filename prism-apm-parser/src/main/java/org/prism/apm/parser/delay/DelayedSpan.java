package org.prism.apm.parser.delay;

import lombok.Data;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * A class representing a delayed task that can be placed in a DelayQueue.
 * The delay time is handled in milliseconds for better precision.
 */
@Data
public class DelayedSpan implements Delayed {

    private long startTime;
    private String spanId;

    /**
     * Constructs a new DelayedSpan.
     *
     * @param delayInMillis The delay time from now, in milliseconds.
     * @param spanId        The identifier for this task.
     */
    public DelayedSpan(long delayInMillis, String spanId) {
        // Calculate the execution time by adding the current time in milliseconds to the specified delay.
        this.startTime = System.currentTimeMillis() + delayInMillis;
        this.spanId = spanId;
    }

    /**
     * Returns the remaining delay associated with this object, in the given time unit.
     * This method is called by the DelayQueue to check if the task has expired.
     *
     * @param unit The time unit in which the delay is requested.
     * @return The remaining delay; zero or a negative value indicates that the delay has already expired.
     */
    @Override
    public long getDelay(TimeUnit unit) {
        // Calculate the remaining delay in milliseconds.
        long remainingDelay = startTime - System.currentTimeMillis();
        // Convert the remaining delay from milliseconds to the unit specified by the caller (e.g., the DelayQueue).
        return unit.convert(remainingDelay, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        // Compare the execution timestamps. Tasks scheduled to run earlier will come first in the queue.
        return Long.compare(this.startTime, ((DelayedSpan) o).startTime);
    }
}
