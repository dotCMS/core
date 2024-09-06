package com.dotcms.rest.api.v1.authentication;

import com.dotcms.rest.ResponseEntityView;

public class ResponseEntityUserView extends ResponseEntityView<AuthenticationForm> {
    public ResponseEntityUserView(AuthenticationForm entity) {
        super(entity);
    }
}
