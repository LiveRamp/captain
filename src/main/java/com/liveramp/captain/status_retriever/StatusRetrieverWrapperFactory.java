package com.liveramp.captain.status_retriever;

public class StatusRetrieverWrapperFactory implements StatusRetrieverFactory {

  private StatusRetriever statusRetriever;

  public StatusRetrieverWrapperFactory(StatusRetriever statusRetriever) {
    this.statusRetriever = statusRetriever;
  }

  @Override
  public StatusRetriever create() {
    return statusRetriever;
  }
}
