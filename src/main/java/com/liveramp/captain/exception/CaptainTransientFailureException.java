package com.liveramp.captain.exception;

public class CaptainTransientFailureException extends RuntimeException {
  public CaptainTransientFailureException(String message, Exception e) {
    super(message, e);
  }
}
