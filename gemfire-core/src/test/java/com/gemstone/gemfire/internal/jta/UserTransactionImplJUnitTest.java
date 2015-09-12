/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.jta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.UserTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.distributed.DistributedSystem;
import com.gemstone.gemfire.distributed.internal.DistributionConfig;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;

/**
 * @author unknown
 * @author Kirk Lund
 */
@Category(IntegrationTest.class)
public class UserTransactionImplJUnitTest {

  private static DistributedSystem ds;
  private static TransactionManagerImpl tm;

  private UserTransaction utx = null;

  @BeforeClass
  public static void beforeClass() throws Exception {
    Properties props = new Properties();
    props.setProperty(DistributionConfig.MCAST_PORT_NAME, "0");
    ds = DistributedSystem.connect(props);
    tm = TransactionManagerImpl.getTransactionManager();
  }

  public static void afterClass() throws Exception {
    ds.disconnect();
    ds = null;
    tm = null;
  }

  @Before
  public void setUp() throws Exception {
    utx = new UserTransactionImpl();
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testBegin() throws Exception {
    utx.begin();
    Transaction txn = tm.getTransaction();
    assertNotNull("transaction not registered in the transaction map", txn);
    GlobalTransaction gtx = tm.getGlobalTransaction();
    assertNotNull("Global transaction not registered with the transaction manager", gtx);
    assertTrue("Transaction not added to the list", gtx.getTransactions().contains(txn));
    int status = gtx.getStatus();
    assertEquals("Transaction status not set to be active", Status.STATUS_ACTIVE, status);
    utx.commit();
  }

  @Test
  public void testCommit() throws Exception {
    utx.begin();
    utx.commit();
    Transaction txn = tm.getTransaction();
    assertNull("transaction not removed from map after commit", txn);
  }

  @Test
  public void testRollback() throws Exception {
    utx.begin();
    utx.rollback();

    Transaction txn = tm.getTransaction();
    assertNull("transaction not removed from map after rollback", txn);
  }

  @Test
  public void testSetRollbackOnly() throws Exception {
    utx.begin();
    utx.setRollbackOnly();
    GlobalTransaction gtx = tm.getGlobalTransaction();
    assertEquals("Status not marked for rollback", Status.STATUS_MARKED_ROLLBACK, gtx.getStatus());
    utx.rollback();
  }

  @Test
  public void testGetStatus() throws Exception {
    utx.begin();
    tm.setRollbackOnly();
    int status = utx.getStatus();
    assertEquals("Get status failed to get correct status", Status.STATUS_MARKED_ROLLBACK, status);
    utx.rollback();
  }

  @Test
  public void testThread() throws Exception {
    utx.begin();
    utx.commit();
    utx.begin();
    utx.commit();
  }
}
