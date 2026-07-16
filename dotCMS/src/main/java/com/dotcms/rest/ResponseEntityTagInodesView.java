package com.dotcms.rest;

import com.dotmarketing.tag.model.TagInode;

import java.util.List;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object to include the expected List<TagInode> and related
 * Used for Tag resource endpoints that return tag-inode relationships
 * @author Steve Bolton
 */
public class ResponseEntityTagInodesView extends ResponseEntityView<List<TagInode>> {
    public ResponseEntityTagInodesView(final List<TagInode> entity) {
        super(entity);
    }
}