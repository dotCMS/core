package com.dotcms.rest;

import com.dotmarketing.tag.model.TagInode;

import java.util.Collections;
import java.util.List;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object for tag-inode operations
 * that return both error information and TagInode data
 * Used for Tag resource endpoints that perform tag-inode linking operations and may have errors
 * @author Steve Bolton
 */
public class ResponseEntityTagInodeOperationView extends ResponseEntityView<List<TagInode>> {
    public ResponseEntityTagInodeOperationView(final List<TagInode> tagInodes, final List<ErrorEntity> errors) {
        super(tagInodes, errors, Collections.emptyList());
    }
}