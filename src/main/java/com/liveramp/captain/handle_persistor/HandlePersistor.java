package com.liveramp.captain.handle_persistor;

public interface HandlePersistor<ServiceHandle> {
  void persist(Long jobId, ServiceHandle serviceHandle);
}
