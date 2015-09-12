/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.cache;

import java.io.File;

import com.gemstone.gemfire.cache.*;

/**
 * 
 * A testing helper factory to get a disk region with the desired configuration
 * 
 * @author Mitul Bid
 *  
 */
public class DiskRegionHelperFactory
{

  /**
   * @param args
   */
  public static Region getRegion(Cache cache, DiskRegionProperties diskProps, Scope regionScope)
  {
    Region region = null;
    DiskStoreFactory dsf = cache.createDiskStoreFactory();
    AttributesFactory factory = new AttributesFactory();
    if (diskProps.getDiskDirs() == null) {
      File dir = new File("testingDirectoryDefault");
      dir.mkdir();
      dir.deleteOnExit();
      File[] dirs = { dir };
      dsf.setDiskDirsAndSizes(dirs, new int[]{Integer.MAX_VALUE});
    }
    else if (diskProps.getDiskDirSizes() == null) {
      int[] ints = new int[diskProps.getDiskDirs().length];
      for(int i=0; i<ints.length; i++){
        ints[i] = Integer.MAX_VALUE;
      }
      dsf.setDiskDirsAndSizes(diskProps.getDiskDirs(), ints);
    }
    else {
      dsf.setDiskDirsAndSizes(diskProps.getDiskDirs(),
                              diskProps.getDiskDirSizes());
    }
//    Properties props = new Properties();
    ((DiskStoreFactoryImpl)dsf).setMaxOplogSizeInBytes(diskProps.getMaxOplogSize());
    dsf.setAutoCompact(diskProps.isRolling());
    dsf.setAllowForceCompaction(diskProps.getAllowForceCompaction());
    dsf.setCompactionThreshold(diskProps.getCompactionThreshold());
    if (diskProps.getTimeInterval() != -1) {
      dsf.setTimeInterval(diskProps.getTimeInterval());
    }

    if (diskProps.getBytesThreshold() > Integer.MAX_VALUE) {
      dsf.setQueueSize(Integer.MAX_VALUE);
    } else {
      dsf.setQueueSize((int)diskProps.getBytesThreshold());
    }
    factory.setDiskSynchronous(diskProps.isSynchronous());
    DirectoryHolder.SET_DIRECTORY_SIZE_IN_BYTES_FOR_TESTING_PURPOSES = true;
    try {
      factory.setDiskStoreName(dsf.create(diskProps.getRegionName()).getName());
    } finally {
      DirectoryHolder.SET_DIRECTORY_SIZE_IN_BYTES_FOR_TESTING_PURPOSES = false;
    }
    if (diskProps.isPersistBackup()) {
      factory.setDataPolicy(DataPolicy.PERSISTENT_REPLICATE);
    }
    factory.setScope(regionScope);

    if (diskProps.isOverflow()) {
      int capacity = diskProps.getOverFlowCapacity();
      factory.setEvictionAttributes(EvictionAttributes
          .createLRUEntryAttributes(capacity, EvictionAction.OVERFLOW_TO_DISK));

    }

    factory.setConcurrencyLevel(diskProps.getConcurrencyLevel());
    factory.setInitialCapacity(diskProps.getInitialCapacity());
    factory.setLoadFactor(diskProps.getLoadFactor());
    factory.setStatisticsEnabled(diskProps.getStatisticsEnabled());

    try {
      region = cache.createVMRegion(diskProps.getRegionName(), factory
          .createRegionAttributes());
    }
    catch (TimeoutException e) {
      throw new RuntimeException(
          " failed to create region due  to a TimeOutException " + e);
    }
    catch (RegionExistsException e) {
      throw new RuntimeException(
          " failed to create region due  to a RegionExistsException " + e);
    }
    return region;
  }

  public static Region getSyncPersistOnlyRegion(Cache cache,
      DiskRegionProperties diskRegionProperties, Scope regionScope)
  {
    if (diskRegionProperties == null) {
      diskRegionProperties = new DiskRegionProperties();
    }
    diskRegionProperties.setPersistBackup(true);
    diskRegionProperties.setSynchronous(true);
    return getRegion(cache, diskRegionProperties, regionScope);

  }
  
  public static Region getAsyncPersistOnlyRegion(Cache cache,
      DiskRegionProperties diskRegionProperties)
  {
    if (diskRegionProperties == null) {
      diskRegionProperties = new DiskRegionProperties();
    }
    diskRegionProperties.setPersistBackup(true);
    diskRegionProperties.setSynchronous(false);
    return getRegion(cache, diskRegionProperties, Scope.LOCAL);
  }
  
  public static Region getSyncOverFlowOnlyRegion(Cache cache,
      DiskRegionProperties diskRegionProperties)
  {
    if (diskRegionProperties == null) {
      diskRegionProperties = new DiskRegionProperties();
    }
    diskRegionProperties.setPersistBackup(false);
    diskRegionProperties.setSynchronous(true);
    diskRegionProperties.setOverflow(true);
    return getRegion(cache, diskRegionProperties, Scope.LOCAL);
  }

  public static Region getAsyncOverFlowOnlyRegion(Cache cache,
      DiskRegionProperties diskRegionProperties)
  {
    if (diskRegionProperties == null) {
      diskRegionProperties = new DiskRegionProperties();
    }
    diskRegionProperties.setPersistBackup(false);
    diskRegionProperties.setSynchronous(false);
    diskRegionProperties.setOverflow(true);
    return getRegion(cache, diskRegionProperties, Scope.LOCAL);
  }

  public static Region getSyncOverFlowAndPersistRegion(Cache cache,
      DiskRegionProperties diskRegionProperties)
  {
    if (diskRegionProperties == null) {
      diskRegionProperties = new DiskRegionProperties();
    }
    diskRegionProperties.setPersistBackup(true);
    diskRegionProperties.setSynchronous(true);
    diskRegionProperties.setOverflow(true);
    return getRegion(cache, diskRegionProperties, Scope.LOCAL);
  }

  public static Region getAsyncOverFlowAndPersistRegion(Cache cache,
      DiskRegionProperties diskRegionProperties)
  {
    if (diskRegionProperties == null) {
      diskRegionProperties = new DiskRegionProperties();
    }
    diskRegionProperties.setPersistBackup(true);
    diskRegionProperties.setSynchronous(false);
    diskRegionProperties.setOverflow(true);
    return getRegion(cache, diskRegionProperties, Scope.LOCAL);
  }

  public static Region getSyncPersistOnlyRegionInfiniteOplog(Cache cache,
      DiskRegionProperties diskRegionProperties, String regionName)
  {
    if (diskRegionProperties == null) {
      diskRegionProperties = new DiskRegionProperties();
    }
    diskRegionProperties.setMaxOplogSize(0);
    diskRegionProperties.setRolling(false);
    diskRegionProperties.setPersistBackup(true);
    diskRegionProperties.setSynchronous(true);
    return getRegion(cache, diskRegionProperties, Scope.LOCAL);

  }

  public static Region getAsyncPersistOnlyRegionInfiniteOplog(Cache cache,
      DiskRegionProperties diskRegionProperties)
  {
    if (diskRegionProperties == null) {
      diskRegionProperties = new DiskRegionProperties();
    }
    diskRegionProperties.setMaxOplogSize(0);
    diskRegionProperties.setRolling(false);
    diskRegionProperties.setPersistBackup(true);
    diskRegionProperties.setSynchronous(false);
    return getRegion(cache, diskRegionProperties, Scope.LOCAL);
  }

  public static Region getSyncOverFlowOnlyRegionInfiniteOplog(Cache cache,
      DiskRegionProperties diskRegionProperties)
  {
    if (diskRegionProperties == null) {
      diskRegionProperties = new DiskRegionProperties();
    }
    diskRegionProperties.setMaxOplogSize(0);
    diskRegionProperties.setRolling(false);
    diskRegionProperties.setPersistBackup(false);
    diskRegionProperties.setSynchronous(true);
    diskRegionProperties.setOverflow(true);
    return getRegion(cache, diskRegionProperties, Scope.LOCAL);
  }

  public static Region getAsyncOverFlowOnlyRegionInfiniteOplog(Cache cache,
      DiskRegionProperties diskRegionProperties)
  {
    if (diskRegionProperties == null) {
      diskRegionProperties = new DiskRegionProperties();
    }
    diskRegionProperties.setMaxOplogSize(0);
    diskRegionProperties.setRolling(false);
    diskRegionProperties.setPersistBackup(false);
    diskRegionProperties.setSynchronous(false);
    diskRegionProperties.setOverflow(true);
    return getRegion(cache, diskRegionProperties, Scope.LOCAL);
  }

  public static Region getSyncOverFlowAndPersistRegionInfiniteOplog(
      Cache cache, DiskRegionProperties diskRegionProperties)
  {
    if (diskRegionProperties == null) {
      diskRegionProperties = new DiskRegionProperties();
    }
    diskRegionProperties.setMaxOplogSize(0);
    diskRegionProperties.setRolling(false);
    diskRegionProperties.setPersistBackup(true);
    diskRegionProperties.setSynchronous(true);
    diskRegionProperties.setOverflow(true);
    return getRegion(cache, diskRegionProperties, Scope.LOCAL);
  }

  public static Region getAsyncOverFlowAndPersistRegionInfiniteOplog(
      Cache cache, DiskRegionProperties diskRegionProperties)
  {
    if (diskRegionProperties == null) {
      diskRegionProperties = new DiskRegionProperties();
    }
    diskRegionProperties.setMaxOplogSize(0);
    diskRegionProperties.setRolling(false);
    diskRegionProperties.setPersistBackup(true);
    diskRegionProperties.setSynchronous(false);
    diskRegionProperties.setOverflow(true);
    return getRegion(cache, diskRegionProperties, Scope.LOCAL);
  }

}
