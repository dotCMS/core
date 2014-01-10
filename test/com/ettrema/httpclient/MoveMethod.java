package com.ettrema.httpclient;

import com.dotcms.repackage.commons_httpclient_3_1.org.apache.commons.httpclient.Header;
import com.dotcms.repackage.commons_httpclient_3_1.org.apache.commons.httpclient.HttpMethodBase;

/**
 *
 * @author mcevoyb
 */
class MoveMethod extends HttpMethodBase {

    final String newUri;

    public MoveMethod( String uri, String newUri ) {
        super( uri );
        this.newUri = newUri;
        addRequestHeader( new Header( "Destination", newUri ) );
    }

    @Override
    public String getName() {
        return "MOVE";
    }

    public String getNewUri() {
        return newUri;
    }
}
