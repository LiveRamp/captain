package com.liveramp.captain.request_context;


public class NoOpRequestContextProducer implements RequestContextProducer {
  @Override
  public EmptyRequestContext get(long id) {
    return new EmptyRequestContext();
  }

  public static RequestContextProducerFactory getProduction() {
    return new NoOpRequestContextProducer.Factory();
  }

  private static class Factory implements RequestContextProducerFactory {
    @Override
    public NoOpRequestContextProducer create() {
      return new NoOpRequestContextProducer();
    }
  }
}
