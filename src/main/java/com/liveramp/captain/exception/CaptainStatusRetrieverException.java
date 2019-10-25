package com.liveramp.captain.exception;

public class CaptainStatusRetrieverException extends RuntimeException {
  public CaptainStatusRetrieverException(String message, Exception e) {
    super(message, e);
  }
}
