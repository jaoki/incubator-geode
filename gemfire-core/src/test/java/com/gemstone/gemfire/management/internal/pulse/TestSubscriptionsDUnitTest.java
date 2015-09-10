/*
 *  =========================================================================
 *  Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 *  ========================================================================
 */
package com.gemstone.gemfire.management.internal.pulse;

import java.util.Properties;

import com.gemstone.gemfire.cache.AttributesFactory;
import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionAttributes;
import com.gemstone.gemfire.cache.Scope;
import com.gemstone.gemfire.cache.client.PoolManager;
import com.gemstone.gemfire.cache.client.internal.PoolImpl;
import com.gemstone.gemfire.cache.server.CacheServer;
import com.gemstone.gemfire.distributed.DistributedMember;
import com.gemstone.gemfire.distributed.DistributedSystem;
import com.gemstone.gemfire.distributed.internal.DistributionConfig;
import com.gemstone.gemfire.internal.AvailablePort;
import com.gemstone.gemfire.internal.cache.GemFireCacheImpl;
import com.gemstone.gemfire.management.DistributedRegionMXBean;
import com.gemstone.gemfire.management.DistributedSystemMXBean;
import com.gemstone.gemfire.management.ManagementService;
import com.gemstone.gemfire.management.ManagementTestBase;
import dunit.DistributedTestCase;
import dunit.Host;
import dunit.SerializableCallable;
import dunit.SerializableRunnable;
import dunit.VM;

/**
 * This is for testing subscriptions
 * 
 * @author ajayp
 * 
 */

public class TestSubscriptionsDUnitTest extends DistributedTestCase {
  private static final String k1 = "k1";
  private static final String k2 = "k2";
  private static final String client_k1 = "client-k1";

  private static final String client_k2 = "client-k2";
  /** name of the test region */
  private static final String REGION_NAME = "TestSubscriptionsDUnitTest_Region";
  private static VM server = null;
  private static VM client = null;
  private static VM client2 = null;
  private static VM managingNode = null;
  private ManagementTestBase helper;

  public TestSubscriptionsDUnitTest(String name) {
    super(name);
    this.helper = new ManagementTestBase(name);
  }

  public void setUp() throws Exception {
    super.setUp();
    final Host host = Host.getHost(0);
    managingNode = host.getVM(0);
    server = host.getVM(1);
    client = host.getVM(2);
    client2 = host.getVM(3);
  }

  public void tearDown2() throws Exception {
    super.tearDown2();
    helper.closeCache(managingNode);
    helper.closeCache(server);
    helper.closeCache(client);
    helper.closeCache(client2);
    disconnectFromDS();
    
  }

  private static final long serialVersionUID = 1L;

  public void testNoOfSubscription() throws Exception {

    helper.createManagementCache(managingNode);
    helper.startManagingNode(managingNode);

    int port = (Integer) createServerCache(server);
    DistributedMember serverMember = helper.getMember(server);
    createClientCache(client, getServerHostName(server.getHost()), port);
    createClientCache(client2, getServerHostName(server.getHost()), port);
    put(client);
    put(client2);
    registerInterest(client);
    registerInterest(client2);
    verifyClientStats(managingNode, serverMember, port);
    helper.stopManagingNode(managingNode);
  }

  @SuppressWarnings("serial")
  private Object createServerCache(VM vm) {
    return vm.invoke(new SerializableCallable(
        "Create Server Cache in TestSubscriptionsDUnitTest") {

      public Object call() {
        try {
          return createServerCache();
        } catch (Exception e) {
          fail("Error while createServerCache in TestSubscriptionsDUnitTest"
              + e);
        }
        return null;
      }
    });
  }

  @SuppressWarnings("serial")
  private void createClientCache(VM vm, final String host, final Integer port1) {
    vm.invoke(new SerializableCallable(
        "Create Client Cache in TestSubscriptionsDUnitTest") {

      public Object call() {
        try {
          createClientCache(host, port1);
        } catch (Exception e) {
          fail("Error while createClientCache in TestSubscriptionsDUnitTest "
              + e);
        }
        return null;
      }
    });
  }

  private Cache createCache(Properties props) throws Exception {
    DistributedSystem ds = getSystem(props);
    ds.disconnect();
    ds = getSystem(props);
    assertNotNull(ds);
    Cache cache = (GemFireCacheImpl) CacheFactory.create(ds);
    assertNotNull(cache);
    return cache;
  }

  private Integer createServerCache(DataPolicy dataPolicy) throws Exception {
    Cache cache = helper.createCache(false);
    AttributesFactory factory = new AttributesFactory();
    factory.setScope(Scope.DISTRIBUTED_ACK);
    factory.setDataPolicy(dataPolicy);
    RegionAttributes attrs = factory.create();
    cache.createRegion(REGION_NAME, attrs);
    int port = AvailablePort.getRandomAvailablePort(AvailablePort.SOCKET);
    CacheServer server1 = cache.addCacheServer();
    server1.setPort(port);
    server1.setNotifyBySubscription(true);
    server1.start();
    return new Integer(server1.getPort());
  }

  public Integer createServerCache() throws Exception {
    return createServerCache(DataPolicy.REPLICATE);
  }

  public Cache createClientCache(String host, Integer port1) throws Exception {

    Properties props = new Properties();
    props.setProperty(DistributionConfig.MCAST_PORT_NAME, "0");
    props.setProperty(DistributionConfig.LOCATORS_NAME, "");
    Cache cache = createCache(props);
    PoolImpl p = (PoolImpl) PoolManager.createFactory()
        .addServer(host, port1.intValue()).setSubscriptionEnabled(true)
        .setThreadLocalConnections(true).setMinConnections(1)
        .setReadTimeout(20000).setPingInterval(10000).setRetryAttempts(1)
        .setSubscriptionEnabled(true).setStatisticInterval(1000)
        .create("TestSubscriptionsDUnitTest");

    AttributesFactory factory = new AttributesFactory();
    factory.setScope(Scope.DISTRIBUTED_ACK);
    factory.setPoolName(p.getName());

    RegionAttributes attrs = factory.create();
    Region region = cache.createRegion(REGION_NAME, attrs);
    return cache;

  }

  /**
   * get member id
   * 
   * @param vm
   */
  @SuppressWarnings("serial")
  protected static DistributedMember getMember() throws Exception {
    GemFireCacheImpl cache = GemFireCacheImpl.getInstance();
    return cache.getDistributedSystem().getDistributedMember();
  }

  /**
   * Verify the Cache Server details
   * 
   * @param vm
   */
  @SuppressWarnings("serial")
  protected void verifyClientStats(final VM vm,
      final DistributedMember serverMember, final int serverPort) {
    SerializableRunnable verifyCacheServerRemote = new SerializableRunnable(
        "TestSubscriptionsDUnitTest Verify Cache Server Remote") {
      public void run() {
        final GemFireCacheImpl cache = GemFireCacheImpl.getInstance();
        try {
          final WaitCriterion waitCriteria = new WaitCriterion() {
            @Override
            public boolean done() {
              ManagementService service = ManagementService
                  .getExistingManagementService(cache);
              final DistributedSystemMXBean dsBean = service
                  .getDistributedSystemMXBean();
              if (dsBean != null) {
                if (dsBean.getNumSubscriptions() > 1) {
                  return true;
                }
              }
              return false;
            }

            @Override
            public String description() {
              return "TestSubscriptionsDUnitTest wait for getDistributedSystemMXBean to complete and get results";
            }
          };
          waitForCriterion(waitCriteria, 2 * 60 * 1000, 3000, true);
          final DistributedSystemMXBean dsBean = ManagementService
              .getExistingManagementService(cache).getDistributedSystemMXBean();
          assertNotNull(dsBean);
          getLogWriter().info(
              "TestSubscriptionsDUnitTest dsBean.getNumSubscriptions() ="
                  + dsBean.getNumSubscriptions());
          assertTrue(dsBean.getNumSubscriptions() == 2 ? true : false);
        } catch (Exception e) {
          fail("TestSubscriptionsDUnitTest Error while verifying subscription "
              + e.getMessage());
        }

      }
    };
    vm.invoke(verifyCacheServerRemote);
  }

  /**
   * Verify the Cache Server details
   * 
   * @param vm
   */
  @SuppressWarnings("serial")
  protected void registerInterest(final VM vm) {
    SerializableRunnable put = new SerializableRunnable(
        "TestSubscriptionsDUnitTest registerInterest") {
      public void run() {
        try {
          Cache cache = GemFireCacheImpl.getInstance();
          Region<Object, Object> r1 = cache.getRegion(Region.SEPARATOR
              + REGION_NAME);
          assertNotNull(r1);
          r1.registerInterest(k1);
          r1.registerInterest(k2);
        } catch (Exception ex) {
          fail("TestSubscriptionsDUnitTest failed while register Interest", ex);
        }
      }

    };
    vm.invoke(put);
  }

  @SuppressWarnings("serial")
  protected void put(final VM vm) {
    SerializableRunnable put = new SerializableRunnable("put") {
      public void run() {
        try {
          Cache cache = GemFireCacheImpl.getInstance();
          Region r1 = cache.getRegion(Region.SEPARATOR + REGION_NAME);
          assertNotNull(r1);
          r1.put(k1, client_k1);
          assertEquals(r1.getEntry(k1).getValue(), client_k1);
          r1.put(k2, client_k2);
          assertEquals(r1.getEntry(k2).getValue(), client_k2);
        } catch (Exception ex) {
          fail("failed while put", ex);
        }
      }

    };
    vm.invoke(put);
  }

}
