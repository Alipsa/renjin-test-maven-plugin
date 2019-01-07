package se.alipsa.renjinhamcrestplugin;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;

import java.io.File;

public class RenjinHamcrestMojoTest extends EnhancedAbstractMojoTestCase {

  /**
   * {@inheritDoc}
   */
  protected void setUp()
      throws Exception {
    // required
    super.setUp();

  }

  /**
   * {@inheritDoc}
   */
  protected void tearDown()
      throws Exception {
    // required
    super.tearDown();
  }

  /**
   * @throws Exception if any
   */
  public void testSomething()
      throws Exception {
    File pom = getTestFile("src/test/resources/plugin/pom.xml");
    assertNotNull(pom);
    assertTrue(pom.exists());

    //RenjinHamcrestMojo myMojo = (RenjinHamcrestMojo) lookupMojo("testR", pom);
    RenjinHamcrestMojo myMojo = (RenjinHamcrestMojo) lookupConfiguredMojo(pom, "testR");
    assertNotNull(myMojo);
    myMojo.execute();
  }
}
