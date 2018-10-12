package com.liveramp.captain.daemon;

import com.liveramp.daemon_lib.JobletConfigProducer;
import com.liveramp.daemon_lib.utils.DaemonException;

public interface CaptainConfigProducer extends JobletConfigProducer<CaptainRequestConfig> {
  CaptainRequestConfig getNextConfig() throws DaemonException;
}

