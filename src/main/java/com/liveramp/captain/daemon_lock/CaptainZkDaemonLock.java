package com.liveramp.captain.daemon;

import com.liveramp.daemon_lib.DaemonLock;
import com.liveramp.daemon_lib.utils.DaemonException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

public class CaptainZkDaemonLock implements DaemonLock {

  private static final String ZK_DAEMON_LOCK_BASE_PATH = "/daemon_lib_zk/zk_daemon_locks/";
  private final InterProcessLock lock;

  public static DaemonLock getProduction(CuratorFramework curatorFramework, String daemonId) {
    curatorFramework.start();
    Runtime.getRuntime()
        .addShutdownHook(new Thread(new CaptainZkDaemonLock.FrameworkShutdown(curatorFramework)));

    return new CaptainZkDaemonLock(curatorFramework, ZK_DAEMON_LOCK_BASE_PATH + daemonId);
  }

  private CaptainZkDaemonLock(CuratorFramework framework, String lockPath) {
    lock = new InterProcessMutex(framework, lockPath);
  }

  @Override
  public void lock() throws DaemonException {
    try {
      lock.acquire();
    } catch (Exception e) {
      throw new DaemonException(e);
    }
  }

  @Override
  public void unlock() {
    try {
      if (lock.isAcquiredInThisProcess()) {
        lock.release();
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static class FrameworkShutdown implements Runnable {

    CuratorFramework fw;

    FrameworkShutdown(CuratorFramework fw) {
      this.fw = fw;
    }

    @Override
    public void run() {
      fw.close();
    }
  }
}
