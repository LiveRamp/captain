package com.liveramp.captain.daemon;

import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

import com.liveramp.captain.app_type.CaptainAppType;
import com.liveramp.captain.exception.CaptainCouldNotFindNextStep;
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
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class TestCaptainJoblet {
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

  private final CaptainStep CAPTAIN_STEP1 = CaptainStep.fromString("CAPTAIN_STEP1");
  private final CaptainStep CAPTAIN_STEP2_A = CaptainStep.fromString("CAPTAIN_STEP2_A");
  private final CaptainStep CAPTAIN_STEP2_B = CaptainStep.fromString("CAPTAIN_STEP2_B");
  private final CaptainStep CAPTAIN_STEP3 = CaptainStep.fromString("CAPTAIN_STEP3");
  private final CaptainStep CAPTAIN_STEP4 = CaptainStep.fromString("CAPTAIN_STEP4");
  private final CaptainStep CAPTAIN_STEP5 = CaptainStep.fromString("CAPTAIN_STEP5");
  private final CaptainStep CAPTAIN_STEP6 = CaptainStep.fromString("CAPTAIN_STEP6");


  private final CaptainAppType APP_TYPE_1 = CaptainAppType.fromString("APP_TYPE_1");
  private final CaptainAppType APP_TYPE_2 = CaptainAppType.fromString("APP_TYPE_2");

  private final FailedRequestPolicy FAILED_REQUEST_POLICY = new DefaultFailedRequestPolicy();

  @Mock
  private FailedRequestPolicy failedRequestPolicyMock;

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
    manifestFactoryMap.put(APP_TYPE_1, new TestManifestFactory(testManifest));
    manifestFactoryMap.put(APP_TYPE_2, new TestManifestFactory(testManifest2));
    manifestManager = MultiAppManifestManager.ofManifestFactories(manifestFactoryMap);

    requestUpdater = mock(RequestUpdater.class);
  }

  @Test
  public void testGoToNextStep() throws Exception {

    final CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.COMPLETED, CAPTAIN_STEP1, APP_TYPE_1);

    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(1)).setStepAndStatus(config.getId(), CAPTAIN_STEP1, CaptainStatus.COMPLETED, CAPTAIN_STEP2_A, CaptainStatus.READY);
  }

  @Test
  public void testGoToNextStepLastStep() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.COMPLETED, CAPTAIN_STEP6, APP_TYPE_1);

    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(1)).fail(config.getId());
  }

  @Test
  public void testSubmitRequestAsync() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.READY, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(requestSubmitterStep2.submit(eq(JOB_ID), any(RequestContext.class))).thenReturn(SERVICE_HANDLE);

    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    verify(requestSubmitterStep2, times(1)).submit(eq(JOB_ID), any(RequestContext.class));
    verify(handlePersistor2, times(1)).persist(JOB_ID, SERVICE_HANDLE);
    verify(requestUpdater, times(1)).setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.READY, CaptainStatus.PENDING);
  }

  @Test
  public void testSubmitRequestAsyncSupportsPendingFalse() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.READY, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(requestSubmitterStep2.submit(eq(JOB_ID), any(RequestContext.class))).thenReturn(SERVICE_HANDLE);

    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, false, false, FAILED_REQUEST_POLICY).run();

    verify(requestSubmitterStep2, times(1)).submit(eq(JOB_ID), any(RequestContext.class));
    verify(handlePersistor2, times(1)).persist(JOB_ID, SERVICE_HANDLE);
    verify(requestUpdater, times(1)).setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.READY, CaptainStatus.IN_PROGRESS);
  }

  @Test
  public void testSubmitRequestSync() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.READY, CAPTAIN_STEP1, APP_TYPE_1);

    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    verify(requestSubmitterStep1, times(1)).submit(eq(JOB_ID), any(RequestContext.class));
    verify(requestUpdater, times(1)).setStatus(config.getId(), CAPTAIN_STEP1, CaptainStatus.READY, CaptainStatus.COMPLETED);
  }

  @Test
  public void testSubmitRequestSyncWithHandlerPersistor() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.READY, CAPTAIN_STEP2_A, APP_TYPE_1);

    when(requestSubmitterStep2.submit(eq(JOB_ID), any(RequestContext.class))).thenReturn(SERVICE_HANDLE);

    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    verify(requestSubmitterStep2, times(1)).submit(eq(JOB_ID), any(RequestContext.class));
    verify(handlePersistor2, times(1)).persist(JOB_ID, SERVICE_HANDLE);
    verify(requestUpdater, times(1)).setStatus(config.getId(), CAPTAIN_STEP2_A, CaptainStatus.READY, CaptainStatus.COMPLETED);
  }

  @Test
  public void testSubmitRequestFlowControl1() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.READY, CAPTAIN_STEP3, APP_TYPE_1);

    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(1)).setStatus(config.getId(), CAPTAIN_STEP3, CaptainStatus.READY, CaptainStatus.PENDING);
  }

  @Test
  public void testCheckRequestStartedInProgress() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.PENDING, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.IN_PROGRESS);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.COMPLETED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.FAILED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(3)).setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.PENDING, CaptainStatus.IN_PROGRESS);
  }

  @Test
  public void testCheckRequestStartedQuarantine() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.PENDING, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.QUARANTINED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.QUARANTINED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(2)).setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.PENDING, CaptainStatus.QUARANTINED);
  }

  @Test
  public void testCheckRequestStartedUnknownStatus() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.PENDING, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.PENDING);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    // i.e. do nothing.
    verify(requestUpdater, times(0)).setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.PENDING, CaptainStatus.IN_PROGRESS);
    verify(requestUpdater, times(0)).setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.PENDING, CaptainStatus.QUARANTINED);
  }

  @Test
  public void testRequestCompleteCompleted() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.IN_PROGRESS, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.COMPLETED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(1)).setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.IN_PROGRESS, CaptainStatus.COMPLETED);
  }

  @Test
  public void testRequestCompleteQuarantined() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.IN_PROGRESS, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.QUARANTINED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.QUARANTINED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(2)).setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.IN_PROGRESS, CaptainStatus.QUARANTINED);
  }

  @Test
  public void testRequestCompleteFailed() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.IN_PROGRESS, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.FAILED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    // i.e. do nothing
    verify(requestUpdater, times(0)).setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.IN_PROGRESS, CaptainStatus.COMPLETED);
    verify(requestUpdater, times(1)).setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.IN_PROGRESS, CaptainStatus.FAILED);
    verify(requestUpdater, times(0)).setStatus(config.getId(), CAPTAIN_STEP2_B, CaptainStatus.IN_PROGRESS, CaptainStatus.QUARANTINED);
  }

  @Test
  public void testRequestCompleteCancelled() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.IN_PROGRESS, CAPTAIN_STEP2_B, APP_TYPE_1);

    when(statusRetrieverStep2.getStatus(JOB_ID)).thenReturn(CaptainStatus.CANCELLED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(1)).cancel(JOB_ID);
  }

  @Test
  public void testRequestCompleteCompletedFlowControl() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.IN_PROGRESS, CAPTAIN_STEP3, APP_TYPE_1);

    when(statusRetrieverStep3.getStatus(JOB_ID)).thenReturn(CaptainStatus.COMPLETED);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(1)).setStatus(config.getId(), CAPTAIN_STEP3, CaptainStatus.IN_PROGRESS, CaptainStatus.COMPLETED);
  }


  @Test
  public void testUsesCorrectManifestForAppType() throws Exception {
    CaptainRequestConfig config = new SimpleCaptainConfig(JOB_ID, CaptainStatus.COMPLETED, CAPTAIN_STEP1, APP_TYPE_2);

    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, FAILED_REQUEST_POLICY).run();

    verify(requestUpdater, times(1)).setStepAndStatus(config.getId(), CAPTAIN_STEP1, CaptainStatus.COMPLETED, CAPTAIN_STEP5, CaptainStatus.READY);
  }

  @Test
  public void testFailedRequestPolicyRetry() throws Exception {
    testFailedRequestPolicy(FailedRequestPolicy.FailedRequestAction.RETRY, times(1), never());
  }

  @Test
  public void testFailedRequestPolicyQuarantine() throws Exception {
    testFailedRequestPolicy(FailedRequestPolicy.FailedRequestAction.QUARANTINE, never(), times(1));
  }

  @Test
  public void testFailedRequestPolicyNoop() throws Exception {
    testFailedRequestPolicy(FailedRequestPolicy.FailedRequestAction.NO_OP, never(), never());
  }

  private void testFailedRequestPolicy(
      FailedRequestPolicy.FailedRequestAction mockedAction,
      VerificationMode timesRetry,
      VerificationMode timesQuarantine) throws Exception {

    CaptainRequestConfig config = new SimpleCaptainConfig(
        JOB_ID,
        CaptainStatus.FAILED,
        CAPTAIN_STEP1,

        APP_TYPE_2);

    when(failedRequestPolicyMock.getFailedRequestAction(JOB_ID)).thenReturn(mockedAction);
    new CaptainJoblet(config, notifier, requestUpdater, manifestManager, true, false, failedRequestPolicyMock).run();

    verify(requestUpdater, timesRetry).retry(JOB_ID);
    verify(requestUpdater, timesQuarantine).quarantine(JOB_ID);
  }
}
