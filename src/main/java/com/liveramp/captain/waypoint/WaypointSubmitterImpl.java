package com.liveramp.captain.waypoint;

import com.liveramp.captain.exception.CaptainPersistorException;
import com.liveramp.captain.handle_persistor.HandlePersistor;
import com.liveramp.captain.handle_persistor.HandlePersistorFactory;
import com.liveramp.captain.request_context.RequestContext;
import com.liveramp.captain.request_submitter.RequestSubmitter;
import com.liveramp.captain.request_submitter.RequestSubmitterFactory;
import com.liveramp.captain.step.CaptainStep;

public class WaypointSubmitterImpl<ServiceHandle> implements WaypointSubmitter {
  private final CaptainStep step;
  private RequestSubmitterFactory<ServiceHandle> requestSubmitterFactory;
  private HandlePersistorFactory<ServiceHandle> serviceHandlePersistorFactory;

  WaypointSubmitterImpl(
      CaptainStep step,
      RequestSubmitterFactory<ServiceHandle> requestSubmitterFactory,
      HandlePersistorFactory<ServiceHandle> serviceHandlePersistorFactory) {
    this.requestSubmitterFactory = requestSubmitterFactory;
    this.serviceHandlePersistorFactory = serviceHandlePersistorFactory;
    this.step = step;
  }

  @Override
  public void submit(long id, RequestContext requestOptions) throws Exception {
    // try to instantiate all of the components before taking ANY action.
    RequestSubmitter<ServiceHandle> requestSubmitter = requestSubmitterFactory.create();
    HandlePersistor<ServiceHandle> serviceHandlePersistor = null;
    if (serviceHandlePersistorFactory != null) {
      serviceHandlePersistor = serviceHandlePersistorFactory.create();
    }

    ServiceHandle serviceHandle = requestSubmitter.submit(id, requestOptions);
    if (serviceHandlePersistor != null) {
      try {
        serviceHandlePersistor.persist(id, serviceHandle);
      } catch (Exception e) {
        throw CaptainPersistorException.of(e, id, step.toString(), serviceHandle);
      }
    }
  }
}
