package com.dotcms.rest.api.v1.container;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for the list of Containers that are still used by at least one Template.
 */
public class ResponseEntityContainerDependenciesView extends ResponseEntityView<List<ContainerDependenciesView>> {
    public ResponseEntityContainerDependenciesView(final List<ContainerDependenciesView> entity) {
        super(entity);
    }
}
