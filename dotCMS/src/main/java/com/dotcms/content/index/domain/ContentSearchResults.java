package com.dotcms.content.index.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Vendor-neutral replacement for {@code ESSearchResults}.
 *
 * <p>Holds a {@link ContentSearchResponse} (timing, scroll, aggregations, hits) plus the
 * populated {@link com.dotmarketing.portlets.contentlet.model.Contentlet} objects that
 * were loaded from the database after the index query.  The class also implements
 * {@link List} — delegating all {@code List} operations to an internal list — so that
 * existing callers that iterate or call {@code size()} continue to work unchanged.</p>
 *
 * <p>Mirrors the shape of the legacy {@code ESSearchResults} class but without any
 * Elasticsearch-specific types in the public API.</p>
 */
public class ContentSearchResults implements List<Object> {

    private static final long serialVersionUID = 1L;

    private final ContentSearchResponse response;
    private final List<Object> contentlets;
    private String query;
    private String rewrittenQuery;
    private long populationTook;

    public ContentSearchResults(final ContentSearchResponse response, final List<?> contentlets) {
        this.response = response;
        this.contentlets = new ArrayList<>(contentlets);
    }

    // -------------------------------------------------------------------------
    // Domain accessors
    // -------------------------------------------------------------------------

    public ContentSearchResponse getResponse() {
        return response;
    }

    public SearchHits getHits() {
        return response.hits();
    }

    public long getTotalResults() {
        return response.hits().totalHits().value();
    }

    public String getScrollId() {
        return response.scrollId();
    }

    public long getQueryTook() {
        return response.tookMillis();
    }

    public Map<String, List<AggregationBucket>> getAggregations() {
        return response.aggregations();
    }

    public List<Object> getContentlets() {
        return contentlets;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    public String getRewrittenQuery() {
        return rewrittenQuery;
    }

    public void setRewrittenQuery(final String rewrittenQuery) {
        this.rewrittenQuery = rewrittenQuery;
    }

    public long getPopulationTook() {
        return populationTook;
    }

    public void setPopulationTook(final long populationTook) {
        this.populationTook = populationTook;
    }

    // -------------------------------------------------------------------------
    // List delegation
    // -------------------------------------------------------------------------

    @Override public int size() { return contentlets.size(); }
    @Override public boolean isEmpty() { return contentlets.isEmpty(); }
    @Override public boolean contains(final Object o) { return contentlets.contains(o); }
    @Override public Iterator<Object> iterator() { return contentlets.iterator(); }
    @Override public Object[] toArray() { return contentlets.toArray(); }
    @Override public <T> T[] toArray(final T[] a) { return contentlets.toArray(a); }
    @Override public boolean add(final Object o) { return contentlets.add(o); }
    @Override public boolean remove(final Object o) { return contentlets.remove(o); }
    @Override public boolean containsAll(final Collection<?> c) { return contentlets.containsAll(c); }
    @Override public boolean addAll(final Collection<?> c) { return contentlets.addAll(c); }
    @Override public boolean addAll(final int index, final Collection<?> c) { return contentlets.addAll(index, c); }
    @Override public boolean removeAll(final Collection<?> c) { return contentlets.removeAll(c); }
    @Override public boolean retainAll(final Collection<?> c) { return contentlets.retainAll(c); }
    @Override public void clear() { contentlets.clear(); }
    @Override public Object get(final int index) { return contentlets.get(index); }
    @Override public Object set(final int index, final Object element) { return contentlets.set(index, element); }
    @Override public void add(final int index, final Object element) { contentlets.add(index, element); }
    @Override public Object remove(final int index) { return contentlets.remove(index); }
    @Override public int indexOf(final Object o) { return contentlets.indexOf(o); }
    @Override public int lastIndexOf(final Object o) { return contentlets.lastIndexOf(o); }
    @Override public ListIterator<Object> listIterator() { return contentlets.listIterator(); }
    @Override public ListIterator<Object> listIterator(final int index) { return contentlets.listIterator(index); }
    @Override public List<Object> subList(final int fromIndex, final int toIndex) { return contentlets.subList(fromIndex, toIndex); }

    @Override
    public String toString() {
        return "ContentSearchResults [response=" + response + "]";
    }
}
