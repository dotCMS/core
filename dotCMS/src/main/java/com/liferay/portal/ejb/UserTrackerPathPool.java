/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.ejb;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.UserTrackerPath;
import com.liferay.util.Validator;

/**
 * <a href="UserTrackerPathPool.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.43 $
 *
 */
public class UserTrackerPathPool implements Cachable {
	public static void clear() {
		_getInstance()._clear();
	}

	public static UserTrackerPath get(String userTrackerPathId) {
		return _getInstance()._get(userTrackerPathId);
	}

	public static UserTrackerPath put(String userTrackerPathId,
		UserTrackerPath obj) {
		return _getInstance()._put(userTrackerPathId, obj);
	}

	public static UserTrackerPath remove(String userTrackerPathId) {
		return _getInstance()._remove(userTrackerPathId);
	}

	private static UserTrackerPathPool _getInstance() {
		if (_instance == null) {
			synchronized (UserTrackerPathPool.class) {
				if (_instance == null) {
					_instance = new UserTrackerPathPool();
				}
			}
		}

		return _instance;
	}

	private UserTrackerPathPool() {
		_cacheable = UserTrackerPath.CACHEABLE;
		_cache = CacheLocator.getCacheAdministrator();
	}

	private void _clear() {
		_cache.flushGroup(primaryGroup);
	}

	private UserTrackerPath _get(String userTrackerPathId) {
		if (!_cacheable) {
			return null;
		}
		else if (userTrackerPathId == null) {
			return null;
		}
		else {
			UserTrackerPath obj = null;
			String key = userTrackerPathId.toString();

			if (Validator.isNull(key)) {
				return null;
			}

			try {
				obj = (UserTrackerPath)_cache.get(key,primaryGroup);
			} catch (DotCacheException e) {
				Logger.debug(this, "Cache Entry not found", e);
			}
			return obj;
		}
	}

	private UserTrackerPath _put(String userTrackerPathId, UserTrackerPath obj) {
		if (!_cacheable) {
			return obj;
		}
		else if (userTrackerPathId == null) {
			return obj;
		}
		else {
			String key = userTrackerPathId.toString();

			if (Validator.isNotNull(key)) {
				_cache.put(key, obj,primaryGroup);
			}

			return obj;
		}
	}

	private UserTrackerPath _remove(String userTrackerPathId) {
		if (!_cacheable) {
			return null;
		}
		else if (userTrackerPathId == null) {
			return null;
		}
		else {
			UserTrackerPath obj = null;
			String key = userTrackerPathId.toString();

			if (Validator.isNull(key)) {
				return null;
			}
			try {
				obj = (UserTrackerPath)_cache.get(key,primaryGroup);
			} catch (DotCacheException e) {
				Logger.debug(this, "Cache Entry not found", e);
			}
			_cache.remove(key,primaryGroup);
			return obj;
		}
	}

	private static String primaryGroup = "UserTrackerPathPool";
    // region's name for the cache
    private static String[] groupNames = {primaryGroup};
    
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
    
    public String[] getGroups() {
    	return groupNames;
    }
	
    private DotCacheAdministrator _cache = CacheLocator.getCacheAdministrator();
	
	private static UserTrackerPathPool _instance;
	private boolean _cacheable;

	public void clearCache() {
		clear();
	}
}