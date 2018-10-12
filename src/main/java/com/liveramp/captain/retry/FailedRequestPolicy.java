package com.liveramp.captain.retry;

import com.liveramp.captain.daemon.RequestUpdater;

/***
 * Implementations of this interface are meant to make a decision on what to do when captain picks up a
 * request in status FAILED. There are three possible return values:
 *
 * RETRY: Captain will call the retry method in {@link RequestUpdater}
 * QUARANTINE: Captain will call the quarantine method in {@link RequestUpdater}
 * NO_OP: Captain will do nothing. By default captain uses a FailedRequestPolicy that always returns NO_OP.
 */
public interface FailedRequestPolicy {

  /***
   * Method called by captain to know what to do with a failed request
   * @param jobId id of the failed request
   * @return action to be executed by captain
   */
  FailedRequestAction getFailedRequestAction(Long jobId);

  enum FailedRequestAction {
    RETRY(1),
    QUARANTINE(2),
    NO_OP(3);

    private final int value;

    FailedRequestAction(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }
}
