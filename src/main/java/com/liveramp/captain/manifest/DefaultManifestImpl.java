package com.liveramp.captain.manifest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;

import com.liveramp.captain.request_context.NoOpRequestContextProducer;
import com.liveramp.captain.request_context.RequestContextProducerFactory;
import com.liveramp.captain.step.CaptainStep;
import com.liveramp.captain.waypoint.Waypoint;

public class DefaultManifestImpl implements Manifest {
  private List<Waypoint> steps;
  private Map<CaptainStep, Integer> indexByStep;
  private Map<Integer, CaptainStep> stepByIndex;
  private Map<CaptainStep, Waypoint> waypointByStep;
  private RequestContextProducerFactory requestContextProducerFactory;

  public DefaultManifestImpl(List<Waypoint> steps, RequestContextProducerFactory requestContextProducerFactory) {
    this.steps = steps;
    this.requestContextProducerFactory = requestContextProducerFactory;

    indexByStep = Maps.newHashMap();
    stepByIndex = Maps.newHashMap();
    int numSteps = steps.size();
    int currentIdx = 0;
    while (currentIdx < numSteps) {
      Waypoint waypoint = steps.get(currentIdx);
      indexByStep.put(waypoint.getStep(), currentIdx);
      stepByIndex.put(currentIdx, waypoint.getStep());
      currentIdx++;
    }
    waypointByStep = steps.stream().collect(Collectors.toMap(Waypoint::getStep, w -> w));
  }

  public DefaultManifestImpl(List<Waypoint> steps) {
    this(steps, NoOpRequestContextProducer.getProduction());
  }

  @Override
  public Optional<CaptainStep> getNextStep(CaptainStep step, long jobId) {
    Integer stepIndex = indexByStep.get(step);
    if (null == stepIndex) {
      throw new RuntimeException(String.format("could not find step: %s in manifest: %s", step, waypointByStep.keySet()));
    }
    Integer nextStepIndex = indexByStep.get(step) + 1;

    if (nextStepIndex < steps.size()) {
      CaptainStep nextStep = stepByIndex.get(nextStepIndex);
      if (!getWaypointForStep(nextStep).isOptional()) {
        return Optional.of(nextStep);
      } else {
        Waypoint waypoint = getWaypointForStep(nextStep);
        if (!waypoint.getOptionalStepPredicate().get().create().shouldSkipStep(jobId)) {
          return Optional.of(nextStep);
        } else {
          return getNextStep(nextStep, jobId);
        }
      }
    }

    return Optional.empty();
  }

  @Override
  public Waypoint getWaypointForStep(CaptainStep step) {
    return waypointByStep.get(step);
  }

  @Override
  public RequestContextProducerFactory getRequestContextProducerFactory() {
    return requestContextProducerFactory;
  }
}
