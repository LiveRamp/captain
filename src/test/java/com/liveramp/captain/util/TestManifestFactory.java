package com.liveramp.captain.util;

import com.liveramp.captain.manifest.Manifest;
import com.liveramp.captain.manifest.ManifestFactory;

public class TestManifestFactory implements ManifestFactory {
  private Manifest manifest;

  public TestManifestFactory(Manifest manifest) {
    this.manifest = manifest;
  }

  @Override
  public Manifest create() {
    return manifest;
  }
}
