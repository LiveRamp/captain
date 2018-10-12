package com.liveramp.captain.daemon;

import com.liveramp.captain.manifest_manager.ManifestManager;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CaptainBuilder extends BaseCaptainBuilder<CaptainBuilder> {
  public CaptainBuilder(String identifier, CaptainConfigProducer configProducer, ManifestManager manifestManager, RequestUpdater requestUpdater) {
    super(identifier, configProducer, manifestManager, requestUpdater);
  }

  public static CaptainBuilder of(String identifier, CaptainConfigProducer configProducer, ManifestManager multiAppManifestManager, RequestUpdater requestUpdater) {
    return new CaptainBuilder(identifier, configProducer, multiAppManifestManager, requestUpdater);
  }
}
