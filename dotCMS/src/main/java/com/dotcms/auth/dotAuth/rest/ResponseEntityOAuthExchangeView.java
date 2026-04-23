package com.dotcms.auth.dotAuth.rest;

import com.dotcms.rest.ResponseEntityView;

/** Response envelope for {@code POST /v1/dotauth/oauth/exchange}. */
public class ResponseEntityOAuthExchangeView extends ResponseEntityView<OAuthExchangeView> {

    public ResponseEntityOAuthExchangeView(final OAuthExchangeView entity) {
        super(entity);
    }
}
