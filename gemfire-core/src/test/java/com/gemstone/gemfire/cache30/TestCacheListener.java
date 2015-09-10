/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.cache30;

import com.gemstone.gemfire.SystemFailure;
import com.gemstone.gemfire.cache.*;
import java.util.*;

/**
 * A <code>CacheListener</code> used in testing.  Its callback methods
 * are implemented to thrown {@link UnsupportedOperationException}
 * unless the user overrides the "2" methods.
 *
 * @see #wasInvoked
 *
 * @author David Whitlock
 * @since 3.0
 */
public abstract class TestCacheListener extends TestCacheCallback
  implements CacheListener {

  private List eventHistory = null;

  /**
   * Should be called for every event delivered to this listener
   */
  private void addEvent(CacheEvent e, boolean setInvoked) {
    if (setInvoked) {
      this.invoked = true;
    }
    if (this.eventHistory != null) {
      synchronized (this.eventHistory) {
        this.eventHistory.add(e);
      }
    }
  }
  private void addEvent(CacheEvent e) {
    addEvent(e, true);
  }
  /**
   * Enables collection of event history.
   * @since 5.0
   */
  public void enableEventHistory() {
    if (this.eventHistory == null) {
      this.eventHistory = new ArrayList();
    }
  }
  /**
   * Disables collection of events.
   * @since 5.0
   */
  public void disableEventHistory() {
    this.eventHistory = null;
  }
  /**
   * Returns a copy of the list of events collected in this listener's history.
   * Also clears the current history.
   * @since 5.0
   */
  public List getEventHistory() {
    if (this.eventHistory == null) {
      return Collections.EMPTY_LIST;
    } else {
      synchronized (this.eventHistory) {
        List result = this.eventHistory;
        this.eventHistory = new ArrayList();
        return result;
      }
    }
  }
  public final void afterCreate(EntryEvent event) {
    addEvent(event);
    try {
      afterCreate2(event);
    }
    catch (VirtualMachineError e) {
      SystemFailure.initiateFailure(e);
      throw e;
    }
    catch (Throwable t) {
      this.callbackError = t;
    }
  }

  public void afterCreate2(EntryEvent event) {
    String s = "Unexpected callback invocation";
    throw new UnsupportedOperationException(s);
  }

  public final void afterUpdate(EntryEvent event) {
    addEvent(event);
    try {
      afterUpdate2(event);
    }
    catch (VirtualMachineError e) {
      SystemFailure.initiateFailure(e);
      throw e;
    }
    catch (Throwable t) {
      this.callbackError = t;
    }
  }

  public void afterUpdate2(EntryEvent event) {
    String s = "Unexpected callback invocation";
    throw new UnsupportedOperationException(s);
  }

  public final void afterInvalidate(EntryEvent event) {
    addEvent(event);
    try {
      afterInvalidate2(event);
    }
    catch (VirtualMachineError e) {
      SystemFailure.initiateFailure(e);
      throw e;
    }
    catch (Throwable t) {
      this.callbackError = t;
    }
  }

  public void afterInvalidate2(EntryEvent event) {
    String s = "Unexpected callback invocation";
    throw new UnsupportedOperationException(s);
  }

  public final void afterDestroy(EntryEvent event) {
    afterDestroyBeforeAddEvent(event);
    addEvent(event);
    try {
      afterDestroy2(event);
    }
    catch (VirtualMachineError e) {
      SystemFailure.initiateFailure(e);
      throw e;
    }
    catch (Throwable t) {
      this.callbackError = t;
    }
  }

  public void afterDestroyBeforeAddEvent(EntryEvent event) {
    // do nothing by default
  }

  public void afterDestroy2(EntryEvent event) {
    String s = "Unexpected callback invocation";
    throw new UnsupportedOperationException(s);
  }

  public final void afterRegionInvalidate(RegionEvent event) {
    addEvent(event);
    try {
      afterRegionInvalidate2(event);
    }
    catch (VirtualMachineError e) {
      SystemFailure.initiateFailure(e);
      throw e;
    }
    catch (Throwable t) {
      this.callbackError = t;
    }
  }

  public void afterRegionInvalidate2(RegionEvent event) {
    String s = "Unexpected callback invocation";
    throw new UnsupportedOperationException(s);
  }

  public final void afterRegionDestroy(RegionEvent event) {
    // check argument to see if this is during tearDown
    if ("teardown".equals(event.getCallbackArgument())) return;
    afterRegionDestroyBeforeAddEvent(event);
    addEvent(event);
    try {
      afterRegionDestroy2(event);
    }
    catch (VirtualMachineError e) {
      SystemFailure.initiateFailure(e);
      throw e;
    }
    catch (Throwable t) {
      this.callbackError = t;
    }
  }

  public void afterRegionDestroyBeforeAddEvent(RegionEvent event) {
    // do nothing by default
  }
  public void afterRegionDestroy2(RegionEvent event) {
    if (!event.getOperation().isClose()) {
      String s = "Unexpected callback invocation";
      throw new UnsupportedOperationException(s);
    }
  }
  
  public void afterRegionClear(RegionEvent event ) {
    addEvent(event, false);
  }
  public void afterRegionCreate(RegionEvent event ) {
    addEvent(event, false);
  }
  public void afterRegionLive(RegionEvent event ) {
    addEvent(event, false);
  }  
}
