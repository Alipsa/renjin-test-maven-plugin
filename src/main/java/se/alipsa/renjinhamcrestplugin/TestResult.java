package se.alipsa.renjinhamcrestplugin;

import java.io.File;

/**
 * This represents the outcome (test result) of each test.
 */
public class TestResult {

  private File testFile;
  private  String testMethod;
  private OutCome result;
  private Throwable error;
  private String issue;
  private long startTime;
  private long endTime;

  public TestResult(File file) {
    this.testFile = file;
  }

  public File getTestFile() {
    return testFile;
  }

  public String getTestMethod() {
    return testMethod;
  }

  public void setTestMethod(String testMethod) {
    this.testMethod = testMethod;
  }

  public OutCome getResult() {
    return result;
  }

  public void setResult(OutCome result) {
    this.result = result;
  }

  public Throwable getError() {
    return error;
  }

  public void setError(Throwable error) {
    this.error = error;
  }

  public String getIssue() {
    return issue;
  }

  public void setIssue(String issue) {
    this.issue = issue;
  }

  public enum OutCome {SUCCESS, FAILURE, ERROR}

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }
}
