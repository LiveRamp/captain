package com.liveramp.captain.optional_step_predicate;

public interface StepPredicate {
  boolean shouldSkipStep(long id);
}
