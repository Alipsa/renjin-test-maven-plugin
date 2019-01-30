package se.alipsa.renjinhamcrestplugin;

import static se.alipsa.renjinhamcrestplugin.ResourceLocator.getResourceAsFile;

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
  public void testRenjinHamcrestMojo()
      throws Exception {
    File pom = getResourceAsFile("testPom.xml");
    //File pom = getTestFile("target/test-classes/pom.xml");
    assertNotNull("Failed to find test pom", pom);
    assertTrue("pom file does not exist", pom.exists());


    //RenjinHamcrestMojo myMojo = (RenjinHamcrestMojo) lookupMojo("testR", pom);
    RenjinHamcrestMojo myMojo = (RenjinHamcrestMojo) lookupConfiguredMojo(pom, "testR");
    assertNotNull(myMojo);
    myMojo.execute();
  }

  public void testSomethingThatFails() {
    fail("This is a deliberate failure");
  }
}
