/*=========================================================================
 * Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */

package com.gemstone.gemfire.internal.cache;

import java.util.UUID;

import com.gemstone.gemfire.internal.offheap.SimpleMemoryAllocatorImpl.ConcurrentBag;


/**
 * Implementation class of RegionEntry interface.
 * VM -> entries stored in VM memory
 * Thin -> no extra statistics
 *
 * @since 3.5.1
 *
 * @author Darrel Schneider
 *
 */
public abstract class VMThinRegionEntry extends AbstractRegionEntry {
  protected VMThinRegionEntry(RegionEntryContext context, Object value) {
    super(context, value);
  }
}

