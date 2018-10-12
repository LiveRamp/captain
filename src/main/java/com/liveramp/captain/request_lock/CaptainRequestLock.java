package com.liveramp.captain.request_lock;

/**
 * this interface is sufficient to get captain to run and lock requests properly. if you want to
 * avoid pulling in already locked requests to your config producer consider using
 * CaptainRequestLockWithRunningIds
 */
public interface CaptainRequestLock {
  void lock(long jobId);

  void unlock(long jobId);
}
