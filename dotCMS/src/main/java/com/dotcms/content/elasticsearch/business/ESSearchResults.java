package com.dotcms.content.elasticsearch.business;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.suggest.Suggest;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ESSearchResults implements List {

	String query;
	String rewrittenQuery;
	long populationTook = 0;

	final SearchResponse response;

	public ESSearchResults(final SearchResponse response, final List contentlets) {
		super();
		this.response = response;
		this.scrollId = response.getScrollId();
		this.cons = contentlets;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String scrollId;

	public String getScrollId() {
		return scrollId;
	}

	@Override
	public Object[] toArray(Object[] a) {
		return cons.toArray();
	}

	@Override
	public boolean addAll(Collection c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(int index, Collection c) {
		// TODO Auto-generated method stub
		return false;
	}

	public void setScrollId(String scrollId) {
		this.scrollId = scrollId;
	}

	public SearchResponse getResponse() {
		return response;
	}

	public long getTotalResults() {
		return response.getHits().getTotalHits().value;
	}

	public SearchHits getHits() {
		return response.getHits();
	}

	public Suggest getSuggestions() {
		return response.getSuggest();
	}

	public Aggregations getAggregations() {
		return response.getAggregations();
	}

	final List cons;

	public long getCount() {
		return response.getHits().getHits().length;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * Returns the ES query after our permissions query is append to it
	 *
	 * @return Rewritten query with a permissions filter
	 */
	public String getRewrittenQuery () {
		return rewrittenQuery;
	}

	public void setRewrittenQuery ( String rewrittenQuery ) {
		this.rewrittenQuery = rewrittenQuery;
	}

	public long getQueryTook() {
		return response.getTook().getMillis();
	}

	public long getPopulationTook() {
		return populationTook;
	}

	public void setPopulationTook(long populationTook) {
		this.populationTook = populationTook;
	}

	public List getContentlets() {

		return cons;

	}

	@Override
	public int size() {

		return cons.size();
	}

	@Override
	public boolean isEmpty() {
		return cons.isEmpty();
	}

	@Override
	public boolean contains(Object o) {

		return cons.contains(o);
	}

	@Override
	public Iterator iterator() {

		return cons.iterator();
	}

	@Override
	public Object[] toArray() {

		return cons.toArray();
	}

	@Override
	public boolean remove(Object o) {

		return cons.remove(o);
	}

	@Override
	public boolean containsAll(Collection c) {

		return cons.containsAll(c);
	}

	@Override
	public boolean removeAll(Collection c) {
		return cons.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection c) {
		return cons.retainAll(c);
	}

	@Override
	public void clear() {
		cons.clear();

	}

	@Override
	public Object get(int index) {
		return cons.get(index);
	}

	@Override
	public Object set(int index, Object element) {
		return cons.set(index, element);
	}

	@Override
	public void add(int index, Object element) {
		cons.add(index, element);

	}

	@Override
	public Object remove(int index) {

		return cons.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return cons.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return cons.lastIndexOf(o);
	}

	@Override
	public ListIterator<Object> listIterator() {
		return cons.listIterator();
	}

	@Override
	public ListIterator<Object> listIterator(int index) {
		return cons.listIterator(index);
	}

	@Override
	public List<Object> subList(int fromIndex, int toIndex) {
		return cons.subList(fromIndex, toIndex);
	}

	@Override
	public String toString() {
		return "ESSearchResults [response=" + response + "]";
	}

	@Override
	public boolean add(Object e) {
		return cons.add(e);
	}

}