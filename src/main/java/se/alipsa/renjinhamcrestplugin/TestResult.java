package se.alipsa.renjinhamcrestplugin;

import java.io.File;

public class TestResult {

  File testFile;

  ;
  OutCome result;
  Throwable error;
  String issue;

  public TestResult() {
  }

  public TestResult(File file) {
    this.testFile = file;
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

  public File getTestFile() {
    return testFile;
  }

  public String getIssue() {
    return issue;
  }

  public void setIssue(String issue) {
    this.issue = issue;
  }

  public enum OutCome {SUCCESS, FAILURE, ERROR}
}
