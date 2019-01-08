package se.alipsa.renjinhamcrestplugin;

import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.renjin.RenjinVersion;
import org.renjin.eval.Context;
import org.renjin.eval.EvalException;
import org.renjin.eval.Session;
import org.renjin.eval.SessionBuilder;
import org.renjin.script.RenjinScriptEngine;
import org.renjin.script.RenjinScriptEngineFactory;
import org.renjin.sexp.Closure;
import org.renjin.sexp.FunctionCall;
import org.renjin.sexp.SEXP;
import org.renjin.sexp.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Goal which runs Renjin Hamcrest test.
 */
@Mojo(name = "testR",
    defaultPhase = LifecyclePhase.TEST,
    requiresDependencyResolution = ResolutionScope.TEST,
    requiresProject = true
)
public class RenjinHamcrestMojo extends AbstractMojo {

  @Parameter(name = "reportOutputDirectory", property = "testR.reportOutputDirectory",
      defaultValue = "${project.build.directory}/renjin-hamcrest-test-reports", required = true)
  private File reportOutputDirectory;

  @Parameter( defaultValue = "${project}", readonly = true )
  private MavenProject project;

  @Parameter(name = "testSourceDirectory", property = "testR.testSourceDirectory",
      defaultValue = "${project.basedir}/src/test/R", required = true)
  private File testSourceDirectory;

  @Parameter(name = "testOutputDirectory", property = "testR.testOutputDirectory",
      defaultValue = "${project.build.testOutputDirectory}", required = true)
  private File testOutputDirectory;

  @Parameter(name = "skipTests", property = "testR.skipTests", defaultValue = "false")
  private boolean skipTests;

  @Parameter(name = "testFailureIgnore", property = "testR.testFailureIgnore", defaultValue = "false")
  private boolean testFailureIgnore;

  @Parameter(name = "printSuccess", property = "testR.printSuccess", defaultValue = "false")
  private boolean printSuccess;



  private Logger logger = LoggerFactory.getLogger(RenjinHamcrestMojo.class);
  private ClassLoader classLoader;
  private String[] extensions = new String[]{"R", "S"};
  private List<TestResult> results;
  private RenjinScriptEngineFactory factory;

  public void execute() throws MojoExecutionException, MojoFailureException {

    logger.info("");
    logger.info("--------------------------------------------------------");
    logger.info("               RENJIN HAMCREST TESTS");
    logger.info("               Renjin ver: {}",RenjinVersion.getVersionName());
    logger.info("--------------------------------------------------------");

    if (project == null) {
      throw new MojoExecutionException("MavenProject is null, cannot continue");
    }
    if (reportOutputDirectory == null) {
      throw new MojoExecutionException("reportOutputDirectory is null, cannot continue");
    }
    if (testSourceDirectory == null) {
      throw new MojoExecutionException("testSourceDirectory is null, cannot continue");
    }

    if (!reportOutputDirectory.exists()) {
      reportOutputDirectory.mkdirs();
    }

    if (!testOutputDirectory.exists()) {
      testOutputDirectory.mkdirs();
    }

    if (testFailureIgnore) {
      logger.info("testFailureIgnore is true, will not halt the build if test failures occur");
    }

    try {
      logger.info("Copying {} to {}", testSourceDirectory, testOutputDirectory);
      FileUtils.copyDirectory(testSourceDirectory, testOutputDirectory);
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to copy files from " + testSourceDirectory + " to " + testOutputDirectory, e);
    }

    factory = new RenjinScriptEngineFactory();

    List<URL> runtimeUrls = new ArrayList<>();
    try {
      // Add classpath from calling pom
      for (String element : project.getTestClasspathElements()) {
        runtimeUrls.add(new File(element).toURI().toURL());
      }

    } catch (DependencyResolutionRequiredException | MalformedURLException e) {
      throw new MojoExecutionException("Failed to set up classLoader", e);
    }
    classLoader = new URLClassLoader(runtimeUrls.toArray(new URL[0]),
        Thread.currentThread().getContextClassLoader());

    results = new ArrayList<>();

    Collection<File> testFiles = FileUtils.listFiles(testOutputDirectory, extensions, true);

    for (File testFile : testFiles) {
      runTestFile(testFile);
    }

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
            result.issue,
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
          , errorResult.error);
    }
  }

  private String formatMessage(final Throwable error) {
    return error.getMessage().trim().replace("\n", ", ");
  }

  private void runTestFile(final File testFile) throws MojoExecutionException {


    String testName = testFile.getAbsolutePath().substring(testOutputDirectory.getAbsolutePath().length() + 1);
    logger.info("");
    logger.info("# Running {}", testName);

    SessionBuilder builder = new SessionBuilder();
    Session session = builder
        .withDefaultPackages()
        .setClassLoader(classLoader) //allows imports in r code to work
        .build();

    try {
      session.setWorkingDirectory(testOutputDirectory);
    } catch (FileSystemException e) {
      throw new MojoExecutionException("Failed to set working dir for session to " + testOutputDirectory);
    }

    RenjinScriptEngine engine = factory.getScriptEngine(session);
    // First run any test that are not defined as functions
    TestResult result = runTest(testFile, engine);
    results.add(result);

    //now run each testFunction in that file, in the same Session
    for (Symbol name : session.getGlobalEnvironment().getSymbolNames()) {
      String methodName = name.getPrintName().trim();
      if (methodName.startsWith("test.")) {
        SEXP value = session.getGlobalEnvironment().getVariable(session.getTopLevelContext(), name);
        if (isNoArgsFunction(value)) {
          Context context = session.getTopLevelContext();
          result = runTestFunction(context, testFile, name);
          results.add(result);
        }
      }
    }
  }

  private TestResult runTestFunction(final Context context, final File testFile, final Symbol name) {
    String methodName = name.getPrintName().trim() + "()";
    String testName = testFile.getName() + ": " + methodName ;
    logger.info("\t# Running test function {} in {}", methodName, testFile.getName());
    String issue;
    Exception exception;
    TestResult result = new TestResult(testFile);
    try {
      context.evaluate(FunctionCall.newCall(name));
      if (printSuccess) {
        logger.info("\t\t# {}: Success", testName);
      }
      result.setResult(TestResult.OutCome.SUCCESS);
      return result;
    } catch (EvalException e) {
      exception = e;
      issue = e.getClass().getSimpleName() + " executing test " + testName;
    } catch (RuntimeException e) {
      exception = e;
      issue = e.getClass().getSimpleName() + " occurred running R script " + testName;
    } catch (Exception e) {
      exception = e;
      issue = e.getClass().getSimpleName() + " thrown when running script " + testName;
    }
    logger.info("\t\t# {}: Failure detected: {}", testName, formatMessage(exception));
    result.setResult(TestResult.OutCome.FAILURE);
    result.setError(exception);
    result.setIssue(issue);
    return result;
  }

  private TestResult runTest(final File testFile, final RenjinScriptEngine engine) {
    TestResult result = new TestResult(testFile);
    String issue;
    Exception exception;
    String testName = testFile.getName();
    try {
      engine.eval(testFile);
      result.setResult(TestResult.OutCome.SUCCESS);
      if (printSuccess) {
        logger.info("\t# {}: Success", testName);
      }
      return result;
    } catch (org.renjin.parser.ParseException e) {
      exception = e;
      issue = e.getClass().getSimpleName() + " parsing R script " + testName;
    } catch (IOException e) {
      exception = e;
      issue = e.getClass().getSimpleName() + " reading file " + testName;
    } catch (ScriptException | EvalException e) {
      exception = e;
      issue = e.getClass().getSimpleName() + " executing test " + testName;
    } catch (RuntimeException e) {
      exception = e;
      issue = e.getClass().getSimpleName() + " occurred running R script " + testName;
    } catch (Exception e) {
      exception = e;
      issue = e.getClass().getSimpleName() + " thrown when running script " + testName;
    }
    logger.warn("\t# {}: Failure detected: {}", testName, formatMessage(exception));
    result.setResult(TestResult.OutCome.FAILURE);
    result.setError(exception);
    result.setIssue(issue);
    return result;
  }

  private boolean isNoArgsFunction(final SEXP value) {
    if (value instanceof Closure) {
      Closure testFunction = (Closure) value;
      return testFunction.getFormals().length() == 0;
    }
    return false;
  }

  public boolean isTestFailureIgnore() {
    return testFailureIgnore;
  }

  public void setTestFailureIgnore(boolean testFailureIgnore) {
    this.testFailureIgnore = testFailureIgnore;
  }

  public File getReportOutputDirectory() {
    return reportOutputDirectory;
  }

  public void setReportOutputDirectory(File reportOutputDirectory) {
    this.reportOutputDirectory = reportOutputDirectory;
  }

  public MavenProject getProject() {
    return project;
  }

  public File getTestSourceDirectory() {
    return testSourceDirectory;
  }

  public void setTestSourceDirectory(File testSourceDirectory) {
    this.testSourceDirectory = testSourceDirectory;
  }

  public File getTestOutputDirectory() {
    return testOutputDirectory;
  }

  public void setTestOutputDirectory(File testOutputDirectory) {
    this.testOutputDirectory = testOutputDirectory;
  }

  public boolean isSkipTests() {
    return skipTests;
  }

  public void setSkipTests(boolean skipTests) {
    this.skipTests = skipTests;
  }
}
