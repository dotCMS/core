package com.ettrema.httpclient;

import com.dotcms.repackage.commons_httpclient_3_1.org.apache.commons.httpclient.HttpMethodBase;

/**
 *
 * @author mcevoyb
 */
public class MkColMethod extends HttpMethodBase {

    public MkColMethod( String uri ) {
        super( uri );
    }

    @Override
    public String getName() {
        return "MKCOL";
    }
}
