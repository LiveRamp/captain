package com.liveramp.captain.daemon.manifest;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import com.liveramp.captain.handle_persistor.HandlePersistor;
import com.liveramp.captain.manifest.DefaultManifestImpl;
import com.liveramp.captain.manifest.Manifest;
import com.liveramp.captain.optional_step_predicate.StepPredicates;
import com.liveramp.captain.request_submitter.RequestSubmitter;
import com.liveramp.captain.step.CaptainStep;
import com.liveramp.captain.waypoint.SyncWaypoint;
import com.liveramp.captain.waypoint.Waypoint;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class TestDefaultManifestImpl {

  private Waypoint nonOptionalWaypoint1;
  private Waypoint nonOptionalWaypoint2;
  private Waypoint optionalWaypoint1;
  private Waypoint optionalWaypoint2;
  private Waypoint waypointMissingStepPredicate;

  private RequestSubmitter<Long> requestSubmitter;
  private HandlePersistor<Long> handlePersistor;

  private final CaptainStep CAPTAIN_STEP1 = CaptainStep.fromString("CAPTAIN_STEP1");
  private final CaptainStep CAPTAIN_STEP2 = CaptainStep.fromString("CAPTAIN_STEP2");
  private final CaptainStep CAPTAIN_STEP3 = CaptainStep.fromString("CAPTAIN_STEP3");
  private final CaptainStep CAPTAIN_STEP4 = CaptainStep.fromString("CAPTAIN_STEP4");
  private final CaptainStep CAPTAIN_STEP5 = CaptainStep.fromString("CAPTAIN_STEP5");

  @Before
  public void setup() {
    requestSubmitter = (RequestSubmitter<Long>) mock(RequestSubmitter.class);
    handlePersistor = (HandlePersistor<Long>) mock(HandlePersistor.class);

    nonOptionalWaypoint1 =
        new SyncWaypoint(
            CAPTAIN_STEP1, requestSubmitter, handlePersistor, StepPredicates.alwaysFalse());
    nonOptionalWaypoint2 =
        new SyncWaypoint(
            CAPTAIN_STEP2, requestSubmitter, handlePersistor, StepPredicates.alwaysFalse());
    optionalWaypoint1 =
        new SyncWaypoint(
            CAPTAIN_STEP3, requestSubmitter, handlePersistor, StepPredicates.alwaysTrue());
    optionalWaypoint2 =
        new SyncWaypoint(
            CAPTAIN_STEP4, requestSubmitter, handlePersistor, StepPredicates.alwaysTrue());
    waypointMissingStepPredicate =
        new SyncWaypoint(
            CAPTAIN_STEP5, requestSubmitter, handlePersistor, StepPredicates.alwaysFalse());
  }

  @Test
  public void testGetNextStepNonoptional() {
    Manifest testManifest =
        new DefaultManifestImpl(Lists.newArrayList(nonOptionalWaypoint1, nonOptionalWaypoint2));
    checkExpectedNextStep(
        testManifest, nonOptionalWaypoint1.getStep(), nonOptionalWaypoint2.getStep());
  }

  @Test
  public void testGetNextStepOptional() {
    Manifest testManifest =
        new DefaultManifestImpl(
            Lists.newArrayList(
                nonOptionalWaypoint1, optionalWaypoint1, optionalWaypoint2, nonOptionalWaypoint2));
    checkExpectedNextStep(
        testManifest, nonOptionalWaypoint1.getStep(), nonOptionalWaypoint2.getStep());
  }

  @Test
  public void testFirstStepOptional() {
    Manifest testManifest =
        new DefaultManifestImpl(Lists.newArrayList(optionalWaypoint1, nonOptionalWaypoint1));
    checkExpectedNextStep(
        testManifest, CaptainStep.fromString("INITIALIZING"), nonOptionalWaypoint1.getStep());
  }

  @Test
  public void testGetNextStepOptionalLastStep() {
    Manifest testManifest =
        new DefaultManifestImpl(Lists.newArrayList(nonOptionalWaypoint1, optionalWaypoint1));
    checkExpectedNextStep(testManifest, nonOptionalWaypoint1.getStep(), null);
  }

  @Test
  public void testMissingOptionalPredicate() {
    Manifest testManifest =
        new DefaultManifestImpl(
            Lists.newArrayList(
                nonOptionalWaypoint1, optionalWaypoint1, waypointMissingStepPredicate));
    checkExpectedNextStep(
        testManifest, nonOptionalWaypoint1.getStep(), waypointMissingStepPredicate.getStep());
  }

  private void checkExpectedNextStep(
      Manifest manifest, CaptainStep currentStep, CaptainStep expectedStep) {
    Optional<CaptainStep> nextStep = manifest.getNextStep(currentStep, 0);
    if (expectedStep == null) {
      assertTrue(!nextStep.isPresent());
    } else {
      assertEquals(expectedStep.toString(), nextStep.get().toString());
    }
  }
}
