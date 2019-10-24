package com.liveramp.captain.daemon;

import com.liveramp.captain.status.CaptainStatus;
import com.liveramp.captain.step.CaptainStep;

public interface RequestUpdater {
  void setStatus(long id, CaptainStep currentStep, CaptainStatus currentStatus, CaptainStatus newStatus);

  void setStepAndStatus(long id, CaptainStep currentStep, CaptainStatus currentStatus, CaptainStep newStep, CaptainStatus newStatus);

  void cancel(long id);

  void fail(long id);

  default void retry(long id) {

  }

  default void quarantine(long id) {

  }

  default void ready(long id) {

  }
}
