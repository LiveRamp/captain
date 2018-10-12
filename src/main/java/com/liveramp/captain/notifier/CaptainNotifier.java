package com.liveramp.captain.notifier;

public interface CaptainNotifier {

  void notify(String header, String message, NotificationLevel notificationLevel);

  void notify(String header, Throwable t, NotificationLevel notificationLevel);

  void notify(String header, String message, Throwable t, NotificationLevel notificationLevel);

  enum NotificationLevel {
    DEBUG,
    INFO,
    ERROR;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }
}
