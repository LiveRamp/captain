package com.liveramp.captain.example;

import java.util.Map;
import java.util.Optional;

import com.liveramp.captain.status.CaptainStatus;
import com.liveramp.commons.collections.map.MapBuilder;

import static com.liveramp.captain.example.ExampleCaptainWorkflow.APP1;
import static com.liveramp.captain.example.ExampleCaptainWorkflow.APP2;
import static com.liveramp.captain.example.ExampleCaptainWorkflow.STEP1;
import static com.liveramp.captain.example.ExampleCaptainWorkflow.STEP2;
import static com.liveramp.captain.example.ExampleCaptainWorkflow.STEP5;

/*
 * code so that the ExampleCaptainWorkflow can run, but not actually useful in learning about captain.
 */
class ExampleInternals {

  // our mock db for the sake of running some requests through our captain workflow. feel free to ignore. use a real db.
  static class MockDb {
    private static final Map<Long, MockRequest> requests = MapBuilder
        .of(1L, new MockRequest(1L, CaptainStatus.READY, STEP1, APP1, Optional.empty()))
        .put(2L, new MockRequest(2L, CaptainStatus.READY, STEP2, APP1, Optional.empty()))
        .put(3L, new MockRequest(3L, CaptainStatus.READY, STEP1, APP2, Optional.empty()))
        .get();

    private long pointer = 1;

    public MockRequest getNextRequest() {
      // if everything is done stop kill the example captain workflow...
      // normally it would run until you killed it, but for the example, we don't want to run forever.
      if (requests.values().stream().allMatch(req -> req.step.equals(STEP5))) {
        Runtime.getRuntime().exit(10);
      }

      // find the request we visited least recently that's not done.
      while (true) {
        pointer = pointer == 3L ? 1L : pointer + 1;
        MockRequest request = requests.get(pointer);
        if (!request.step.equals(STEP5)) {
          return request;
        }
      }
    }

    /*
     * Mock Queries
     */
    public MockRequest getRequest(long id) {
      return requests.get(id);
    }

    public void setRequestState(long id, CaptainStatus status, String step) {
      requests.put(id, new MockRequest(id, status, step, requests.get(id).appType, requests.get(id).requestHandle));
    }

    public void setRequestState(long id, CaptainStatus status) {
      setRequestState(id, status, requests.get(id).step);
    }

    public void setRequestHandle(long id, long requestHandle) {
      MockRequest previousState = requests.get(id);
      requests.put(id, new MockRequest(id, previousState.status, previousState.step, previousState.appType, Optional.of(requestHandle)));
    }

  }

  // representation of what a record my look like in our mockdb.
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // we're not going to serialize this class; it's okay.
  static class MockRequest {
    final long id;
    final CaptainStatus status;
    final String step;
    final String appType;
    final Optional<Long> requestHandle;

    MockRequest(long id, CaptainStatus status, String step, String appType, Optional<Long> requestHandle) {
      this.id = id;
      this.status = status;
      this.step = step;
      this.appType = appType;
      this.requestHandle = requestHandle;
    }
  }
}
