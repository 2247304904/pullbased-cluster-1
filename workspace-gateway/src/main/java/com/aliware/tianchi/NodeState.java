package com.aliware.tianchi;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.exp;

/**
 * @author Viber
 * @version 1.0
 * @apiNote 节点的状态
 * @since 2021/9/10 14:00
 */
public class NodeState {
    private static final long timeInterval = TimeUnit.SECONDS.toMillis(1);
    private static final long oneMill = TimeUnit.MILLISECONDS.toNanos(1);
    public volatile int avgTime = 1;
    public volatile int weight = 100;
    private final Counter<StateCounter> counter = new Counter<>(o -> new StateCounter());
    public volatile long timeout = 30L;
    private static final double ALPHA = 1 - exp(-1 / 5.0);//来自框架metrics的计算系数
    private final int windowSize = 5;

    public NodeState(ScheduledExecutorService scheduledExecutor) {
        scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                long high = offset();
                long low = high - windowSize;
                long[] ret = sum(low, high);
                if (ret[0] > 0) {
                    long newTimeout = 8 + (ret[1] / ret[0]);
                    newTimeout = (long) (timeout + (newTimeout - timeout) * ALPHA);
                    timeout = newTimeout;
                }
                clean(high);
            }
        }, 5, 1, TimeUnit.SECONDS);
    }

    public int getWeight() {
        return Math.max(1, weight);
    }

    public void setWeight(int w) {
        if (weight != w) {
            weight = w;
        }
    }

    public void end(long duration) {
        long offset = offset();
        StateCounter state = counter.get(offset);
        state.getDuration().add(duration / oneMill);
        state.getTotal().add(1);
    }

    public long[] sum(long fromOffset, long toOffset) {
        long[] result = {0, 0};
        Collection<StateCounter> sub = counter.sub(fromOffset, toOffset);
        if (!sub.isEmpty()) {
            sub.forEach(state -> {
                result[0] += state.getTotal().sum();
                result[1] += state.getDuration().sum();
            });
        }
        return result;
    }

    public long offset() {
        return System.currentTimeMillis() / timeInterval;
    }

    public void clean(long high) {
        long toKey = high - (windowSize << 1);
        counter.clean(toKey);
    }

}
