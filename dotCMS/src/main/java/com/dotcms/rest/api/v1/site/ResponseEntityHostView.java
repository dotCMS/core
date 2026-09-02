package com.dotcms.rest.api.v1.site;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.beans.Host;

/**
 * Response View for {@link Host}
 */
public class ResponseEntityHostView extends ResponseEntityView<Host> {

    public ResponseEntityHostView(final Host entity) {
        super(entity);
    }
}
