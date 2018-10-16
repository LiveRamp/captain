package com.liveramp.captain.example.internals;

import com.liveramp.captain.request_context.RequestContext;

/*
 * code so that the ExampleCaptainWorkflow can run, but not actually useful in learning about captain.
 */
public class MockAnalyticsService {
  private MockServiceStatusProvider mockServiceStatusProvider = new MockServiceStatusProvider();

  @SuppressWarnings("unused")
  public long submit(long id, RequestContext options) {
    return id + 255; // generate some id.
  }

  public boolean isRequestComplete(long reqId) {
    return mockServiceStatusProvider.isDone(reqId);
  }

}
