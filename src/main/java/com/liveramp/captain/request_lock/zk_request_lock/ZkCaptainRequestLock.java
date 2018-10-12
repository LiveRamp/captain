package com.liveramp.captain.request_lock.zk_request_lock;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.curator.framework.CuratorFramework;

import com.liveramp.captain.request_lock.CaptainRequestLockWithRunningIds;

public class ZkCaptainRequestLock implements CaptainRequestLockWithRunningIds {
  private ZKRequestLock requestLock;

  private ZkCaptainRequestLock(ZKRequestLock requestLock) {
    this.requestLock = requestLock;
  }

  public Set<Long> getLockedRequestIds() {
    try {
      return requestLock.getLockedIds().stream().map(Long::valueOf).collect(Collectors.toSet());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void lock(long jobId) {
    try {
      requestLock.acquire(String.valueOf(jobId));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void unlock(long jobId) {
    try {
      requestLock.releaseSafe(String.valueOf(jobId));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static ZkCaptainRequestLock getProduction(CuratorFramework curatorFramework, String zkRootPath) {
    return new ZkCaptainRequestLock(new ZKRequestLock(curatorFramework, zkRootPath));
  }
}
