package com.liveramp.captain.example.internals;

/*
 * code so that the ExampleCaptainWorkflow can run, but not actually useful in learning about captain.
 */
public class MockIngestionService {
  private MockServiceStatusProvider mockServiceStatusProvider = new MockServiceStatusProvider();

  public boolean isFileDone(long fileId) {
    return mockServiceStatusProvider.isDone(fileId);
  }

}
