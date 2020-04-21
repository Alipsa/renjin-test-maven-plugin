package se.alipsa.renjintestplugin;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class TestSample2 {

  @Test
  public void testTestSample2Method1() {
    assertTrue(true);
  }

  @Test
  public void testTestSample2Method2() {
    System.out.println("Some output from TestSample2testMethod2");
    assertTrue(true);
  }
}
