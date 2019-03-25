package com.smartbear.loadninja.helper;

public class TestSummary {
  private int totalSteps;
  private int passedStepCount;
  private int failedStepCount;
  private int avgResponseTime;

  public TestSummary(int totalSteps, int passedStepCount, int failedStepCount, int avgResponseTime) {
    this.totalSteps = totalSteps;
    this.passedStepCount = passedStepCount;
    this.failedStepCount = failedStepCount;
    this.avgResponseTime = avgResponseTime;
  }

  public int getTotalSteps() {
    return totalSteps;
  }

  public void setTotalSteps(int totalSteps) {
      this.totalSteps = totalSteps;
  }

  public int getPassedStepCount() {
    return passedStepCount;
  }

  public void setPassedStepCount(int passedStepCount) {
      this.passedStepCount = passedStepCount;
  }

  public int getFailedStepCount() {
    return failedStepCount;
  }

  public void setFailedStepCount(int failedStepCount) {
      this.failedStepCount = failedStepCount;
  }

  public int getAvgResponseTime() {
    return avgResponseTime;
  }

  public void setAvgResponseTime(int avgResponseTime) {
      this.avgResponseTime = avgResponseTime;
  }
}