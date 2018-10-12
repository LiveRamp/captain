package com.liveramp.captain.request_lock.running_ids_manager_request_lock;

import com.liveramp.captain.request_lock.CaptainRequestLockWithRunningIds;
import java.util.Set;

public class RunningIdsManagerCaptainLock implements CaptainRequestLockWithRunningIds {
  private final RunningIdsManager runningIdsManager;

  public static RunningIdsManagerCaptainLock getProduction() {
    return new RunningIdsManagerCaptainLock(new RunningIdsManager());
  }

  private RunningIdsManagerCaptainLock(RunningIdsManager runningIdsManager) {
    this.runningIdsManager = runningIdsManager;
  }

  @Override
  public void lock(long jobId) {
    runningIdsManager.add(jobId);
  }

  @Override
  public void unlock(long jobId) {
    runningIdsManager.remove(jobId);
  }

  @Override
  public Set<Long> getLockedRequestIds() {
    return runningIdsManager.getRunningIds();
  }
}
