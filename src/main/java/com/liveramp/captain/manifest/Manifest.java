package com.liveramp.captain.manifest;

import java.util.Optional;

import com.liveramp.captain.request_context.RequestContextProducerFactory;
import com.liveramp.captain.step.CaptainStep;
import com.liveramp.captain.waypoint.Waypoint;

public interface Manifest {
  Optional<CaptainStep> getNextStep(CaptainStep step, long jobId);

  Waypoint getWaypointForStep(CaptainStep step);

  RequestContextProducerFactory getRequestContextProducerFactory();
}
