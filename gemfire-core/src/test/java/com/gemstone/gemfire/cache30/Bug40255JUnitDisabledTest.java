/*=========================================================================
 * Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */

package com.gemstone.gemfire.cache30;

import java.io.File;
import java.util.Properties;

import junit.framework.TestCase;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionAttributes;
import com.gemstone.gemfire.distributed.DistributedSystem;
import com.gemstone.gemfire.distributed.internal.DistributionConfig;

/**
 * @author Shobhit Agarwal
 *
 */
public class Bug40255JUnitDisabledTest extends TestCase{

  private static final String BUG_40255_XML = Bug40255JUnitDisabledTest.class.getResource("bug40255xmlparameterization.xml").getFile();
  private static final String BUG_40255_PROPS = Bug40255JUnitDisabledTest.class.getResource("bug40255_gemfire.properties").getFile();

  private static final String ATTR_PROPERTY_STRING = "region.disk.store";

  private static final String ATTR_PROPERTY_VALUE = "teststore";

  private static final String NESTED_ATTR_PROPERTY_STRING = "custom-nested.test";

  private static final String NESTED_ATTR_PROPERTY_VALUE = "disk";

  private static final String ELEMENT_PROPERTY_STRING = "custom-string.element";

  private static final String ELEMENT_PROPERTY_VALUE = "example-string";

  private static final String CONCAT_ELEMENT_PROPERTY_STRING = "concat.test";

  private static final String CONCAT_ELEMENT_PROPERTY_VALUE = "-name";

  private static final String ELEMENT_KEY_VALUE = "example-value";

  DistributedSystem ds;
  Cache cache;

  @Override
  public void setName(String name) {
    super.setName(name);
  }

  public void testResolveReplacePropertyStringForLonerCache(){
    Properties props = new Properties();
    props.setProperty("mcast-port", "0");
    props.setProperty("locators", "");
    System.setProperty("gemfirePropertyFile", BUG_40255_PROPS);
    props.setProperty(DistributionConfig.CACHE_XML_FILE_NAME, BUG_40255_XML);
    System.setProperty(NESTED_ATTR_PROPERTY_STRING, NESTED_ATTR_PROPERTY_VALUE);
    System.setProperty(ATTR_PROPERTY_STRING, ATTR_PROPERTY_VALUE);
    System.setProperty(ELEMENT_PROPERTY_STRING, ELEMENT_PROPERTY_VALUE);
    System.setProperty(CONCAT_ELEMENT_PROPERTY_STRING, CONCAT_ELEMENT_PROPERTY_VALUE);
    
    // create the directory where data is going to be stored
    File dir = new File("persistData1");
    dir.mkdir();

    this.ds = DistributedSystem.connect(props);
    this.cache = CacheFactory.create(this.ds);

    Region exampleRegion = this.cache.getRegion("example-region");
    RegionAttributes<Object, Object> attrs = exampleRegion.getAttributes();

    //Check if disk store got same name as passed in system properties in setup().
    assertEquals(attrs.getDiskStoreName(), System.getProperty(ATTR_PROPERTY_STRING));
    assertNotNull(exampleRegion.get(ELEMENT_PROPERTY_VALUE+CONCAT_ELEMENT_PROPERTY_VALUE));
    assertEquals(exampleRegion.get(ELEMENT_PROPERTY_VALUE+CONCAT_ELEMENT_PROPERTY_VALUE), ELEMENT_KEY_VALUE);
    assertNotNull(exampleRegion.get(ELEMENT_PROPERTY_VALUE));
    assertEquals(exampleRegion.get(ELEMENT_PROPERTY_VALUE), CONCAT_ELEMENT_PROPERTY_VALUE);
  }

  public void testResolveReplacePropertyStringForNonLonerCache(){
    Properties props = new Properties();
    props.setProperty("mcast-port", "10333");
    props.setProperty("locators", "");
    System.setProperty("gemfirePropertyFile", BUG_40255_PROPS);
    props.setProperty(DistributionConfig.CACHE_XML_FILE_NAME, BUG_40255_XML);
    System.setProperty(NESTED_ATTR_PROPERTY_STRING, NESTED_ATTR_PROPERTY_VALUE);
    System.setProperty(ATTR_PROPERTY_STRING, ATTR_PROPERTY_VALUE);
    System.setProperty(ELEMENT_PROPERTY_STRING, ELEMENT_PROPERTY_VALUE);
    System.setProperty(CONCAT_ELEMENT_PROPERTY_STRING, CONCAT_ELEMENT_PROPERTY_VALUE);
    
    // create the directory where data is going to be stored
    File dir = new File("persistData1");
    dir.mkdir();

    this.ds = DistributedSystem.connect(props);
    this.cache = CacheFactory.create(this.ds);

    Region exampleRegion = this.cache.getRegion("example-region");
    RegionAttributes<Object, Object> attrs = exampleRegion.getAttributes();

    //Check if disk store got same name as passed in system properties in setup().
    assertEquals(attrs.getDiskStoreName(), System.getProperty(ATTR_PROPERTY_STRING));
    assertNotNull(exampleRegion.get(ELEMENT_PROPERTY_VALUE+CONCAT_ELEMENT_PROPERTY_VALUE));
    assertEquals(exampleRegion.get(ELEMENT_PROPERTY_VALUE+CONCAT_ELEMENT_PROPERTY_VALUE), ELEMENT_KEY_VALUE);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    if (this.cache != null) {
      this.cache.close();
      this.cache = null;
    }
    if (this.ds != null) {
      this.ds.disconnect();
      this.ds = null;
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }
}
