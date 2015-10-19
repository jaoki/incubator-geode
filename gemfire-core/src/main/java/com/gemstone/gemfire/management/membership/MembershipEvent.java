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
package com.gemstone.gemfire.management.membership;

import com.gemstone.gemfire.distributed.DistributedMember;

/**
 * An event that describes the member that originated this event.
 * Instances of this are delivered to a {@link MembershipListener} when a member
 * has joined or left the distributed system.
 *
 * @author rishim
 * @since 8.0
 */
public interface MembershipEvent {
  /**
   * Returns the distributed member as a String.
   */
  public String getMemberId();

  /**
   * Returns the {@link DistributedMember} that this event originated in.
   * 
   * @return the member that performed the operation that originated this event.
   * @since 8.0
   */
  public DistributedMember getDistributedMember();
}
