package com.dotcms.rest.api.v1.user;

import com.dotcms.rest.ResponseEntityView;

/**
 * Returns a map such as
 * userID -> {userId}
 * user   -> {userMap}
 */
public class ResponseUserDeletedEntityView extends ResponseEntityView<UserDeletedView> {
    public ResponseUserDeletedEntityView(final UserDeletedView entity) {
        super(entity);
    }
}
