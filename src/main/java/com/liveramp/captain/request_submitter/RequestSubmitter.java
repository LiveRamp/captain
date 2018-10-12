package com.liveramp.captain.request_submitter;


import com.liveramp.captain.request_context.RequestContext;

public interface RequestSubmitter<RequestHandle> {
  RequestHandle submit(long jobId, RequestContext options) throws Exception;
}

