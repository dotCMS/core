package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.ResponseEntityView;

/**
 * Typed response wrapper for the {@code DELETE /v1/roles/{roleId}} endpoint (issue #36939).
 *
 * @author hassandotcms
 * @since Aug 2026
 */
public class ResponseEntityRoleDeletionView extends ResponseEntityView<RoleDeletionView> {

    public ResponseEntityRoleDeletionView(final RoleDeletionView entity) {
        super(entity);
    }
}
