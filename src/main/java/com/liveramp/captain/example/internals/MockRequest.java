package com.liveramp.captain.example.internals;

import java.util.Optional;

import com.liveramp.captain.status.CaptainStatus;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class MockRequest {
  // representation of what a record my look like in our mockdb.
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // we're not going to serialize this class; it's okay.
  public final long id;
  public final CaptainStatus status;
  public final String step;
  public final String appType;
  public final Optional<Long> requestHandle;

  MockRequest(long id, CaptainStatus status, String step, String appType, Optional<Long> requestHandle) {
    this.id = id;
    this.status = status;
    this.step = step;
    this.appType = appType;
    this.requestHandle = requestHandle;
  }
}
