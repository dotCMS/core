package com.dotmarketing.business;

import com.dotmarketing.beans.UserProxy;

public abstract class UserProxyCache implements Cachable {

  protected abstract UserProxy addToUserProxyCache(UserProxy userProxy);

  protected abstract UserProxy getUserProxyFromUserId(String userId);

  protected abstract UserProxy getUserProxyFromLongCookie(String longLivedCookie);

  public abstract void clearCache();

  protected abstract void remove(UserProxy userProxy);
}
