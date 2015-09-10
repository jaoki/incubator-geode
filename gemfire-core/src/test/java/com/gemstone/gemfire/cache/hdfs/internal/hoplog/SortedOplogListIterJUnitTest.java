/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.cache.hdfs.internal.hoplog;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.cache.hdfs.internal.PersistedEventImpl;
import com.gemstone.gemfire.cache.hdfs.internal.SortedHoplogPersistedEvent;
import com.gemstone.gemfire.internal.cache.persistence.soplog.TrackedReference;
import com.gemstone.gemfire.internal.util.BlobHelper;
import com.gemstone.gemfire.test.junit.categories.HoplogTest;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest
;

@Category({IntegrationTest.class, HoplogTest.class})
public class SortedOplogListIterJUnitTest extends BaseHoplogTestCase {
  public void testOneIterOneKey() throws Exception {
    HdfsSortedOplogOrganizer organizer = new HdfsSortedOplogOrganizer(regionManager, 0);

    ArrayList<TestEvent> items = new ArrayList<TestEvent>();
    items.add(new TestEvent(("0"), ("0")));
    organizer.flush(items.iterator(), items.size());

    List<TrackedReference<Hoplog>> oplogs = organizer.getSortedOplogs();
    HoplogSetIterator iter = new HoplogSetIterator(oplogs);
    assertTrue(iter.hasNext());
    int count = 0;
    for (ByteBuffer keyBB = null; iter.hasNext();) {
      keyBB = iter.next();
      byte[] key = HFileSortedOplog.byteBufferToArray(keyBB);
      assertEquals(String.valueOf(count), BlobHelper.deserializeBlob(key));
      count++;
    }
    assertEquals(1, count);
    organizer.close();
  }
  
  public void testOneIterDuplicateKey() throws Exception {
    HdfsSortedOplogOrganizer organizer = new HdfsSortedOplogOrganizer(regionManager, 0);
    
    ArrayList<TestEvent> items = new ArrayList<TestEvent>();
    items.add(new TestEvent(("0"), ("V2")));
    items.add(new TestEvent(("0"), ("V1")));
    items.add(new TestEvent(("1"), ("V2")));
    items.add(new TestEvent(("1"), ("V1")));
    organizer.flush(items.iterator(), items.size());
    
    List<TrackedReference<Hoplog>> oplogs = organizer.getSortedOplogs();
    HoplogSetIterator iter = new HoplogSetIterator(oplogs);
    assertTrue(iter.hasNext());
    int count = 0;
    for (ByteBuffer keyBB = null; iter.hasNext();) {
      keyBB = iter.next();
      byte[] key = HFileSortedOplog.byteBufferToArray(keyBB);
      byte[] value = HFileSortedOplog.byteBufferToArray(iter.getValue());
      assertEquals(String.valueOf(count), BlobHelper.deserializeBlob(key));
      assertEquals("V2", ((PersistedEventImpl) SortedHoplogPersistedEvent.fromBytes(value)).getValue());
      count++;
    }
    assertEquals(2, count);
    organizer.close();
  }
  
  public void testTwoIterSameKey() throws Exception {
    HdfsSortedOplogOrganizer organizer = new HdfsSortedOplogOrganizer(regionManager, 0);
    
    ArrayList<TestEvent> items = new ArrayList<TestEvent>();
    items.add(new TestEvent(("0"), ("V1")));
    organizer.flush(items.iterator(), items.size());
    items.clear();
    items.add(new TestEvent(("0"), ("V2")));
    organizer.flush(items.iterator(), items.size());
    
    List<TrackedReference<Hoplog>> oplogs = organizer.getSortedOplogs();
    HoplogSetIterator iter = new HoplogSetIterator(oplogs);
    assertTrue(iter.hasNext());
    int count = 0;
    for (ByteBuffer keyBB = null; iter.hasNext();) {
      keyBB = iter.next();
      byte[] key = HFileSortedOplog.byteBufferToArray(keyBB);
      byte[] value = HFileSortedOplog.byteBufferToArray(iter.getValue());
      assertEquals(String.valueOf(count), BlobHelper.deserializeBlob(key));
      assertEquals("V2", ((PersistedEventImpl) SortedHoplogPersistedEvent.fromBytes(value)).getValue());
      count++;
    }
    assertEquals(1, count);
    organizer.close();
  }
  
  public void testTwoIterDiffKey() throws Exception {
    HdfsSortedOplogOrganizer organizer = new HdfsSortedOplogOrganizer(regionManager, 0);
    
    ArrayList<TestEvent> items = new ArrayList<TestEvent>();
    items.add(new TestEvent(("0"), ("V1")));
    organizer.flush(items.iterator(), items.size());
    items.clear();
    items.add(new TestEvent(("1"), ("V1")));
    organizer.flush(items.iterator(), items.size());
    
    List<TrackedReference<Hoplog>> oplogs = organizer.getSortedOplogs();
    HoplogSetIterator iter = new HoplogSetIterator(oplogs);
    assertTrue(iter.hasNext());
    int count = 0;
    for (ByteBuffer keyBB = null; iter.hasNext();) {
      keyBB = iter.next();
      byte[] key = HFileSortedOplog.byteBufferToArray(keyBB);
      byte[] value = HFileSortedOplog.byteBufferToArray(iter.getValue());
      assertEquals(String.valueOf(count), BlobHelper.deserializeBlob(key));
      assertEquals("V1", ((PersistedEventImpl) SortedHoplogPersistedEvent.fromBytes(value)).getValue());
      count++;
    }
    assertEquals(2, count);
    organizer.close();
  }
  
  public void testMergedIterator() throws Exception {
    HdfsSortedOplogOrganizer organizer = new HdfsSortedOplogOrganizer(regionManager, 0);

    // #1
    ArrayList<TestEvent> items = new ArrayList<TestEvent>();
    items.add(new TestEvent(("1"), ("1")));
    items.add(new TestEvent(("2"), ("1")));
    items.add(new TestEvent(("3"), ("1")));
    items.add(new TestEvent(("4"), ("1")));
    organizer.flush(items.iterator(), items.size());

    // #2
    items.clear();
    items.add(new TestEvent(("2"), ("1")));
    items.add(new TestEvent(("4"), ("1")));
    items.add(new TestEvent(("6"), ("1")));
    items.add(new TestEvent(("8"), ("1")));
    organizer.flush(items.iterator(), items.size());

    // #3
    items.clear();
    items.add(new TestEvent(("1"), ("1")));
    items.add(new TestEvent(("3"), ("1")));
    items.add(new TestEvent(("5"), ("1")));
    items.add(new TestEvent(("7"), ("1")));
    items.add(new TestEvent(("9"), ("1")));
    organizer.flush(items.iterator(), items.size());

    // #4
    items.clear();
    items.add(new TestEvent(("0"), ("1")));
    items.add(new TestEvent(("1"), ("1")));
    items.add(new TestEvent(("4"), ("1")));
    items.add(new TestEvent(("5"), ("1")));
    organizer.flush(items.iterator(), items.size());

    List<TrackedReference<Hoplog>> oplogs = organizer.getSortedOplogs();
    HoplogSetIterator iter = new HoplogSetIterator(oplogs);
    // the iteration pattern for this test should be 0-9:
    // 0 1 4 5 oplog #4
    // 1 3 5 7 9 oplog #3
    // 2 4 6 8 oplog #2
    // 1 2 3 4 oplog #1
    int count = 0;
    for (ByteBuffer keyBB = null; iter.hasNext();) {
      keyBB = iter.next();
      byte[] key = HFileSortedOplog.byteBufferToArray(keyBB);
      assertEquals(String.valueOf(count), BlobHelper.deserializeBlob(key));
      count++;
    }
    assertEquals(10, count);
    organizer.close();
  }
}
