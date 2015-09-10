/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal;

import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.distributed.internal.DistributionConfig;
import com.gemstone.gemfire.distributed.internal.DistributionConfigImpl;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;

@Category(IntegrationTest.class)
public class GemFireVersionIntegrationJUnitTest {

  /**
   * test that the gemfire.properties generated by default is able
   * to start a server
   */
  @Test
  public void testDefaultConfig() throws IOException {
    String[] args = new String[1];
    args[0] = "gf"+System.nanoTime()+".properties";
    DistributionConfigImpl.main(args);
    Properties props = new Properties();
    props.load(new FileInputStream(args[0]));
    props.setProperty(DistributionConfig.MCAST_PORT_NAME, "0");
    CacheFactory cacheFactory = new CacheFactory(props);
    Cache c = cacheFactory.create();
    assertNotNull(c);
    c.close();
  }
}
