package com.dotcms.rest.api.v2.tags;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.tag.model.TagInode;

import java.util.List;

/**
 * Tags by Inode view
 * @author jsanca
 */
public class ResponseEntityTagInodesMapView  extends ResponseEntityView<List<TagInode>>  {
    public ResponseEntityTagInodesMapView(final List<TagInode> entity) {
        super(entity);
    }
}
