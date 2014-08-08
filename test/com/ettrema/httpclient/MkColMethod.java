package com.ettrema.httpclient;

import com.dotcms.repackage.org.apache.commons.httpclient.HttpMethodBase;

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
