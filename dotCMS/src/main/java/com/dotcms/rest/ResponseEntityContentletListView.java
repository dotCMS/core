package com.dotcms.rest;

import java.util.List;
import java.util.Map;

/**
 * Entity View that represents a list of contentlet data maps in dotCMS.
 * Used for endpoints that return multiple contentlets.
 *
 * @author Steve Bolton
 */
public class ResponseEntityContentletListView extends ResponseEntityView<List<Map<String, Object>>> {

    public ResponseEntityContentletListView(final List<Map<String, Object>> entity) {
        super(entity);
    }

}