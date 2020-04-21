package se.alipsa.renjintestplugin;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;

import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class TestResultPrinter {




  static String formatMessage(final Throwable error) {
    if (error == null || error.getMessage() == null) {
      return error + "";
    }
    return error.getMessage().trim().replace("\n", ", ");
  }

  static void printResultToConsole(Logger logger, List<TestResult> results, boolean testFailureIgnore, Collection<File> testFiles) throws MojoFailureException {
    Map<TestResult.OutCome, List<TestResult>> resultMap = results.stream()
        .collect(Collectors.groupingBy(TestResult::getResult));

    List<TestResult> successResults = resultMap.get(TestResult.OutCome.SUCCESS);
    List<TestResult> failureResults = resultMap.get(TestResult.OutCome.FAILURE);
    List<TestResult> errorResults = resultMap.get(TestResult.OutCome.ERROR);
    long successCount = successResults == null ? 0 : successResults.size();
    long failCount = failureResults == null ? 0 : failureResults.size();
    long errorCount = errorResults == null ? 0 : errorResults.size();
    logger.info("");
    logger.info("R tests summary:");
    logger.info("----------------");
    logger.info("{} Files executed, Tests run: {}, Sucesses: {}, Failures: {}, Errors: {}",
        testFiles.size(), results.size(), successCount, failCount, errorCount);

    boolean errorsDuringTests = failCount > 0 || errorCount > 0;
    if (errorsDuringTests) {
      logger.info("");
      logger.info("Results: ");
      for (TestResult result : results) {
        if (result.getResult().equals(TestResult.OutCome.SUCCESS)) {
          continue;
        }
        String errMsg = formatMessage(result.getError());
        logger.error("\t{} : {} : {}",
            result.getResult(),
            result.getIssue(),
            errMsg);
      }
    } else {
      logger.info("\tSUCCESS! {} tests run", results.size());
    }
    long totalTime = 0;
    for(TestResult res : results) {
      totalTime += res.getEndTime() - res.getStartTime();
    }

    logger.info("");
    logger.info("Total time: {}", DurationFormatUtils.formatDuration(totalTime,
        "'minutes: 'mm' seconds: 'ss' millis: 'SSS"), false);
    logger.info("");
    logger.info("--------------END OF RENJIN TESTS--------------\n");

    if (errorsDuringTests && !testFailureIgnore) {
      // pick the first one otherwise the only thing that will be seen is this class having an issue

      // Suppose that errors are more interesting than Failures, so check those first
      TestResult errorResult = results.stream()
          .filter(p -> p.getResult().equals(TestResult.OutCome.ERROR))
          .findAny()
          .orElse(null);

      if (errorResult == null) {
        errorResult = results.stream()
            .filter(p -> p.getResult().equals(TestResult.OutCome.FAILURE))
            .findAny()
            .orElse(null);
      }

      throw new MojoFailureException("There were " + failCount + " failures and " + errorCount + " errors"
          , errorResult.getError());
    }
  }


  public static void printResultsToFile(File reportOutputDirectory, File testOutputDirectory,
                                        List<TestResult> results, boolean testFailureIgnore) {

    Map<File, List<TestResult>> resultGroups = results.stream().collect(Collectors.groupingBy(TestResult::getTestFile));
    resultGroups.forEach((k, v) -> printResultToFile(reportOutputDirectory, testOutputDirectory, k, v));
  }

  /**
   *<testsuite tests="3" failures="1" name="se.alipsa.renjintestplugin.RenjinTestMojoTest" time="2.301" errors="0" skipped="0>
   *     <testcase classname="foo1" name="ASuccessfulTest" time="1.868"/>
   *     <testcase classname="foo2" name="AnotherSuccessfulTest" time="1.868"/>
   *     <testcase classname="foo3" name="AFailingTest" time="1.868">
   *         <failure message="NotEnoughFoo" type="SomeExceptionType"> details about failure </failure>
   *     </testcase>
   * </testsuite>
   */
  private static void printResultToFile(File reportOutputDirectory, File testOutputDirectory, File testFile, List<TestResult> resultGroup){

    DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.getDefault());
    otherSymbols.setDecimalSeparator('.');
    otherSymbols.setGroupingSeparator(',');
    DecimalFormat format = new DecimalFormat("###.###", otherSymbols);

    Document document = DocumentHelper.createDocument();
    Element root = document.addElement("testsuite");
    root.addAttribute("tests", resultGroup.size() + "");
    Long totalTime = 0L;
    for (TestResult res : resultGroup) {
      Element testCase = root.addElement("testcase");
      String testName = res.getTestFile().getName();
      testCase.addAttribute("classname", testName.substring(0, testName.lastIndexOf(".")));
      testCase.addAttribute("name", res.getTestMethod());
      Long time = res.getEndTime() - res.getStartTime();
      totalTime = totalTime + time;
      double sec = time.doubleValue() / 1000.0;
      //System.out.println("sec for test " + testName + " id " + sec);
      testCase.addAttribute("time", format.format(sec) );
      if (!res.getResult().equals(TestResult.OutCome.SUCCESS)) {
        Element failure = testCase.addElement("failure");
        failure.addAttribute("message", res.getIssue());
        failure.addAttribute("type", res.getResult().toString());
        failure.addText(asString(res.getError()));
      }
    }
    Map<TestResult.OutCome, List<TestResult>> resultMap = resultGroup.stream()
        .collect(Collectors.groupingBy(TestResult::getResult));
    List<TestResult> failureResults = resultMap.get(TestResult.OutCome.FAILURE);
    List<TestResult> errorResults = resultMap.get(TestResult.OutCome.ERROR);

    String file = testFile.getAbsolutePath();
    String strippedPath = file.substring(testOutputDirectory.getAbsolutePath().length() +1).replace(File.separatorChar, '.');

    root.addAttribute("failures",  (failureResults == null ? 0 : failureResults.size()) + "");
    root.addAttribute("errors", (errorResults == null ? 0 : errorResults.size()) + "");
    String name = strippedPath.substring(0, strippedPath.lastIndexOf("."));
    root.addAttribute("name", name);
    double totalSec = totalTime.doubleValue()/1000.0;
    root.addAttribute("time", format.format(totalSec));
    //System.out.println("Time for test file " + name + " is " + totalSec);

    // All the file names are the same so we can just grab the first

    File outFile = new File(reportOutputDirectory, "TEST-" + strippedPath + ".xml");
    try (FileWriter out = new FileWriter(outFile)) {
      document.write(out);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String asString(Throwable e) {
    if (e == null) {
      return "";
    }
    String stackTrace = e.toString();
    try(StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw)) {
      e.printStackTrace(pw);
      stackTrace = sw.toString();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
    return stackTrace;
  }

}
