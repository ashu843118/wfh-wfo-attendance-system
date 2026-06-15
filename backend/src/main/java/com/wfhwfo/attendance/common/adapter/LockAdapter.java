package com.wfhwfo.attendance.common.adapter;

public interface LockAdapter {

    void executeWithLock(String key, long waitTimeMs, long leaseTimeMs, Runnable action);
}
