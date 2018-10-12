package com.liveramp.captain.waypoint;

import com.liveramp.captain.handle_persistor.HandlePersistor;
import com.liveramp.captain.handle_persistor.HandlePersistorFactory;
import com.liveramp.captain.handle_persistor.HandlePersistorWrapperFactory;
import com.liveramp.captain.optional_step_predicate.StepPredicate;
import com.liveramp.captain.optional_step_predicate.StepPredicateFactory;
import com.liveramp.captain.optional_step_predicate.StepPredicateWrapperFactory;
import com.liveramp.captain.request_submitter.RequestSubmitter;
import com.liveramp.captain.request_submitter.RequestSubmitterFactory;
import com.liveramp.captain.request_submitter.RequestSubmitterWrapperFactory;
import com.liveramp.captain.status_retriever.StatusRetrieverFactory;
import com.liveramp.captain.step.CaptainStep;
import java.util.Optional;

public class SyncWaypoint<ServiceHandle> implements Waypoint {
  private final CaptainStep step;
  private final WaypointSubmitter waypointSubmitter;
  private final Optional<StepPredicateFactory> optionalStepPredicate;

  public SyncWaypoint(
      CaptainStep step,
      RequestSubmitterFactory<ServiceHandle> requestSubmitter,
      HandlePersistorFactory<ServiceHandle> handlePersistor,
      Optional<StepPredicateFactory> optionalStepPredicateFactory) {
    this.step = step;
    this.waypointSubmitter = new WaypointSubmitterImpl<>(step, requestSubmitter, handlePersistor);
    this.optionalStepPredicate = optionalStepPredicateFactory;
  }

  public SyncWaypoint(CaptainStep step, RequestSubmitter<ServiceHandle> requestSubmitter) {
    this(step, new RequestSubmitterWrapperFactory<>(requestSubmitter), null, Optional.empty());
  }

  public SyncWaypoint(
      CaptainStep step,
      RequestSubmitter<ServiceHandle> requestSubmitter,
      HandlePersistor<ServiceHandle> handlePersistor) {
    this(
        step,
        new RequestSubmitterWrapperFactory<>(requestSubmitter),
        new HandlePersistorWrapperFactory<>(handlePersistor),
        Optional.empty());
  }

  public SyncWaypoint(
      CaptainStep step,
      RequestSubmitter<ServiceHandle> requestSubmitter,
      HandlePersistor<ServiceHandle> handlePersistor,
      StepPredicate stepPredicate) {
    this(
        step,
        new RequestSubmitterWrapperFactory<>(requestSubmitter),
        new HandlePersistorWrapperFactory<>(handlePersistor),
        Optional.of(new StepPredicateWrapperFactory(stepPredicate)));
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
    return null;
  }

  @Override
  public WaypointType getType() {
    return WaypointType.SYNC;
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
