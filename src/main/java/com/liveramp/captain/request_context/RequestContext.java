package com.liveramp.captain.request_context;

import java.util.Map;

public interface RequestContext {
  int getPriority();

  ExternalId getExternalId();

  Map<String, String> getOptions();
}
