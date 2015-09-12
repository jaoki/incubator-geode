/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.cache;

import com.gemstone.gemfire.internal.cache.wan.GatewaySenderEventImpl;
import com.gemstone.gemfire.internal.cache.wan.serial.SerialGatewaySenderQueue;
import com.gemstone.gemfire.internal.offheap.annotations.Released;
import com.gemstone.gemfire.internal.offheap.annotations.Retained;
/**
 * 
 * @author sbawaska
 *
 */
public abstract class AbstractDiskRegionEntry
  extends AbstractRegionEntry
  implements DiskEntry
{
  protected AbstractDiskRegionEntry(RegionEntryContext context, Object value) {
    super(context, value);
  }
  
  @Override
  public  void setValue(RegionEntryContext context, Object v) throws RegionClearedException {
    setValue(context, v, null);
  }
  
  @Override
  public void setValue(RegionEntryContext context, Object value, EntryEventImpl event) throws RegionClearedException {
    Helper.update(this, (LocalRegion) context, value, event);
    setRecentlyUsed(); // fix for bug #42284 - entry just put into the cache is evicted
  }

  /**
   * Sets the value with a {@link RegionEntryContext}.
   * @param context the value's context.
   * @param value an entry value.
   */
  @Override
  public void setValueWithContext(RegionEntryContext context, Object value) {
    _setValue(value);
    if (value != null && context != null && (this instanceof OffHeapRegionEntry) 
        && context instanceof LocalRegion && ((LocalRegion)context).isThisRegionBeingClosedOrDestroyed()) {
      ((OffHeapRegionEntry)this).release();
      ((LocalRegion)context).checkReadiness();
    }
  }
  
  // Do not add any instances fields to this class.
  // Instead add them to the DISK section of LeafRegionEntry.cpp.

  @Override
  public void handleValueOverflow(RegionEntryContext context) {
    if (context instanceof BucketRegionQueue || context instanceof SerialGatewaySenderQueue.SerialGatewaySenderQueueMetaRegion) {
      GatewaySenderEventImpl.release(this._getValue()); // OFFHEAP _getValue ok
    }
  }
  @Override
  public void afterValueOverflow(RegionEntryContext context) {
    //NO OP
    //Overridden in sqlf RegionEntry
  }
}
