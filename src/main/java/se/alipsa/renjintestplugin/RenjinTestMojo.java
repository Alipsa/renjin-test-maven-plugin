package se.alipsa.renjintestplugin;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
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

import static se.alipsa.renjintestplugin.TestResultPrinter.formatMessage;

/**
 * Goal which runs Renjin tests.
 */
@Mojo(name = "testR",
    defaultPhase = LifecyclePhase.TEST,
    requiresDependencyResolution = ResolutionScope.TEST,
    requiresProject = true
)
public class RenjinTestMojo extends AbstractMojo {

  @Parameter(name = "reportOutputDirectory", property = "testR.reportOutputDirectory",
      defaultValue = "${project.build.directory}/renjin-test-reports", required = true)
  private File reportOutputDirectory;

  @Parameter( defaultValue = "${project}", readonly = true )
  private MavenProject project;

  @Parameter(name = "testSourceDirectory", property = "testR.testSourceDirectory",
      defaultValue = "${project.basedir}/src/test/R", required = true)
  private File testSourceDirectory;

  @Parameter(name = "testResourceDirectory", property = "testR.testResourceDirectory",
      defaultValue = "${project.basedir}/src/test/resources")
  private File testResourceDirectory;

  @Parameter(name = "testOutputDirectory", property = "testR.testOutputDirectory",
      defaultValue = "${project.build.testOutputDirectory}", required = true)
  private File testOutputDirectory;

  @Parameter(name = "sourceDirectory", property = "testR.sourceDirectory",
      defaultValue = "${project.basedir}/src/main/R", required = true)
  private File sourceDirectory;

  @Parameter(name = "runSourceScriptsBeforeTests", property = "testR.runSourceScriptsBeforeTests", defaultValue = "false")
  private boolean runSourceScriptsBeforeTests;

  @Parameter(name = "skipTests", property = "testR.skipTests", defaultValue = "false")
  private boolean skipTests;

  @Parameter(name = "testFailureIgnore", property = "testR.testFailureIgnore", defaultValue = "false")
  private boolean testFailureIgnore;

  @Parameter(name = "printSuccess", property = "testR.printSuccess", defaultValue = "false")
  private boolean printSuccess;

  /**
   * key value pairs e.g. library('xmlr')
   */
  @Parameter(name = "replaceStringsWhenCopy", property = "testR.replaceStringsWhenCopy")
  private Properties replaceStringsWhenCopy;

  private final Logger logger = LoggerFactory.getLogger(RenjinTestMojo.class);
  private ClassLoader classLoader;
  private final String[] extensions = new String[]{"R", "r", "S", "s"};
  private List<TestResult> results;
  private RenjinScriptEngineFactory factory;
  private Session session;

  public void execute() throws MojoExecutionException, MojoFailureException {
    if (isSkipTests()) {
      logger.info("Renjin Tests are skipped");
      return;
    }
    logger.info("");
    logger.info("--------------------------------------------------------");
    logger.info("               RENJIN TESTS");
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
      String[] files = testOutputDirectory.list();
      if (testOutputDirectory.exists() && files != null && files.length > 0) {
        logger.info("Cleaning up after previous run...");
        FileUtils.listFiles(testOutputDirectory, extensions, true).forEach(File::delete);
      }

      logger.debug("replaceStringsWhenCopy = {}", replaceStringsWhenCopy);
      // If there was R files in the test resource dir,
      // they would have been deleted in the cleanup above so we need to restore
      IOFileFilter suffixFilter = new SuffixFileFilter(extensions);
      FileFilter filter = FileFilterUtils.or(DirectoryFileFilter.DIRECTORY, suffixFilter);

      if (testResourceDirectory != null && testResourceDirectory.exists()) {
        try {
          logger.info("Copying testResourceDirectory = {} to testOutputDirectory = {}",
              testResourceDirectory, testOutputDirectory);
          if (replaceStringsWhenCopy != null && replaceStringsWhenCopy.size() > 0) {
            StringReplacementFileCopier.copyDirectory(testResourceDirectory, testOutputDirectory, filter, replaceStringsWhenCopy);
          } else {
            FileUtils.copyDirectory(testResourceDirectory, testOutputDirectory, filter);
          }
        } catch (IOException e) {
          throw new MojoExecutionException("Failed to copy files from " + testResourceDirectory + " to " + testOutputDirectory, e);
        }
      }
      logger.info("Copying {} to {}", testSourceDirectory, testOutputDirectory);
      if (testSourceDirectory.exists()) {
        if (replaceStringsWhenCopy != null && replaceStringsWhenCopy.size() > 0) {
          StringReplacementFileCopier.copyDirectory(testSourceDirectory, testOutputDirectory, filter, replaceStringsWhenCopy);
        } else {
          FileUtils.copyDirectory(testSourceDirectory, testOutputDirectory);
        }
      } else {
        logger.info("No test files found in test source directory {}", testSourceDirectory);
        return;
      }

    } catch (IOException e) {
      throw new MojoExecutionException("Failed to copy files from " + testSourceDirectory + " to " + testOutputDirectory, e);
    }

    List<URL> runtimeUrls = new ArrayList<>();
    try {
      // Add classpath from calling pom, i.e. compile + system + provided + runtime + test
      for (String element : project.getTestClasspathElements()) {
        runtimeUrls.add(new File(element).toURI().toURL());
      }
    } catch (DependencyResolutionRequiredException | MalformedURLException e) {
      throw new MojoExecutionException("Failed to set up classLoader", e);
    }
    classLoader = new URLClassLoader(runtimeUrls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());

    factory = new RenjinScriptEngineFactory();
    SessionBuilder builder = new SessionBuilder();
    session = builder
        .withDefaultPackages()
        .setClassLoader(classLoader) //allows imports in r code to work
        .build();

    results = new ArrayList<>();

    if (runSourceScriptsBeforeTests) {
      Collection<File> sourceFiles = FileUtils.listFiles(sourceDirectory, extensions, true);
      for (File sourceFile : sourceFiles) {
        runRscript(sourceFile);
      }
    }

    Collection<File> testFiles = FileUtils.listFiles(testOutputDirectory, extensions, true);

    Iterator<File> it = testFiles.iterator();
    while(it.hasNext()) {
      File testFile = it.next();
      boolean didRun = runTestFile(testFile);
      // We should not report on tests that did not run
      if (!didRun) {
        it.remove();
      }
    }

    TestResultPrinter.printResultToConsole(logger, results, testFailureIgnore, testFiles);
    TestResultPrinter.printResultsToFile(reportOutputDirectory, testOutputDirectory, results, testFailureIgnore);
  }

  private void runRscript(final File sourceFile) throws MojoExecutionException {
    String sourceName = sourceFile.getName();

    logger.info("");
    logger.info("# Running src script {}", sourceName);
    RenjinScriptEngine engine = factory.getScriptEngine(session);
    try {
      engine.getSession().setWorkingDirectory(sourceFile.getParentFile());
      engine.eval(sourceFile);
    } catch (Exception e) {
      throw new MojoExecutionException("Failed to run rscript " + sourceFile.getAbsolutePath(), e);
    }
  }

  private boolean runTestFile(final File testFile) throws MojoExecutionException {
    // Skip testthat tests, they will be executed by the base testthat.R
    if ("testthat".equals(testFile.getParentFile().getName())) {
      return false;
    }

    String testName = testFile.getAbsolutePath().substring(testOutputDirectory.getAbsolutePath().length() + 1);
    logger.info("");
    logger.info("# Running {}", testName);

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
      if (methodName.startsWith("test.") || methodName.startsWith("test_check")) {
        SEXP value = session.getGlobalEnvironment().getVariable(session.getTopLevelContext(), name);
        if (isNoArgsFunction(value)) {
          Context context = session.getTopLevelContext();
          result = runTestFunction(context, testFile, name);
          results.add(result);
          try {
            engine.eval("rm(" + name.getPrintName().trim() + ")");
          } catch (ScriptException e) {
            throw new MojoExecutionException("Failed to remove test method after execution", e);
          }
        }
      }
    }
    return true;
  }

  private TestResult runTestFunction(final Context context, final File testFile, final Symbol name) {
    TestResult result = new TestResult(testFile);
    result.setStartTime(System.currentTimeMillis());
    String methodName = name.getPrintName().trim() + "()";
    String testName = testFile.getName() + ": " + methodName ;
    logger.info("\t# Running test function {} in {}", methodName, testFile.getName());
    String issue;
    Exception exception;
    result.setTestMethod(methodName);
    try {
      context.evaluate(FunctionCall.newCall(name));
      if (printSuccess) {
        logger.info("\t\t# {}: Success", testName);
      }
      result.setResult(TestResult.OutCome.SUCCESS);
      result.setEndTime(System.currentTimeMillis());
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
    result.setEndTime(System.currentTimeMillis());
    return result;
  }

  private TestResult runTest(final File testFile, final RenjinScriptEngine engine) {
    TestResult result = new TestResult(testFile);
    String methodName = testFile.getName();
    methodName = methodName.substring(0, methodName.lastIndexOf("."));
    result.setStartTime(System.currentTimeMillis());
    result.setTestMethod(methodName + "()");
    String issue;
    Exception exception;
    String testName = testFile.getName();
    try {
      engine.eval(testFile);
      result.setResult(TestResult.OutCome.SUCCESS);
      if (printSuccess) {
        logger.info("\t# {}: Success", testName);
      }
      result.setEndTime(System.currentTimeMillis());
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
    result.setEndTime(System.currentTimeMillis());
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
    boolean syspropSkip = System.getProperty("skipTests") != null
                          && !System.getProperty("skipTests").equalsIgnoreCase("false");
    return skipTests || syspropSkip;
  }

  public void setSkipTests(boolean skipTests) {
    this.skipTests = skipTests;
  }
}
