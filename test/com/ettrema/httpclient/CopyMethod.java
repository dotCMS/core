package com.ettrema.httpclient;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethodBase;

/**
 *
 * @author mcevoyb
 */
class CopyMethod extends HttpMethodBase {

    final String newUri;

    public CopyMethod( String uri, String newUri ) {
        super( uri );
        this.newUri = newUri;
        addRequestHeader( new Header( "Destination", newUri ) );
    }

    @Override
    public String getName() {
        return "COPY";
    }

    public String getNewUri() {
        return newUri;
    }
}
