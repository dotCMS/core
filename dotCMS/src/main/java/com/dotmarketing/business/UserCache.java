package com.dotmarketing.business;

import com.liferay.portal.model.User;

public abstract class UserCache implements Cachable {

  public abstract User add(String key, User user);

  /**
   * @param key Can be email or userId
   * @return
   */
  public abstract User get(String key);

  public abstract void clearCache();

  public abstract void remove(String key);
}
