package com.liveramp.captain.notifier;

import com.liveramp.daemon_lib.DaemonNotifier;
import java.util.Optional;

public class CaptainDaemonInternalNotifier implements DaemonNotifier {

  private final CaptainNotifier notifier;

  public CaptainDaemonInternalNotifier(final CaptainNotifier notifier) {
    this.notifier = notifier;
  }

  @Override
  public void notify(String subject, Optional<String> body, Optional<? extends Throwable> t) {
    if (t.isPresent()) {
      notifier.notify(subject, body.orElse(""), t.get(), CaptainNotifier.NotificationLevel.INFO);
    } else {
      notifier.notify(subject, body.orElse(""), CaptainNotifier.NotificationLevel.INFO);
    }
  }
}
