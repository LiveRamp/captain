package com.liveramp.captain.request_lock.running_ids_manager_request_lock;

import java.util.Set;

import com.google.common.collect.Sets;

class RunningIdsManager {

  private final Set<Long> runningIds;

  RunningIdsManager() {
    this.runningIds = Sets.newHashSet();
  }

  public synchronized void remove(long id) {
    runningIds.remove(id);
  }

  public synchronized void add(long id) {
    runningIds.add(id);
  }

  public synchronized Set<Long> getRunningIds() {
    return Sets.newHashSet(runningIds);
  }
}

