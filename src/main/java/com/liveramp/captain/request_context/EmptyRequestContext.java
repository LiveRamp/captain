package com.liveramp.captain.request_context;

import java.util.Map;

public class EmptyRequestContext implements RequestContext {
  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public void setPriority(int priority) {
    throw new RuntimeException("Cannot set priority on an EmptyRequestContext");
  }

  @Override public boolean hasPriority() {
    return false;
  }

  @Override
  public ExternalId getExternalId() {
    return null;
  }

  @Override
  public void setExternalId(ExternalId externalId) {
    throw new RuntimeException("Cannot set ExternalId on an EmptyRequestContext");
  }

  @Override public boolean hasExternalId() {
    return false;
  }

  @Override
  public Map<String, String> getOptions() {
    return null;
  }

  @Override
  public boolean hasOptions() {
    return false;
  }

  @Override
  public void setOptions(Map<String, String> options) {
    throw new RuntimeException("Cannot set options on an EmptyRequestContext");
  }
}
