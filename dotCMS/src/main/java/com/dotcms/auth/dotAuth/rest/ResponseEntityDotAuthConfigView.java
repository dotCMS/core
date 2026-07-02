package com.dotcms.auth.dotAuth.rest;

import com.dotcms.rest.ResponseEntityView;

/** Response envelope for {@code GET /v1/dotauth/sites/{hostId}}. */
public class ResponseEntityDotAuthConfigView extends ResponseEntityView<DotAuthConfigView> {

    public ResponseEntityDotAuthConfigView(final DotAuthConfigView entity) {
        super(entity);
    }
}
