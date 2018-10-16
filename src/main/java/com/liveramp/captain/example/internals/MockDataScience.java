package com.liveramp.captain.example.internals;

/*
 * code so that the ExampleCaptainWorkflow can run, but not actually useful in learning about captain.
 */
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
