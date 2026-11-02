package com.dotcms.rest.api.v1.container;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Response wrapper for {@code GET /api/v1/containers/dependencies}: one
 * {@link ContainerDependenciesView} per requested Container, under the standard {@code entity} key.
 */
public class ResponseEntityContainerDependenciesListView extends ResponseEntityView<List<ContainerDependenciesView>> {

    /**
     * Wraps the given views.
     *
     * @param entity the {@link ContainerDependenciesView}s to return under {@code entity}, in the
     *               order the Containers were requested
     */
    public ResponseEntityContainerDependenciesListView(final List<ContainerDependenciesView> entity) {
        super(entity);
    }
}
