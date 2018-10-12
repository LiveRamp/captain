package com.liveramp.captain.request_lock.zk_request_lock;

import com.google.common.collect.Sets;
import java.util.Set;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ZKRequestLock {
  private static final Logger LOG = LoggerFactory.getLogger(ZKRequestLock.class);

  private CuratorFramework framework;
  private String zkRootPath;
  private final Set<String> locks = Sets.newHashSet();

  public static ZKRequestLock getTest(CuratorFramework framework, String zkRootPath) {
    return new ZKRequestLock(framework, zkRootPath);
  }

  ZKRequestLock(CuratorFramework framework, String zkRootPath) {
    this.framework = framework;
    this.zkRootPath = zkRootPath;
  }

  public void acquire(String requestId) throws Exception {
    // if for some reason this process has already locked this request, don't try to lock it again.
    if (!locks.contains(requestId)) {
      String requestPath = zkRootPath + "/" + requestId;

      if (framework.checkExists().forPath(requestPath) != null) {
        throw new RuntimeException(
            String.format("request: %s already locked by another process.", requestId));
      }
      framework
          .create()
          .creatingParentsIfNeeded()
          .withMode(CreateMode.EPHEMERAL)
          .forPath(requestPath);

      locks.add(requestId);
    }
  }

  public void release(String requestId) throws Exception {
    if (locks.contains(requestId)) {
      framework.delete().deletingChildrenIfNeeded().forPath(zkRootPath + "/" + requestId);
      locks.remove(requestId);
    } else {
      throw new RuntimeException(
          String.format(
              "trying to unlock a lock owned by another process. request id: %s", requestId));
    }
  }

  public void releaseSafe(String requestId) {
    try {
      release(requestId);
    } catch (Exception e) {
      LOG.error(
          "caught exception during safe release of lock. logging exception and continuing execution.",
          e);
    }
  }

  public Set<String> getLockedIds() throws Exception {
    if (framework.checkExists().forPath(zkRootPath) == null) {
      return Sets.newHashSet();
    }

    return Sets.newHashSet(framework.getChildren().forPath(zkRootPath));
  }
}
