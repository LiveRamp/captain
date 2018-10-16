package com.liveramp.captain.example.internals;

import java.util.HashMap;
import java.util.Map;

// tracks the number of times the status has been requested for an id. on the third try says it's done. arbitrary, but
// good enough to simulate behavior we might see.
public class MockServiceStatusProvider {
  private Map<Long, Integer> idToCounter = new HashMap<>();

  public boolean isDone(long id) {
    idToCounter.putIfAbsent(id, 0);

    if (idToCounter.get(id) == 2) {
      return true;
    }

    idToCounter.put(id, idToCounter.get(id) + 1);
    return false;
  }

}
