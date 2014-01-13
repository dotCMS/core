package com.ettrema.httpclient;

import com.dotcms.repackage.commons_httpclient_3_1.org.apache.commons.httpclient.HttpMethodBase;

/**
 *
 * @author mcevoyb
 */
public class UnLockMethod extends HttpMethodBase {

	private final String lockToken;
	
    public UnLockMethod( String uri, String lockToken ) {
        super( uri );
		this.lockToken = lockToken;
		addRequestHeader("Lock-Token", lockToken);
    }

    @Override
    public String getName() {
        return "UNLOCK";
    }

	public String getLockToken() {
		return lockToken;
	}
	
	
}
