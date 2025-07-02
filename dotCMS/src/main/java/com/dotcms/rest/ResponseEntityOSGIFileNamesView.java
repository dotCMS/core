package com.dotcms.rest;

import java.util.Set;

/**
 * Entity View for OSGI file names collection responses.
 * Contains sets of uploaded OSGI bundle file names.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityOSGIFileNamesView extends ResponseEntityView<Set<String>> {
    public ResponseEntityOSGIFileNamesView(final Set<String> entity) {
        super(entity);
    }
}