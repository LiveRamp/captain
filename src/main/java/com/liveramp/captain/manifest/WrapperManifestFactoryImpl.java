package com.liveramp.captain.manifest;

public class WrapperManifestFactoryImpl implements ManifestFactory{
  private final Manifest manifest;

  public WrapperManifestFactoryImpl(Manifest manifest) {
    this.manifest = manifest;
  }

  @Override
  public Manifest create() {
    return manifest;
  }
}
