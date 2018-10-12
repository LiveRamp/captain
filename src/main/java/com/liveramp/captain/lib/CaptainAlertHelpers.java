package com.liveramp.captain.lib;

import java.net.InetAddress;
import org.apache.commons.io.IOUtils;

public class CaptainAlertHelpers {
  public static String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      try {
        return IOUtils.toString(Runtime.getRuntime().exec("hostname").getInputStream());
      } catch (Exception e1) {
        return "Unable to determine host";
      }
    }
  }
}
