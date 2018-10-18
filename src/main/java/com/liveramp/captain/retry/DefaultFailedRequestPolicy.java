package com.liveramp.captain.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultFailedRequestPolicy implements FailedRequestPolicy {
 @Override
  public FailedRequestAction getFailedRequestAction(Long id) {
        return FailedRequestAction.NO_OP;
      }
}
