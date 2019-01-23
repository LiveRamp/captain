package com.liveramp.captain.request_context;

import java.util.Map;

public interface RequestContext {
  int getPriority();

  boolean hasPriority();

  void setPriority(int priority);

  ExternalId getExternalId();

  boolean hasExternalId();

  void setExternalId(ExternalId externalId);

  Map<String, String> getOptions();

  boolean hasOptions();

  void setOptions(Map<String, String> options);
}
