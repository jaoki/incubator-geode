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
package com.gemstone.gemfire.management.internal.cli.functions;

import org.apache.logging.log4j.Logger;

import com.gemstone.gemfire.SystemFailure;
import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheClosedException;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.execute.FunctionAdapter;
import com.gemstone.gemfire.cache.execute.FunctionContext;
import com.gemstone.gemfire.cache.hdfs.internal.HDFSStoreImpl;
import com.gemstone.gemfire.distributed.DistributedMember;
import com.gemstone.gemfire.internal.InternalEntity;
import com.gemstone.gemfire.internal.cache.GemFireCacheImpl;
import com.gemstone.gemfire.internal.cache.InternalCache;
import com.gemstone.gemfire.internal.cache.xmlcache.CacheXml;
import com.gemstone.gemfire.internal.logging.LogService;
import com.gemstone.gemfire.management.internal.configuration.domain.XmlEntity;

/**
 * Function used by the 'destroy hdfs-store' gfsh command to destroy a hdfs
 * store on each member.
 * 
 * @author Namrata Thanvi
 */

public class DestroyHDFSStoreFunction extends FunctionAdapter implements InternalEntity {
  private static final Logger logger = LogService.getLogger();

  private static final String ID = DestroyHDFSStoreFunction.class.getName();

  private static final long serialVersionUID = 1L;

  @Override
  public void execute(FunctionContext context) {
    String memberId = "";
    try {
      final String hdfsStoreName = (String)context.getArguments();
      GemFireCacheImpl cache = (GemFireCacheImpl)getCache();      
      DistributedMember member = getDistributedMember(cache);     
      CliFunctionResult result;
      
      memberId = member.getId();
      if (!member.getName().equals("")) {
        memberId = member.getName();
      }
      
      HDFSStoreImpl hdfsStore = cache.findHDFSStore(hdfsStoreName);
      
      if (hdfsStore != null) {
        hdfsStore.destroy();
        // TODO - Need to verify what all attributes needs to be persisted in cache.xml and how
        XmlEntity xmlEntity = getXMLEntity(hdfsStoreName); 
        result = new CliFunctionResult(memberId, xmlEntity, "Success");
      }
      else {
        result = new CliFunctionResult(memberId, false, "Hdfs store not found on this member");
      }
      context.getResultSender().lastResult(result);   

    } catch (CacheClosedException cce) {
      CliFunctionResult result = new CliFunctionResult(memberId, false, null);
      context.getResultSender().lastResult(result);

    } catch (VirtualMachineError e) {
      SystemFailure.initiateFailure(e);
      throw e;

    } catch (Throwable th) {
      SystemFailure.checkFailure();
      logger.error("Could not destroy hdfs store: {}", th.getMessage(), th);
      CliFunctionResult result = new CliFunctionResult(memberId, th, null);
      context.getResultSender().lastResult(result);
    }
  }

  @Override
  public String getId() {
    return ID;
  }
  
  protected Cache getCache() {
    return CacheFactory.getAnyInstance();
  }
  
  protected DistributedMember getDistributedMember(Cache cache){
    return ((InternalCache)cache).getMyId();
  }
  
  protected XmlEntity getXMLEntity(String storeName){
    return new XmlEntity(CacheXml.HDFS_STORE, "name", storeName);
  }
}
