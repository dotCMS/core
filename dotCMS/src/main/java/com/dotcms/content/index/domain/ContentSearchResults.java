package com.dotcms.content.index.domain;

import java.io.Serial;
import java.io.Serializable;
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
 * populated objects that were loaded after the index query.  The class also implements
 * {@link List} — delegating all {@code List} operations to an internal list — so that
 * existing callers that iterate or call {@code size()} continue to work unchanged.</p>
 *
 * <p>Mirrors the shape of the legacy {@code ESSearchResults} class but without any
 * Elasticsearch-specific types in the public API.</p>
 *
 * @param <T> the type of elements in this list — typically {@code Contentlet} when produced
 *            by {@code SearchAPI}, or {@code ContentMap} when produced by {@code ESContentTool}
 */
public class ContentSearchResults<T> implements List<T>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ContentSearchResponse response;
    private final List<T> contentlets;
    private String query;
    private String rewrittenQuery;
    private long populationTook;

    public ContentSearchResults(final ContentSearchResponse response, final List<? extends T> contentlets) {
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
        return response.hits().getTotalHits().value();
    }

    public String getScrollId() {
        return response.scrollId();
    }

    public long getQueryTook() {
        return response.tookMillis();
    }

    /**
     * Returns the full neutral aggregation tree exposed to Velocity as
     * {@code $results.aggregations}. Preserves nested sub-aggregations and {@code top_hits}, so
     * legacy templates that walk {@code .buckets} / {@code getKeyAsNumber()} / {@code getDocCount()}
     * / {@code getAggregations()} keep working. For the flat first-level terms map, use
     * {@link #getResponse()}.{@link ContentSearchResponse#aggregations() aggregations()}.
     */
    public Map<String, Aggregation> getAggregations() {
        return response.aggregationTree();
    }

    public List<T> getContentlets() {
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
    @Override public Iterator<T> iterator() { return contentlets.iterator(); }
    @Override public Object[] toArray() { return contentlets.toArray(); }
    @Override public <A> A[] toArray(final A[] a) { return contentlets.toArray(a); }
    @Override public boolean add(final T o) { return contentlets.add(o); }
    @Override public boolean remove(final Object o) { return contentlets.remove(o); }
    @Override public boolean containsAll(final Collection<?> c) { return contentlets.containsAll(c); }
    @Override public boolean addAll(final Collection<? extends T> c) { return contentlets.addAll(c); }
    @Override public boolean addAll(final int index, final Collection<? extends T> c) { return contentlets.addAll(index, c); }
    @Override public boolean removeAll(final Collection<?> c) { return contentlets.removeAll(c); }
    @Override public boolean retainAll(final Collection<?> c) { return contentlets.retainAll(c); }
    @Override public void clear() { contentlets.clear(); }
    @Override public T get(final int index) { return contentlets.get(index); }
    @Override public T set(final int index, final T element) { return contentlets.set(index, element); }
    @Override public void add(final int index, final T element) { contentlets.add(index, element); }
    @Override public T remove(final int index) { return contentlets.remove(index); }
    @Override public int indexOf(final Object o) { return contentlets.indexOf(o); }
    @Override public int lastIndexOf(final Object o) { return contentlets.lastIndexOf(o); }
    @Override public ListIterator<T> listIterator() { return contentlets.listIterator(); }
    @Override public ListIterator<T> listIterator(final int index) { return contentlets.listIterator(index); }
    @Override public List<T> subList(final int fromIndex, final int toIndex) { return contentlets.subList(fromIndex, toIndex); }

    @Override
    public String toString() {
        return "ContentSearchResults [response=" + response + "]";
    }
}
