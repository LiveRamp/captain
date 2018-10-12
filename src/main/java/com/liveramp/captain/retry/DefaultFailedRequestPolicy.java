package com.liveramp.captain.retry;


public class DefaultFailedRequestPolicy implements FailedRequestPolicy {
  @Override
  public FailedRequestAction getFailedRequestAction(Long jobId) {
    return FailedRequestAction.NO_OP;
  }
}
