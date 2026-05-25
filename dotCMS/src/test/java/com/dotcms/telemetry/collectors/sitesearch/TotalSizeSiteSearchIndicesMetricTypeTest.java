package com.dotcms.telemetry.collectors.sitesearch;

import com.dotcms.content.index.domain.ImmutableIndexStats;
import com.dotcms.content.index.domain.IndexStats;
import com.dotmarketing.exception.DotDataException;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TotalSizeSiteSearchIndicesMetricTypeTest {

    private final TotalSizeSiteSearchIndicesMetricType metric = new TotalSizeSiteSearchIndicesMetricType();

    // -- metadata --

    @Test
    public void testGetName() {
        assertEquals("TOTAL_INDICES_SIZE", metric.getName());
    }

    // -- formatBytes boundary cases --

    @Test
    public void testEmpty_returnsZeroBytes() throws DotDataException {
        assertEquals("0b", getValue(List.of()));
    }

    @Test
    public void testBelowKb_returnsRawBytes() throws DotDataException {
        assertEquals("512b", getValue(index(512)));
        assertEquals("1023b", getValue(index(1023)));
    }

    @Test
    public void testExactKb_dropsDecimal() throws DotDataException {
        assertEquals("1kb", getValue(index(1024)));
    }

    @Test
    public void testFractionalKb_keepsOneDecimal() throws DotDataException {
        assertEquals("1.5kb", getValue(index(1536)));
    }

    @Test
    public void testExactMb_dropsDecimal() throws DotDataException {
        assertEquals("1mb", getValue(index(1024L * 1024)));
    }

    @Test
    public void testFractionalGb_keepsOneDecimal() throws DotDataException {
        assertEquals("1.5gb", getValue(index((long) (1.5 * 1024 * 1024 * 1024))));
    }

    @Test
    public void testLargeMb_noDecimal() throws DotDataException {
        assertEquals("128mb", getValue(index(128L * 1024 * 1024)));
    }

    // -- aggregation across multiple indices --

    @Test
    public void testMultipleIndices_sumsCorrectly() throws DotDataException {
        final IndexStats a = index(512L * 1024 * 1024);  // 512 MB
        final IndexStats b = index(512L * 1024 * 1024);  // 512 MB → total 1 GB
        assertEquals("1gb", getValue(a, b));
    }

    // -- helpers --

    private String getValue(final IndexStats... indices) throws DotDataException {
        return getValue(List.of(indices));
    }

    private String getValue(final List<IndexStats> indices) throws DotDataException {
        final Optional<Object> result = metric.getValue(indices);
        assertTrue(result.isPresent());
        return (String) result.get();
    }

    private static IndexStats index(final long sizeRaw) {
        return ImmutableIndexStats.builder()
                .indexName("test")
                .documentCount(0)
                .sizeRaw(sizeRaw)
                .size("")
                .build();
    }
}
