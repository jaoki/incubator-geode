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
package com.gemstone.gemfire.internal.cache.persistence.soplog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumMap;

/**
 * Provides a unified view of the current SBuffer, the unflushed SBuffers, and
 * the existing soplogs.
 * 
 * @author bakera
 */
public interface SortedOplogSet extends SortedReader<ByteBuffer> {
  /**
   * Defines a callback handler for asynchronous operations.
   */
  public interface FlushHandler {
    /**
     * Invoked when the operation completed successfully.
     */
    void complete();
    
    /**
     * Invoked when the operation completed with an error.
     * @param t the error
     */
    void error(Throwable t);
  }

  /**
   * Inserts or updates an entry in the current buffer.  This invocation may
   * block if the current buffer is full and there are too many outstanding
   * write requests.
   * 
   * @param key the key
   * @param value the value
   * @throws IOException 
   */
  void put(byte[] key, byte[] value) throws IOException;
  
  /**
   * Returns the size of the current buffer in bytes.
   * @return the buffer size
   */
  long bufferSize();
  
  /**
   * Returns the size of the unflushed buffers in bytes.
   * @return the unflushed size
   */
  long unflushedSize();
  
  /**
   * Requests that the current buffer be flushed to disk.  This invocation may
   * block if there are too many outstanding write requests.
   * 
   * @param metadata supplemental data to be included in the soplog
   * @param handler the flush completion callback
   * @throws IOException error preparing flush
   */
  void flush(EnumMap<Metadata, byte[]> metadata, FlushHandler handler) throws IOException;

  /**
   * Flushes the current buffer and closes the soplog set.  Blocks until the flush
   * is completed.
   * 
   * @param metadata supplemental data to be included in the soplog
   * @throws IOException error during flush
   */
  void flushAndClose(EnumMap<Metadata, byte[]> metadata) throws IOException;

  /**
   * Returns the configured compaction strategy.
   * @return the compactor
   */
  Compactor getCompactor();

  /**
   * Clears the current buffer, any existing buffers, and all active soplogs.
   * 
   * @throws IOException unable to clear
   */
  void clear() throws IOException;
  
  /**
   * Clears existing and closes the soplog set.
   * @throws IOException unable to destroy
   */
  void destroy() throws IOException;
  
  /**
   * Returns true if the set is closed.
   * @return true if closed
   */
  boolean isClosed();

  /**
   * Returns the soplog factory.
   * @return the factory
   */
  SortedOplogFactory getFactory();
}
