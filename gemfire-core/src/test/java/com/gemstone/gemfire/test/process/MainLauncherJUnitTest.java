package com.gemstone.gemfire.test.process;

import static org.junit.Assert.*;
import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.*;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;
import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.test.junit.categories.UnitTest;

/**
 * Quick sanity tests to make sure MainLauncher is functional.
 * 
 * @author Kirk Lund
 */
@Category(UnitTest.class)
public class MainLauncherJUnitTest {

  private static final long TIMEOUT_SECONDS = 10;

  @Rule
  public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();
  
  private static volatile boolean flag = false;
  
  private final String launchedClass = getClass().getName();
  private ExecutorService futures;

  @Before
  public void before() {
    flag = false;
    this.futures = Executors.newSingleThreadExecutor();
    assertFalse(flag);
  }
  
  @After
  public void after() {
    flag = false;
    assertTrue(this.futures.shutdownNow().isEmpty());
  }
  
  @Test
  public void testInvokeMainWithNullArgs() throws Exception {
    Class<?> clazz = getClass();
    Method mainMethod = clazz.getMethod("main", String[].class);
    String[] args = null;
    mainMethod.invoke(null, new Object[] { args });
    assertTrue(flag);
  }
  
  @Test
  public void testInvokeMainWithEmptyArgs() throws Exception {
    Class<?> clazz = getClass();
    Method mainMethod = clazz.getMethod("main", String[].class);
    String[] args = new String[0];
    mainMethod.invoke(null, new Object[] { args });
    assertTrue(flag);
  }
  
  @Test
  public void testInvokeMainWithOneArg() throws Exception {
    Class<?> clazz = getClass();
    Method mainMethod = clazz.getMethod("main", String[].class);
    String[] args = new String[] { "arg0" };
    mainMethod.invoke(null, new Object[] { args });
    assertTrue(flag);
  }
  
  @Test
  public void testInvokeMainWithTwoArgs() throws Exception {
    Class<?> clazz = getClass();
    Method mainMethod = clazz.getMethod("main", String[].class);
    String[] args = new String[] { "arg0", "arg1" };
    mainMethod.invoke(null, new Object[] { args });
    assertTrue(flag);
  }
  
  @Test
  public void testInvokeMainWithMainLauncherWithNoArgs() throws Exception {
    Future<Boolean> future = this.futures.submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        Class<?> clazz = MainLauncher.class;
        Method mainMethod = clazz.getMethod("main", String[].class);
        String[] args = new String[] { launchedClass };
        mainMethod.invoke(null, new Object[] { args }); // this will block until "\n" is fed to System.in
        return true;
      }
    });
    systemInMock.provideText("\n");
    assertTrue(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertTrue(flag);
  }
  
  @Test
  public void testInvokeMainWithMainLauncherWithOneArg() throws Exception {
    Future<Boolean> future = this.futures.submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        Class<?> clazz = MainLauncher.class;
        Method mainMethod = clazz.getMethod("main", String[].class);
        String[] args = new String[] { launchedClass, "arg0" };
        mainMethod.invoke(null, new Object[] { args });
        return true;
      }
    });
    systemInMock.provideText("\n");
    assertTrue(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertTrue(flag);
  }
  
  @Test
  public void testInvokeMainWithMainLauncherWithTwoArgs() throws Exception {
    Future<Boolean> future = this.futures.submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        Class<?> clazz = MainLauncher.class;
        Method mainMethod = clazz.getMethod("main", String[].class);
        String[] args = new String[] { launchedClass, "arg0", "arg1" };
        mainMethod.invoke(null, new Object[] { args });
        return true;
      }
    });
    systemInMock.provideText("\n");
    assertTrue(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    assertTrue(flag);
  }
  
  public static void main(String... args) throws Exception {
    flag = true;
  }
}