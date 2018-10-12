package com.liveramp.captain.handle_persistor;

public class NoOpHandlePersistor<ServiceHandle> implements HandlePersistor<ServiceHandle> {

  private NoOpHandlePersistor() {

  }

  @Override
  public void persist(Long jobId, ServiceHandle o) {

  }

  public static HandlePersistorFactory get() {
    return new NoOpHandlePersistor.Factory();
  }

  private static class Factory implements HandlePersistorFactory {

    @Override
    public HandlePersistor create() {
      return new NoOpHandlePersistor();
    }
  }
}
