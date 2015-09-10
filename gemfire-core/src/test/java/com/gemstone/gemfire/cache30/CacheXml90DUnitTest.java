/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
/**
 * 
 */
package com.gemstone.gemfire.cache30;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.distributed.internal.DistributionConfig;
import com.gemstone.gemfire.internal.cache.LocalRegion;
import com.gemstone.gemfire.internal.cache.xmlcache.CacheCreation;
import com.gemstone.gemfire.internal.cache.xmlcache.CacheXml;
import com.gemstone.gemfire.internal.cache.xmlcache.RegionAttributesCreation;
import com.gemstone.gemfire.internal.cache.xmlcache.ResourceManagerCreation;


public class CacheXml90DUnitTest extends CacheXml81DUnitTest {
  private static final long serialVersionUID = -6437436147079728413L;

  public CacheXml90DUnitTest(String name) {
    super(name);
  }

  
  // ////// Helper methods

  protected String getGemFireVersion()
  {
    return CacheXml.VERSION_9_0;
  }

  @SuppressWarnings("rawtypes")
  public void testEnableOffHeapMemory() {
    try {
      System.setProperty("gemfire."+DistributionConfig.OFF_HEAP_MEMORY_SIZE_NAME, "1m");
      
      final String regionName = "testEnableOffHeapMemory";
      
      final CacheCreation cache = new CacheCreation();
      final RegionAttributesCreation attrs = new RegionAttributesCreation(cache);
      attrs.setOffHeap(true);
      assertEquals(true, attrs.getOffHeap());
      
      final Region regionBefore = cache.createRegion(regionName, attrs);
      assertNotNull(regionBefore);
      assertEquals(true, regionBefore.getAttributes().getOffHeap());
  
      testXml(cache);
      
      final Cache c = getCache();
      assertNotNull(c);
  
      final Region regionAfter = c.getRegion(regionName);
      assertNotNull(regionAfter);
      assertEquals(true, regionAfter.getAttributes().getOffHeap());
      assertEquals(true, ((LocalRegion)regionAfter).getOffHeap());
      regionAfter.localDestroyRegion();
    } finally {
      System.clearProperty("gemfire."+DistributionConfig.OFF_HEAP_MEMORY_SIZE_NAME);
    }
  }

  @SuppressWarnings("rawtypes")
  public void testEnableOffHeapMemoryRootRegionWithoutOffHeapMemoryThrowsException() {
    final String regionName = getUniqueName();
    
    final CacheCreation cache = new CacheCreation();
    final RegionAttributesCreation attrs = new RegionAttributesCreation(cache);
    attrs.setOffHeap(true);
    assertEquals(true, attrs.getOffHeap());
    
    final Region regionBefore = cache.createRegion(regionName, attrs);
    assertNotNull(regionBefore);
    assertEquals(true, regionBefore.getAttributes().getOffHeap());

    try {
      testXml(cache);
    } catch (IllegalStateException e) {
      // expected
      String msg = "The region /" + regionName + " was configured to use off heap memory but no off heap memory was configured.";
      assertEquals(msg, e.getMessage());
    }
  }
  
  @SuppressWarnings({ "rawtypes", "deprecation", "unchecked" })
  public void testEnableOffHeapMemorySubRegionWithoutOffHeapMemoryThrowsException() {
    final String rootRegionName = getUniqueName();
    final String subRegionName = "subRegion";
    
    final CacheCreation cache = new CacheCreation();
    final RegionAttributesCreation rootRegionAttrs = new RegionAttributesCreation(cache);
    assertEquals(false, rootRegionAttrs.getOffHeap());
    
    final Region rootRegionBefore = cache.createRegion(rootRegionName, rootRegionAttrs);
    assertNotNull(rootRegionBefore);
    assertEquals(false, rootRegionBefore.getAttributes().getOffHeap());
    
    final RegionAttributesCreation subRegionAttrs = new RegionAttributesCreation(cache);
    subRegionAttrs.setOffHeap(true);
    assertEquals(true, subRegionAttrs.getOffHeap());
    
    final Region subRegionBefore = rootRegionBefore.createSubregion(subRegionName, subRegionAttrs);
    assertNotNull(subRegionBefore);
    assertEquals(true, subRegionBefore.getAttributes().getOffHeap());

    try {
      testXml(cache);
    } catch (IllegalStateException e) {
      // expected
      final String msg = "The region /" + rootRegionName + "/" + subRegionName +
          " was configured to use off heap memory but no off heap memory was configured.";
      assertEquals(msg, e.getMessage());
    }
  }

  /**
   * Test the ResourceManager element's critical-off-heap-percentage and 
   * eviction-off-heap-percentage attributes
   * @throws Exception
   */
  public void testResourceManagerThresholds() throws Exception {
    CacheCreation cache = new CacheCreation();
    final float low = 90.0f;
    final float high = 95.0f;

    try {
      System.setProperty("gemfire."+DistributionConfig.OFF_HEAP_MEMORY_SIZE_NAME, "1m");

      Cache c;
      ResourceManagerCreation rmc = new ResourceManagerCreation();
      rmc.setEvictionOffHeapPercentage(low);
      rmc.setCriticalOffHeapPercentage(high);
      cache.setResourceManagerCreation(rmc);
      testXml(cache);
      {
        c = getCache();
        assertEquals(low, c.getResourceManager().getEvictionOffHeapPercentage());
        assertEquals(high, c.getResourceManager().getCriticalOffHeapPercentage());
      }
      closeCache();
      
      rmc = new ResourceManagerCreation();
      // Set them to similar values
      rmc.setEvictionOffHeapPercentage(low);
      rmc.setCriticalOffHeapPercentage(low + 1);
      cache.setResourceManagerCreation(rmc);
      testXml(cache);
      {
        c = getCache();
        assertEquals(low, c.getResourceManager().getEvictionOffHeapPercentage());
        assertEquals(low + 1, c.getResourceManager().getCriticalOffHeapPercentage());
      }
      closeCache();
  
      rmc = new ResourceManagerCreation();
      rmc.setEvictionOffHeapPercentage(high);
      rmc.setCriticalOffHeapPercentage(low);
      cache.setResourceManagerCreation(rmc);
      try {
        testXml(cache);
        assertTrue(false);
      } catch (IllegalArgumentException expected) {
      } finally {
        closeCache();
      }
  
      // Disable eviction
      rmc = new ResourceManagerCreation();
      rmc.setEvictionOffHeapPercentage(0);
      rmc.setCriticalOffHeapPercentage(low);
      cache.setResourceManagerCreation(rmc);
      testXml(cache);
      {
        c = getCache();
        assertEquals(0f, c.getResourceManager().getEvictionOffHeapPercentage());
        assertEquals(low, c.getResourceManager().getCriticalOffHeapPercentage());
      }
      closeCache();
  
      // Disable refusing ops in "red zone"
      rmc = new ResourceManagerCreation();
      rmc.setEvictionOffHeapPercentage(low);
      rmc.setCriticalOffHeapPercentage(0);
      cache.setResourceManagerCreation(rmc);
      testXml(cache);
      {
        c = getCache();
        assertEquals(low, c.getResourceManager().getEvictionOffHeapPercentage());
        assertEquals(0f, c.getResourceManager().getCriticalOffHeapPercentage());
      }
      closeCache();
  
      // Disable both
      rmc = new ResourceManagerCreation();
      rmc.setEvictionOffHeapPercentage(0);
      rmc.setCriticalOffHeapPercentage(0);
      cache.setResourceManagerCreation(rmc);
      testXml(cache);
      c = getCache();
      assertEquals(0f, c.getResourceManager().getEvictionOffHeapPercentage());
      assertEquals(0f, c.getResourceManager().getCriticalOffHeapPercentage());
    } finally {
      System.clearProperty("gemfire."+DistributionConfig.OFF_HEAP_MEMORY_SIZE_NAME);
    }
  }
}
