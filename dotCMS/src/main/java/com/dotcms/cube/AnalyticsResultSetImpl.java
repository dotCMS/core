package com.dotcms.cube;

import com.dotcms.analytics.model.ResultSetItem;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represent a Result from running a query against an analytics backend.
 */
public class AnalyticsResultSetImpl implements AnalyticsResultSet {

    private final List<ResultSetItem> data;

    public AnalyticsResultSetImpl(final List<Map<String, Object>> data){
        this.data = data.stream().map(ResultSetItem::new).collect(Collectors.toList());
    }

    public long size() {
        return data.size();
    }

    @Override
    public Iterator<ResultSetItem> iterator() {
        return data.iterator();
    }

}
