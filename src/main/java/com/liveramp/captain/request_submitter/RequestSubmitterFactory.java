package com.liveramp.captain.request_submitter;

public interface RequestSubmitterFactory<RequestHandle> {
  RequestSubmitter<RequestHandle> create();
}
