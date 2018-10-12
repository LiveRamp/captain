package com.liveramp.captain.daemon;

import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.liveramp.captain.app_type.CaptainAppType;
import com.liveramp.captain.handle_persistor.HandlePersistor;
import com.liveramp.captain.manifest.DefaultManifestImpl;
import com.liveramp.captain.manifest.Manifest;
import com.liveramp.captain.manifest.ManifestFactory;
import com.liveramp.captain.manifest_manager.ManifestManager;
import com.liveramp.captain.manifest_manager.MultiAppManifestManager;
import com.liveramp.captain.notifier.CaptainNotifier;
import com.liveramp.captain.request_context.RequestContext;
import com.liveramp.captain.request_submitter.RequestSubmitter;
import com.liveramp.captain.retry.DefaultFailedRequestPolicy;
import com.liveramp.captain.retry.FailedRequestPolicy;
import com.liveramp.captain.status.CaptainStatus;
import com.liveramp.captain.status_retriever.StatusRetriever;
import com.liveramp.captain.step.CaptainStep;
import com.liveramp.captain.util.TestManifestFactory;
import com.liveramp.captain.waypoint.AsyncWaypoint;
import com.liveramp.captain.waypoint.ControlFlowWaypoint;
import com.liveramp.captain.waypoint.SyncWaypoint;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class TestCaptainJobletWithRammingSpeed {

  private Long JOB_ID = 10L;
  private Long SERVICE_HANDLE = 22L;

  @Mock
  private CaptainNotifier notifier;

  @Mock
  private RequestSubmitter<Long> requestSubmitterStep1;
  @Mock
  private RequestSubmitter<Long> requestSubmitterStep2;
  @Mock
  private RequestSubmitter<Long> requestSubmitterStep4;
  @Mock
  private RequestSubmitter<Map<Long, Long>> requestSubmitterStep5;
  @Mock
  private RequestSubmitter<Long> requestSubmitterStep6;

  @Mock
  private HandlePersistor<Long> handlePersistor2;
  @Mock
  private HandlePersistor<Long> handlePersistor4;
  @Mock
  private HandlePersistor<Map<Long, Long>> handlePersistor5;

  @Mock
  private StatusRetriever statusRetrieverStep2;
  @Mock
  private StatusRetriever statusRetrieverStep3;
  @Mock
  private StatusRetriever statusRetrieverStep4;
  @Mock
  private StatusRetriever statusRetrieverStep5;

  private ManifestManager manifestManager;
  private RequestUpdater requestUpdater;

  private final CaptainStep INITIALIZING = CaptainStep.fromString("INITIALIZING");
  private final CaptainStep CAPTAIN_STEP1 = CaptainStep.fromString("CAPTAIN_STEP1");
  private final CaptainStep CAPTAIN_STEP2_A = CaptainStep.fromString("CAPTAIN_STEP2_A");
  private final CaptainStep CAPTAIN_STEP2_B = CaptainStep.fromString("CAPTAIN_STEP2_B");
  private final CaptainStep CAPTAIN_STEP3 = CaptainStep.fromString("CAPTAIN_STEP3");
  private final CaptainStep CAPTAIN_STEP4 = CaptainStep.fromString("CAPTAIN_STEP4");
  private final CaptainStep CAPTAIN_STEP5 = CaptainStep.fromString("CAPTAIN_STEP5");
  private final CaptainStep CAPTAIN_STEP6 = CaptainStep.fromString("CAPTAIN_STEP6");
  private final CaptainStep DONE = CaptainStep.fromString("DONE");


  private final CaptainAppType APP_TYPE_1 = CaptainAppType.fromString("APP_TYPE_1");
  private final CaptainAppType APP_TYPE_2 = CaptainAppType.fromString("APP_TYPE_2");

  private FailedRequestPolicy FAILED_REQUEST_POLICY = new DefaultFailedRequestPolicy();

  @Before
  public void setup() {
    Manifest testManifest = new DefaultManifestImpl(Lists.newArrayList(
        new SyncWaypoint(CAPTAIN_STEP1, requestSubmitterStep1, null),
        new SyncWaypoint(CAPTAIN_STEP2_A, requestSubmitterStep2, handlePersistor2),
        new AsyncWaypoint(CAPTAIN_STEP2_B, requestSubmitterStep2, handlePersistor2, statusRetrieverStep2),
        new ControlFlowWaypoint(CAPTAIN_STEP3, statusRetrieverStep3),
        new AsyncWaypoint(CAPTAIN_STEP4, requestSubmitterStep4, handlePersistor4, statusRetrieverStep4),
        new AsyncWaypoint(CAPTAIN_STEP5, requestSubmitterStep5, handlePersistor5, statusRetrieverStep5),
        new SyncWaypoint(CAPTAIN_STEP6, requestSubmitterStep6, null)
    ));

    Manifest testManifest2 = new DefaultManifestImpl(Lists.newArrayList(
        new SyncWaypoint<>(CAPTAIN_STEP1, requestSubmitterStep1),
        new AsyncWaypoint<>(CAPTAIN_STEP5, requestSubmitterStep5, handlePersistor5, statusRetrieverStep5)
    ));

    Map<CaptainAppType, ManifestFactory> manifestFactoryMap = Maps.newHashMap();
    manifestFactoryMap
        .put(CaptainAppType.fromString("APP_TYPE_1"), new TestManifestFactory(testManifest));
    manifestFactoryMap
        .put(CaptainAppType.fromString("APP_TYPE_2"), new TestManifestFactory(testManifest2));
    manifestManager = MultiAppManifestManager.ofManifestFactories(manifestFactoryMap);

    requestUpdater = spy(RequestUpdater.class);
  }

  @Test
  public void testGoToNextStep() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(
        JOB_ID, CaptainStatus.COMPLETED, CAPTAIN_STEP1,
        APP_TYPE_1);

    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, false, true, FAILED_REQUEST_POLICY)
        .run();

    InOrder inOrder = inOrder(requestUpdater);
    inOrder.verify(requestUpdater, atLeastOnce())
        .setStepAndStatus(config.getId(), CAPTAIN_STEP1, CaptainStatus.COMPLETED, CAPTAIN_STEP2_A, CaptainStatus.READY);
    inOrder.verify(requestUpdater, atLeastOnce())
        .setStatus(config.getId(), CAPTAIN_STEP2_A, CaptainStatus.READY, CaptainStatus.COMPLETED);
    inOrder.verify(requestUpdater, atLeastOnce())
        .setStepAndStatus(config.getId(), CAPTAIN_STEP2_A, CaptainStatus.COMPLETED, CAPTAIN_STEP2_B, CaptainStatus.READY);
    inOrder.verify(requestUpdater, atLeastOnce())
        .setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.READY, CaptainStatus.IN_PROGRESS);
  }

  @Test
  public void testGoToNextStepFromAsync() throws Exception {
    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.COMPLETED);
    when(statusRetrieverStep3.getStatus(JOB_ID)).thenReturn(CaptainStatus.COMPLETED);

    CaptainRequestConfig config = new SimpleCaptainConfig(
        JOB_ID, CaptainStatus.IN_PROGRESS, CAPTAIN_STEP2_B,
        APP_TYPE_1);

    new CaptainJoblet(
        config, notifier, requestUpdater, manifestManager, false, true, FAILED_REQUEST_POLICY)
        .run();

    InOrder inOrder = inOrder(requestUpdater);
    inOrder.verify(requestUpdater, atLeastOnce())
        .setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.IN_PROGRESS, CaptainStatus.COMPLETED);
    inOrder.verify(requestUpdater, atLeastOnce())
        .setStepAndStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.COMPLETED, CAPTAIN_STEP3, CaptainStatus.READY);
    inOrder.verify(requestUpdater, atLeastOnce())
        .setStatus(config.getId(), CAPTAIN_STEP3, CaptainStatus.READY, CaptainStatus.IN_PROGRESS);
    inOrder.verify(requestUpdater, atLeastOnce())
        .setStatus(config.getId(), CAPTAIN_STEP3, CaptainStatus.IN_PROGRESS, CaptainStatus.COMPLETED);
    inOrder.verify(requestUpdater, atLeastOnce())
        .setStepAndStatus(config.getId(), CAPTAIN_STEP3, CaptainStatus.COMPLETED, CAPTAIN_STEP4, CaptainStatus.READY);
    inOrder.verify(requestUpdater, atLeastOnce())
        .setStatus(config.getId(), CAPTAIN_STEP4, CaptainStatus.READY, CaptainStatus.IN_PROGRESS);
  }

  @Test
  public void testGoToNextStepInitializing() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.COMPLETED, INITIALIZING, APP_TYPE_1);

    new CaptainJoblet(
        config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(1))
        .setStepAndStatus(config.getId(), INITIALIZING, CaptainStatus.COMPLETED, CAPTAIN_STEP1, CaptainStatus.READY);
  }

  @Test
  public void testGoToNextStepLastStep() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.COMPLETED, CAPTAIN_STEP6, APP_TYPE_1);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(1))
        .setStepAndStatus(config.getId(), CAPTAIN_STEP6, CaptainStatus.COMPLETED, DONE, CaptainStatus.COMPLETED);
  }

  @Test
  public void testSubmitRequestAsync() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.READY, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(requestSubmitterStep2.submit(eq(JOB_ID), any(RequestContext.class))).thenReturn(SERVICE_HANDLE);

    new CaptainJoblet(
        config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    verify(requestSubmitterStep2, times(1)).submit(eq(JOB_ID), any(RequestContext.class));
    verify(handlePersistor2, times(1)).persist(JOB_ID, SERVICE_HANDLE);
    verify(requestUpdater, times(1))
        .setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.READY, CaptainStatus.PENDING);
  }

  @Test
  public void testSubmitRequestAsyncSupportsPendingFalse() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.READY, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(requestSubmitterStep2.submit(eq(JOB_ID), any(RequestContext.class))).thenReturn(SERVICE_HANDLE);

    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, false, true, FAILED_REQUEST_POLICY)
        .run();

    verify(requestSubmitterStep2, times(1)).submit(eq(JOB_ID), any(RequestContext.class));
    verify(handlePersistor2, times(1)).persist(JOB_ID, SERVICE_HANDLE);
    verify(requestUpdater, times(1))
        .setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.READY, CaptainStatus.IN_PROGRESS);
  }

  @Test
  public void testSubmitRequestSync() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.READY, CAPTAIN_STEP1, APP_TYPE_1);

    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    verify(requestSubmitterStep1, times(1)).submit(eq(JOB_ID), any(RequestContext.class));
    verify(requestUpdater, times(1))
        .setStatus(config.getId(), CAPTAIN_STEP1, CaptainStatus.READY, CaptainStatus.COMPLETED);
  }

  @Test
  public void testSubmitRequestSyncWithHandlerPersistor() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.READY, CAPTAIN_STEP2_A, APP_TYPE_1);

    when(requestSubmitterStep2.submit(eq(JOB_ID), any(RequestContext.class))).thenReturn(SERVICE_HANDLE);

    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, false, true, FAILED_REQUEST_POLICY)
        .run();

    verify(requestSubmitterStep2, times(2)).submit(eq(JOB_ID), any(RequestContext.class));
    verify(handlePersistor2, times(2)).persist(JOB_ID, SERVICE_HANDLE);

    InOrder inOrder = inOrder(requestUpdater);
    inOrder.verify(requestUpdater, atLeastOnce())
        .setStatus(config.getId(), CAPTAIN_STEP2_A, CaptainStatus.READY, CaptainStatus.COMPLETED);
    inOrder.verify(requestUpdater, atLeastOnce())
        .setStepAndStatus(config.getId(), CAPTAIN_STEP2_A, CaptainStatus.COMPLETED, CAPTAIN_STEP2_B, CaptainStatus.READY);
    inOrder.verify(requestUpdater, atLeastOnce())
        .setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.READY, CaptainStatus.IN_PROGRESS);
  }

  @Test
  public void testSubmitRequestFlowControl1() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.READY, CAPTAIN_STEP3, APP_TYPE_1);

    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(1)).setStatus(config.getId(), CAPTAIN_STEP3, CaptainStatus.READY, CaptainStatus.PENDING);
  }

  @Test
  public void testCheckRequestStartedInProgress() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.PENDING, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.IN_PROGRESS);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.COMPLETED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.FAILED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(3))
        .setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.PENDING, CaptainStatus.IN_PROGRESS);
  }

  @Test
  public void testCheckRequestStartedQuarantine() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.PENDING, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.QUARANTINED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.QUARANTINED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(2))
        .setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.PENDING, CaptainStatus.QUARANTINED);
  }

  @Test
  public void testCheckRequestStartedUnknownStatus() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.PENDING, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.PENDING);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    // i.e. do nothing.
    verify(requestUpdater, times(0))
        .setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.PENDING, CaptainStatus.IN_PROGRESS);
    verify(requestUpdater, times(0))
        .setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.PENDING, CaptainStatus.QUARANTINED);
  }

  @Test
  public void testRequestCompleteCompleted() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.IN_PROGRESS, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.COMPLETED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(1))
        .setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.IN_PROGRESS, CaptainStatus.COMPLETED);
  }

  @Test
  public void testRequestCompleteQuarantined() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.IN_PROGRESS, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.QUARANTINED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.QUARANTINED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(2))
        .setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.IN_PROGRESS, CaptainStatus.QUARANTINED);
  }

  @Test
  public void testRequestCompleteFailed() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.IN_PROGRESS, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.FAILED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    // i.e. do nothing
    verify(requestUpdater, times(0))
        .setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.IN_PROGRESS, CaptainStatus.COMPLETED);
    verify(requestUpdater, times(0))
        .setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.IN_PROGRESS, CaptainStatus.QUARANTINED);
  }

  @Test
  public void testRequestCompleteCancelled() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.IN_PROGRESS, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.CANCELLED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(1)).cancel(JOB_ID);
  }

  @Test
  public void testRequestCompleteCompletedFlowControl() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.IN_PROGRESS, CAPTAIN_STEP3, APP_TYPE_1);

    when(statusRetrieverStep3.getStatus(JOB_ID)).thenReturn(CaptainStatus.COMPLETED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(1))
        .setStatus(config.getId(), CAPTAIN_STEP3, CaptainStatus.IN_PROGRESS, CaptainStatus.COMPLETED);
  }


  @Test
  public void testUsesCorrectManifestForAppType() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.COMPLETED, CAPTAIN_STEP1, APP_TYPE_2);

    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, true, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(1))
        .setStepAndStatus(config.getId(), CAPTAIN_STEP1, CaptainStatus.COMPLETED, CAPTAIN_STEP5, CaptainStatus.READY);
  }
}
