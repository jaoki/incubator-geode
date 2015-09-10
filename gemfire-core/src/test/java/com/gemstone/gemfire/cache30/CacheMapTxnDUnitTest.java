/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
/*
 * CacheMapTxnDUnitTest.java
 *
 * Created on August 9, 2005, 11:18 AM
 */

package com.gemstone.gemfire.cache30;

/**
 *
 * @author  prafulla
 */

import dunit.*;

import com.gemstone.gemfire.cache.*;

import java.util.*;

import com.gemstone.gemfire.distributed.DistributedSystem;

public class CacheMapTxnDUnitTest extends DistributedTestCase{
    
    protected static Cache cache;
    protected static Properties props = new Properties();
    static DistributedSystem ds = null;
    static Region region;
    static Region mirroredRegion;
    protected static CacheTransactionManager cacheTxnMgr;
    
    /** Creates a new instance of CacheMapTxnDUnitTest */
    public CacheMapTxnDUnitTest(String name) {
        super(name);
    }
    
    @Override
    public void setUp() throws Exception {
      super.setUp();
      Host host = Host.getHost(0);
      VM vm0 = host.getVM(0);
      VM vm1 = host.getVM(1);
      vm0.invoke(CacheMapTxnDUnitTest.class, "createCache");
      vm1.invoke(CacheMapTxnDUnitTest.class, "createCache");
    }
    
    public void tearDown2(){
        Host host = Host.getHost(0);
        VM vm0 = host.getVM(0);
        VM vm1 = host.getVM(1);
        vm0.invoke(CacheMapTxnDUnitTest.class, "closeCache");
        vm1.invoke(CacheMapTxnDUnitTest.class, "closeCache");
        
    }
    
    public static void createCache(){
        try{
            //            props.setProperty("mcast-port", "1234");
            //            ds = DistributedSystem.connect(props);
            ds = (new CacheMapTxnDUnitTest("temp")).getSystem(props);
            cache = CacheFactory.create(ds);
            AttributesFactory factory  = new AttributesFactory();
            factory.setScope(Scope.DISTRIBUTED_ACK);
            factory.setDataPolicy(DataPolicy.REPLICATE);
            RegionAttributes attr = factory.create();
            region = cache.createRegion("map", attr);
            
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }
    
    public static void closeCache(){
        try{            
            cache.close();
            ds.disconnect();            
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }
    
    public void testCommitTxn() {
        //this is to test single VM region transactions
        Host host = Host.getHost(0);
        VM vm0 = host.getVM(0);
        vm0.invoke(CacheMapTxnDUnitTest.class, "commitTxn");
    }//end of testCommitTxn
    
    public void testRollbackTxn() {
        //this is to test single VM region transactions
        Host host = Host.getHost(0);
        VM vm0 = host.getVM(0);
        vm0.invoke(CacheMapTxnDUnitTest.class, "rollbackTxn");
    }//end of testRollbackTxn
    
    public void testRollbackTxnClear() {
        //this is to test single VM region transactions
        Host host = Host.getHost(0);
        VM vm0 = host.getVM(0);
        VM vm1 = host.getVM(1);
        int i=0;
        Object ob2;
        Object [] objArr = new Object[1];
        
        for (i=0;i<5; i++){
            objArr [0] = ""+i;
            vm1.invoke(CacheMapTxnDUnitTest.class, "putMethod", objArr);
        }
        
        vm0.invoke(CacheMapTxnDUnitTest.class, "rollbackTxnClear");
        
        i=3;
        objArr [0] = ""+i;
        ob2 = vm1.invoke(CacheMapTxnDUnitTest.class, "getMethod", objArr);
        
        if(ob2 != null){
            fail("failed in testRollbackTxnClear");
        }
    }//end of testRollbackTxnClear
    
    public void testMiscMethods() throws Throwable{
        Host host = Host.getHost(0);
        VM vm0 = host.getVM(0);
        VM vm1 = host.getVM(1);
        vm0.invoke(CacheMapTxnDUnitTest.class, "miscMethodsOwner");
        AsyncInvocation o2 = vm0.invokeAsync(CacheMapTxnDUnitTest.class, "miscMethodsNotOwner");//invoke in same vm but in seperate thread
        AsyncInvocation o3 = vm1.invokeAsync(CacheMapTxnDUnitTest.class, "miscMethodsNotOwner");//invoke in another vm
        DistributedTestCase.join(o2, 30 * 1000, getLogWriter());
        DistributedTestCase.join(o3, 30 * 1000, getLogWriter());
        
        if(o2.exceptionOccurred()){
          fail("o2 failed", o2.getException());
        }
        
        if(o3.exceptionOccurred()){
          fail("o3 failed", o3.getException());
        }
        
    }//end of testMiscMethods
    
    
    //methods to be executed in remote vms...and called through vm.invoke
    
    public static void commitTxn(){
        try{
            cacheTxnMgr = cache.getCacheTransactionManager();
            int [] i = {0,1,2,3,4,5,6,7,8,9};
            Object o3;
            o3="init";
            region.put(""+i[0], "zero");
            region.create(""+i[5], "fifth");
            //begin transaction
            cacheTxnMgr.begin();
            region.put(""+i[1], "first");
            //test get
            o3 = region.get(""+i[1]);
            if( !(o3.toString().equals("first"))){
                fail("get inside transaction returns incorrect object");
            }
            
            //test remove
            region.put(""+i[2], "second");
            o3 = region.remove(""+i[2]);
            if( !(o3.toString().equals("second"))){
                fail("remove inside transaction returns incorrect object");
            }
            
            boolean flag = region.containsKey(""+i[2]);
            if (flag){
                fail("region.containsKey after region.remove inside txn returns incorrect value");
            }
            
            region.put(""+i[3], "third");
            region.put(""+i[0], "updatedZero");

            // test putIfAbsent
            region.putIfAbsent(""+i[4], "fourth");
            if (!region.get(""+i[4]).toString().equals("fourth")) {
              fail("putIfAbsent inside transaction returns incorrect object");
            }
            
            // test replace
            region.replace(""+i[4], "fourth2");
            if (!region.get(""+i[4]).toString().equals("fourth2")) {
              fail("replace inside transaction returns incorrect object)");
            }

            // test replace
            region.replace(""+i[4], "fourth2", "fourth3");
            if (!region.get(""+i[4]).toString().equals("fourth3")) {
              fail("replace inside transaction returns incorrect object)");
            }
            
            // test a failed removal
            boolean succeeded = region.remove(""+i[5], new Object());
            assertTrue(!succeeded);
            assertTrue(region.get(""+i[5]).equals("fifth"));
            
            // test remove
            region.remove(""+i[5]);
            
            //commit the transaction
            cacheTxnMgr.commit();
            
            //verify for persistent data now
            o3 = region.get(""+i[1]);
            if( !(o3.toString().equals("first"))){
                fail("get after committed transaction returns incorrect object");
            }
            
            o3 = region.get(""+i[2]);
            if(o3 != null){
                fail("get after committed transaction returns incorrect object");
            }
            
            o3 = region.get(""+i[3]);
            if( !(o3.toString().equals("third"))){
                fail("get after committed transaction returns incorrect object");
            }
            
            if (!region.get(""+i[4]).toString().equals("fourth3")) {
              fail("get after committed transaction returns incorrect object");
            }
            
            if (region.containsKey(""+i[5])) {
              fail("containsKey after committed transaction was true");
            }
            
            boolean val = region.containsValue("updatedZero");
            if(! val){
                fail("containsValue after committed transaction returns incorrect result");
            }
            
        }
        catch(Exception ex){
            ex.printStackTrace();
            fail("failed in commitTxn");
        }
        finally{
            if(cacheTxnMgr.exists()){
                try {cacheTxnMgr.commit();} catch (Exception cce){cce.printStackTrace();}
            }
        }
        
    }//end of commitTxn
    
    
    public static void rollbackTxn(){
        try{
            cacheTxnMgr = cache.getCacheTransactionManager();
            int [] i = {0,1,2,3,4,5,6,7,8,9};
            Object o3;
            o3="init";
            region.put(""+i[0], "zero");
            region.put(""+i[5], "fifth");
            cacheTxnMgr.begin();
            region.put(""+i[1], "first");
            //test get
            o3 = region.get(""+i[1]);
            if( !(o3.toString().equals("first"))){
                fail("get inside transaction returns incorrect object");
            }
            
            //test containsValue
            boolean flag = region.containsValue(new String("first"));
            //assertEquals(true, flag);
            
            //test remove
            region.put(""+i[2], "second");
            o3 = region.remove(""+i[2]);
            if( !(o3.toString().equals("second"))){
                fail("remove inside transaction returns incorrect object");
            }
            
            region.put(""+i[3], "third");
            region.put(""+i[0], "updatedZero");

            
            // test putIfAbsent
            region.putIfAbsent(""+i[4], "fourth");
            if (!region.get(""+i[4]).toString().equals("fourth")) {
              fail("putIfAbsent inside transaction returns incorrect object");
            }
            
            // test replace
            region.replace(""+i[4], "fourth2");
            if (!region.get(""+i[4]).toString().equals("fourth2")) {
              fail("replace inside transaction returns incorrect object)");
            }

            // test replace
            region.replace(""+i[4], "fourth2", "fourth3");
            if (!region.get(""+i[4]).toString().equals("fourth3")) {
              fail("replace inside transaction returns incorrect object)");
            }
            
            // test a failed removal
            boolean succeeded = region.remove(""+i[5], new Object());
            assertTrue(!succeeded);
            assertTrue(region.get(""+i[5]).equals("fifth"));

            // test remove
            region.remove(""+i[5]);
            
            //rollback the transaction
            cacheTxnMgr.rollback();
            
            //verify for persistent data now
            o3 = region.get(""+i[1]);
            if( o3 != null){ //null?
                fail("get after rolled back transaction returns incorrect object");
            }
            
            o3 = region.get(""+i[2]);
            if( o3 != null){ //null?
                fail("get after rolled back transaction returns incorrect object");
            }
            
            o3 = region.get(""+i[3]);
            if( o3 != null){ //null?
                fail("get after rolled back transaction returns incorrect object");
            }
            
            o3 = region.get(""+i[4]);
            if( o3 != null){ //null?
                fail("get after rolled back transaction returns incorrect object");
            }
            
            o3 = region.get(""+i[5]);
            if (o3 == null) {
              fail("get after rolled back transaction returns incorrect object");
            }
            
            //            boolean val = region.containsValue("zero");
            //            if( !val){
            //                fail("containsValue after rolled back transaction returns incorrect result");
            //            }
            
        }
        catch(Exception ex){
            ex.printStackTrace();
            fail("failed in rollbackTxn");
        }
        finally{
            if(cacheTxnMgr.exists()){
                try {cacheTxnMgr.commit();} catch (Exception cce){cce.printStackTrace();}
            }
        }
        
    }//end of rollbackTxn
    
    public static void rollbackTxnClear(){
        try{
            cacheTxnMgr = cache.getCacheTransactionManager();
            int [] i = {0,1,2,3,4,5,6,7,8,9};
            region.put(""+i[0], "zero");
            //begin transaction
            cacheTxnMgr.begin();
            region.put(""+i[1], "first");
            region.put(""+i[0], "updatedZero");
            
            try {
              region.clear(); //clear is not transactional operation
              fail("excpected exception not thrown");
            } catch (UnsupportedOperationInTransactionException e) {
            }
            
            //rollback the transaction
            cacheTxnMgr.rollback();
            
            //verify for persistent data now
            
            boolean val = region.containsValue("updatedZero");
            if(val){
                fail("containsValue after region.clear & rolled back transaction returns incorrect result");
            }
            region.clear();
            val = region.containsValue("first");
            if(val){
                fail("containsValue after region.clear & rolled back transaction returns incorrect result");
            }
            
        }
        catch(Exception ex){
            cacheTxnMgr = null;
            ex.printStackTrace();
            fail("failed in rollbackTxnClear");
        }
        
    }//end of rollbackTxnClear
    
    public static void miscMethodsOwner(){
        try{
            cacheTxnMgr = cache.getCacheTransactionManager();
            int [] i = {0,1,2,3,4,5,6,7,8,9};
            region.clear();
            region.put(""+i[0], "zero");
            region.put(""+i[1], "first");
            region.put(""+i[2], "second");
            region.put(""+i[3], "third");
            
            cacheTxnMgr.begin();
            region.put(""+i[4], "forth");
            region.put(""+i[5], "fifth");
            
            //test size method
            int j = region.size();
            if(j != 6){
                fail("region.size inside transaction returns incorrect results");
            }
            
            //test entrySet method
            Set set = region.entrySet();
            int k = set.size();
            if(j != 6){
                fail("region.entrySet inside transaction returns incorrect results");
            }
            
            //test keySet method
            set = region.keySet();
            k = set.size();
            if(j != 6){
                fail("region.keySet inside transaction returns incorrect results");
            }
            
            boolean val = region.containsValue("forth");
            if( !val){
                fail("containsValue inside transaction returns incorrect result");
            }
            
            //commit the transaction
            cacheTxnMgr.rollback();
            
            //verify for persistent data now
            
        }
        catch(Exception ex){
            ex.printStackTrace();
            fail("failed in miscMethodsOwner");
        }
        finally{
            if(cacheTxnMgr.exists()){
                try {cacheTxnMgr.commit();} catch (Exception cce){cce.printStackTrace();}
            }
        }
        
    }//end of miscMethodsOwner
    
    public static void miscMethodsNotOwner(){
        try{
//            int [] i = {0,1,2,3,4,5,6,7,8,9};
            //it is assumed that there are already four committed entried inside region
            //test size method
            int j = region.size();
            if(j != 4){
                fail("region.size for not owner of transaction returns incorrect results, size is "+j+ " but expected 4");
            }
            
            //test entrySet method
            Set set = region.entrySet();
            int k = set.size();
            if(j != 4){
                fail("region.entrySet for not owner of transaction returns incorrect results");
            }
            
            //test keySet method
            set = region.keySet();
            k = set.size();
            if(j != 4){
                fail("region.keySet for not owner of transaction returns incorrect results");
            }
            boolean val = region.containsKey("forth");
            if(val){
                fail("containsValue for not owner of transaction returns incorrect result");
            }
            
        }
        catch(Exception ex){
            ex.printStackTrace();
            fail("failed in miscMethodsNotOwner");
        }
    }//end of miscMethodsNotOwner
    
    
    
    //helper methods
    public static Object putMethod(Object ob){
        Object obj=null;
        try{
            if(ob != null){
                String str = "first";
                obj = region.put(ob, str);
            }
        }catch(Exception ex){
            ex.printStackTrace();
            fail("Failed while region.put");
        }
        return obj;
    }
    
    public static Object getMethod(Object ob){
        Object obj=null;
        try{
            obj = region.get(ob);
        } catch(Exception ex){
            fail("Failed while region.get");
        }
        return obj;
    }
    
    
}//end of test class
