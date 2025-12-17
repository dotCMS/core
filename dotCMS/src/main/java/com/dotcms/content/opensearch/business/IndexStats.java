package com.dotcms.content.opensearch.business;

/**
 * Represents statistics for an OpenSearch index
 *
 * @author fabrizio
 */
public class IndexStats {

    private final String indexName;
    private final long documentCount;
    private final long size;
    private final String prettySize;

    public IndexStats(final String indexName, final long documentCount, final long size) {
        this.indexName = indexName;
        this.documentCount = documentCount;
        this.size = size;
        this.prettySize = formatBytes(size);
    }

    public long getDocumentCount() {
        return documentCount;
    }

    public String getSize() {
        return prettySize;
    }

    public long getSizeRaw() {
        return size;
    }

    public String getIndexName() {
        return indexName;
    }

    /**
     * Format bytes to human readable string
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "b";
        }

        String[] units = {"b", "kb", "mb", "gb", "tb", "pb"};
        double size = bytes;
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        if (size >= 100) {
            return String.format("%.0f%s", size, units[unitIndex]);
        } else if (size >= 10) {
            return String.format("%.1f%s", size, units[unitIndex]);
        } else {
            return String.format("%.2f%s", size, units[unitIndex]);
        }
    }
}