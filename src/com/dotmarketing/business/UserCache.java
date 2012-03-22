package com.dotmarketing.business;

import com.liferay.portal.model.User;


public abstract class UserCache implements Cachable {

	abstract public User add(String key,User user);

	/**
	 * 
	 * @param key Can be email or userId
	 * @return
	 */
	abstract public User get(String key);

	abstract public void clearCache();

	abstract public void remove(String key);

}
