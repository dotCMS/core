package com.dotcms.rest.api.v2.tags;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.tag.RestTag;

import java.util.List;
import java.util.Map;

/**
 * Response entity view for the tag creation endpoint.
 * Wraps a map with a {@code "created"} key containing the list of created (or retrieved) tags.
 *
 * @author dotCMS
 */
public class ResponseEntityTagCreateView extends ResponseEntityView<Map<String, List<RestTag>>> {
    public ResponseEntityTagCreateView(final Map<String, List<RestTag>> entity) {
        super(entity);
    }
}
