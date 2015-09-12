/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.pdx;

import com.gemstone.gemfire.cache.AttributesFactory;
import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.Scope;
import com.gemstone.gemfire.cache30.CacheTestCase;
import com.gemstone.gemfire.pdx.internal.PeerTypeRegistration;

import dunit.AsyncInvocation;
import dunit.Host;
import dunit.SerializableCallable;
import dunit.VM;

public class PdxSerializableDUnitTest extends CacheTestCase {

  public PdxSerializableDUnitTest(String name) {
    super(name);
  }
  

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  public void testSimplePut() {
    Host host = Host.getHost(0);
    VM vm1 = host.getVM(0);
    VM vm2 = host.getVM(1);
    VM vm3 = host.getVM(2);
    
    SerializableCallable createRegion = new SerializableCallable() {
      public Object call() throws Exception {
        AttributesFactory af = new AttributesFactory();
        af.setScope(Scope.DISTRIBUTED_ACK);
        af.setDataPolicy(DataPolicy.REPLICATE);
        createRootRegion("testSimplePdx", af.create());
        return null;
      }
    };

    vm1.invoke(createRegion);
    vm2.invoke(createRegion);
    vm3.invoke(createRegion);
    vm1.invoke(new SerializableCallable() {
      public Object call() throws Exception {
        //Check to make sure the type region is not yet created
        Region r = getRootRegion("testSimplePdx");
        r.put(1, new SimpleClass(57, (byte) 3));
        //Ok, now the type registry should exist
        assertNotNull(getRootRegion(PeerTypeRegistration.REGION_NAME));
        return null;
      }
    });
    vm2.invoke(new SerializableCallable() {
      public Object call() throws Exception {
      //Ok, now the type registry should exist
        assertNotNull(getRootRegion(PeerTypeRegistration.REGION_NAME));
        Region r = getRootRegion("testSimplePdx");
        assertEquals(new SimpleClass(57, (byte) 3), r.get(1));
        return null;
      }
    });
    
    vm3.invoke(new SerializableCallable("check for PDX") {
      
      public Object call() throws Exception {
        assertNotNull(getRootRegion(PeerTypeRegistration.REGION_NAME));
        return null;
      }
    });
  }
  
  public void testPersistenceDefaultDiskStore() throws Throwable {
    
    SerializableCallable createRegion = new SerializableCallable() {
      public Object call() throws Exception {
        //Make sure the type registry is persistent
        CacheFactory cf = new CacheFactory();
        cf.setPdxPersistent(true);
        getCache(cf);
        AttributesFactory af = new AttributesFactory();
        af.setScope(Scope.DISTRIBUTED_ACK);
        af.setDataPolicy(DataPolicy.PERSISTENT_REPLICATE);
        createRootRegion("testSimplePdx", af.create());
        return null;
      }
    };

    persistenceTest(createRegion);
  }
  
  public void testPersistenceExplicitDiskStore() throws Throwable {
    SerializableCallable createRegion = new SerializableCallable() {
      public Object call() throws Exception {
        //Make sure the type registry is persistent
        CacheFactory cf = new CacheFactory();
        cf.setPdxPersistent(true);
        cf.setPdxDiskStore("store1");
        Cache cache = getCache(cf);
        cache.createDiskStoreFactory()
          .setMaxOplogSize(1)
          .setDiskDirs(getDiskDirs())
          .create("store1");
        AttributesFactory af = new AttributesFactory();
        af.setScope(Scope.DISTRIBUTED_ACK);
        af.setDataPolicy(DataPolicy.PERSISTENT_REPLICATE);
        af.setDiskStoreName("store1");
        createRootRegion("testSimplePdx", af.create());
        return null;
      }
    };
    persistenceTest(createRegion);
  }


  private void persistenceTest(SerializableCallable createRegion)
      throws Throwable {
    Host host = Host.getHost(0);
    VM vm1 = host.getVM(0);
    VM vm2 = host.getVM(1);
    VM vm3 = host.getVM(2);
    vm1.invoke(createRegion);
    vm2.invoke(createRegion);
    
    vm1.invoke(new SerializableCallable() {
      public Object call() throws Exception {
        //Check to make sure the type region is not yet created
        Region r = getRootRegion("testSimplePdx");
        r.put(1, new SimpleClass(57, (byte) 3));
        //Ok, now the type registry should exist
        assertNotNull(getRootRegion(PeerTypeRegistration.REGION_NAME));
        return null;
      }
    });
    
    final SerializableCallable checkForObject = new SerializableCallable() {
      public Object call() throws Exception {
        Region r = getRootRegion("testSimplePdx");
        assertEquals(new SimpleClass(57, (byte) 3), r.get(1));
        //Ok, now the type registry should exist
        assertNotNull(getRootRegion(PeerTypeRegistration.REGION_NAME));
        return null;
      }
    };
    
    vm2.invoke(checkForObject);
    
    SerializableCallable closeCache = new SerializableCallable() {
      public Object call() throws Exception {
        closeCache();
        return null;
      }
    };
    
    
    //Close the cache in both VMs
    vm1.invoke(closeCache);
    vm2.invoke(closeCache);
    
    
    //Now recreate the region, recoverying from disk
    AsyncInvocation future1 = vm1.invokeAsync(createRegion);
    AsyncInvocation future2 = vm2.invokeAsync(createRegion);
    
    future1.getResult();
    future2.getResult();
    
    //Make sure we can still find and deserialize the result.
    vm1.invoke(checkForObject);
    vm2.invoke(checkForObject);
    
    //Make sure a late comer can still create the type registry.
    vm3.invoke(createRegion);
    
    vm3.invoke(checkForObject);
  }
}
