package com.dotcms.api.client.files.traversal.data;

import com.dotcms.api.client.RestClientFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class Pusher {

    @Inject
    protected RestClientFactory clientFactory;

}
