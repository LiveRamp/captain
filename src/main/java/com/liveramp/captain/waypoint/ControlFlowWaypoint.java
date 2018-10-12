package com.liveramp.captain.waypoint;

import com.liveramp.captain.optional_step_predicate.StepPredicateFactory;
import com.liveramp.captain.status_retriever.StatusRetriever;
import com.liveramp.captain.status_retriever.StatusRetrieverFactory;
import com.liveramp.captain.status_retriever.StatusRetrieverWrapperFactory;
import com.liveramp.captain.step.CaptainStep;

import java.util.Optional;

public class ControlFlowWaypoint implements Waypoint {
  private final CaptainStep step;
  private final WaypointSubmitter waypointSubmitter;
  private final StatusRetrieverFactory statusRetriever;
  private final Optional<StepPredicateFactory> optionalStepPredicate;

  public ControlFlowWaypoint(CaptainStep step, StatusRetrieverFactory statusRetriever, Optional<StepPredicateFactory> optionalStepPredicate) {
    this.step = step;
    this.statusRetriever = statusRetriever;
    this.optionalStepPredicate = optionalStepPredicate;
    this.waypointSubmitter = NoOpWaypointSubmitter.get();
  }

  public ControlFlowWaypoint(CaptainStep step, StatusRetriever statusRetriever) {
    this(step, new StatusRetrieverWrapperFactory(statusRetriever), Optional.empty());
  }

  @Override
  public CaptainStep getStep() {
    return step;
  }

  @Override
  public WaypointSubmitter getSubmitter() {
    return waypointSubmitter;
  }

  @Override
  public StatusRetrieverFactory getStatusRetrieverFactory() {
    return statusRetriever;
  }

  @Override
  public WaypointType getType() {
    return WaypointType.FLOW_CONTROL;
  }

  @Override
  public boolean isOptional() {
    return optionalStepPredicate.isPresent();
  }

  @Override
  public Optional<StepPredicateFactory> getOptionalStepPredicate() {
    return optionalStepPredicate;
  }
}
