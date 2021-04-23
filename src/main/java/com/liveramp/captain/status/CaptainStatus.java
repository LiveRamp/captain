package com.liveramp.captain.status;

public enum CaptainStatus {
  CANCELLED(0),
  READY(1),
  PENDING(2),
  IN_PROGRESS(3),
  COMPLETED(4),
  FAILED(5),
  QUARANTINED(6),
  QUARANTINED_INTERNAL(7),
  QUARANTINED_EXTERNAL(8),
  QUARANTINED_ENG(9);

  private final int value;

  CaptainStatus(int value) {
    this.value = value;
  }

  public int getValue() {
    return this.value;
  }

  public static CaptainStatus findByValue(int value) {
    switch (value) {
      case 0:
        return CANCELLED;
      case 1:
        return READY;
      case 2:
        return PENDING;
      case 3:
        return IN_PROGRESS;
      case 4:
        return COMPLETED;
      case 5:
        return FAILED;
      case 6:
        return QUARANTINED;
      case 7:
        return QUARANTINED_INTERNAL;
      case 8:
        return QUARANTINED_EXTERNAL;
      case 9:
        return QUARANTINED_ENG;
      default:
        return null;
    }
  }
}
