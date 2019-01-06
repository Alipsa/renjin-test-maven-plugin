package se.alipsa.renjinhamcrestplugin;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Goal which runs Renjin Hamcrest test.
 */
@Mojo(name = "testR",
    defaultPhase = LifecyclePhase.TEST,
    requiresDependencyResolution = ResolutionScope.TEST,
    requiresProject = true
)
public class RenjinHamcrestMojo extends AbstractMojo {

  /*
  static {
    System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "warn");
    java.util.logging.Logger utilLog = java.util.logging.LogManager.getLogManager().getLogger("org.apache.commons.vfs");
    if (utilLog != null) {
      utilLog.setLevel(java.util.logging.Level.WARNING);
    }
    java.util.logging.Logger utilLog2 = java.util.logging.LogManager.getLogManager().getLogger("org.apache.commons.vfs2");
    if (utilLog2 != null) {
      utilLog2.setLevel(java.util.logging.Level.WARNING);
    }
  }
  */

  Logger logger = LoggerFactory.getLogger(RenjinHamcrestMojo.class);
  ClassLoader classLoader;
  String[] extensions = new String[]{"R", "S"};
  List<TestResult> results;
  @Parameter(name = "outputDirectory", property = "testR.outputDirectory",
      defaultValue = "${project.build.outputDirectory}/renjin-hamcrest-test-reports", required = true)
  private File outputDirectory;

  //@Parameter( defaultValue="${project}", readonly = true, required = true)
  //private MavenProject project;
  @Parameter(name = "testSourceDirectory", property = "testR.testSourceDirectory",
      defaultValue = "${project.basedir}/src/test/R", required = true)
  private File testSourceDirectory;
  @Parameter(name = "skipTests", property = "testR.skipTests", defaultValue = "false")
  private boolean skipTests;
  @Parameter(name = "testFailureIgnore", property = "testR.testFailureIgnore", defaultValue = "false")
  private boolean testFailureIgnore;
  private RenjinScriptEngineFactory factory;

  public void execute() throws MojoExecutionException, MojoFailureException {

    /*
    if (project == null) {
      throw new MojoExecutionException("MavenProject is null, cannot continue");
    }*/
    if (outputDirectory == null) {
      throw new MojoExecutionException("outputDirectory is null, cannot continue");
    }
    if (testSourceDirectory == null) {
      throw new MojoExecutionException("testSourceDirectory is null, cannot continue");
    }

    factory = new RenjinScriptEngineFactory();

    /*
    URL[] runtimeUrls = new URL[0];
    try {
      List runtimeClasspathElements = project.getRuntimeClasspathElements();
      runtimeUrls = new URL[runtimeClasspathElements.size()];
      for (int i = 0; i < runtimeClasspathElements.size(); i++) {
        String element = (String) runtimeClasspathElements.get(i);
        runtimeUrls[i] = new File(element).toURI().toURL();
      }
    } catch (DependencyResolutionRequiredException | MalformedURLException e) {
      throw new MojoExecutionException("Failed to set up classLoader", e);
    }
    classLoader = new URLClassLoader(runtimeUrls,
        Thread.currentThread().getContextClassLoader());
    */
    classLoader = Thread.currentThread().getContextClassLoader();
    results = new ArrayList<>();

    logger.info("");
    logger.info("--------------------------------------------------------");
    logger.info("               RENJIN HAMCREST TESTS");
    logger.info("--------------------------------------------------------");

    if (!outputDirectory.exists()) {
      outputDirectory.mkdirs();
    }

    // throw new MojoExecutionException("Error creating file " + touch, e);
    Collection<File> testFiles = FileUtils.listFiles(testSourceDirectory, extensions, true);

    for (File testFile : testFiles) {
      runTestFile(testFile);

    }

    long successes = results.stream().filter(p -> p.getResult().equals(TestResult.OutCome.SUCCESS)).count();
    long failures = results.stream().filter(p -> p.getResult().equals(TestResult.OutCome.FAILURE)).count();
    long errors = results.stream().filter(p -> p.getResult().equals(TestResult.OutCome.ERROR)).count();
    logger.info("");
    logger.info("R tests summary:");
    logger.info("----------------");
    logger.info("{} Files executed, Tests run: {}, Sucesses: {}, Failures: {}, Errors: {}",
        testFiles.size(), results.size(), successes, failures, errors);

    boolean errorsDuringTests = failures > 0 || errors > 0;
    if (errorsDuringTests) {
      logger.info("");
      logger.info("Results: ");
      for (TestResult result : results) {
        if (result.getResult().equals(TestResult.OutCome.SUCCESS)) {
          continue;
        }
        String errMsg = formatMessage(result.getError());
        logger.warn("\t{} : {} : {}",
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

      throw new MojoFailureException("There were " + failures + " failures and " + errors + " errors"
          , errorResult.error);
    }
  }

  private String formatMessage(final Throwable error) {
    return error.getMessage().trim().replace("\n", ", ");
  }

  private void runTestFile(final File testFile) {

    String testName = testFile.toString();
    logger.info("");
    logger.info("# Running test file {}", testName);

    SessionBuilder builder = new SessionBuilder();
    Session session = builder
        .withDefaultPackages()
        .setClassLoader(classLoader) //allows imports in r code to work
        .build();
    RenjinScriptEngine engine = factory.getScriptEngine(session);
    // First run any test that are not defined as functions
    TestResult result = runTest(testFile, engine);
    results.add(result);

    //now run each testFunction in that file, in the same Session
    String orgTestName = testName;
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
    String methodName = name.getPrintName().trim();
    String testName = testFile + ": " + methodName + "()";
    logger.info("\t# Running test function {} in {}", methodName, testFile.getName());
    String issue;
    Exception exception;
    TestResult result = new TestResult(testFile);
    try {
      context.evaluate(FunctionCall.newCall(name));
      logger.debug("\t\t# {}: Success", testName);
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
    String testName = testFile.toString();
    try {
      engine.eval(testFile);
      result.setResult(TestResult.OutCome.SUCCESS);
      logger.debug("\t# {}: Success", testName);
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
    logger.info("\t# {}: Failure detected: {}", testName, formatMessage(exception));
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
}
