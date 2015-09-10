/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.cache.snapshot;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

import com.examples.snapshot.MyPdxSerializer;
import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.snapshot.RegionGenerator.RegionType;
import com.gemstone.gemfire.cache.snapshot.SnapshotOptions.SnapshotFormat;
import com.gemstone.gemfire.cache30.CacheTestCase;
import com.gemstone.gemfire.distributed.DistributedMember;
import com.gemstone.gemfire.internal.cache.snapshot.SnapshotFileMapper;
import com.gemstone.gemfire.internal.cache.snapshot.SnapshotOptionsImpl;

import dunit.Host;
import dunit.SerializableCallable;
import dunit.VM;

public class ParallelSnapshotDUnitTest extends CacheTestCase {
  static byte[] ffff = new byte[] { 0xf, 0xf, 0xf, 0xf };
  static byte[] eeee = new byte[] { 0xe, 0xe, 0xe, 0xe };
  
  private static class TestSnapshotFileMapper implements SnapshotFileMapper {
    volatile boolean explode;
    
    @Override
    public File mapExportPath(DistributedMember member, File snapshot) {
      if (explode) {
        throw new RuntimeException();
      }
      return new File(snapshot.getAbsoluteFile().toString() + VM.getCurrentVMNum());
    }

    @Override
    public File[] mapImportPath(DistributedMember member, File snapshot) {
      if (explode) {
        throw new RuntimeException();
      }

      File f = new File(snapshot.getAbsoluteFile().toString() + VM.getCurrentVMNum());
      return new File[] { f };
    }
  }
  
  public ParallelSnapshotDUnitTest(String name) {
    super(name);
  }
  
  public void testExportImport() throws Exception {
    doExport(false);
    doImport(false);
  }
  
  public void testExportImportErrors() throws Exception {
    try {
      doExport(true);
      fail();
    } catch (Exception e) {
    }
    
    doExport(false);
    try {
      doImport(true);
      fail();
    } catch (Exception e) {
    }
  }
  
  private void doExport(boolean explode) throws Exception {
    Region region = getCache().getRegion("test");
    for (int i = 0; i < 1000; i++) {
      region.put(i, ffff);
    }
    
    RegionSnapshotService rss = region.getSnapshotService();
    
    final TestSnapshotFileMapper mapper = new TestSnapshotFileMapper();
    mapper.explode = explode;

    SnapshotOptionsImpl opt = (SnapshotOptionsImpl) rss.createOptions();
    opt.setParallelMode(true);
    opt.setMapper(mapper);
    
    final File f = new File("mysnap");
    rss.save(f, SnapshotFormat.GEMFIRE, opt);
    
    mapper.explode = false;
    SerializableCallable check = new SerializableCallable() {
      @Override
      public Object call() throws Exception {
        getCache().getDistributedSystem().getDistributedMember();
        File snap = mapper.mapExportPath(
            getCache().getDistributedSystem().getDistributedMember(), f);

        assertTrue("Could not find snapshot: " + snap, snap.exists());
        return null;
      }
    };
    
    forEachVm(check, true);
  }
  
  private void doImport(boolean explode) throws ClassNotFoundException, IOException {
    Region region = getCache().getRegion("test");
    RegionSnapshotService rss = region.getSnapshotService();

    final TestSnapshotFileMapper mapper = new TestSnapshotFileMapper();
    mapper.explode = explode;

    SnapshotOptionsImpl opt = (SnapshotOptionsImpl) rss.createOptions();
    opt.setParallelMode(true);
    opt.setMapper(mapper);
    
    final File f = new File("mysnap");

    for (int i = 0; i < 1000; i++) {
      region.put(i, eeee);
    }
    
    rss.load(f, SnapshotFormat.GEMFIRE, opt);
    for (int i = 0; i < 1000; i++) {
      assertTrue(Arrays.equals(ffff, (byte[]) region.get(i)));
    }
  }
  
  public Object forEachVm(SerializableCallable call, boolean local) throws Exception {
    Host host = Host.getHost(0);
    int vms = host.getVMCount();
    
    for(int i = 0; i < vms; ++i) {
      host.getVM(i).invoke(call);
    }
    
    if (local) {
      return call.call();
    }
    return null;
  }
  
  public void setUp() throws Exception {
    super.setUp();
    loadCache();
  }
  
  public void tearDown2() throws Exception {
    super.tearDown2();
    
    File[] snaps = new File(".").listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith("mysnap");
      }
    });
    
    if (snaps != null) {
      for (File f : snaps) {
        f.delete();
      }
    }
  }
  
  public void loadCache() throws Exception {
    SerializableCallable setup = new SerializableCallable() {
      @Override
      public Object call() throws Exception {
        CacheFactory cf = new CacheFactory()
          .setPdxSerializer(new MyPdxSerializer());
    
        Cache cache = getCache(cf);
        RegionGenerator rgen = new RegionGenerator();
        rgen.createRegion(cache, null, RegionType.PARTITION, "test");

        return null;
      }
    };
    
    forEachVm(setup, true);
  }
}
