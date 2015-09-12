/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
                                                                                                                             
package com.gemstone.gemfire.internal.jta.dunit;
                                                                                                                             
//import dunit.*;
//import java.io.*;
//import java.util.*;
//import java.net.*;
import com.gemstone.gemfire.LogWriter;
import com.gemstone.gemfire.cache.*;
//import com.gemstone.gemfire.distributed.*;
//import java.util.Hashtable;
//import javax.naming.InitialContext;
import javax.naming.Context;
import javax.sql.*;
import javax.transaction.*;
import java.sql.*;
//import java.lang.Exception.*;
//import java.lang.RuntimeException;
//import java.sql.SQLException.*;
import javax.naming.NamingException;
//import javax.naming.NoInitialContextException;
//import javax.transaction.SystemException;
import com.gemstone.gemfire.internal.jta.CacheUtils;
//import com.gemstone.gemfire.internal.jta.JTAUtils;
                                                                                                                             
/**
*This is thread class
*The objective of this thread class is to implement the inserts and rollback 
*This thread will be called from TxnManagerMultiThreadDUnitTest.java
*This is to test the concurrent execution of the run method and see if transaction manager handles it properly
*
*
*@author Prafulla Chaudhari
*
*/


public class RollbackThread implements Runnable{

/////constructor/////

public Thread thd;
public String threadName;
private LogWriter log;

public RollbackThread(String name, LogWriter log){
    threadName=name;
    this.log = log;
    thd = new Thread(this, threadName);
    thd.start();
}//end of constuctor RollbackThread

/////synchronized method/////
/*
*This is to make sure that key field in table is getting inserted with a unique value by every thread.
*
*/
                                                                                                                             
static int keyFld = 0;
                                                                                                                             
synchronized public static int getUniqueKey(){
    keyFld = keyFld + 5;
    return keyFld;
}

/*
*Following the the run method of this thread.
*This method is implemented to inserts the rows in the database and rollback them
*
*/

public void run(){
                                                                                                                             
    //Region currRegion=null;
    Cache cache;
    int tblIDFld;
    String tblNameFld;
//    boolean to_continue = true;
    final int XA_INSERTS= 2;
                                                                                                                             
    //get the cache
    //this is used to get the context for transaction later in the same method
    cache = TxnManagerMultiThreadDUnitTest.getCache ();
                                                                                                                             
    //get the table name from CacheUtils
    String tblName = CacheUtils.getTableName();
                                                                                                                             
    tblIDFld = 1;
    tblNameFld = "thdOneCommit";
                                                                                                                             
    //initialize cache and get user transaction                                                                                                                              
    Context ctx = cache.getJNDIContext();
    UserTransaction ta = null;
    Connection xa_conn = null;
    try {
        ta = (UserTransaction)ctx.lookup("java:/UserTransaction");
        //ta.setTransactionTimeout(300);
        } catch (NamingException nme) {
               nme.printStackTrace();
        } catch (Exception e) {
               e.printStackTrace();
           }
                                                                                                                             
    try{
        DataSource d1 = (DataSource)ctx.lookup("java:/SimpleDataSource");
        Connection con = d1.getConnection();
        con.close();                                                                                                                     
        //Begin the user transaction
        ta.begin();
                                                                                                                             
        //Obtain XAPooledDataSource                                                                                                                              
        DataSource da = (DataSource)ctx.lookup("java:/XAPooledDataSource");
                                                                                                                             
        //obtain connection from XAPooledDataSource                                                                                                                              
        xa_conn = da.getConnection();
                                                                                                                             
        Statement xa_stmt = xa_conn.createStatement();
                                                                                                                             
        String sqlSTR;
                                                                                                                             
        //get the unique value for key to be inserted
        int uniqueKey = getUniqueKey();
                                                                                                                             
        //insert XA_INSERTS rows into timestamped table
        for (int i=0; i<XA_INSERTS;i++){
        tblIDFld= tblIDFld + uniqueKey+i;
        sqlSTR = "insert into " + tblName + " values (" + tblIDFld + "," + "'" + tblNameFld + "'" + ")" ;
        //log.info("Thread= "+Thread.currentThread()+" ... sqlStr= "+ sqlSTR + "Before  update");
        xa_stmt.executeUpdate(sqlSTR);
        //log.info("Thread= "+Thread.currentThread()+" ... sqlStr= "+ sqlSTR + "after  update");
        }
                                                                                                                             
        //close the Simple and XA statements                                                                                                                              
        xa_stmt.close();
                                                                                                                             
        //close the connections
        xa_conn.close();
                                                                                                                             
        //log.info("Thread Before Commit..."+Thread.currentThread());

         //rollback the transaction
        ta.rollback();
                                                                                                                             
        }
        catch (NamingException nme){
            nme.printStackTrace();
        }
        catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        catch (Exception e){
           e.printStackTrace();
        }
        finally
        {
               if (xa_conn != null) {
                     try {
                        //close the connections
                        xa_conn.close();
                        } catch (Exception e) {
                          e.printStackTrace();
                           }
                 }
        }
                                                                                                                             
    log.info(XA_INSERTS+": Rows were inserted and rolled back successfully");
}//end of run method


}
