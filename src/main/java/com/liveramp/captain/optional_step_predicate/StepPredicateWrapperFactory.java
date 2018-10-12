package com.liveramp.captain.optional_step_predicate;

public class StepPredicateWrapperFactory implements StepPredicateFactory {
  private final StepPredicate stepPredicate;

  public StepPredicateWrapperFactory(StepPredicate stepPredicate) {
    this.stepPredicate = stepPredicate;
  }

  @Override
  public StepPredicate create() {
    return stepPredicate;
  }
}
