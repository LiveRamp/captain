package com.liveramp.captain.daemon;


import com.liveramp.captain.exception.CaptainCouldNotFindNextStep;
import com.liveramp.captain.exception.CaptainPersistorException;
import com.liveramp.captain.exception.CaptainTransientFailureException;
import com.liveramp.captain.lib.CaptainAlertHelpers;
import com.liveramp.captain.manifest.Manifest;
import com.liveramp.captain.manifest.ManifestFactory;
import com.liveramp.captain.manifest_manager.ManifestManager;
import com.liveramp.captain.notifier.CaptainNotifier;
import com.liveramp.captain.request_context.RequestContext;
import com.liveramp.captain.retry.FailedRequestPolicy;
import com.liveramp.captain.status.CaptainStatus;
import com.liveramp.captain.step.CaptainStep;
import com.liveramp.captain.waypoint.Waypoint;
import com.liveramp.captain.waypoint.WaypointSubmitter;
import com.liveramp.captain.waypoint.WaypointType;
import com.liveramp.daemon_lib.Joblet;
import com.liveramp.daemon_lib.utils.DaemonException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class CaptainJoblet implements Joblet {
  private CaptainNotifier notifier;
  private Logger LOG = LoggerFactory.getLogger(CaptainJoblet.class);
  private boolean supportsPending;
  private boolean rammingSpeed;
  private FailedRequestPolicy failedRequestPolicy;
  private CaptainRequestConfig config;
  private RequestUpdater requestUpdater;
  private ManifestManager manifestManager;

  CaptainJoblet(
      CaptainRequestConfig config,
      CaptainNotifier notifier,
      RequestUpdater requestUpdater,
      ManifestManager manifestManager,
      boolean supportsPending,
      boolean rammingSpeed,
      FailedRequestPolicy failedRequestPolicy
  ) {
    this.notifier = notifier;
    this.config = config;
    this.requestUpdater = requestUpdater;
    this.manifestManager = manifestManager;
    this.supportsPending = supportsPending;
    this.rammingSpeed = rammingSpeed;
    this.failedRequestPolicy = failedRequestPolicy;

    LOG.info("captain config: " + config);
  }

  public static CaptainJoblet of(
      CaptainRequestConfig config,
      CaptainNotifier notifier,
      RequestUpdater requestUpdater,
      ManifestManager manifestManager,
      boolean supportsPending,
      boolean rammingSpeed,
      FailedRequestPolicy failedRequestPolicy
  ) {
    return new CaptainJoblet(config, notifier, requestUpdater, manifestManager, supportsPending, rammingSpeed, failedRequestPolicy);
  }

  private Manifest getAccountManifest() {
    ManifestFactory manifestFactory = manifestManager.getManifestFactory(config.getAppType());
    if (null == manifestFactory) {
      throw new RuntimeException(
          String.format("could not find a manifest (host: %s) for given request type: %s. the manifests are available " +
                  "for the following request types: %s",
              CaptainAlertHelpers.getHostName(),
              config.getAppType(), manifestManager.getAvailableCaptainAppTypes()
          )
      );
    }
    return manifestFactory.create();
  }

  @Override
  public void run() throws DaemonException {
    try {
      MDC.put("id", String.valueOf(config.getId()));
      switch (config.getStatus()) {
        case COMPLETED:
          goToNextStep(config);
          break;
        case READY:
          submitRequest(config);
          break;
        case PENDING:
          checkRequestStarted(config);
          break;
        case IN_PROGRESS:
          checkRequestComplete(config);
          break;
        case FAILED:
          executeFailedRequestPolicy(config);
          break;
        default:
          throw new DaemonException("Provided config has a job status that shouldn't be handled by the captain " + config);
      }
    } catch (Exception e) {
      notifier.notify(
          String.format("%s: error in CaptainJoblet", CaptainAlertHelpers.getHostName()),
          e,
          CaptainNotifier.NotificationLevel.ERROR);
      LOG.error(e.getLocalizedMessage());
      requestUpdater.fail(config.getId());
      throw e;
    } finally {
      MDC.remove("id");
    }
  }

  private void executeFailedRequestPolicy(CaptainRequestConfig config) {
    long id = config.getId();
    FailedRequestPolicy.FailedRequestAction failedRequestAction = failedRequestPolicy.getFailedRequestAction(id);
    switch (failedRequestAction) {
      case RETRY:
        requestUpdater.retry(id);
        break;
      case QUARANTINE:
        requestUpdater.quarantine(id);
        break;
      case NO_OP:
        break;
      default:
        throw new RuntimeException("Unknown enum value: " + failedRequestAction);
    }
  }

  private void goToNextStep(CaptainRequestConfig config) {
    try {
      Manifest manifest = getAccountManifest();

      long id = config.getId();
      Optional<CaptainStep> nextStepOptional = manifest.getNextStep(config.getStep(), config.getId());

      LOG.info(String.format("current step: %s, current status: %s, next step: %s, next status: %s", config.getStep(), config.getStatus(), nextStepOptional, CaptainStatus.READY));

      if (nextStepOptional.isPresent()) {

        requestUpdater.setStepAndStatus(id, config.getStep(), config.getStatus(), nextStepOptional.get(), CaptainStatus.READY);

        if (rammingSpeed) {
          SimpleCaptainConfig newConfig = new SimpleCaptainConfig(
              config.getId(),
              CaptainStatus.READY,
              nextStepOptional.get(),
              config.getAppType()
          );
          CaptainJoblet.of(newConfig, notifier, requestUpdater, manifestManager, supportsPending, rammingSpeed, failedRequestPolicy).run();
        }
      } else {
        if (!rammingSpeed) {
          throw new CaptainCouldNotFindNextStep(config.getId(), config.getStep(), config.getStatus());
        }
      }

    } catch (Exception e) {
      String subject = String.format("%s: error while transitioning steps for request %s from step: %s.",
          CaptainAlertHelpers.getHostName(), config.getId(), config.getStep()
      );

      notifier.notify(subject, e, CaptainNotifier.NotificationLevel.ERROR);
      requestUpdater.fail(config.getId());
    }
  }

  private void submitRequest(CaptainRequestConfig config) throws DaemonException {
    Manifest manifest = getAccountManifest();
    Waypoint waypoint = manifest.getWaypointForStep(config.getStep());
    long id = config.getId();

    // if db is out we will make the same request over and over again. not good.
    boolean isAsyncStep = waypoint.getType().equals(WaypointType.ASYNC);
    boolean isFlowControl = waypoint.getType().equals(WaypointType.FLOW_CONTROL);

    try {
      WaypointSubmitter waypointSubmitter = waypoint.getSubmitter();
      RequestContext requestOptions = manifest.getRequestContextProducerFactory().create().get(id);
      waypointSubmitter.submit(config.getId(), requestOptions);

    } catch (CaptainPersistorException e) {
      String subject = String.format("%s: handle persistence failed for request %s", CaptainAlertHelpers.getHostName(), id);
      notifier.notify(subject, e, CaptainNotifier.NotificationLevel.ERROR);
      requestUpdater.cancel(id);
      return;
    } catch (CaptainTransientFailureException ce) {
      String subject = String.format("%s: Transient failure while submitting requests %s. Do nothing", CaptainAlertHelpers.getHostName(), id);
      notifier.notify(subject, ce, CaptainNotifier.NotificationLevel.ERROR);
      return;
    } catch (Throwable e) {
      String subject = String.format("%s: error while submitting request %s", CaptainAlertHelpers.getHostName(), id);
      notifier.notify(subject, e, CaptainNotifier.NotificationLevel.ERROR);
      requestUpdater.fail(id);
      return;
    }

    CaptainStatus targetStatus;

    if (isAsyncStep || isFlowControl) {
      if (supportsPending) {
        targetStatus = CaptainStatus.PENDING;
      } else {
        targetStatus = CaptainStatus.IN_PROGRESS;
      }
    } else {
      targetStatus = CaptainStatus.COMPLETED;
    }

    LOG.info(String.format("current step %s, current status: %s, next status: %s", config.getStep(), config.getStatus(), targetStatus));
    requestUpdater.setStatus(id, config.getStep(), config.getStatus(), targetStatus);

    if (rammingSpeed) {
      SimpleCaptainConfig newConfig = new SimpleCaptainConfig(
          config.getId(),
          targetStatus,
          config.getStep(),
          config.getAppType());
      CaptainJoblet.of(newConfig, notifier, requestUpdater, manifestManager, supportsPending, rammingSpeed, failedRequestPolicy).run();

    }
  }

  private void checkRequestStarted(CaptainRequestConfig config) {
    try {
      Manifest manifest = getAccountManifest();
      Waypoint waypoint = manifest.getWaypointForStep(config.getStep());
      long id = config.getId();

      CaptainStatus status = waypoint.getStatusRetrieverFactory().create().getStatus(config.getId());

      CaptainStatus targetStatus;
      if (status.equals(CaptainStatus.IN_PROGRESS) || status.equals(CaptainStatus.FAILED) || status.equals(
          CaptainStatus.COMPLETED)) {
        targetStatus = CaptainStatus.IN_PROGRESS;
      } else if (status.equals(CaptainStatus.QUARANTINED)) {
        targetStatus = CaptainStatus.QUARANTINED;
      } else if (status.equals(CaptainStatus.CANCELLED)) {
        requestUpdater.cancel(id);
        return;
      } else {
        // do nothing
        targetStatus = config.getStatus();
      }

      LOG.info(String.format("current step %s, current status: %s, next status: %s", config.getStep(), config.getStatus(), targetStatus));
      requestUpdater.setStatus(id, config.getStep(), config.getStatus(), targetStatus);
    } catch (CaptainTransientFailureException ce) {
      String subject = String.format("%s: Transient failure while retrieving status for request %s. Doing nothing.", CaptainAlertHelpers.getHostName(), config.getId());
      notifier.notify(subject, ce, CaptainNotifier.NotificationLevel.ERROR);
      return;
    } catch (Exception e) {
      String subject = String.format("%s: error while checking if service has begun processing req %s in step: %s",
          config.getId(), CaptainAlertHelpers.getHostName(), config.getStep()
      );

      notifier.notify(subject, e, CaptainNotifier.NotificationLevel.ERROR);
      requestUpdater.fail(config.getId());
    }

  }

  private void checkRequestComplete(CaptainRequestConfig config) {
    try {
      Manifest manifest = getAccountManifest();
      Waypoint waypoint = manifest.getWaypointForStep(config.getStep());
      long id = config.getId();

      CaptainStatus status = waypoint.getStatusRetrieverFactory().create().getStatus(config.getId());

      CaptainStatus targetStatus;
      if (status.equals(CaptainStatus.COMPLETED)) {
        targetStatus = CaptainStatus.COMPLETED;
      } else if (status.equals(CaptainStatus.FAILED)) {
        targetStatus = CaptainStatus.FAILED;
      } else if (status.equals(CaptainStatus.PENDING) || status.equals(CaptainStatus.IN_PROGRESS)) {
        // do nothing.
        targetStatus = config.getStatus();
      } else if (status.equals(CaptainStatus.QUARANTINED)) {
        targetStatus = CaptainStatus.QUARANTINED;
      } else if (status.equals(CaptainStatus.QUARANTINED_PO)) {
        targetStatus = CaptainStatus.QUARANTINED_PO;
      } else if (status.equals(CaptainStatus.CANCELLED)) {
        requestUpdater.cancel(id);
        return;
      } else { // i.e. status.equals(ServiceRequestStatus.PENDING) || status.equals(ServiceRequestStatus.IN_PROGRESS)
        throw new RuntimeException(String.format("request %s in an unexpected state. in the status_retriever step it was in status %s", id, status));
      }

      LOG.info(String.format("current step %s, current status: %s, next status: %s", config.getStep(), config.getStatus(), targetStatus));
      requestUpdater.setStatus(id, config.getStep(), config.getStatus(), targetStatus);

      if (rammingSpeed && targetStatus.equals(CaptainStatus.COMPLETED)) {
        SimpleCaptainConfig newConfig = new SimpleCaptainConfig(
            config.getId(),
            CaptainStatus.COMPLETED,
            config.getStep(),
            config.getAppType());

        CaptainJoblet.of(newConfig, notifier, requestUpdater, manifestManager, supportsPending, rammingSpeed, failedRequestPolicy).run();
      }

    } catch (CaptainTransientFailureException ce) {
      String subject = String.format("%s: Transient failure while retrieving status for request %s. Doing nothing.", CaptainAlertHelpers.getHostName(), config.getId());
      notifier.notify(subject, ce, CaptainNotifier.NotificationLevel.ERROR);
      return;
    } catch (Exception e) {
      String subject = String.format("%s: error while checking status of req %s in step: %s",
          CaptainAlertHelpers.getHostName(), config.getId(), config.getStep()
      );

      notifier.notify(subject, e, CaptainNotifier.NotificationLevel.ERROR);
      requestUpdater.fail(config.getId());
    }
  }
}
