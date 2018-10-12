package com.liveramp.captain.app_type;

import org.jetbrains.annotations.NotNull;

public class CaptainAppType {
  private String appType;

  private CaptainAppType(String appType) {
    this.appType = appType;
  }

  public String get() {
    return appType;
  }

  @NotNull
  public static CaptainAppType fromString(String appType) {
    return new CaptainAppType(appType);
  }

  @Override
  public int hashCode() {
    return appType.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CaptainAppType)) {
      return false;
    }

    return appType.equals(((CaptainAppType)o).get());
  }

  @Override
  public String toString() {
    return "CaptainAppType{ appType=" + appType + " }";
  }
}
