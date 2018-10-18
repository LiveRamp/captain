package com.liveramp.captain.exception;

public class CaptainPersistorException extends RuntimeException {
  public CaptainPersistorException(String message, Exception e) {
    super(message, e);
  }

  public static <ServiceHandle> CaptainPersistorException of(Exception e, long id, String stepString, ServiceHandle serviceHandle) {
    String message = String.format("handle persistor failed for request id: %s at step: %s while attempting to persist service handle: %s. quarantining request.",
        id, stepString, serviceHandle
    );

    return new CaptainPersistorException(message, e);
  }
}
