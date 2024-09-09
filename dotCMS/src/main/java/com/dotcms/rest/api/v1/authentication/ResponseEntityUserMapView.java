package com.dotcms.rest.api.v1.authentication;

import com.dotcms.rest.ResponseEntityView;

public class ResponseEntityUserMapView extends ResponseEntityView<AuthenticationForm> {
    public ResponseEntityUserMapView(AuthenticationForm entity) {
        super(entity);
    }
}
