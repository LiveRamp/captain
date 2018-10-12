package com.liveramp.captain.manifest;

import com.liveramp.captain.request_context.RequestContextProducerFactory;
import com.liveramp.captain.step.CaptainStep;
import com.liveramp.captain.waypoint.Waypoint;
import java.util.Optional;

public interface Manifest {
  Optional<CaptainStep> getNextStep(CaptainStep step, long jobId);

  Waypoint getWaypointForStep(CaptainStep step);

  RequestContextProducerFactory getRequestContextProducerFactory();
}
