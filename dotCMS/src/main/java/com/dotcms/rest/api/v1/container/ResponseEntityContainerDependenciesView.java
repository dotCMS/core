package com.dotcms.rest.api.v1.container;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper for {@code GET /api/v1/containers/dependencies}: a
 * {@link ContainerDependenciesView} under the standard {@code entity} key.
 */
public class ResponseEntityContainerDependenciesView extends ResponseEntityView<ContainerDependenciesView> {

    /**
     * Wraps the given view.
     *
     * @param entity the {@link ContainerDependenciesView} to return under {@code entity}
     */
    public ResponseEntityContainerDependenciesView(final ContainerDependenciesView entity) {
        super(entity);
    }
}
