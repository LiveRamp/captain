package com.liveramp.captain.manifest_manager;

import com.google.common.collect.Sets;
import com.liveramp.captain.app_type.CaptainAppType;
import com.liveramp.captain.manifest.Manifest;
import com.liveramp.captain.manifest.ManifestFactory;
import com.liveramp.captain.manifest.WrapperManifestFactoryImpl;
import java.util.Set;

public class SingleAppManifestManager implements ManifestManager {
  private final String SINGLE_APP_TYPE_PLACEHOLDER_KEY = "ALL";
  private ManifestFactory manifestFactory;

  SingleAppManifestManager(Manifest manifest) {
    this(new WrapperManifestFactoryImpl(manifest));
  }

  SingleAppManifestManager(ManifestFactory manifestFactory) {
    this.manifestFactory = manifestFactory;
  }

  @Override
  public ManifestFactory getManifestFactory(CaptainAppType appType) {
    return manifestFactory;
  }

  @Override
  public Set<CaptainAppType> getAvailableCaptainAppTypes() {
    return Sets.newHashSet(CaptainAppType.fromString(SINGLE_APP_TYPE_PLACEHOLDER_KEY));
  }
}
