package com.liveramp.captain.handle_persistor;

public interface HandlePersistor<ServiceHandle> {
  void persist(Long id, ServiceHandle serviceHandle);
}
