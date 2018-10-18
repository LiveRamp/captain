package com.liveramp.captain.waypoint;


import com.liveramp.captain.request_context.RequestContext;

public interface WaypointSubmitter {
  void submit(long id, RequestContext requestOptions) throws Exception;
}
