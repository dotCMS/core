package com.dotcms.rest.api.v1.container;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.containers.model.Container;
import java.util.List;

public class ResponseEntityContainerView extends ResponseEntityView<List<Container>> {
    public ResponseEntityContainerView(final List<Container> entity) {
        super(entity);
    }
}
