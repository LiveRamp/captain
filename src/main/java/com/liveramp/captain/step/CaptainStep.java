package com.liveramp.captain.step;

import org.jetbrains.annotations.NotNull;

public class CaptainStep {
  private String step;

  private CaptainStep(String step) {
    this.step = step;
  }

  public String get() {
    return step;
  }

  @NotNull
  public static CaptainStep fromString(String step) {
    return new CaptainStep(step);
  }

  @Override
  public int hashCode() {
    return step.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CaptainStep)) {
      return false;
    }

    return step.equals(((CaptainStep)o).get());
  }

  @Override
  public String toString() {
    return "CaptainStep{ step=" + step + " }";
  }
}
