package com.dotmarketing.business;

import com.dotmarketing.beans.UserProxy;


public abstract class UserProxyCache implements Cachable {

	abstract protected UserProxy addToUserProxyCache(UserProxy userProxy);

	abstract protected UserProxy getUserProxyFromUserId(String userId);
	
	abstract protected UserProxy getUserProxyFromLongCookie(String longLivedCookie);

	abstract public void clearCache();

	abstract protected void remove(UserProxy userProxy);

}
