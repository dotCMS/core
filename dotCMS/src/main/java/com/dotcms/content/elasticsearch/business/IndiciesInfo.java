package com.dotcms.content.elasticsearch.business;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.control.Try;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;

/**
 * IMPORTANT: This Is marked Deprecated and will be removed once we complete migration to OpenSearch 3.x
 * Handle the current Index name into the cluster.
 * Also provide some util method for index name, the index name has to have the follow sintax:
 *
 * <b>cluster_<CLUSTER_ID>.<INDEX_TYPE_PREFIX>_<TIME_STAMP></b>
 *
 * where:
 *
 * - CLUSTER_ID: it is the cluster identifier who created the index.
 * - INDEX_TYPE_PREFIX: Prefix define for the Index's typs, also see {@link IndexType}
 * - TIME_STAMP: current time stamop when the index was created
 */
@Deprecated(forRemoval = true)
public class IndiciesInfo implements Serializable {

    /**
     * Build a new {@link IndiciesInfo} instance
     */
    public static class Builder {
        private String live, working, reindexLive, reindexWorking, siteSearch;

        public Builder setLive(String live) {
            this.live = live;
            return this;
        }

        public Builder setWorking(String working) {
            this.working = working;
            return this;
        }

        public Builder setReindexLive(String reindexLive) {
            this.reindexLive = reindexLive;
            return this;
        }

        public Builder setReindexWorking(String reindexWorking) {
            this.reindexWorking = reindexWorking;
            return this;
        }

        public Builder setSiteSearch(String siteSearch) {
            this.siteSearch = siteSearch;
            return this;
        }

        public IndiciesInfo build() {
            return new IndiciesInfo(this);
        }

        public static Builder copy(final IndiciesInfo info) {
            final IndiciesInfo.Builder builder = new IndiciesInfo.Builder();
            builder.setWorking(info.getWorking());
            builder.setLive(info.getLive());
            builder.setReindexWorking(info.getReindexWorking());
            builder.setSiteSearch(info.getSiteSearch());

            return builder;
        }
    }

    public static final SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final String CLUSTER_PREFIX = "cluster_";
    private final static String INDEX_NAME_PATTERN = CLUSTER_PREFIX + "%s.%s_%s";

    private final Map<IndexType, String> indiciesNames = new HashMap<>();

    public IndiciesInfo(final Builder builder) {
        this.indiciesNames.put(IndexType.LIVE, builder.live);
        this.indiciesNames.put(IndexType.WORKING, builder.working);
        this.indiciesNames.put(IndexType.REINDEX_LIVE, builder.reindexLive);
        this.indiciesNames.put(IndexType.REINDEX_WORKING, builder.reindexWorking);
        this.indiciesNames.put(IndexType.SITE_SEARCH, builder.siteSearch);
    }

    /**
     * Return the current LIVE index name
     * @return
     */
    public String getLive() {
        return this.indiciesNames.get(IndexType.LIVE);
    }

    /**
     * Return the current WORKING index name
     * @return
     */
    public String getWorking() {
        return this.indiciesNames.get(IndexType.WORKING);
    }

    /**
     * Return the current REINDEX LIVE index name
     * @return
     */
    public String getReindexLive() {
        return this.indiciesNames.get(IndexType.REINDEX_LIVE);
    }

    /**
     * Return the current REINDEX WORKING index name
     * @return
     */
    public String getReindexWorking() {
        return this.indiciesNames.get(IndexType.REINDEX_WORKING);
    }

    /**
     * Return the current SITE SEARCH index name
     * @return
     */
    public String getSiteSearch() {
        return this.indiciesNames.get(IndexType.SITE_SEARCH);
    }

    /**
     * Return the timestamp from a index name, a index name has the follow sintax:
     *
     * <b>cluster_<CLUSTER_ID>.<INDEX_NAME_PREFIX>_<TIME_STAMP></b>
     * @return
     */
    public long getIndexTimeStamp(final IndexType indexType) {
        Date startTime;
        try {
            final String indexName = indiciesNames.get(indexType);
            final String indexTimestamp = indexName.substring(indexName.lastIndexOf("_") + 1);
            startTime = timestampFormatter.parse(indexTimestamp);

            return System.currentTimeMillis() - startTime.getTime();
        } catch (ParseException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Create a new indicies name for the type pass in indiciesType
     *
     * @param indiciesType types for what you want to create a new name
     * @return timestamp for the newly indicies, to get the newly indices name use one of the follow methods:
     * {@link IndiciesInfo#getLive()}, {@link IndiciesInfo#getWorking()}, {@link IndiciesInfo#getReindexLive()},
     * {@link IndiciesInfo#getReindexWorking()} {@link IndiciesInfo#getSiteSearch()}
     */
    public String createNewIndiciesName(final IndexType... indiciesType) {
        final String timeStamp = timestampFormatter.format(new Date());

        for (final IndexType indexType : indiciesType) {
            final String indexName = String.format(
                    INDEX_NAME_PATTERN,
                    ClusterFactory.getClusterId(),
                    indexType.getPrefix(),
                    timeStamp);

            this.indiciesNames.put(indexType, indexName);
        }

        return timeStamp;
    }

    public Map<IndexType, String> asMap() {
        final Map<IndexType, String> actives = new HashMap<>();
        for (IndexType type : IndexType.values()) {
            final String newValue = Try.of(() -> (String) PropertyUtils
                    .getProperty(this, type.getPropertyName())).getOrNull();
            if (newValue != null) {
                actives.put(type, newValue);
            }

        }
        return actives;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.getLive() == null) ? 0 : this.getLive().hashCode());
        result = prime * result + ((this.getReindexLive() == null) ? 0 : this.getReindexLive().hashCode());
        result = prime * result + ((this.getReindexWorking() == null) ? 0 : this.getReindexWorking().hashCode());
        result = prime * result + ((this.getSiteSearch() == null) ? 0 : this.getSiteSearch().hashCode());
        result = prime * result + ((this.getWorking() == null) ? 0 : this.getWorking().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IndiciesInfo other = (IndiciesInfo) obj;
        if (this.getLive() == null) {
            if (other.getLive() != null)
                return false;
        } else if (!this.getLive().equals(other.getLive()))
            return false;
        if (this.getReindexLive() == null) {
            if (other.getReindexLive() != null)
                return false;
        } else if (!this.getReindexLive().equals(other.getReindexLive()))
            return false;
        if (this.getReindexWorking() == null) {
            if (other.getReindexWorking() != null)
                return false;
        } else if (!this.getReindexWorking().equals(other.getReindexWorking()))
            return false;
        if (this.getSiteSearch() == null) {
            if (other.getSiteSearch() != null)
                return false;
        } else if (!this.getSiteSearch().equals(other.getSiteSearch()))
            return false;
        if (this.getWorking() == null) {
            if (other.getWorking() != null)
                return false;
        } else if (!this.getWorking().equals(other.getWorking()))
            return false;
        return true;
    }

}