package com.liveramp.captain.daemon;

import com.liveramp.captain.request_lock.CaptainRequestLock;
import com.liveramp.daemon_lib.JobletCallback;

public class CaptainRequestLockingCallbacks {
  static class CaptainRequestLockCallback implements JobletCallback<CaptainRequestConfig> {
    private CaptainRequestLock captainRequestLock;

    CaptainRequestLockCallback(CaptainRequestLock captainRequestLock) {
      this.captainRequestLock = captainRequestLock;
    }

    @Override
    public void callback(CaptainRequestConfig config) {
      captainRequestLock.lock(config.getId());
    }
  }

  static class CaptainRequestUnlockCallback implements JobletCallback<CaptainRequestConfig> {
    private CaptainRequestLock captainRequestLock;

    CaptainRequestUnlockCallback(CaptainRequestLock captainRequestLock) {
      this.captainRequestLock = captainRequestLock;
    }

    @Override
    public void callback(CaptainRequestConfig config) {
      captainRequestLock.unlock(config.getId());
    }
  }

}
