/*=========================================================================
 * Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.stats50;

import static org.junit.Assert.assertEquals;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.StatisticDescriptor;
import com.gemstone.gemfire.Statistics;
import com.gemstone.gemfire.StatisticsType;
import com.gemstone.gemfire.StatisticsTypeFactory;
import com.gemstone.gemfire.distributed.DistributedSystem;
import com.gemstone.gemfire.internal.StatisticsTypeFactoryImpl;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;
import com.gemstone.org.jgroups.oswego.concurrent.BrokenBarrierException;
import com.gemstone.org.jgroups.oswego.concurrent.CyclicBarrier;

/**
 * @author dsmith
 *
 */
@Category(IntegrationTest.class)
public class AtomicStatsJUnitTest {
  
  /**
   * Test for bug 41340. Do two gets at the same time of a dirty
   * stat, and make sure we get the correct value for the stat.
   * @throws Throwable
   */
  @Test
  public void testConcurrentGets() throws Throwable {
    
    Properties props = new Properties();
    props.setProperty("mcast-port", "0");
    //    props.setProperty("statistic-sample-rate", "60000");
    props.setProperty("statistic-sampling-enabled", "false");
    DistributedSystem ds = DistributedSystem.connect(props);
    
    String statName = "TestStats";
    String statDescription =
      "Tests stats";

    final String statDesc =
      "blah blah blah";

    StatisticsTypeFactory f = StatisticsTypeFactoryImpl.singleton();

    StatisticsType type = f.createType(statName, statDescription,
       new StatisticDescriptor[] {
         f.createIntGauge("stat", statDesc, "bottles of beer on the wall"),
       });

    final int statId = type.nameToId("stat");

    try {

      final AtomicReference<Statistics> statsRef = new AtomicReference<Statistics>();
      final CyclicBarrier beforeIncrement = new CyclicBarrier(3);
      final CyclicBarrier afterIncrement = new CyclicBarrier(3);
      Thread thread1 = new Thread("thread1") {
        public void run() {
          try {
            while(true) {
              beforeIncrement.barrier();
              statsRef.get().incInt(statId, 1);
              afterIncrement.barrier();
            }
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          } catch (BrokenBarrierException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      };
      Thread thread3 = new Thread("thread1") {
        public void run() {
          try {
            while(true) {
              beforeIncrement.barrier();
              afterIncrement.barrier();
              statsRef.get().getInt(statId);
            }
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          } catch (BrokenBarrierException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      };
      thread1.start();
      thread3.start();
      for(int i =0; i < 5000; i++) {
        Statistics stats = ds.createAtomicStatistics(type, "stats");
        statsRef.set(stats);
        beforeIncrement.barrier();
        afterIncrement.barrier();
        assertEquals("On loop " + i, 1, stats.getInt(statId));
        stats.close();
      }
    
    } finally {
      ds.disconnect();
    }
  }
}
