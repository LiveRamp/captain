package com.liveramp.captain.request_context;

import java.util.Map;

public class EmptyRequestContext implements RequestContext {
  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public ExternalId getExternalId() {
    return null;
  }

  @Override
  public Map<String, String> getOptions() {
    return null;
  }
}
