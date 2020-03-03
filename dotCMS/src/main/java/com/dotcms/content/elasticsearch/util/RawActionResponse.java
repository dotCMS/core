package com.dotcms.content.elasticsearch.util;


import org.elasticsearch.action.ActionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.client.ClientResponse;

class RawActionResponse extends ActionResponse {

    private final ClientResponse delegate;

    private RawActionResponse(ClientResponse delegate) {
        this.delegate = delegate;
    }

    static RawActionResponse create(ClientResponse response) {
        return new RawActionResponse(response);
    }

    public HttpStatus statusCode() {
        return delegate.statusCode();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.reactive.function.client.ClientResponse#headers()
     */
    public ClientResponse.Headers headers() {
        return delegate.headers();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.reactive.function.client.ClientResponse#body(org.springframework.web.reactive.function.BodyExtractor)
     */
    public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
        return delegate.body(extractor);
    }

}
