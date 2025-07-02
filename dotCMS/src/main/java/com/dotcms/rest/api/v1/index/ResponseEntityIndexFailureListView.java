package com.dotcms.rest.api.v1.index;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;
import java.util.Map;

/**
 * Entity View for index failure list responses.
 * Contains list of failed reindex records with detailed failure information.
 * 
 * Each failure map contains:
 * - identifier: Content identifier that failed to index
 * - serverId: Server that attempted the indexing
 * - failureReason: Reason for the indexing failure
 * - priority: Priority level of the reindex operation
 * - title: Content title (if available)
 * - inode: Content inode (if available)
 * - contentlet: Full contentlet data map (if available)
 * 
 * @author Steve Bolton
 */
public class ResponseEntityIndexFailureListView extends ResponseEntityView<List<Map<String, Object>>> {
    public ResponseEntityIndexFailureListView(final List<Map<String, Object>> entity) {
        super(entity);
    }
}