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
import com.liveramp.captain.status_retriever.StatusRetriever;
import com.liveramp.captain.status_retriever.StatusRetrieverFactory;
import com.liveramp.captain.status_retriever.StatusRetrieverWrapperFactory;
import com.liveramp.captain.step.CaptainStep;

import java.util.Optional;

public class AsyncWaypoint<ServiceHandle> implements Waypoint {
  private final CaptainStep step;
  private final WaypointSubmitter waypointSubmitter;
  private final StatusRetrieverFactory statusRetriever;
  private final Optional<StepPredicateFactory> optionalStepPredicate;

  public AsyncWaypoint(
      CaptainStep step,
      RequestSubmitterFactory<ServiceHandle> requestSubmitter,
      HandlePersistorFactory<ServiceHandle> handlePersistor,
      StatusRetrieverFactory statusRetriever,
      Optional<StepPredicateFactory> optionalStepPredicate) {
    this.step = step;
    this.waypointSubmitter = new WaypointSubmitterImpl<>(step, requestSubmitter, handlePersistor);
    this.statusRetriever = statusRetriever;
    this.optionalStepPredicate = optionalStepPredicate;
  }

  public AsyncWaypoint(CaptainStep step, RequestSubmitter<ServiceHandle> requestSubmitter, HandlePersistor<ServiceHandle> handlePersistor, StatusRetriever statusRetriever) {
    this(step, new RequestSubmitterWrapperFactory<>(requestSubmitter), new HandlePersistorWrapperFactory<>(handlePersistor), new StatusRetrieverWrapperFactory(statusRetriever), Optional.empty());
  }

  public AsyncWaypoint(CaptainStep step, RequestSubmitter<ServiceHandle> requestSubmitter, HandlePersistor<ServiceHandle> handlePersistor, StatusRetriever statusRetriever, StepPredicate stepPredicate) {
    this(step, new RequestSubmitterWrapperFactory<>(requestSubmitter), new HandlePersistorWrapperFactory<>(handlePersistor), new StatusRetrieverWrapperFactory(statusRetriever), Optional.of(new StepPredicateWrapperFactory(stepPredicate)));
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
    return WaypointType.ASYNC;
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
