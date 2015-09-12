package com.gemstone.gemfire.cache.query.dunit;

import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.PartitionAttributesFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionShortcut;
import com.gemstone.gemfire.cache.query.Index;
import com.gemstone.gemfire.cache.query.IndexExistsException;
import com.gemstone.gemfire.cache.query.IndexInvalidException;
import com.gemstone.gemfire.cache.query.IndexNameConflictException;
import com.gemstone.gemfire.cache.query.IndexType;
import com.gemstone.gemfire.cache.query.RegionNotFoundException;
import com.gemstone.gemfire.cache.query.functional.NonDistinctOrderByTestImplementation;
import com.gemstone.gemfire.cache30.CacheTestCase;
import com.gemstone.gemfire.test.junit.categories.DistributedTest;

import dunit.Host;
import dunit.SerializableRunnable;
import dunit.VM;

/**
 * 
 * @author ashahid
 *
 */
@Category(DistributedTest.class)
public class NonDistinctOrderByPartitionedDUnitTest extends
    NonDistinctOrderByDUnitImpl {

  public NonDistinctOrderByPartitionedDUnitTest(String name) {
    super(name);
  }

  @Override
  protected NonDistinctOrderByTestImplementation createTestInstance() {
    Host host = Host.getHost(0);
    final VM vm0 = host.getVM(0);
    final VM vm1 = host.getVM(1);
    final VM vm2 = host.getVM(2);
    final VM vm3 = host.getVM(3);

    NonDistinctOrderByTestImplementation test = new NonDistinctOrderByTestImplementation(
        ) {

      @Override
      public Region createRegion(String regionName, Class valueConstraint) {
        // TODO Auto-generated method stub
        Region rgn = createAccessor(regionName, valueConstraint);
        createPR(vm1, regionName, valueConstraint);
        createPR(vm2, regionName, valueConstraint);
        createPR(vm3, regionName, valueConstraint);
        return rgn;
      }

      @Override
      public Index createIndex(String indexName, String indexedExpression,
          String regionPath) throws IndexInvalidException,
          IndexNameConflictException, IndexExistsException,
          RegionNotFoundException, UnsupportedOperationException {
        Index indx = createIndexOnAccessor(indexName, indexedExpression,
            regionPath);
        /*
         * NonDistinctOrderByPartitionedDUnit.this.createIndex(vm1, indexName,
         * indexedExpression, regionPath);
         * NonDistinctOrderByPartitionedDUnit.this.createIndex(vm2, indexName,
         * indexedExpression, regionPath);
         * NonDistinctOrderByPartitionedDUnit.this.createIndex(vm3, indexName,
         * indexedExpression, regionPath);
         */
        return indx;
      }

      @Override
      public Index createIndex(String indexName, IndexType indexType,
          String indexedExpression, String fromClause)
          throws IndexInvalidException, IndexNameConflictException,
          IndexExistsException, RegionNotFoundException,
          UnsupportedOperationException {
        Index indx = createIndexOnAccessor(indexName, indexType,
            indexedExpression, fromClause);
        /*
         * NonDistinctOrderByPartitionedDUnit.this.createIndex(vm1, indexName,
         * indexType, indexedExpression, fromClause);
         * NonDistinctOrderByPartitionedDUnit.this.createIndex(vm2, indexName,
         * indexType, indexedExpression, fromClause);
         * NonDistinctOrderByPartitionedDUnit.this.createIndex(vm3, indexName,
         * indexType,indexedExpression, fromClause);
         */
        return indx;
      }

      @Override
      public boolean assertIndexUsedOnQueryNode() {
        return false;
      }
    };
    return test;
  }

  private void createBuckets(VM vm) {
    vm.invoke(new SerializableRunnable("create accessor") {
      public void run() {
        Cache cache = getCache();
        Region region = cache.getRegion("region");
        for (int i = 0; i < 10; i++) {
          region.put(i, i);
        }
      }
    });
  }

  private void createPR(VM vm, final String regionName,
      final Class valueConstraint) {
    vm.invoke(new SerializableRunnable("create data store") {
      public void run() {
        Cache cache = getCache();
        PartitionAttributesFactory paf = new PartitionAttributesFactory();
        paf.setTotalNumBuckets(10);
        cache.createRegionFactory(RegionShortcut.PARTITION)
            .setValueConstraint(valueConstraint)
            .setPartitionAttributes(paf.create()).create(regionName);
      }
    });
  }

  private Region createAccessor(String regionName, Class valueConstraint) {

    Cache cache = getCache();
    PartitionAttributesFactory paf = new PartitionAttributesFactory();
    paf.setTotalNumBuckets(10);
    paf.setLocalMaxMemory(0);
    return cache.createRegionFactory(RegionShortcut.PARTITION_PROXY)
        .setValueConstraint(valueConstraint)
        .setPartitionAttributes(paf.create()).create(regionName);
  }

}
