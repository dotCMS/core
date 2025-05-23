package com.dotcms.rest.api.v1.user;

import com.dotcms.rest.ResponseEntityView;

/**
 * Returns the response entity view for an user deleted view
 */
public class ResponseUserDeletedEntityView extends ResponseEntityView <UserDeletedView> {
    public ResponseUserDeletedEntityView(final UserDeletedView entity) {
        super(entity);
    }
}
