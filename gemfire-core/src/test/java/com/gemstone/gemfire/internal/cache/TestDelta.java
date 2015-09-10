/*=========================================================================
 * Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.cache;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.gemstone.gemfire.DataSerializable;
import com.gemstone.gemfire.DataSerializer;
import com.gemstone.gemfire.Delta;
import com.gemstone.gemfire.InvalidDeltaException;

public final class TestDelta implements Delta, DataSerializable, Cloneable {

    public boolean hasDelta;
    public String info;
    public int serializations;
    public int deserializations;
    public int deltas;
    public int clones;
    
    public TestDelta() {
    }

    public TestDelta(boolean hasDelta, String info) {
      this.hasDelta = hasDelta;
      this.info = info;
    }
    
    public synchronized void checkFields(final int serializations, final int deserializations, final int deltas, final int clones) {
      DeltaSizingDUnitTest.assertEquals(serializations, this.serializations);
      DeltaSizingDUnitTest.assertEquals(deserializations, this.deserializations);
      DeltaSizingDUnitTest.assertEquals(deltas, this.deltas);
      DeltaSizingDUnitTest.assertEquals(clones, this.clones);
    }

    public synchronized void fromDelta(DataInput in) throws IOException,
        InvalidDeltaException {
//      new Exception("DAN - From Delta Called").printStackTrace();
      this.hasDelta = true;
      info = DataSerializer.readString(in);
      deltas++;
    }

    public boolean hasDelta() {
      return hasDelta;
    }

    public synchronized void toDelta(DataOutput out) throws IOException {
//      new Exception("DAN - To Delta Called").printStackTrace();
      DataSerializer.writeString(info, out);
    }

    public synchronized void fromData(DataInput in) throws IOException,
        ClassNotFoundException {
//      new Exception("DAN - From Data Called").printStackTrace();
      info = DataSerializer.readString(in);
      serializations = in.readInt();
      deserializations = in.readInt();
      deltas = in.readInt();
      clones = in.readInt();
      deserializations++;
    }

    public synchronized void toData(DataOutput out) throws IOException {
//      new Exception("DAN - To Data Called").printStackTrace();
      serializations++;
      DataSerializer.writeString(info, out);
      out.writeInt(serializations);
      out.writeInt(deserializations);
      out.writeInt(deltas);
      out.writeInt(clones);
    }

    @Override
    public synchronized Object clone() throws CloneNotSupportedException {
//    new Exception("DAN - Clone Called").printStackTrace();
      clones++;
      return super.clone();
    } 
  }