package io.pivotal.gemfire.spark.connector.javaapi;

import io.pivotal.gemfire.spark.connector.GemFireConnectionConf;
import io.pivotal.gemfire.spark.connector.streaming.GemFirePairDStreamFunctions;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import java.util.Properties;

import static io.pivotal.gemfire.spark.connector.javaapi.JavaAPIHelper.*;

/**
 * A Java API wrapper over {@link org.apache.spark.streaming.api.java.JavaPairDStream}
 * to provide GemFire Spark Connector functionality.
 *
 * <p>To obtain an instance of this wrapper, use one of the factory methods in {@link
 * io.pivotal.gemfire.spark.connector.javaapi.GemFireJavaUtil} class.</p>
 */
public class GemFireJavaPairDStreamFunctions<K, V> {
  
  public final GemFirePairDStreamFunctions<K, V> dsf;

  public GemFireJavaPairDStreamFunctions(JavaPairDStream<K, V> ds) {    
    this.dsf = new GemFirePairDStreamFunctions<K, V>(ds.dstream());
  }

  /**
   * Save the JavaPairDStream to GemFire key-value store.
   * @param regionPath the full path of region that the DStream is stored  
   * @param connConf the GemFireConnectionConf object that provides connection to GemFire cluster
   * @param opConf the optional parameters for this operation
   */  
  public void saveToGemfire(String regionPath, GemFireConnectionConf connConf, Properties opConf) {
    dsf.saveToGemfire(regionPath, connConf, propertiesToScalaMap(opConf));
  }

  /**
   * Save the JavaPairDStream to GemFire key-value store.
   * @param regionPath the full path of region that the DStream is stored  
   * @param connConf the GemFireConnectionConf object that provides connection to GemFire cluster
   */
  public void saveToGemfire(String regionPath, GemFireConnectionConf connConf) {
    dsf.saveToGemfire(regionPath, connConf, emptyStrStrMap());
  }

  /**
   * Save the JavaPairDStream to GemFire key-value store.
   * @param regionPath the full path of region that the DStream is stored
   * @param opConf the optional parameters for this operation
   */
  public void saveToGemfire(String regionPath, Properties opConf) {
    dsf.saveToGemfire(regionPath, dsf.defaultConnectionConf(), propertiesToScalaMap(opConf));
  }

  /**
   * Save the JavaPairDStream to GemFire key-value store.
   * @param regionPath the full path of region that the DStream is stored
   */
  public void saveToGemfire(String regionPath) {
    dsf.saveToGemfire(regionPath, dsf.defaultConnectionConf(), emptyStrStrMap());
  }

}
