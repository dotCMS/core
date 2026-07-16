package com.dotcms.rest;

/**
 * ResponseEntityView wrapper for SearchView data.
 * Used by ContentResource and other search endpoints that return SearchView objects
 * containing search results, metadata, and query performance information.
 */
public class ResponseEntitySearchView extends ResponseEntityView<SearchView> {
    
    public ResponseEntitySearchView(SearchView entity) {
        super(entity);
    }
}