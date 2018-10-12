package com.liveramp.captain.manifest_manager;

import java.util.Set;

import com.liveramp.captain.app_type.CaptainAppType;
import com.liveramp.captain.manifest.ManifestFactory;

public interface ManifestManager {
  ManifestFactory getManifestFactory(CaptainAppType appType);

  Set<CaptainAppType> getAvailableCaptainAppTypes();
}
