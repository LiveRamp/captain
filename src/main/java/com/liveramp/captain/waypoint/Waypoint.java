package com.liveramp.captain.waypoint;

import java.util.Optional;

import com.liveramp.captain.optional_step_predicate.StepPredicateFactory;
import com.liveramp.captain.status_retriever.StatusRetrieverFactory;
import com.liveramp.captain.step.CaptainStep;

public interface Waypoint {
  CaptainStep getStep();

  WaypointSubmitter getSubmitter();

  StatusRetrieverFactory getStatusRetrieverFactory();

  WaypointType getType();

  boolean isOptional();

  Optional<StepPredicateFactory> getOptionalStepPredicate();
}
