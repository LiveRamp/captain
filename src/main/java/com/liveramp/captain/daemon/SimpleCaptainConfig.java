package com.liveramp.captain.daemon;

import com.liveramp.captain.app_type.CaptainAppType;
import com.liveramp.captain.status.CaptainStatus;
import com.liveramp.captain.step.CaptainStep;

public class SimpleCaptainConfig extends CaptainRequestConfig {
  private long id;
  private CaptainStatus status;
  private CaptainStep step;
  private CaptainAppType appType;

  public SimpleCaptainConfig(
      long id, CaptainStatus status, CaptainStep step, CaptainAppType appType) {
    this.id = id;
    this.status = status;
    this.step = step;
    this.appType = appType;
  }

  @Override
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @Override
  public CaptainStatus getStatus() {
    return status;
  }

  public void setStatus(CaptainStatus status) {
    this.status = status;
  }

  @Override
  public CaptainStep getStep() {
    return step;
  }

  public void setStep(CaptainStep step) {
    this.step = step;
  }

  @Override
  public CaptainAppType getAppType() {
    return appType;
  }

  public void setAppType(CaptainAppType appType) {
    this.appType = appType;
  }
}
