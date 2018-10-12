package com.liveramp.captain.waypoint;


import com.liveramp.captain.request_context.RequestContext;

public class NoOpWaypointSubmitter implements WaypointSubmitter {
  private NoOpWaypointSubmitter() {
  }

  @Override
  public void submitServiceRequest(long jobId, RequestContext requestOptions) {
  }

  public static NoOpWaypointSubmitter get() {
    return new NoOpWaypointSubmitter();
  }
}
