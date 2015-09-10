package io.pivotal.gemfire.spark.connector.internal.rdd

import io.pivotal.gemfire.spark.connector.GemFireConnection
import io.pivotal.gemfire.spark.connector.internal.RegionMetadata
import io.pivotal.gemfire.spark.connector.NumberPartitionsPerServerPropKey
import org.apache.spark.Partition
import scala.collection.JavaConversions._
import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.reflect.ClassTag

/** This partitioner maps whole region to one GemFireRDDPartition */
object OnePartitionPartitioner extends GemFireRDDPartitioner {

  override val name = "OnePartition"

  override def partitions[K: ClassTag, V: ClassTag]
    (conn: GemFireConnection, md: RegionMetadata, env: Map[String, String]): Array[Partition] =
    Array[Partition](new GemFireRDDPartition(0, Set.empty))
}

/**
  * This partitioner maps whole region to N * M GemFire RDD partitions, where M is the number of 
  * GemFire servers that contain the data for the given region. Th default value of N is 1.
  */
object ServerSplitsPartitioner extends GemFireRDDPartitioner {

  override val name = "ServerSplits"

  override def partitions[K: ClassTag, V: ClassTag]
  (conn: GemFireConnection, md: RegionMetadata, env: Map[String, String]): Array[Partition] = {
    if (md == null) throw new RuntimeException("RegionMetadata is null")
    val n = try { env.getOrElse(NumberPartitionsPerServerPropKey, "2").toInt } catch { case e: NumberFormatException => 2 }
    if (!md.isPartitioned || md.getServerBucketMap == null || md.getServerBucketMap.isEmpty)
      Array[Partition](new GemFireRDDPartition(0, Set.empty))
    else {
      val map = mapAsScalaMap(md.getServerBucketMap)
        .map { case (srv, set) => (srv, asScalaSet(set).map(_.toInt)) }.toList
        .map { case (srv, set) => (srv.getHostName, set) }
       doPartitions(map, md.getTotalBuckets, n)
    }
  }

  /** Converts server to bucket ID set list to array of RDD partitions */
  def doPartitions(serverBucketMap: List[(String, mutable.Set[Int])], totalBuckets: Int, n: Int)
    : Array[Partition] = {

    // method that calculates the group size for splitting "k" items into "g" groups
    def groupSize(k: Int, g: Int): Int = scala.math.ceil(k / g.toDouble).toInt

    // 1. convert list of server and bucket set pairs to a list of server and sorted bucket set pairs
    val srvToSortedBucketSet = serverBucketMap.map { case (srv, set) => (srv, SortedSet[Int]() ++ set) }

    // 2. split bucket set of each server into n splits if possible, and server to Seq(server)
    val srvToSplitedBuckeSet = srvToSortedBucketSet.flatMap { case (host, set) =>
      if (set.isEmpty) Nil else set.grouped(groupSize(set.size, n)).toList.map(s => (Seq(host), s)) }

    // 3. calculate empty bucket IDs by removing all bucket sets of all servers from the full bucket sets
    val emptyIDs = SortedSet[Int]() ++ ((0 until totalBuckets).toSet /: srvToSortedBucketSet) {case (s1, (k, s2)) => s1 &~ s2}

    // 4. distribute empty bucket IDs to all partitions evenly.
    //    The empty buckets do not contain data when partitions are created, but they may contain data
    //    when RDD is materialized, so need to include those bucket IDs in the partitions.
    val srvToFinalBucketSet = if (emptyIDs.isEmpty) srvToSplitedBuckeSet
      else srvToSplitedBuckeSet.zipAll(
        emptyIDs.grouped(groupSize(emptyIDs.size, srvToSplitedBuckeSet.size)).toList, (Nil, Set.empty), Set.empty).map
          { case ((server, set1), set2) => (server, SortedSet[Int]() ++ set1 ++ set2) }

    // 5. create array of partitions w/ 0-based index
    (0 until srvToFinalBucketSet.size).toList.zip(srvToFinalBucketSet).map
      { case (i, (srv, set)) => new GemFireRDDPartition(i, set, srv) }.toArray
  }
}
