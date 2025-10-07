package com.dotcms.rest.api.v1.user;

import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

/**
 * Returns a map such as
 * userID -> {userId}
 * user   -> {userMap}
 */
public class ResponseUserMapEntityView extends ResponseEntityView <UserView> {
    public ResponseUserMapEntityView(UserView entity) {
        super(entity);
    }
}
