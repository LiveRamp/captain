package com.liveramp.captain.request_submitter;


import com.liveramp.captain.request_context.RequestContext;


public class NoOpSubmitter implements RequestSubmitter<Long> {
  @Override
  public Long submit(long id, RequestContext options) {
    return null;
  }

  public static RequestSubmitterFactory<Long> getProduction() {
    return new NoOpSubmitter.Factory();
  }

  private static class Factory implements RequestSubmitterFactory<Long> {

    @Override
    public NoOpSubmitter create() {
      return new NoOpSubmitter();
    }
  }
}
