package com.liveramp.captain.exception;

import com.liveramp.captain.status.CaptainStatus;
import com.liveramp.captain.step.CaptainStep;

public class CaptainCouldNotFindNextStep extends RuntimeException {
  public CaptainCouldNotFindNextStep(CaptainStep step, CaptainStatus status) {
    super(String.format("could not find a next step after current step: %s, current status: %s", step, status));
  }
}
