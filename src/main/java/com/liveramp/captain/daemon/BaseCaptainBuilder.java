package com.liveramp.captain.daemon;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;

import com.liveramp.captain.manifest_manager.ManifestManager;
import com.liveramp.captain.notifier.CaptainDaemonInternalNotifier;
import com.liveramp.captain.notifier.CaptainNotifier;
import com.liveramp.captain.notifier.DefaultCaptainLoggingNotifier;
import com.liveramp.captain.request_lock.CaptainRequestLock;
import com.liveramp.captain.request_lock.zk_request_lock.ZkCaptainRequestLock;
import com.liveramp.captain.retry.DefaultFailedRequestPolicy;
import com.liveramp.captain.retry.FailedRequestPolicy;
import com.liveramp.daemon_lib.Daemon;
import com.liveramp.daemon_lib.DaemonLock;
import com.liveramp.daemon_lib.JobletCallback;
import com.liveramp.daemon_lib.builders.ThreadingDaemonBuilder;
import com.liveramp.daemon_lib.utils.JobletCallbackUtil;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType, WeakerAccess", "UnusedReturnValue", "unused"})
public class BaseCaptainBuilder<T extends BaseCaptainBuilder<T>> {

  /**
   * allows the base builder to return the type of the subclass that using it.
   * allows us to have the unchecked exception in one place so that it can suppressed once for code cleanliness.
   * by making it protected, it allows a consumer to extend a builder that they build.
   * by handling this in an instance variable as opposed to an abstract method, we avoid making the extender having
   * to worry about implementing a "get self" method.
   */
  @SuppressWarnings({"unchecked", "WeakerAccess"})
  protected final T self = (T)this;

  private boolean DEFAULT_SUPPORTS_PENDING = false;
  private boolean DEFAULT_SUPPORTS_RAMMING_SPEED = false;

  private final String identifier;
  private final CaptainConfigProducer configProducer;
  private final ManifestManager manifestManager;
  private final RequestUpdater requestUpdater;

  @SuppressWarnings("ConstantConditions")
  private boolean supportsPending = DEFAULT_SUPPORTS_PENDING;
  @SuppressWarnings("ConstantConditions")
  private boolean rammingSpeed = DEFAULT_SUPPORTS_RAMMING_SPEED;
  private CaptainNotifier notifier;

  private Optional<DaemonLock> daemonConfigProducerLock = Optional.empty();
  private Optional<Integer> maxThreads = Optional.empty();
  private Optional<Integer> nextConfigWaitSeconds = Optional.empty();
  private Optional<Integer> configWaitSeconds = Optional.empty();
  private Optional<Integer> executionSlotWaitSeconds = Optional.empty();
  private Optional<Integer> failureWaitSeconds = Optional.empty();
  private Optional<CaptainRequestLock> requestLock = Optional.empty();

  private Optional<JobletCallback<CaptainRequestConfig>> onNewConfigCallback = Optional.empty();
  private Optional<JobletCallback<CaptainRequestConfig>> successCallback = Optional.empty();
  private Optional<JobletCallback<CaptainRequestConfig>> failureCallback = Optional.empty();
  private FailedRequestPolicy failedRequestPolicy = new DefaultFailedRequestPolicy();

  protected BaseCaptainBuilder(String identifier, CaptainConfigProducer configProducer, ManifestManager manifestManager, RequestUpdater requestUpdater) {
    this.identifier = identifier;
    this.configProducer = configProducer;
    this.manifestManager = manifestManager;
    this.requestUpdater = requestUpdater;
  }

  /**
   * Allows you to specify how Captain should handle failed requests.
   * @param failedRequestPolicy retry and quarantine policy that implements FailedRequestPolicy
   * @return self
   */
  public T setFailedRequestPolicy(FailedRequestPolicy failedRequestPolicy) {
    this.failedRequestPolicy = failedRequestPolicy;
    return self;
  }

  /**
   * BETA
   * Allows Captain to skip running the config producer again for your request, if it thinks it can just execute the next step of the state machine immediately.
   * @param rammingSpeed, defaults to false
   * @return self
   */
  public T setRammingSpeed(boolean rammingSpeed) {
    this.rammingSpeed = rammingSpeed;

    return self;
  }

  /**
   * Adds a notifier that handles where logging is output to and formatting thereof.
   * @param notifier, implements CaptainNotifier specifying output and formatting of logs.
   * @return self
   */
  public T setNotifier(CaptainNotifier notifier) {
    this.notifier = notifier;

    return self;
  }

  /**
   * Turns on ability to track when an async step has started processing a request.
   * @param supportsPending, defaults to false
   * @return self
   */
  public T setSupportsPending(boolean supportsPending) {
    this.supportsPending = supportsPending;

    return self;
  }

  /**
   * Sets a lock so that no two instances of Captain try to pull in new requests to process at the same time. See the README for more context ("Running Captain in a Distributed Fashion").
   * @param daemonConfigProducerLock implements DaemonLock
   * @return self
   */
  public T setConfigProducerLock(DaemonLock daemonConfigProducerLock) {
    this.daemonConfigProducerLock = Optional.of(daemonConfigProducerLock);

    return self;
  }

  /**
   * Sets a lock so that no other instance of Captain tries to process a request until the instance that has "claimed" it finishes using it. See the README for more context ("Running Captain in a Distributed Fashion").
   * @param requestLock implements CaptainRequestLock
   * @return self
   */
  public T setRequestLock(CaptainRequestLock requestLock) {
    this.requestLock = Optional.of(requestLock);

    return self;
  }

  /**
   * built-in: if you have a zookeeper cluster you can point to, we can handle the config producer lock for you.  See the README for more context ("Running Captain in a Distributed Fashion").
   * @param curatorFramework as instantiated from the Apache Curator project
   * @return self
   */
  public T setZkConfigProducerLock(CuratorFramework curatorFramework) {
    setConfigProducerLock(com.liveramp.captain.daemon.CaptainZkDaemonLock.getProduction(curatorFramework, identifier));

    return self;
  }

  /**
   * built-in: if you have a zookeeper cluster you can point to, we can handle the request lock for you.  See the README for more context ("Running Captain in a Distributed Fashion").
   * @param curatorFramework as instantiated from the Apache Curator project
   * @return self
   */
  public T setZkRequestLock(CuratorFramework curatorFramework) {
    this.requestLock = Optional.of(ZkCaptainRequestLock.getProduction(curatorFramework, identifier));

    return self;
  }

  /**
   * built-in: if you have a zookeeper cluster you can point to, we can handle the locking infra for you.  See the README for more context ("Running Captain in a Distributed Fashion").
   * @param curatorFramework as instantiated from the Apache Curator project
   * @return self
   */
  public T setZkLocks(CuratorFramework curatorFramework) {
    setZkConfigProducerLock(curatorFramework);
    setZkRequestLock(curatorFramework);

    return self;
  }

  /**
   * sets max number of threads a captain node will run at once. each thread can handle one request at a time.
   * @return self
   */
  public T setMaxThreads(int maxThreads) {
    this.maxThreads = Optional.of(maxThreads);

    return self;
  }

  /**
   * wait time after last config provided
   * @return self
   */
  public T setNextConfigWaitTime(int nextConfigWaitTime, TimeUnit unit) {
    this.nextConfigWaitSeconds = Optional.of(Math.toIntExact(unit.toSeconds(nextConfigWaitTime)));

    return self;
  }

  /**
   * wait time when no config found
   * @return self
   */
  public T setConfigWaitTime(int configWaitTime, TimeUnit unit) {
    this.configWaitSeconds = Optional.of(Math.toIntExact(unit.toSeconds(configWaitTime)));

    return self;
  }

  /**
   * wait time when all execution slots were full
   * @return self
   */
  public T setExecutionSlotWaitTime(int executionSlotWaitTime, TimeUnit unit) {
    this.executionSlotWaitSeconds = Optional.of(Math.toIntExact(unit.toSeconds(executionSlotWaitTime)));

    return self;
  }

  /**
   * wait time after failure
   * @return self
   */
  public T setFailureWaitTime(int failureWaitTime, TimeUnit unit) {
    this.failureWaitSeconds = Optional.of(Math.toIntExact(unit.toSeconds(failureWaitTime)));

    return self;
  }

  /**
   * user-provided callback, triggered when captain finds a request to process.
   * @return self
   */
  public T setOnNewConfigCallback(JobletCallback<CaptainRequestConfig> onNewConfigCallback) {
    this.onNewConfigCallback = Optional.of(onNewConfigCallback);

    return self;
  }

  /**
   * user-provided callback, triggered when captain finishes one run of the state machine.
   * @return self
   */
  public T setSuccessCallback(JobletCallback<CaptainRequestConfig> successCallback) {
    this.successCallback = Optional.of(successCallback);

    return self;
  }

  /**
   * user-provided callback, triggered when a run of the captain state machine fails.
   * @return self
   */
  public T setFailureCallback(JobletCallback<CaptainRequestConfig> failureCallback) {
    this.failureCallback = Optional.of(failureCallback);

    return self;
  }

  /**
   * builds a captain instance
   * @return - an instance of captain
   */
  @SuppressWarnings("ConstantConditions")
  public Daemon<CaptainRequestConfig> build() throws IllegalAccessException, IOException, InstantiationException {
    CaptainNotifier resolvedCaptainNotifier = notifier != null ? notifier : new DefaultCaptainLoggingNotifier();

    CaptainJobletFactory jobletFactory = new ThreadedCaptainJobletFactoryImpl(
        requestUpdater,
        manifestManager,
        resolvedCaptainNotifier,
        supportsPending,
        rammingSpeed,
        failedRequestPolicy
    );

    ThreadingDaemonBuilder<CaptainRequestConfig> daemonBuilder = new ThreadingDaemonBuilder<>(
        identifier,
        jobletFactory,
        configProducer
    )
        .setNotifier(new CaptainDaemonInternalNotifier(resolvedCaptainNotifier));

    maxThreads.ifPresent(daemonBuilder::setMaxThreads);
    nextConfigWaitSeconds.ifPresent(daemonBuilder::setNextConfigWaitSeconds);
    configWaitSeconds.ifPresent(daemonBuilder::setConfigWaitSeconds);
    executionSlotWaitSeconds.ifPresent(daemonBuilder::setExecutionSlotWaitSeconds);
    failureWaitSeconds.ifPresent(daemonBuilder::setFailureWaitSeconds);
    daemonConfigProducerLock.ifPresent(daemonBuilder::setDaemonConfigProductionLock);

    Optional<JobletCallback<CaptainRequestConfig>> lockRequestCallbackOptional = generateCallbackOptionalFromRequestLockOptional(requestLock, true);
    Optional<JobletCallback<CaptainRequestConfig>> unlockRequestCallbackOptional = generateCallbackOptionalFromRequestLockOptional(requestLock, false);

    composeRequestLockCallbackAndOtherCallbacks(lockRequestCallbackOptional, onNewConfigCallback)
        .ifPresent(daemonBuilder::setOnNewConfigCallback);
    composeRequestLockCallbackAndOtherCallbacks(unlockRequestCallbackOptional, successCallback)
        .ifPresent(daemonBuilder::setSuccessCallback);
    composeRequestLockCallbackAndOtherCallbacks(unlockRequestCallbackOptional, failureCallback)
        .ifPresent(daemonBuilder::setFailureCallback);

    return daemonBuilder.build();
  }

  private Optional<JobletCallback<CaptainRequestConfig>> generateCallbackOptionalFromRequestLockOptional(Optional<CaptainRequestLock> requestLock, boolean lock) {
    if (requestLock.isPresent()) {
      if (lock) {
        return Optional.of(new CaptainRequestLockingCallbacks.CaptainRequestLockCallback(requestLock.get()));
      } else {
        return Optional.of(new CaptainRequestLockingCallbacks.CaptainRequestUnlockCallback(requestLock.get()));
      }
    } else {
      return Optional.empty();
    }
  }

  @SuppressWarnings("OptionalIsPresent")
  private Optional<JobletCallback<CaptainRequestConfig>> composeRequestLockCallbackAndOtherCallbacks(Optional<JobletCallback<CaptainRequestConfig>> requestLock, Optional<JobletCallback<CaptainRequestConfig>> callback) {
    if (requestLock.isPresent() && callback.isPresent()) {
      return Optional.of(JobletCallbackUtil.compose(requestLock.get(), callback.get()));
    } else if (requestLock.isPresent()) {
      return requestLock;
    } else if (callback.isPresent()) {
      return callback;
    } else {
      return Optional.empty();
    }
  }
}
