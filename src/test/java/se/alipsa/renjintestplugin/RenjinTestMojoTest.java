package se.alipsa.renjintestplugin;

import static se.alipsa.renjintestplugin.ResourceLocator.getResourceAsFile;

import java.io.File;

public class RenjinTestMojoTest extends EnhancedAbstractMojoTestCase {

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
  public void testRenjinTestMojo()
      throws Exception {
    File pom = getResourceAsFile("testPom.xml");
    //File pom = getTestFile("target/test-classes/pom.xml");
    assertNotNull("Failed to find test pom", pom);
    assertTrue("pom file does not exist", pom.exists());


    //RenjinHamcrestMojo myMojo = (RenjinHamcrestMojo) lookupMojo("testR", pom);
    RenjinTestMojo myMojo = (RenjinTestMojo) lookupConfiguredMojo(pom, "testR");
    assertNotNull(myMojo);
    myMojo.execute();
  }

  public void testSomethingThatFails() {
    fail("This is a deliberate failure");
  }
}
