package com.liveramp.captain.handle_persistor;

public class NoOpHandlePersistor<ServiceHandle> implements HandlePersistor<ServiceHandle> {

  @Override
  public void persist(Long id, ServiceHandle o) {
    // no-op
  }

  /**
   * This method predates the use of method references and functional
   * interfaces.  Modern callers can use {@code NoOpHandlePersistor::new}
   * instead.
   */
  public static <ServiceHandle> HandlePersistorFactory<ServiceHandle> get() {
    return NoOpHandlePersistor::new;
  }
}
