package com.dotcms.cache.lettuce;

/**
 * Result for set operation see {@link RedisClient#set(Object, Object)}
 * @author jsanca
 */
public enum SetResult {

    NO_CONN, FAIL, SUCCESS
}
