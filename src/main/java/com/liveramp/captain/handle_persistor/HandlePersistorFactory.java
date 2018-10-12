package com.liveramp.captain.handle_persistor;

public interface HandlePersistorFactory<ServiceHandle> {
  HandlePersistor<ServiceHandle> create();
}
