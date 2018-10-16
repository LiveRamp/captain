package com.liveramp.captain.example.internals;

public class MockIngestionService {
  private MockServiceStatusProvider mockServiceStatusProvider = new MockServiceStatusProvider();

  public boolean isFileDone(long fileId) {
    return mockServiceStatusProvider.isDone(fileId);
  }

}
