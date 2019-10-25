package com.liveramp.captain.exception;

public class CaptainSubmissionFailedException extends RuntimeException {
  public CaptainSubmissionFailedException(String message, Exception e) {
    super(message, e);
  }
}
