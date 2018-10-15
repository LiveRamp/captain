package com.liveramp.captain.optional_step_predicate;

public class StepPredicates {
  public static StepPredicate alwaysTrue() {
    return new AlwaysTrueStepPredicate();
  }

  public static StepPredicate alwaysFalse() {
    return new AlwaysFalseStepPredicate();
  }

  private static class AlwaysTrueStepPredicate implements StepPredicate {
    @Override
    public boolean shouldSkipStep(long id) {
      return true;
    }
  }

  private static class AlwaysFalseStepPredicate implements StepPredicate {
    @Override
    public boolean shouldSkipStep(long id) {
      return false;
    }
  }
}
