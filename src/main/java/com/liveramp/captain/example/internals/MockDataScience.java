package com.liveramp.captain.example.internals;

public class MockDataScience {
  public static class Service1 extends DataScienceService {

  }

  public static class Service2 extends DataScienceService {

  }

  private static class DataScienceService {
    @SuppressWarnings("unused")
    public void submitData(String pathToData) {
    }
  }
}
