package com.liveramp.captain.request_lock.running_ids_manager_request_lock;

import java.util.Set;

import com.liveramp.captain.request_lock.CaptainRequestLockWithRunningIds;

public class RunningIdsManagerCaptainLock implements CaptainRequestLockWithRunningIds {
  private final RunningIdsManager runningIdsManager;

  public static RunningIdsManagerCaptainLock getProduction() {
    return new RunningIdsManagerCaptainLock(new RunningIdsManager());
  }

  private RunningIdsManagerCaptainLock(RunningIdsManager runningIdsManager) {
    this.runningIdsManager = runningIdsManager;
  }

  @Override
  public void lock(long id) {
    runningIdsManager.add(id);
  }

  @Override
  public void unlock(long id) {
    runningIdsManager.remove(id);
  }

  @Override
  public Set<Long> getLockedRequestIds() {
    return runningIdsManager.getRunningIds();
  }
}
