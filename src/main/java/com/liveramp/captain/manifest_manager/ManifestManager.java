package com.liveramp.captain.manifest_manager;

import com.liveramp.captain.app_type.CaptainAppType;
import com.liveramp.captain.manifest.ManifestFactory;
import java.util.Set;

public interface ManifestManager {
  ManifestFactory getManifestFactory(CaptainAppType appType);

  Set<CaptainAppType> getAvailableCaptainAppTypes();
}
