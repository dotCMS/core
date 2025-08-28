package com.dotcms.rest;

import com.dotcms.rest.tag.RestTag;

import java.util.List;

/**
 * Response entity view for REST Tag list operations.
 * Used specifically for Tag API endpoints that return lists of RestTag objects.
 * 
 * @author dotCMS
 */
public class ResponseEntityRestTagListView extends ResponseEntityListView<RestTag> {

    public ResponseEntityRestTagListView(List<RestTag> tags) {
        super(tags);
    }
}