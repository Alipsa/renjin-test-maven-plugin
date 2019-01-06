package se.alipsa.renjinhamcrestplugin;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;

public class RenjinHamcrestMojoTest extends AbstractMojoTestCase {

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

    RenjinHamcrestMojo myMojo = (RenjinHamcrestMojo) lookupMojo("testR", pom);
    assertNotNull(myMojo);
    myMojo.execute();
  }
}
