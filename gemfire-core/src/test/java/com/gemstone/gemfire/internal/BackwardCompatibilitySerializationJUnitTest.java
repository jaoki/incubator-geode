/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.internal.cache.DistributedPutAllOperation.EntryVersionsList;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;
import com.gemstone.org.jgroups.util.VersionedStreamable;

/**
 * Test the DSFID serialization framework added for rolling upgrades in 7.1
 * 
 * @author tnomulwar
 * 
 * 
 */
@Category(IntegrationTest.class)
public class BackwardCompatibilitySerializationJUnitTest {

  private transient ByteArrayOutputStream baos;
  private transient ByteArrayInputStream bais;

  public static boolean toDataCalled = false;
  public static boolean toDataPre66Called = false;
  public static boolean toDataPre70called = false;
  public static boolean fromDataCalled = false;
  public static boolean fromDataPre66Called = false;
  public static boolean fromDataPre70Called = false;

  public TestMessage msg = new TestMessage();

  public BackwardCompatibilitySerializationJUnitTest() {
  }

  @Before
  public void setUp() {
    baos = new ByteArrayOutputStream();
    // register TestMessage using an existing dsfid
    DSFIDFactory.registerDSFID(DataSerializableFixedID.PUTALL_VERSIONS_LIST,
        TestMessage.class);
  }

  @After
  public void tearDown() {
    resetFlags();
    // reset the class mapped to the dsfid
    DSFIDFactory.registerDSFID(DataSerializableFixedID.PUTALL_VERSIONS_LIST,
        EntryVersionsList.class);
    this.baos = null;
    this.bais = null;
  }

  /**
   * Test if correct toData/toDataPreXXX is called when changes are made to the
   * TestMessage in 66 and 70 and version of peer is 56
   * 
   * @throws Exception
   */
  @Test
  public void testToDataFromHigherVersionToLower() throws Exception {
    DataOutputStream dos = new VersionedDataOutputStream(new DataOutputStream(
        baos), Version.GFE_56);
    InternalDataSerializer.writeDSFID(msg, dos);
    assertTrue(toDataPre66Called);
    assertFalse(toDataCalled);
  }

  /**
   * Test if correct toData/toDataXXX is called when changes are made to the
   * TestMessage in 66 and 70 and version of peer is 70
   * 
   * @throws Exception
   */
  @Test
  public void testToDataFromLowerVersionToHigher() throws Exception {
    DataOutputStream dos = new VersionedDataOutputStream(new DataOutputStream(
        baos), Version.GFE_701);
    InternalDataSerializer.writeDSFID(msg, dos);
    assertTrue(toDataCalled);
  }

  /**
   * Test if correct fromData/fromDataXXX is called when changes are made to the
   * TestMessage in 66 and 70 and version of peer is 70
   * 
   * @throws Exception
   */
  @Test
  public void testFromDataFromHigherVersionToLower() throws Exception {
    InternalDataSerializer.writeDSFID(msg, new DataOutputStream(baos));
    this.bais = new ByteArrayInputStream(baos.toByteArray());

    DataInputStream dis = new VersionedDataInputStream(
        new DataInputStream(bais), Version.GFE_701);
    Object o = InternalDataSerializer.basicReadObject(dis);
    assertTrue(o instanceof TestMessage);
    assertTrue(fromDataCalled);
  }

  /**
   * Test if correct fromData/fromDataXXX is called when changes are made to the
   * TestMessage in 66 and 70 and version of peer is 56
   * 
   * @throws Exception
   */
  @Test
  public void testFromDataFromLowerVersionToHigher() throws Exception {
    InternalDataSerializer.writeDSFID(msg, new DataOutputStream(baos));
    this.bais = new ByteArrayInputStream(baos.toByteArray());

    DataInputStream dis = new VersionedDataInputStream(
        new DataInputStream(bais), Version.GFE_56);
    Object o = InternalDataSerializer.basicReadObject(dis);
    assertTrue(o instanceof TestMessage);
    assertTrue(fromDataPre66Called);
  }

  /**
   * Test if all messages implement toDataPreXXX and fromDataPreXXX if the
   * message has been upgraded in any of the versions
   * 
   * @throws Exception
   */
  @Test
  public void testAllMessages() throws Exception {
    // list of msgs not created using reflection
    // taken from DSFIDFactory.create()
    ArrayList<Integer> constdsfids = new ArrayList<Integer>();
    constdsfids.add(new Byte(DataSerializableFixedID.REGION).intValue());
    constdsfids.add(new Byte(DataSerializableFixedID.END_OF_STREAM_TOKEN)
        .intValue());
    constdsfids.add(new Byte(DataSerializableFixedID.DLOCK_REMOTE_TOKEN)
        .intValue());
    constdsfids
        .add(new Byte(DataSerializableFixedID.TRANSACTION_ID).intValue());
    constdsfids.add(new Byte(DataSerializableFixedID.INTEREST_RESULT_POLICY)
        .intValue());
    constdsfids.add(new Byte(DataSerializableFixedID.UNDEFINED).intValue());
    constdsfids.add(new Byte(DataSerializableFixedID.RESULTS_BAG).intValue());
    constdsfids.add(new Byte(DataSerializableFixedID.GATEWAY_EVENT_IMPL_66)
        .intValue());
    constdsfids.add(new Byte(DataSerializableFixedID.SQLF_TYPE).intValue());
    constdsfids.add(new Byte(DataSerializableFixedID.SQLF_DVD_OBJECT)
        .intValue());
    constdsfids.add(new Byte(DataSerializableFixedID.SQLF_GLOBAL_ROWLOC)
        .intValue());
    constdsfids.add(new Byte(DataSerializableFixedID.SQLF_GEMFIRE_KEY)
        .intValue());
    constdsfids.add(new Byte(DataSerializableFixedID.SQLF_FORMATIBLEBITSET)
        .intValue());
    constdsfids
        .add(new Short(DataSerializableFixedID.TOKEN_INVALID).intValue());
    constdsfids.add(new Short(DataSerializableFixedID.TOKEN_LOCAL_INVALID)
        .intValue());
    constdsfids.add(new Short(DataSerializableFixedID.TOKEN_DESTROYED)
        .intValue());
    constdsfids
        .add(new Short(DataSerializableFixedID.TOKEN_REMOVED).intValue());
    constdsfids.add(new Short(DataSerializableFixedID.TOKEN_REMOVED2)
        .intValue());
    constdsfids.add(new Short(DataSerializableFixedID.TOKEN_TOMBSTONE)
        .intValue());

    for (int i = 0; i < 256; i++) {
      Constructor<?> cons = DSFIDFactory.getDsfidmap()[i];
      if (!constdsfids.contains(i - Byte.MAX_VALUE - 1) && cons != null) {
        Object ds = cons.newInstance((Object[]) null);
        checkSupportForRollingUpgrade(ds);
      }
    }
    
    // some msgs require distributed system
    Cache c = new CacheFactory().set("mcast-port", ""+AvailablePort.getRandomAvailablePort(AvailablePort.JGROUPS)).create();
    for (Object o : DSFIDFactory.getDsfidmap2().values()) {
      Constructor<?> cons = (Constructor<?>) o;
      if (cons != null) {
        DataSerializableFixedID ds = (DataSerializableFixedID) cons
            .newInstance((Object[]) null);
        checkSupportForRollingUpgrade(ds);
      }
    }
    c.close();
  }

  private void checkSupportForRollingUpgrade(Object ds) {
    Version[] versions = null;
    if (ds instanceof SerializationVersions) {
      versions = ((SerializationVersions)ds).getSerializationVersions();
    } else {
      short[] ordinals = ((VersionedStreamable)ds).getSerializationVersions();
      if (ordinals != null && ordinals.length > 0) {
        versions = new Version[ordinals.length];
        for (int i=ordinals.length; i>=0; i--) {
          versions[i] = Version.fromOrdinalNoThrow(ordinals[i], false);
        }
      }
    }

    if (versions != null && versions.length > 0) {
      for (int i = 0; i < versions.length; i++) {
        try {
          ds.getClass().getMethod(
              "toDataPre_" + versions[i].getMethodSuffix(),
              new Class[] { DataOutput.class });

          ds.getClass().getMethod(
              "fromDataPre_" + versions[i].getMethodSuffix(),
              new Class[] { DataInput.class });
        } catch (NoSuchMethodException e) {
          fail("toDataPreXXX or fromDataPreXXX for previous versions not found "
              + e.getMessage());
        }
      }
    } else {
      for(Method method : ds.getClass().getMethods()) {
        if(method.getName().startsWith("toDataPre")) {
          fail("Found backwards compatible toData, but class does not implement getSerializationVersions()" + method);
        } else if(method.getName().startsWith("fromDataPre")) {
          fail("Found backwards compatible fromData, but class does not implement getSerializationVersions()" + method);
        }
      }
     
    }
  }

  private void resetFlags() {
    toDataCalled = false;
    toDataPre66Called = false;
    toDataPre70called = false;
    fromDataCalled = false;
    fromDataPre66Called = false;
    fromDataPre70Called = false;
  }

  public static final class TestMessage implements DataSerializableFixedID {
    /** The versions in which this message was modified */
    private static final Version[] dsfidVersions = new Version[] {
        Version.GFE_66, Version.GFE_70 };

    public TestMessage() {
    }

    @Override
    public Version[] getSerializationVersions() {
      return dsfidVersions;
    }

    @Override
    public void toData(DataOutput out) throws IOException {
      toDataCalled = true;
    }

    public void toDataPre_GFE_6_6_0_0(DataOutput out) throws IOException {
      toDataPre66Called = true;
    }

    public void toDataPre_GFE_7_0_0_0(DataOutput out) throws IOException {
      toDataPre70called = true;
    }

    @Override
    public void fromData(DataInput in) throws IOException {
      fromDataCalled = true;
    }

    public void fromDataPre_GFE_6_6_0_0(DataInput out) throws IOException {
      fromDataPre66Called = true;
    }

    public void fromDataPre_GFE_7_0_0_0(DataInput out) throws IOException {
      fromDataPre70Called = true;
    }

    @Override
    public int getDSFID() {
      return DataSerializableFixedID.PUTALL_VERSIONS_LIST;
    }

  }
}
