package se.alipsa.renjinhamcrestplugin;

import org.apache.maven.plugin.MojoFailureException;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
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

    logger.info("");
    logger.info("--------------END OF RENJIN HAMCREST TESTS--------------\n");

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

  /**
   *<testsuite tests="3">
   *     <testcase classname="foo1" name="ASuccessfulTest"/>
   *     <testcase classname="foo2" name="AnotherSuccessfulTest"/>
   *     <testcase classname="foo3" name="AFailingTest">
   *         <failure type="NotEnoughFoo"> details about failure </failure>
   *     </testcase>
   * </testsuite>
   */
  public static void printResultsToFile(File reportOutputDirectory, File testOutputDirectory,
                                        List<TestResult> results, boolean testFailureIgnore) {

    Map<File, List<TestResult>> resultGroups = results.stream().collect(Collectors.groupingBy(TestResult::getTestFile));
    resultGroups.forEach((k, v) -> printResultToFile(reportOutputDirectory, testOutputDirectory, v));
  }

  private static void printResultToFile(File reportOutputDirectory, File testOutputDirectory, List<TestResult> resultGroup){
    Document document = DocumentHelper.createDocument();
    Element root = document.addElement("testsuite");
    root.addAttribute("tests", resultGroup.size() + "");
    for (TestResult res : resultGroup) {
      Element testCase = root.addElement("testcase");
      testCase.addAttribute("classname", res.getTestFile().getName());
      testCase.addAttribute("name", res.getTestMethod());
      if (!res.getResult().equals(TestResult.OutCome.SUCCESS)) {
        Element failure = testCase.addElement("failure");
        failure.addAttribute("type", res.getResult().toString());
        failure.addText(res.getIssue());
      }
    }
    // All the file names are the same so we can just grab the first
    String file = resultGroup.get(0).getTestFile().getAbsolutePath();
    String strippedPath = file.substring(testOutputDirectory.getAbsolutePath().length(), file.length());
    File outFile = new File(reportOutputDirectory, "TEST-" + strippedPath);
    try (FileWriter out = new FileWriter(outFile)) {
      document.write(out);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
