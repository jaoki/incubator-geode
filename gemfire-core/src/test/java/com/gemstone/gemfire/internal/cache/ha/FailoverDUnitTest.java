/*=========================================================================
 * Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.cache.ha;



import java.util.Iterator;
import java.util.Properties;

import com.gemstone.gemfire.cache.AttributesFactory;
import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionAttributes;
import com.gemstone.gemfire.cache.Scope;
import com.gemstone.gemfire.cache.util.BridgeServer;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;
import com.gemstone.gemfire.cache30.BridgeTestCase;
import com.gemstone.gemfire.distributed.DistributedSystem;
import com.gemstone.gemfire.distributed.internal.DistributionConfig;
import com.gemstone.gemfire.internal.AvailablePort;
import com.gemstone.gemfire.internal.cache.BridgeObserverAdapter;
import com.gemstone.gemfire.internal.cache.BridgeObserverHolder;
import com.gemstone.gemfire.internal.cache.tier.sockets.CacheServerTestUtil;
import com.gemstone.gemfire.internal.cache.tier.sockets.ConflationDUnitTest;
import com.gemstone.gemfire.cache.client.PoolManager;
import com.gemstone.gemfire.cache.client.internal.PoolImpl;

import dunit.DistributedTestCase;
import dunit.Host;
import dunit.VM;

/**
 *
 *  Dunit test to verify HA feature. Have 2 nodes S1 & S2. Client is connected to S1 & S2 with S1 as the primary end point.
 *  Do some puts on S1 .The expiry is on high side. Stop S1 , the client is failing to S2.During fail over duration do some
 *  puts on S1. The client on failing to S2 may receive duplicate events but should not miss any events.
 *
 *  @author Suyog Bhokare
 *
 */
public class FailoverDUnitTest extends DistributedTestCase
{
  protected static Cache cache = null;
  //server
  private static VM vm0 = null;
  private static VM vm1 = null;
  protected static VM primary = null;

  private static int PORT1;
  private static int PORT2;

  private static final String regionName = "interestRegion";

  /** constructor */
  public FailoverDUnitTest(String name) {
    super(name);
  }

  public void setUp() throws Exception
  {
	super.setUp();
    final Host host = Host.getHost(0);
    vm0 = host.getVM(0);
    vm1 = host.getVM(1);

    //start servers first
    vm0.invoke(ConflationDUnitTest.class, "unsetIsSlowStart");
    vm1.invoke(ConflationDUnitTest.class, "unsetIsSlowStart");
    PORT1 =  ((Integer)vm0.invoke(FailoverDUnitTest.class, "createServerCache" )).intValue();
    PORT2 =  ((Integer)vm1.invoke(FailoverDUnitTest.class, "createServerCache" )).intValue();

    CacheServerTestUtil.disableShufflingOfEndpoints();
    createClientCache(getServerHostName(host), new Integer(PORT1),new Integer(PORT2));
    { // calculate the primary vm
      waitForPrimaryAndBackups(1);
      PoolImpl pool = (PoolImpl)PoolManager.find("FailoverPool");
      if (pool.getPrimaryPort() == PORT1) {
        primary = vm0;
      } else {
        assertEquals(PORT2, pool.getPrimaryPort());
        primary = vm1;
      }
    }
  }

  public void testFailover()
  {
    createEntries();
    waitForPrimaryAndBackups(1);
    registerInterestList();
    primary.invoke(FailoverDUnitTest.class, "put");
    verifyEntries();
    setBridgeObserver();
    primary.invoke(FailoverDUnitTest.class, "stopServer");
    verifyEntriesAfterFailover();
  }

  private void createCache(Properties props) throws Exception
  {
    DistributedSystem ds = getSystem(props);
    ds.disconnect();
    ds = getSystem(props);
    assertNotNull(ds);
    cache = CacheFactory.create(ds);
    assertNotNull(cache);
  }

  public static void createClientCache(String hostName, Integer port1 , Integer port2) throws Exception
  {
    PORT1 = port1.intValue();
    PORT2 = port2.intValue();
    Properties props = new Properties();
    props.setProperty(DistributionConfig.MCAST_PORT_NAME, "0");
    props.setProperty(DistributionConfig.LOCATORS_NAME, "");
    new FailoverDUnitTest("temp").createCache(props);

    /*props.setProperty("retryAttempts", "5");
    props.setProperty("endpoints", "ep1=" + hostName + ":"+PORT1+",ep2="
        + hostName + ":"+PORT2);
    props.setProperty("redundancyLevel", "-1");
    props.setProperty("establishCallbackConnection", "true");
    props.setProperty("LBPolicy", "RoundRobin");
    props.setProperty("readTimeout", "250");
    props.setProperty("socketBufferSize", "32768");
    props.setProperty("retryInterval", "1000");
    props.setProperty("connectionsPerServer", "2");
*/
    AttributesFactory factory = new AttributesFactory();
    factory.setScope(Scope.DISTRIBUTED_ACK);
    BridgeTestCase.configureConnectionPoolWithName(factory, hostName, new int[] {PORT1,PORT2}, true, -1, 2, null, "FailoverPool");
    factory.setCacheListener(new CacheListenerAdapter() {
      public void afterUpdate(EntryEvent event)
      {
        synchronized (this) {
          cache.getLogger().info("Event Received : key..."+ event.getKey());
          cache.getLogger().info("Event Received : value..."+ event.getNewValue());
        }
      }
     });
    cache.createRegion(regionName, factory.create());
  }

  public static Integer createServerCache() throws Exception
  {
    new FailoverDUnitTest("temp").createCache(new Properties());
    AttributesFactory factory = new AttributesFactory();
    factory.setScope(Scope.DISTRIBUTED_ACK);
    factory.setDataPolicy(DataPolicy.REPLICATE);
    RegionAttributes attrs = factory.create();
    cache.createRegion(regionName, attrs);
    int port = AvailablePort.getRandomAvailablePort(AvailablePort.SOCKET) ;
    BridgeServer server1 = cache.addBridgeServer();
    server1.setPort(port);
    server1.setNotifyBySubscription(true);
    server1.start();
    return new Integer(server1.getPort());
  }

  public void waitForPrimaryAndBackups(final int numBackups) {
    final PoolImpl pool = (PoolImpl)PoolManager.find("FailoverPool");
    WaitCriterion ev = new WaitCriterion() {
      public boolean done() {
        if (pool.getPrimary() == null) {
          return false;
        }
        if (pool.getRedundants().size() < numBackups) {
          return false;
        }
        return true;
      }
      public String description() {
        return null;
      }
    };
    DistributedTestCase.waitForCriterion(ev, 20 * 1000, 200, true);
    assertNotNull(pool.getPrimary());
    assertTrue("backups="+pool.getRedundants() + " expected=" + numBackups,
               pool.getRedundants().size() >= numBackups);
  }

  public static void registerInterestList()
  {
    try {
      Region r = cache.getRegion("/"+ regionName);
      assertNotNull(r);
      r.registerInterest("key-1");
      r.registerInterest("key-2");
      r.registerInterest("key-3");
      r.registerInterest("key-4");
      r.registerInterest("key-5");
    }
    catch (Exception ex) {
      fail("failed while registering keys k1 to k5", ex);
    }
  }

  public static void createEntries()
  {
    try {

      Region r = cache.getRegion("/"+ regionName);
      assertNotNull(r);

      r.create("key-1", "key-1");
      r.create("key-2", "key-2");
      r.create("key-3", "key-3");
      r.create("key-4", "key-4");
      r.create("key-5", "key-5");
    }
    catch (Exception ex) {
      fail("failed while createEntries()", ex);
    }
  }


  public static void stopServer()
  {
    try {
      Iterator iter = cache.getBridgeServers().iterator();
      if (iter.hasNext()) {
        BridgeServer server = (BridgeServer)iter.next();
          server.stop();
      }
    }
    catch (Exception e) {
      fail("failed while stopServer()" + e);
    }
  }

  public static void put()
  {
    try {
      Region r = cache.getRegion("/"+ regionName);
      assertNotNull(r);

      r.put("key-1", "value-1");
      r.put("key-2", "value-2");
      r.put("key-3", "value-3");

    }
    catch (Exception ex) {
      fail("failed while r.put()", ex);
    }
  }

  public void verifyEntries()
  {
    final Region r = cache.getRegion("/"+regionName);
    assertNotNull(r);
    WaitCriterion ev = new WaitCriterion() {
      public boolean done() {
        return !r.getEntry("key-3").getValue().equals("key-3");
      }
      public String description() {
        return null;
      }
    };
    DistributedTestCase.waitForCriterion(ev, 20 * 1000, 200, true);

    assertEquals("value-1", r.getEntry("key-1").getValue());
    assertEquals("value-2", r.getEntry("key-2").getValue());
    assertEquals("value-3", r.getEntry("key-3").getValue());
  }

  public static void setBridgeObserver() {
    PoolImpl.BEFORE_PRIMARY_IDENTIFICATION_FROM_BACKUP_CALLBACK_FLAG = true;
    BridgeObserverHolder.setInstance(new BridgeObserverAdapter() {
        public void beforePrimaryIdentificationFromBackup() {
          primary.invoke(FailoverDUnitTest.class, "putDuringFailover");
          PoolImpl.BEFORE_PRIMARY_IDENTIFICATION_FROM_BACKUP_CALLBACK_FLAG = false;
        }
    });
}

  public static void putDuringFailover()
  {
    try {
      Region r = cache.getRegion("/"+ regionName);
      assertNotNull(r);
      r.put("key-4", "value-4");
      r.put("key-5", "value-5");

    }
    catch (Exception ex) {
      fail("failed while r.putDuringFailover()", ex);
    }
  }

  public void verifyEntriesAfterFailover()
  {
    final Region r = cache.getRegion("/"+ regionName);
    assertNotNull(r);
    WaitCriterion ev = new WaitCriterion() {
      public boolean done() {
        return !r.getEntry("key-5").getValue().equals("key-5");
      }
      public String description() {
        return null;
      }
    };
    DistributedTestCase.waitForCriterion(ev, 20 * 1000, 200, true);
    assertEquals("value-5", r.getEntry("key-5").getValue());
    assertEquals("value-4", r.getEntry("key-4").getValue());
  }


  public void tearDown2() throws Exception
  {
	super.tearDown2();
    // close the clients first
    closeCache();
    // then close the servers
    vm0.invoke(FailoverDUnitTest.class, "closeCache");
    vm1.invoke(FailoverDUnitTest.class, "closeCache");
    CacheServerTestUtil.resetDisableShufflingOfEndpointsFlag();
  }

  public static void closeCache()
  {
    if (cache != null && !cache.isClosed()) {
      cache.close();
      cache.getDistributedSystem().disconnect();
      cache = null;
    }
  }
}
