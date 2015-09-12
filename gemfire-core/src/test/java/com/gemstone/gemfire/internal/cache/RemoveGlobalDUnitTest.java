/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
/*
 * RemoveGlobalDUintTest.java
 *
 * Created on September 15, 2005, 4:36 PM
 */

package com.gemstone.gemfire.internal.cache;
import com.gemstone.gemfire.cache.*;
import com.gemstone.gemfire.cache.util.*;
import com.gemstone.gemfire.cache30.CacheSerializableRunnable;
import com.gemstone.gemfire.distributed.DistributedSystem;

import dunit.*;

import java.util.Properties;
/**
 *
 * @author vjadhav
 */
public class RemoveGlobalDUnitTest extends DistributedTestCase {
    
    /** Creates a new instance of RemoveGlobalDUintTest */
    public RemoveGlobalDUnitTest(String name) {
        super(name);
    }
    static Cache cache;
    static Properties props = new Properties();
    static Properties propsWork = new Properties();
    static DistributedSystem ds = null;
    static Region region;
    static boolean lockedForRemove = false;
    static VM vm0 = null;
    static VM vm1 = null;
    
    @Override
    public void setUp() throws Exception {
      super.setUp();
      Host host = Host.getHost(0);
      vm0 = host.getVM(0);
      vm1 = host.getVM(1);
      vm0.invoke(RemoveGlobalDUnitTest.class, "createCache");
      vm1.invoke(RemoveGlobalDUnitTest.class, "createCache");
    }
    
    public void tearDown2(){
        vm0.invoke(RemoveGlobalDUnitTest.class, "resetFlag");
        vm1.invoke(RemoveGlobalDUnitTest.class, "resetFlag");
        vm0.invoke(RemoveGlobalDUnitTest.class, "closeCache");
        vm1.invoke(RemoveGlobalDUnitTest.class, "closeCache");
        
    }
    
    public static void resetFlag()
    {
      lockedForRemove = false; 
    }
    
    public static void createCache(){
        try{
            ds = (new RemoveGlobalDUnitTest("temp")).getSystem(props);
            cache = CacheFactory.create(ds);
        } catch (Exception ex){
            ex.printStackTrace();
        }
    } //end of create cache for VM
    
    
    public static void closeCache(){
        try{
            cache.close();
            ds.disconnect();
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }
    
    //test methods
    
    public void testRemoveGlobalSingleVM() throws Throwable{
        
        SerializableRunnable createRegionWithWriter = new CacheSerializableRunnable("create region with cache writer"){
            
            public void run2() throws CacheException{
                  cache.setLockTimeout(5);
                CacheWriter cacheWriter = new CacheWriterCallBack();
                AttributesFactory factory  = new AttributesFactory();
                factory.setScope(Scope.GLOBAL);
                factory.setCacheWriter(cacheWriter);
                region = cache.createRegion("map", factory.create());
            }
        };
        
        vm0.invoke(createRegionWithWriter);
        
        
        AsyncInvocation async = vm0.invokeAsync(new CacheSerializableRunnable("put object"){
            public void run2() throws CacheException{
                for (int i=1; i<5; i++){
                    region.put(new Integer(i), java.lang.Integer.toString(i));
                }
                
                region.remove(new Integer(2));
            }
        });
        
        vm0.invoke(new CacheSerializableRunnable("verify locking"){
            public void run2() throws CacheException{
              
                synchronized(RemoveGlobalDUnitTest.class) {
                    if(! lockedForRemove){
                        try{RemoveGlobalDUnitTest.class.wait();}catch(Exception ex){ex.printStackTrace();}
                    }
                }
                try{
                    //getLogWriter().fine("000000000000000");
                    region.put(new Integer(2),"newEntry");                    
                    fail("Should have thrown TimeoutException");
                } catch(TimeoutException tme){
                    //pass
                }
            }
        });
        
        DistributedTestCase.join(async, 30 * 1000, getLogWriter());
        if(async.exceptionOccurred())
          throw async.getException();
        
    }//end of testRemoveGlobalSingleVM
    
    
    public void testRemoveGlobalMultiVM() throws Throwable{
        //Commented the Test.As it is failing @ line no 145 : AssertionFailedError
       
        SerializableRunnable createSimpleRegion = new CacheSerializableRunnable("create region with cache writer"){
            public void run2() throws CacheException{
                AttributesFactory factory  = new AttributesFactory();
                factory.setScope(Scope.GLOBAL);
                region = cache.createRegion("map", factory.create());
            }
        };
        
        
        SerializableRunnable createRegionWithWriter = new CacheSerializableRunnable("create region with capacity controller"){
            public void run2() throws CacheException{
                CacheWriter cw = new CacheWriterCallBack();
                AttributesFactory factory  = new AttributesFactory();
                factory.setScope(Scope.GLOBAL);
                factory.setCacheWriter(cw);
                region = cache.createRegion("map", factory.create());
            }
        };
        
        vm0.invoke(createSimpleRegion);
        vm1.invoke(createRegionWithWriter);
        
        vm0.invoke(new CacheSerializableRunnable("put object"){
            public void run2() throws CacheException{
                for (int i=1; i<5; i++){
                    region.put(new Integer(i), java.lang.Integer.toString(i));
                }
            }
        });
        
        vm1.invoke(new CacheSerializableRunnable("get object"){
            public void run2() throws CacheException{
                for (int i=1; i<5; i++){
                    region.get(new Integer(i));
                }
            }
        });
        
        AsyncInvocation async = vm0.invokeAsync(new CacheSerializableRunnable("remove object"){
            public void run2() throws CacheException{
                region.remove(new Integer(2));
            }
        });
        
        
        vm1.invoke(new CacheSerializableRunnable("verify locking"){
            public void run2() throws CacheException{
                cache.setLockTimeout(5);
                synchronized(RemoveGlobalDUnitTest.class) {
                    if(! lockedForRemove){
                        try{RemoveGlobalDUnitTest.class.wait();}catch(Exception ex){ex.printStackTrace();}
                    }
                }
                try{
                    //getLogWriter().fine("11111111111111");
                    region.put(new Integer(2),"newEntry");
                    fail("Should have thrown TimeoutException");
                } catch(TimeoutException tme){
                    //pass
                }
            }
        });
        
        DistributedTestCase.join(async, 30 * 1000, getLogWriter());
        if(async.exceptionOccurred())
          throw async.getException();
        
    }//end of testRemoveGlobalMultiVM
    
    static class CacheWriterCallBack extends CacheWriterAdapter {
        public void beforeDestroy(EntryEvent event) {
            
            synchronized (RemoveGlobalDUnitTest.class) {                
                lockedForRemove = true;
                RemoveGlobalDUnitTest.class.notify();
            }
            try{                
                Thread.sleep(30*1000);
            }catch(InterruptedException ex){
                fail("interrupted");
            }
            getLogWriter().fine("quitingfromcachewriter");
        }
    }///////////    
    
}// end of class
