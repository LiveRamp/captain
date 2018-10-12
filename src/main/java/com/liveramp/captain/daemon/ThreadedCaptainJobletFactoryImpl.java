package com.liveramp.captain.daemon;

import com.liveramp.captain.manifest_manager.ManifestManager;
import com.liveramp.captain.notifier.CaptainNotifier;
import com.liveramp.daemon_lib.Joblet;
import com.liveramp.daemon_lib.utils.DaemonException;

public class ThreadedCaptainJobletFactoryImpl implements CaptainJobletFactory {
  private RequestUpdater requestUpdater;
  private ManifestManager manifestManager;
  private CaptainNotifier notifier;
  private boolean supportsPending;
  private boolean rammingSpeed;

  ThreadedCaptainJobletFactoryImpl(
      RequestUpdater requestUpdater,
      ManifestManager manifestManager,
      CaptainNotifier notifier,
      boolean supportsPending,
      boolean rammingSpeed) {
    this.requestUpdater = requestUpdater;
    this.manifestManager = manifestManager;
    this.notifier = notifier;
    this.supportsPending = supportsPending;
    this.rammingSpeed = rammingSpeed;
  }

  @Override
  public Joblet create(CaptainRequestConfig config) throws DaemonException {
    return CaptainJoblet.of(
        config,
        notifier,
        requestUpdater,
        manifestManager,
        supportsPending,
        rammingSpeed);
  }
}
