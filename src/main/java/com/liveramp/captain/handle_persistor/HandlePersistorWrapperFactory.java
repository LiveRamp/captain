package com.liveramp.captain.handle_persistor;

public class HandlePersistorWrapperFactory<ServiceHandle> implements HandlePersistorFactory<ServiceHandle> {

  private HandlePersistor<ServiceHandle> handlePersistor;

  public HandlePersistorWrapperFactory(HandlePersistor<ServiceHandle> handlePersistor) {
    this.handlePersistor = handlePersistor;
  }

  @Override
  public HandlePersistor<ServiceHandle> create() {
    return handlePersistor;
  }
}
