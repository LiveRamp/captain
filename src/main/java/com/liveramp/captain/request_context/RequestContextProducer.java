package com.liveramp.captain.request_context;

public interface RequestContextProducer {
  RequestContext get(long jobId);
}
