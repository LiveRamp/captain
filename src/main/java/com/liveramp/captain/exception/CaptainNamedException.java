package com.liveramp.captain.exception;

public class CaptainNamedException extends RuntimeException {
  public CaptainNamedException(String message, Exception e) {
    super(message, e);
  }

  public static <ServiceHandle> CaptainNamedException of(Exception e, long id, String stepString, ServiceHandle serviceHandle) {
    String message = String.format("handle transient failure for request id: %s at step: %s while attempting to execute service handle: %s. status will remain the same for the request.",
        id, stepString, serviceHandle
    );
    return new CaptainNamedException(message, e);
  }
}
