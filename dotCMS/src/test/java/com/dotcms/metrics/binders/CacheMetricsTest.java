package com.dotcms.metrics.binders;

import static org.junit.Assert.assertEquals;

import com.dotmarketing.business.cache.provider.CacheStats;
import java.lang.reflect.Method;
import org.junit.Test;

public class CacheMetricsTest {

    /** Ordinary memory statistics retain their existing units and values. */
    @Test
    public void testMemoryStatisticsRemainAvailable() throws Exception {
        final CacheStats stats = new CacheStats()
                .addStat(CacheStats.REGION_MEM_TOTAL, 2048)
                .addStat(CacheStats.REGION_MEM_PER_OBJECT, "1 KB");

        assertEquals(2048, memoryMetric("getMemoryBytesValue", stats), 0);
        assertEquals(1024, memoryMetric("getMemoryPerObjectBytesValue", stats), 0);
    }

    /** Legacy H22 aliases must not turn serialized write statistics into memory gauges. */
    @Test
    public void testSerializedWriteAliasesAreNotReportedAsMemory() throws Exception {
        final CacheStats stats = new CacheStats()
                .addStat(CacheStats.REGION_BYTES_WRITTEN, "16 KB")
                .addStat(CacheStats.REGION_AVG_SERIALIZED_SIZE, "4 KB")
                .addStat(CacheStats.REGION_MEM_TOTAL_PRETTY, "16 KB")
                .addStat(CacheStats.REGION_MEM_PER_OBJECT, "4 KB");

        assertEquals(0, memoryMetric("getMemoryBytesValue", stats), 0);
        assertEquals(0, memoryMetric("getMemoryPerObjectBytesValue", stats), 0);
    }

    /** Exercises existing private readers without widening production visibility. */
    private double memoryMetric(final String name, final CacheStats stats) throws Exception {
        final Method method = CacheMetrics.class.getDeclaredMethod(name, CacheStats.class);
        method.setAccessible(true);
        return (double) method.invoke(new CacheMetrics(), stats);
    }
}
