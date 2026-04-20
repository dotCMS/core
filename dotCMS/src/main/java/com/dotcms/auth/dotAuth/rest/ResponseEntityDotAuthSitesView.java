package com.dotcms.auth.dotAuth.rest;

import com.dotcms.rest.ResponseEntityView;

/** Response envelope for {@code GET /v1/dotauth/sites}. */
public class ResponseEntityDotAuthSitesView extends ResponseEntityView<DotAuthSitesView> {

    public ResponseEntityDotAuthSitesView(final DotAuthSitesView entity) {
        super(entity);
    }
}
