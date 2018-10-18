package com.liveramp.captain.daemon;

import com.liveramp.captain.app_type.CaptainAppType;
import com.liveramp.captain.status.CaptainStatus;
import com.liveramp.captain.step.CaptainStep;
import com.liveramp.daemon_lib.JobletConfig;

public abstract class CaptainRequestConfig implements JobletConfig {
  public abstract long getId();

  public abstract CaptainStatus getStatus();

  public abstract CaptainStep getStep();

  public abstract CaptainAppType getAppType();

  @Override
  public String toString() {
    return "CaptainRequestConfig{" +
        "id=" + getId() +
        ",status=" + getStatus() +
        ",step=" + getStep() +
        ",appType=" + getAppType() +
        "}";
  }
}
