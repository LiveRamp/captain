package com.liveramp.captain.exception;

public class CaptainSubmissionException extends RuntimeException {
  public CaptainSubmissionException(String message, Exception e) {
    super(message, e);
  }
}
