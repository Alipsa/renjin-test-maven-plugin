package se.alipsa.renjinhamcrestplugin;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class TestSample1 {

  @Test
  public void testMethod1() {
    assertTrue(true);
  }

  @Test
  public void testMethod2() {
    System.out.println("Some output from testMethod2");
    assertTrue(true);
  }
}
