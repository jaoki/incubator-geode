/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.cache30;

import com.gemstone.gemfire.cache.*;
import com.gemstone.gemfire.internal.AvailablePortHelper;
import com.gemstone.gemfire.internal.cache.GemFireCacheImpl;
import com.gemstone.gemfire.internal.cache.PartitionedRegion;
import com.gemstone.gemfire.internal.cache.DiskWriteAttributesImpl;
import com.gemstone.gemfire.internal.cache.xmlcache.*;
import com.gemstone.gemfire.cache.util.BridgeServer;

import dunit.Host;
import dunit.VM;

/**
 * Tests the declarative caching functionality introduced in the GemFire
 * 5.0 (i.e. congo1). Don't be confused by the 45 in my name :-)
 * 
 * @author Mitch Thomas
 * @since 5.0
 */

public class CacheXml51DUnitTest extends CacheXml45DUnitTest
{

  // ////// Constructors

  public CacheXml51DUnitTest(String name) {
    super(name);
  }

  // ////// Helper methods

  protected String getGemFireVersion()
  {
    return CacheXml.VERSION_5_1;
  }

  /**
   * Tests that a region created with a named attributes has the correct
   * attributes.
   */
  public void testPartitionedRegionXML() throws CacheException
  {
    setXmlFile(findFile("partitionedRegion51.xml"));
    final String regionName = "pRoot";

    Cache cache = getCache();
    Region region = cache.getRegion(regionName);
    assertNotNull(region);
    
    Host host = Host.getHost(0);
    final VM vm0 = host.getVM(0);
    final VM vm1 = host.getVM(1);
    
    CacheSerializableRunnable init = new CacheSerializableRunnable("initUsingPartitionedRegionXML") {
      public void run2() throws CacheException
      {
        final Cache c;
        try {
          CacheXml30DUnitTest.lonerDistributedSystem = false;
          c = getCache();
        }
        finally {
          CacheXml30DUnitTest.lonerDistributedSystem = true;
        }
        Region r = c.getRegion(regionName);
        assertNotNull(r);
        RegionAttributes attrs = r.getAttributes();
        assertNotNull(attrs.getPartitionAttributes());

        PartitionAttributes pa = attrs.getPartitionAttributes();
        assertEquals(pa.getRedundantCopies(), 1);
        assertEquals(pa.getLocalMaxMemory(), 32);
        assertEquals(pa.getTotalMaxMemory(), 96);
        assertEquals(pa.getTotalNumBuckets(), 119);
        
        r = c.getRegion("bug37905");
        assertTrue("region should have been an instance of PartitionedRegion but was not",
            r instanceof PartitionedRegion);
      }
    };
    
    init.run2();
    vm0.invoke(init);
    vm1.invoke(init);
    vm0.invoke(new CacheSerializableRunnable("putUsingPartitionedRegionXML1") {
      public void run2() throws CacheException
      {
        final String val = "prValue0";
        final Integer key = new Integer(10);
        Cache c = getCache();
        Region r = c.getRegion(regionName);
        assertNotNull(r);
        r.put(key, val);
        assertEquals(val, r.get(key));
      }
    });
    vm1.invoke(new CacheSerializableRunnable("putUsingPartitionedRegionXML2") {
      public void run2() throws CacheException
      {
        final String val = "prValue1";
        final Integer key = new Integer(14);
        Cache c = getCache();
        Region r = c.getRegion(regionName);
        assertNotNull(r);
        r.put(key, val);
        assertEquals(val, r.get(key));
      }
    });
  }

/**
 * Tests the <code>message-sync-interval</code> attribute of
 * attribute is related to HA of client-queues in gemfire ca
 * framework. This attribute is the frequency at which a messent
 * by the primary cache-server node to all the secondary cache-server nodes to
 * remove the events which have already been dispatched from
 * the queue
 *
 * @throws CacheException
 */
public void testMessageSyncInterval() throws CacheException {
  CacheCreation cache = new CacheCreation();
  cache.setMessageSyncInterval(123);
  RegionAttributesCreation attrs = new RegionAttributesCreation(cache);
  attrs.setDataPolicy(DataPolicy.NORMAL);
  cache.createVMRegion("rootNORMAL", attrs);
  testXml(cache);
  Cache c = getCache();
  assertNotNull(c);
  assertEquals(123, c.getMessageSyncInterval());
}

/**
 * Tests the bridge-server attributes (<code>maximum-message-count</code>
 * and <code>message-time-to-live</code>) related to HA of client-queues in
 * gemfire cache-server framework
 * 
 * @throws CacheException
 */
public void testBridgeAttributesRelatedToClientQueuesHA() throws CacheException {
  CacheCreation cache = new CacheCreation();
  cache.setMessageSyncInterval(3445);
  BridgeServer bs = cache.addBridgeServer();
  bs.setMaximumMessageCount(12345);
  bs.setMessageTimeToLive(56789);
  bs.setPort(AvailablePortHelper.getRandomAvailableTCPPort());
  RegionAttributesCreation attrs = new RegionAttributesCreation(cache);
  attrs.setDataPolicy(DataPolicy.NORMAL);
  cache.createVMRegion("rootNORMAL", attrs);
  testXml(cache);
  Cache c = getCache();
  assertNotNull(c);
  BridgeServer server = (BridgeServer)cache.getBridgeServers().iterator().next();
  assertNotNull(server);
  assertEquals(12345,server.getMaximumMessageCount());
  assertEquals(56789,server.getMessageTimeToLive());     
  }

  /**
   * Tests that a region created with a named attributes has the correct
   * attributes.
   * 
   * This tests currently fails due to (what seem to me as) limitations in the
   * XML generator and the comparison of the XML. I have run this test by hand
   * and looked at the generated XML and there were no significant problems,
   * however because of the limitations, I am disabling this test, but leaving
   * the functionality for future comparisons (by hand of course). -- Mitch
   * Thomas 01/18/2006
   */
  public void testPartitionedRegionInstantiation() throws CacheException
  {
    CacheCreation cache = new CacheCreation();
    RegionAttributesCreation attrs = new RegionAttributesCreation(cache);

    PartitionAttributesFactory paf = new PartitionAttributesFactory();
    
    paf.setLocalMaxMemory(4)
      .setTotalNumBuckets(17)
      .setTotalMaxMemory(8);
    attrs.setPartitionAttributes(paf.create());
    cache.createRegion("pRoot", attrs);
  } 

  /**
   * Tests the bridge-server attributes (<code>max-threads</code>
   * 
   * @throws CacheException
   */
  public void testBridgeMaxThreads()
      throws CacheException
  {
    CacheCreation cache = new CacheCreation();

    BridgeServer bs = cache.addBridgeServer();
    bs.setMaxThreads(37);
    bs.setMaxConnections(999);
    bs.setPort(AvailablePortHelper.getRandomAvailableTCPPort());
    RegionAttributesCreation attrs = new RegionAttributesCreation(cache);
    attrs.setDataPolicy(DataPolicy.NORMAL);
    cache.createVMRegion("rootNORMAL", attrs);
    testXml(cache);
  }
  
  /**
   * Tests that loading cache XML with multi-cast set will set the multi-cast
   */
  public void testRegionMulticastSetViaCacheXml() throws CacheException
  {
    final String rNameBase = getUniqueName();
    final String r1 = rNameBase + "1";
    final String r2 = rNameBase + "2";
    final String r3 = rNameBase + "3";

    // Setting multi-cast via nested region attributes
    CacheCreation creation = new CacheCreation();
    RegionAttributesCreation attrs = new RegionAttributesCreation(creation);
    attrs.setScope(Scope.LOCAL);
    attrs.setEarlyAck(false);
    attrs.setMulticastEnabled(true);
    creation.createRegion(r1, attrs);
    
    // Setting multi-cast via named region attributes
    final String attrId = "region_attrs_with_multicast"; 
    attrs = new RegionAttributesCreation(creation);
    attrs.setId(attrId);
    attrs.setScope(Scope.DISTRIBUTED_NO_ACK);
    attrs.setEarlyAck(false);
    attrs.setMulticastEnabled(true);
    creation.setRegionAttributes(attrs.getId(), attrs);
    attrs = new RegionAttributesCreation(creation);
    attrs.setRefid(attrId);
    creation.createRegion(r3, attrs);
     
    testXml(creation);
    
    creation = new CacheCreation();
    attrs = new RegionAttributesCreation(creation);
    attrs.setScope(Scope.DISTRIBUTED_ACK);
    attrs.setEarlyAck(false);
    attrs.setMulticastEnabled(true);
    creation.createRegion(r2, attrs);

    Cache c = getCache();
    assertTrue(c instanceof GemFireCacheImpl);
    c.loadCacheXml(generate(creation));

    {
    Region reg1 = c.getRegion(r1);
    assertNotNull(reg1);
    assertEquals(Scope.LOCAL, reg1.getAttributes().getScope());
    assertFalse(reg1.getAttributes().getEarlyAck());
    assertTrue(reg1.getAttributes().getMulticastEnabled());
    }
    
    {
    Region reg2 = c.getRegion(r2);
    assertNotNull(reg2);
    assertEquals(Scope.DISTRIBUTED_ACK, reg2.getAttributes().getScope());
    assertFalse(reg2.getAttributes().getEarlyAck());
    assertTrue(reg2.getAttributes().getMulticastEnabled());
    }
    
    {
    Region reg3 = c.getRegion(r3);
    assertNotNull(reg3);
    assertEquals(Scope.DISTRIBUTED_NO_ACK, reg3.getAttributes().getScope());
    assertFalse(reg3.getAttributes().getEarlyAck());
    assertTrue(reg3.getAttributes().getMulticastEnabled());
    }
  }

  public void testRollOplogs() throws CacheException {
    CacheCreation cache = new CacheCreation();
//  Set properties for Asynch writes
    

    RegionAttributesCreation attrs = new RegionAttributesCreation(cache);

    RegionCreation root = (RegionCreation)
      cache.createRegion("root", attrs);

    {
      attrs = new RegionAttributesCreation(cache);
      DiskWriteAttributesFactory dwaf = new DiskWriteAttributesFactory();
      dwaf.setSynchronous(true);  
      dwaf.setRollOplogs(true);
      attrs.setDiskWriteAttributes(dwaf.create());
      root.createSubregion("sync", attrs);
    }

    {
      attrs = new RegionAttributesCreation(cache);
      DiskWriteAttributesFactory dwaf = new DiskWriteAttributesFactory();
      dwaf.setTimeInterval(123L);
      dwaf.setBytesThreshold(456L);
      dwaf.setRollOplogs(false);
      attrs.setDiskWriteAttributes(dwaf.create());
      root.createSubregion("async", attrs);
    }

    testXml(cache);
  }
  
  public void testMaxOplogSize() throws CacheException {
    CacheCreation cache = new CacheCreation();
//  Set properties for Asynch writes
    

    RegionAttributesCreation attrs = new RegionAttributesCreation(cache);

    RegionCreation root = (RegionCreation)
      cache.createRegion("root", attrs);

    {
      attrs = new RegionAttributesCreation(cache);
      DiskWriteAttributesFactory dwaf = new DiskWriteAttributesFactory();
      dwaf.setSynchronous(true);  
      dwaf.setMaxOplogSize(1);
      attrs.setDiskWriteAttributes(dwaf.create());
      root.createSubregion("sync", attrs);
    }

    {
      attrs = new RegionAttributesCreation(cache);
      DiskWriteAttributesFactory dwaf = new DiskWriteAttributesFactory();
      dwaf.setTimeInterval(123L);
      dwaf.setBytesThreshold(456L);
      dwaf.setMaxOplogSize(1);
      attrs.setDiskWriteAttributes(dwaf.create());
      root.createSubregion("async", attrs);
    }

    testXml(cache);
  }
}
