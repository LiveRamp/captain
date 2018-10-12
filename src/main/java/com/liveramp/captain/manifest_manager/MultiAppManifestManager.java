package com.liveramp.captain.manifest_manager;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.liveramp.captain.app_type.CaptainAppType;
import com.liveramp.captain.manifest.Manifest;
import com.liveramp.captain.manifest.ManifestFactory;
import com.liveramp.captain.manifest.WrapperManifestFactoryImpl;

public class MultiAppManifestManager implements ManifestManager {
  private Map<CaptainAppType, ManifestFactory> manifestByAppType;

  private MultiAppManifestManager(Map<CaptainAppType, ManifestFactory> manifestByAppType) {
    this.manifestByAppType = manifestByAppType;
  }

  public static MultiAppManifestManager ofManifests(Map<CaptainAppType, Manifest> manifestByAppType) {
    return new MultiAppManifestManager(manifestMapToManifestFactoryMap(manifestByAppType));
  }

  public static MultiAppManifestManager ofManifestFactories(Map<CaptainAppType, ManifestFactory> manifestFactoryByAppType) {
    return new MultiAppManifestManager(manifestFactoryByAppType);
  }

  public Map<CaptainAppType, ManifestFactory> create() {
    return manifestByAppType;
  }

  @Override
  public ManifestFactory getManifestFactory(CaptainAppType appType) {
    if (!manifestByAppType.containsKey(appType)) {
      throw new RuntimeException(String.format("no manifest with app type %s in the registered manifests: %s.", appType, manifestByAppType));
    }
    return manifestByAppType.get(appType);
  }

  @Override
  public Set<CaptainAppType> getAvailableCaptainAppTypes() {
    return manifestByAppType.keySet();
  }

  private static Map<CaptainAppType, ManifestFactory> manifestMapToManifestFactoryMap(Map<CaptainAppType, Manifest> manifestByAppType) {
    return manifestByAppType.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> new WrapperManifestFactoryImpl(entry.getValue())));
  }
}
