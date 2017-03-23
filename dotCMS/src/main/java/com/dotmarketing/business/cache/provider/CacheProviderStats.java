package com.dotmarketing.business.cache.provider;

import java.util.*;

/**
 * Created by jasontesser on 3/21/17.
 */
public class CacheProviderStats {

    private LinkedHashSet<String> statColumns = new LinkedHashSet<String>();
    private List<CacheStats> stats = new ArrayList<CacheStats>();

    private CacheStats providerStats;
    private String providerName;

    /**
     * CacheProviderStats is a class to help return the stats for different cache providers in dotCMS
     *
     * @param providerStats a single row for summary data ie... Total Memory: 20MG, Another property: Something ....
     */
    public CacheProviderStats(CacheStats providerStats, String providerName) {
        this.providerStats = providerStats;
        this.providerName = providerName;
    }

    public void addStatRecord(CacheStats stats) {
        statColumns.addAll(stats.getStatColumns());
        this.stats.add(stats);
    }

    public LinkedHashSet<String> getStatColumns() {
        return statColumns;
    }

    public List<CacheStats> getStats(){
        return this.stats;
    }

    public String getProviderName(){
        return providerName;
    }

}


