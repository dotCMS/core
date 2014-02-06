package com.dotcms.notifications.business;

import com.dotmarketing.business.Cachable;

//This interface should have default package access
public abstract class NewNotificationCache implements Cachable {

	abstract protected Long add(String key, Long newNotifications);

	abstract protected Long get(String key);

	abstract public void clearCache();

	abstract public void remove(String key);
}