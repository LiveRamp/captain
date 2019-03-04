package com.liveramp.captain.notifier;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultCaptainLoggingNotifier implements CaptainNotifier {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultCaptainLoggingNotifier.class);

  private void notifyInternal(String subject, String body, NotificationLevel notificationLevel) {
    String msg = String.format("[%s]: %s %n%n %s)", notificationLevel.toString(), subject, body);
    switch (notificationLevel) {
      case DEBUG:
        LOG.debug(msg);
        break;
      case INFO:
        LOG.info(msg);
        break;
      case ERROR:
        LOG.error(msg);
        break;
      default:
        throw new RuntimeException("Unknown notification level: " + notificationLevel);
    }
  }

  @Override
  public void notify(
      String header, String message, NotificationLevel notificationLevel) {
    notifyInternal(header, message, notificationLevel);
  }

  @Override
  public void notify(
      String header, Throwable t, NotificationLevel notificationLevel) {
    notify(header, "", t, notificationLevel);
  }

  @Override
  public void notify(
      String header, String message, Throwable t,
      NotificationLevel notificationLevel) {
    String body = String.format("%s %n %s", message, ExceptionUtils.getFullStackTrace(t));
    notifyInternal(header, body, notificationLevel);
  }
}
