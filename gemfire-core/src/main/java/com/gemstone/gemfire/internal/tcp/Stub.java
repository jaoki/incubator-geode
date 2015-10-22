/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gemstone.gemfire.internal.tcp;

import java.io.*;
import java.net.*;

import com.gemstone.gemfire.DataSerializable;
import com.gemstone.gemfire.DataSerializer;
import com.gemstone.gemfire.internal.InternalDataSerializer;

/** Stub represents an ip address and port.

    @author Bruce Schuchardt
    @since 2.0
   
 */

public class Stub implements Externalizable, DataSerializable
{
  private InetAddress inAddr;
  private int port;
  private int viewID;

  public Stub() {
    // public default needed for deserialization
  }
  
  public Stub(InetAddress addr, int port, int vmViewID) {
    viewID = vmViewID;
    inAddr = addr;
    this.port = port;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Stub) {
      Stub s = (Stub)o;
      boolean result;
      if (inAddr == null)
        result = s.inAddr == null;
      else
        result = inAddr.equals(s.inAddr);
      result = result && port == s.port;
      if (this.viewID != 0 && s.viewID != 0) {
        result = result && (this.viewID == s.viewID);
      }
      return result;
    }
    else {
      return false;
    }
  }
  
  // hashCode equates to the address hashCode for fast connection lookup
  @Override
  public int hashCode() {
    // do not use viewID in hashCode because it is changed after creating a stub
    int result = 0;
    // result += inAddr.hashCode(); // useless
    result += port;
    return result;
  }
  
  public void setViewID(int viewID) {
    this.viewID = viewID;
  }
  
  public int getPort() {
    return port;
  }
  
  public int getViewID() {
    return this.viewID;
  }
  
  public InetAddress getInetAddress() {
    return inAddr;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(80);
    sb.append("tcp://");
    if (inAddr == null)
      sb.append("<null>");
    else
      sb.append(inAddr.toString());
    if (this.viewID != 0) {
      sb.append("<v"+this.viewID+">");
    }
    sb.append(":" + port);
    return sb.toString();
  }
  
  /**
   * Writes the contents of this <code>Stub</code> to a
   * <code>DataOutput</code>. 
   *
   * @since 3.0
   */
  public void toData(DataOutput out) 
    throws IOException
  {
    DataSerializer.writeInetAddress(inAddr, out);
    out.writeInt(port);
    out.writeInt(viewID);
  }
  
  /**
   * Reads the contents of this <code>Stub</code> from a
   * <code>DataOutput</code>. 
   *
   * @since 3.0
   */
  public void fromData(DataInput in)
    throws IOException, ClassNotFoundException
  {
    inAddr = DataSerializer.readInetAddress(in);
    this.port = in.readInt();
    this.viewID = in.readInt();
  }

  /**
   * static factory method
   * @since 5.0.2
   */
  public static Stub createFromData(DataInput in)
    throws IOException, ClassNotFoundException
  {
    Stub result = new Stub();
    InternalDataSerializer.invokeFromData(result, in);
    return result;
  }
  
  public void writeExternal(ObjectOutput os) 
    throws IOException
  {
    this.toData(os);
  }
  
  public void readExternal(ObjectInput is)
    throws IOException, ClassNotFoundException
  {
    this.fromData(is);
  }
}
