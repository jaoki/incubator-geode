/*
 * ========================================================================= 
 * Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved. 
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 * =========================================================================
 */
package com.gemstone.gemfire.internal.cache.tier.sockets.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.internal.cache.BucketServerLocation;
import com.gemstone.gemfire.internal.cache.BucketServerLocation66;
import com.gemstone.gemfire.internal.cache.PartitionedRegion;
import com.gemstone.gemfire.internal.cache.tier.CachedRegionHelper;
import com.gemstone.gemfire.internal.cache.tier.Command;
import com.gemstone.gemfire.internal.cache.tier.MessageType;
import com.gemstone.gemfire.internal.cache.tier.sockets.BaseCommand;
import com.gemstone.gemfire.internal.cache.tier.sockets.Message;
import com.gemstone.gemfire.internal.cache.tier.sockets.ServerConnection;
import com.gemstone.gemfire.internal.i18n.LocalizedStrings;
import com.gemstone.gemfire.internal.logging.log4j.LocalizedMessage;
/**
 * {@link Command} for {@link GetClientPRMetadataCommand}
 * 
 * @author Suranjan Kumar
 * @author Yogesh Mahajan
 * 
 * @since 6.5 
 */
public class GetClientPRMetadataCommand extends BaseCommand {

  private final static GetClientPRMetadataCommand singleton = new GetClientPRMetadataCommand();

  public static Command getCommand() {
    return singleton;
  }

  private GetClientPRMetadataCommand() {
  }

  @Override
  public void cmdExecute(Message msg, ServerConnection servConn, long start)
      throws IOException, ClassNotFoundException, InterruptedException {
    String regionFullPath = null;
    CachedRegionHelper crHelper = servConn.getCachedRegionHelper();
    regionFullPath = msg.getPart(0).getString();
    String errMessage = "";
    if (regionFullPath == null) {
      logger.warn(LocalizedMessage.create(LocalizedStrings.GetClientPRMetadata_THE_INPUT_REGION_PATH_IS_NULL));
      errMessage = LocalizedStrings.GetClientPRMetadata_THE_INPUT_REGION_PATH_IS_NULL
          .toLocalizedString();
      writeErrorResponse(msg, MessageType.GET_CLIENT_PR_METADATA_ERROR,
          errMessage.toString(), servConn);
      servConn.setAsTrue(RESPONDED);
    }
    else {
      Region region = crHelper.getRegion(regionFullPath);
      if (region == null) {
        logger.warn(LocalizedMessage.create(LocalizedStrings.GetClientPRMetadata_REGION_NOT_FOUND_FOR_SPECIFIED_REGION_PATH, regionFullPath));
        errMessage = LocalizedStrings.GetClientPRMetadata_REGION_NOT_FOUND
            .toLocalizedString()
            + regionFullPath;
        writeErrorResponse(msg, MessageType.GET_CLIENT_PR_METADATA_ERROR,
            errMessage.toString(), servConn);
        servConn.setAsTrue(RESPONDED);
      }
      else {
        try {
          Message responseMsg = servConn.getResponseMessage();
          responseMsg.setTransactionId(msg.getTransactionId());
          responseMsg.setMessageType(MessageType.RESPONSE_CLIENT_PR_METADATA);

          PartitionedRegion prRgion = (PartitionedRegion)region;
          Map<Integer, List<BucketServerLocation66>> bucketToServerLocations = prRgion
              .getRegionAdvisor().getAllClientBucketProfiles();
          responseMsg.setNumberOfParts(bucketToServerLocations.size());
          for (List<BucketServerLocation66> serverLocations : bucketToServerLocations
              .values()) {
            List<BucketServerLocation> oldServerLocations = new ArrayList<BucketServerLocation>();
            for (BucketServerLocation66 bs : serverLocations) {
              oldServerLocations.add(new BucketServerLocation(bs.getBucketId(),
                  bs.getPort(), bs.getHostName(), bs.isPrimary(), bs
                      .getVersion()));
              responseMsg.addObjPart(oldServerLocations);
            }
          }
          responseMsg.send();
          msg.clearParts();
        }
        catch (Exception e) {
          writeException(msg, e, false, servConn);
        }
        finally {
          servConn.setAsTrue(Command.RESPONDED);
        }
      }
    }
  }

}
