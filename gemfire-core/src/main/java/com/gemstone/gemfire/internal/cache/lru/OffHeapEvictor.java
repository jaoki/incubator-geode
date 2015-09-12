/*=========================================================================
 * Copyright (c) 2010-2011 VMware, Inc. All rights reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. VMware products are covered by
 * one or more patents listed at http://www.vmware.com/go/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.cache.lru;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.control.ResourceManager;
import com.gemstone.gemfire.internal.cache.GemFireCacheImpl;
import com.gemstone.gemfire.internal.cache.LocalRegion;
import com.gemstone.gemfire.internal.cache.PartitionedRegion;
import com.gemstone.gemfire.internal.cache.control.InternalResourceManager.ResourceType;
import com.gemstone.gemfire.internal.i18n.LocalizedStrings;
import com.gemstone.gemfire.internal.offheap.MemoryAllocator;

/**
 * Triggers centralized eviction(asynchronously) when the ResourceManager sends
 * an eviction event for off-heap regions. This is registered with the ResourceManager.
 *
 * @author rholmes
 * @since 9.0
 */
public class OffHeapEvictor extends HeapEvictor {
  private static final String EVICTOR_THREAD_GROUP_NAME = "OffHeapEvictorThreadGroup";
  
  private static final String EVICTOR_THREAD_NAME = "OffHeapEvictorThread";
  
  private long bytesToEvictWithEachBurst;
  
  public OffHeapEvictor(Cache gemFireCache) {
    super(gemFireCache);    
    calculateEvictionBurst();
  }

  private void calculateEvictionBurst() {
    float evictionBurstPercentage = Float.parseFloat(System.getProperty("gemfire.HeapLRUCapacityController.evictionBurstPercentage", "0.4"));
    
    MemoryAllocator allocator = ((GemFireCacheImpl) this.cache).getOffHeapStore();
    
    /*
     * Bail if there is no off-heap memory to evict.
     */
    if(null == allocator) {
      throw new IllegalStateException(LocalizedStrings.MEMSCALE_EVICTION_INIT_FAIL.toLocalizedString());
    }
    
    bytesToEvictWithEachBurst = (long)(allocator.getTotalMemory() * 0.01 * evictionBurstPercentage);       
  }
  
  protected int getEvictionLoopDelayTime() {
    if (numEvictionLoopsCompleted < Math.max(3, numFastLoops)) {
      return 250;
    }
    
    return 1000;
  }
  
  protected boolean includePartitionedRegion(PartitionedRegion region) {
    return (region.getEvictionAttributes().getAlgorithm().isLRUHeap() 
        && (region.getDataStore() != null) 
        && region.getAttributes().getOffHeap());
  }
  
  protected boolean includeLocalRegion(LocalRegion region) {
    return (region.getEvictionAttributes().getAlgorithm().isLRUHeap() 
        && region.getAttributes().getOffHeap());
  }
  
  protected String getEvictorThreadGroupName() {
    return OffHeapEvictor.EVICTOR_THREAD_GROUP_NAME;
  }
  
  protected String getEvictorThreadName() {
    return OffHeapEvictor.EVICTOR_THREAD_NAME;
  }

  public long getTotalBytesToEvict() {
    return bytesToEvictWithEachBurst;
  }

  @Override
  protected ResourceType getResourceType() {
    return ResourceType.OFFHEAP_MEMORY;
  }  
}
