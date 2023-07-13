package com.dotcms.rest;

import java.util.Map;

/**
 * Entity View that represents the data map of a Contentlet in dotCMS.
 *
 * @author Jonathan Sanchez
 * @since Sep 6th, 2022
 */
public class ResponseEntityContentletView extends ResponseEntityView<Map<String, Object>> {

    public ResponseEntityContentletView(final Map<String, Object> entity) {
        super(entity);
    }

}