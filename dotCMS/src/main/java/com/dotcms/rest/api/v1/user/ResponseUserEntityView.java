package com.dotcms.rest.api.v1.user;

import com.dotcms.rest.ResponseEntityView;

/**
 * Returns a map such as
 * userID -> {userId}
 * user   -> {userMap}
 */
public class ResponseUserEntityView extends ResponseEntityView <UserView> {
    public ResponseUserEntityView(UserView entity) {
        super(entity);
    }
}
