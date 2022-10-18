package com.dotcms.rest.api.v1.tags;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.tag.model.Tag;
import java.util.List;

public class ResponseEntityTagView extends ResponseEntityView<List<Tag>> {
    public ResponseEntityTagView(final List<Tag> entity) {
        super(entity);
    }
}
