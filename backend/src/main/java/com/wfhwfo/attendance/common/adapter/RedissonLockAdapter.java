package com.wfhwfo.attendance.common.adapter;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedissonLockAdapter implements LockAdapter {

    private final RedissonClient redissonClient;

    @Override
    public void executeWithLock(String key, long waitTimeMs, long leaseTimeMs, Runnable action) {
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTimeMs, leaseTimeMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new IllegalStateException("Could not acquire lock for key: " + key);
            }
            action.run();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while acquiring lock for key: " + key, ex);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
