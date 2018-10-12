package com.liveramp.captain.request_submitter;

public class RequestSubmitterWrapperFactory<RequestHandle> implements RequestSubmitterFactory<RequestHandle> {

  private RequestSubmitter<RequestHandle> requestSubmitter;

  public RequestSubmitterWrapperFactory(RequestSubmitter<RequestHandle> requestSubmitter) {
    this.requestSubmitter = requestSubmitter;
  }

  @Override
  public RequestSubmitter<RequestHandle> create() {
    return requestSubmitter;
  }
}
