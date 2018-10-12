package com.liveramp.captain.daemon;

import com.liveramp.captain.status.CaptainStatus;
import com.liveramp.captain.step.CaptainStep;

public interface RequestUpdater {
  void setStatus(
      long jobId, CaptainStep currentStep, CaptainStatus currentStatus, CaptainStatus newStatus);

  void setStepAndStatus(
      long jobId,
      CaptainStep currentStep,
      CaptainStatus currentStatus,
      CaptainStep newStep,
      CaptainStatus newStatus);

  void cancel(long jobId);

  void fail(long jobId);

  default void retry(long jobId) {}

  default void quarantine(long jobId) {}
}
