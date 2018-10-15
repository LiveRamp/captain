package com.liveramp.captain.example;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.liveramp.captain.app_type.CaptainAppType;
import com.liveramp.captain.daemon.CaptainBuilder;
import com.liveramp.captain.daemon.CaptainConfigProducer;
import com.liveramp.captain.daemon.CaptainRequestConfig;
import com.liveramp.captain.daemon.RequestUpdater;
import com.liveramp.captain.daemon.SimpleCaptainConfig;
import com.liveramp.captain.handle_persistor.HandlePersistor;
import com.liveramp.captain.manifest.DefaultManifestImpl;
import com.liveramp.captain.manifest.Manifest;
import com.liveramp.captain.manifest_manager.ManifestManager;
import com.liveramp.captain.manifest_manager.MultiAppManifestManager;
import com.liveramp.captain.notifier.CaptainNotifier;
import com.liveramp.captain.request_context.RequestContext;
import com.liveramp.captain.request_submitter.NoOpSubmitter;
import com.liveramp.captain.request_submitter.RequestSubmitter;
import com.liveramp.captain.status.CaptainStatus;
import com.liveramp.captain.status_retriever.StatusRetriever;
import com.liveramp.captain.step.CaptainStep;
import com.liveramp.captain.waypoint.AsyncWaypoint;
import com.liveramp.captain.waypoint.ControlFlowWaypoint;
import com.liveramp.captain.waypoint.SyncWaypoint;
import com.liveramp.commons.collections.map.MapBuilder;
import com.liveramp.daemon_lib.Daemon;

public class ExampleCaptainWorkflow {
  static final String APP1 = "App1";
  static final String APP2 = "App2";
  static final String STEP1 = "Step1";
  static final String STEP2 = "Step2";
  static final String STEP3 = "Step3";
  static final String STEP4 = "Step4";
  static final String STEP5 = "DONE";

  private Logger LOG = LoggerFactory.getLogger(ExampleCaptainWorkflow.class);

  /**
   * For sake of example let's say we have our team is responsible for running some job that performs analytics on a file.
   * <p>
   * Doing this requires 3 main steps:
   * 1) Wait for ingestion of the file to complete by some other team (i.e. poll until we know the file is ready for us to use.
   * 2) Kick off a map reduce job that's handled by some service.
   * 3) When that completes, report the information from the map reduce job to some other team's service. In this example,
   * we'll have a split pipeline, where depending on the file, we report the information to the Data Science Team 1's
   * Service or to Data Science Team 2's Service.
   * <p>
   * When thinking about this with Captain, the fact that we have two distinct pipelines should hint that we want to
   * use a "MultiApp" system (or perhaps optional steps, but for this example we're not going to do that).
   * <p>
   * In this class, you will see a skeleton version of how you might implement this use case using captain.
   */
  public static void main(String[] args) throws IllegalAccessException, IOException, InstantiationException {
    new ExampleCaptainWorkflow().build().start();
  }

  private Daemon<CaptainRequestConfig> build() throws IOException, IllegalAccessException, InstantiationException {
    // thin persistence layer for sake of example that looks something like one might store a request in their own db.
    final ExampleInternals.MockDb mockDb = new ExampleInternals.MockDb();

    // Remember we need to provide Captain 3 main resources: 1) Manifest, 2) Config Producer, 3) Request Updater

    // 1) Manifest
    // Since we have have two apps running in our captain instance. We provide captain with two manifests and a key
    // to determine manifest it should use for each request. Hopefully by skimming over these manifests it's pretty easy
    // to map what they're doing at a high level to our stated goals above.
    final Manifest exampleManifestApp1 = new DefaultManifestImpl(Lists.newArrayList(
        // Control Flow Waypoints help us hold progress on a request until certain conditions are met. In this case, we
        // we are waiting for the ingestion of the file to complete.
        new ControlFlowWaypoint(CaptainStep.fromString(STEP1), new IngestionComplete()),
        // Async Waypoints are designed to submit work to an external service and then wait for that work to be completed.
        // In this case you can see the code for how we would submit the work, how we would track the external id for that work,
        // and finally how we determine that work is completed.
        new AsyncWaypoint<>(CaptainStep.fromString(STEP2), new MapReduceJobSubmitter(), new MapReduceJobHandlePersistor(mockDb), new MapReduceJobStatusRetriever(mockDb)),
        // Sync Waypoints allows us to submit work to an external service and then move on. For example, maybe we report
        // a number to the data science team, and once that REST call is complete, we can move on.
        new SyncWaypoint<>(CaptainStep.fromString(STEP3), new ReportToDataScienceTeam1()),
        // By convention only, it's sometimes helpful to just have a No Op "DONE" step. This is not necessary; this step
        // could be removed entirely.
        new SyncWaypoint<>(CaptainStep.fromString(STEP5), new NoOpSubmitter())
    ));

    final Manifest exampleManifestApp2 = new DefaultManifestImpl(Lists.newArrayList(
        new ControlFlowWaypoint(CaptainStep.fromString(STEP1), new IngestionComplete()),
        new AsyncWaypoint<>(CaptainStep.fromString(STEP2), new MapReduceJobSubmitter(), new MapReduceJobHandlePersistor(mockDb), new MapReduceJobStatusRetriever(mockDb)),
        // Note: this is our second app type where we're reporting to Data Science Team 2.
        new SyncWaypoint<>(CaptainStep.fromString(STEP4), new ReportToDataScienceTeam2()),
        new SyncWaypoint<>(CaptainStep.fromString(STEP5), new NoOpSubmitter())
    ));

    final ManifestManager manifestManager = MultiAppManifestManager.ofManifests(MapBuilder
        .of(CaptainAppType.fromString(APP1), exampleManifestApp1)
        .put(CaptainAppType.fromString(APP2), exampleManifestApp2)
        .asUnmodifiableMap()
    );

    // 2) Config Producer: This is how you find new requests for Captain to process.
    final ExampleConfigProducer configProducer = new ExampleConfigProducer(mockDb);


    // 3) Request Updater: This is how persist the changes that that captain makes to your own persistence layer (e.g. your database).
    // Captain simply needs an interface to talk to, and is otherwise entirely un-opinionated about how you store the output
    // of the state machine.
    final RequestUpdater requestUpdater = new ExampleRequestUpdater(mockDb);

    // The notifier provides an interface for you to route logging and error messaging.
    final CaptainNotifier daemonNotifier = new ExampleCaptainNotifier();

    // Put it all together, and you've got a Captain!
    // check out the README or the CaptainBuilder javadocs for more info on some of the other settings that are set below.
    return CaptainBuilder.of("example-captain-daemon", configProducer, manifestManager, requestUpdater)
        .setNotifier(daemonNotifier)
        .setRammingSpeed(false)
        .setMaxThreads(1)
        .setConfigWaitTime(1, TimeUnit.SECONDS)
        .setNextConfigWaitTime(1, TimeUnit.SECONDS)
        .build();
  }

  class ExampleConfigProducer implements CaptainConfigProducer {
    private ExampleInternals.MockDb mockDb;

    ExampleConfigProducer(ExampleInternals.MockDb mockDb) {
      this.mockDb = mockDb;
    }

    @Override
    public CaptainRequestConfig getNextConfig() {
      System.out.println("config producer running...");
      // pretty common parameters we see in querying "next" requests is that we only pull in requests that are in an "active"
      // state. practically speaking that means that they are: 1) not in the "DONE" step--this is an example of where having a
      // "DONE" step just makes reasoning about things a bit easier and 2) not in a status of quarantined.
      ExampleInternals.MockRequest mockRequest = mockDb.getNextRequest();

      SimpleCaptainConfig config = new SimpleCaptainConfig(
          mockRequest.id,
          mockRequest.status,
          CaptainStep.fromString(mockRequest.step),
          CaptainAppType.fromString(mockRequest.appType)
      );

      System.out.println("config = " + config);
      return config;
    }
  }

  class ExampleRequestUpdater implements RequestUpdater {
    private ExampleInternals.MockDb mockDb;

    ExampleRequestUpdater(ExampleInternals.MockDb mockDb) {
      this.mockDb = mockDb;
    }

    @Override
    public void setStatus(long id, CaptainStep currentStep, CaptainStatus currentStatus, CaptainStatus newStatus) {
      setStepAndStatus(id, currentStep, currentStatus, currentStep, newStatus);
    }

    @Override
    public void setStepAndStatus(long id, CaptainStep currentStep, CaptainStatus currentStatus, CaptainStep newStep, CaptainStatus newStatus) {
      mockDb.setRequestState(id, newStatus, newStep.get());
    }

    @Override
    public void cancel(long id) {
      mockDb.setRequestState(id, CaptainStatus.CANCELLED);
    }

    @Override
    public void fail(long id) {
      mockDb.setRequestState(id, CaptainStatus.FAILED);
    }
  }

  class IngestionComplete implements StatusRetriever {

    @Override
    public CaptainStatus getStatus(long id) {
      // pretend this is a call to the ingestion team that's checking if the file is ready for us to process.
      // e.g. ingestionService.getFileStatus(id);

      // instead we have a random boolean generator for the sake of example.
      System.out.println(String.format("check if file with id: %s is ready", id));
      boolean fileReady = new Random().nextBoolean();
      System.out.println("fileReady = " + fileReady);
      return fileReady ? CaptainStatus.COMPLETED : CaptainStatus.IN_PROGRESS; // returning COMPLETED means we go forward.
      // IN_PROGRESS means we'll try again later.
    }
  }

  class MapReduceJobSubmitter implements RequestSubmitter<Long> {

    @Override
    public Long submit(long id, RequestContext options) {
      // pretend there's some rest service that we can hit with the path of our file and perhaps some other parameters.
      // it returns back to us some id of the job that it's going to kick off so we can check back later.
      // e.g. analyticsEngineService.submit(id, filePath, some other params);

      // for sake of example let's just say we submitted everything and here's the job id we got back.
      long jobId = 255L + id;
      System.out.println(String.format("submitting request with id: %s map reduce service and got job id: %s", id, jobId));
      return jobId;
    }
  }

  class MapReduceJobHandlePersistor implements HandlePersistor<Long> {
    private ExampleInternals.MockDb mockDb;

    MapReduceJobHandlePersistor(ExampleInternals.MockDb mockDb) {
      this.mockDb = mockDb;
    }

    @Override
    public void persist(Long id, Long mapReduceServiceId) {
      // now let's save that handle to our database so we can keep track of it.
      mockDb.setRequestHandle(id, mapReduceServiceId);
    }
  }

  class MapReduceJobStatusRetriever implements StatusRetriever {
    private ExampleInternals.MockDb mockDb;

    MapReduceJobStatusRetriever(ExampleInternals.MockDb mockDb) {
      this.mockDb = mockDb;
    }

    @Override
    public CaptainStatus getStatus(long id) {
      // now we can check back in with our map reduce analytics service to see if it finished our job. we pull the handle
      // out of our db...
      ExampleInternals.MockRequest request = mockDb.getRequest(id);
      @SuppressWarnings("ConstantConditions") Long jobId = request.requestHandle.get();
      // and then make a rest call to the analytics service.
      // e.g. analyticsEngineService.getStatus(jobId);
      // same deal as before, if it's done, then we return CaptainStatus.COMPLETED and move on. Otherwise IN_PROGRESS.

      // for the sake of our example we'll call our random boolean generator instead of the a real service.
      System.out.println(String.format("check if job id: %s for id: %s is completed", jobId, id));
      boolean jobDone = new Random().nextBoolean();
      System.out.println("jobDone = " + jobDone);
      return jobDone ? CaptainStatus.COMPLETED : CaptainStatus.IN_PROGRESS;
    }
  }

  class ReportToDataScienceTeam1 implements RequestSubmitter<Long> {

    @Override
    public Long submit(long id, RequestContext options) {
      // now we report to our data science team.
      // eg. dataScienceTeam1Service.submitResults(id, and whatever else we output from our job...);

      System.out.println("calling a data science team 1's with request id:" + id);
      // in this case we're not worried about saving a handle. so we just use Long as the generic in RequestSubmitter
      // and return null;
      return null;
    }
  }

  class ReportToDataScienceTeam2 implements RequestSubmitter<Long> {

    @Override
    public Long submit(long id, RequestContext options) {
      // same for team 2.
      System.out.println("calling a data science team 1's with request id:" + id);
      return null;
    }
  }

  class ExampleCaptainNotifier implements CaptainNotifier {

    @Override
    public void notify(String header, String message, NotificationLevel notificationLevel) {
      notify(header, message, null, notificationLevel);
    }

    @Override
    public void notify(
        String header, Throwable t, NotificationLevel notificationLevel) {
      notify(header, "", t, notificationLevel);
    }

    @Override
    public void notify(String header, String message, Throwable t, NotificationLevel notificationLevel) {
      if (t == null) {
        LOG.info(String.format(
            "%s\n%s\n%s",
            header,
            message,
            notificationLevel.toString()
        ));
      } else {
        LOG.info(String.format(
            "%s\n%s\n%s\n%s",
            header,
            message,
            t.getCause(),
            notificationLevel.toString()
        ));
      }
    }
  }
}
