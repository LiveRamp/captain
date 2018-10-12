package com.liveramp.captain.request_lock;

import java.util.Set;

/**
 * this addition to the CaptainRequestLock iface, gives you the opportunity to, in your config producer, view all of the
 * currently locked ids, and ever pulling them in the config producer.
 */
public interface CaptainRequestLockWithRunningIds extends CaptainRequestLock {
  Set<Long> getLockedRequestIds();
}
