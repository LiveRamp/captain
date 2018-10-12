package com.liveramp.captain.status_retriever;


import com.liveramp.captain.status.CaptainStatus;

public interface StatusRetriever {
  CaptainStatus getStatus(long jobId);
}
