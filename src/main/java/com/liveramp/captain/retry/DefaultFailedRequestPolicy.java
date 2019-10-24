package com.liveramp.captain.retry;

public class DefaultFailedRequestPolicy implements FailedRequestPolicy {
 @Override
  public FailedRequestAction getFailedRequestAction(Long id) {
        return FailedRequestAction.NO_OP;
      }
}
