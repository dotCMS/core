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

package com.liferay.portlet.admin.ejb;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.liferay.portlet.admin.model.AdminConfig;
import com.liferay.util.Validator;

/**
 * <a href="AdminConfigPool.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.47 $
 *
 */
public class AdminConfigPool implements Cachable {
	public static void clear() {
		_getInstance()._clear();
	}

	public static AdminConfig get(String configId) {
		return _getInstance()._get(configId);
	}

	public static AdminConfig put(String configId, AdminConfig obj) {
		return _getInstance()._put(configId, obj);
	}

	public static AdminConfig remove(String configId) {
		return _getInstance()._remove(configId);
	}

	private static AdminConfigPool _getInstance() {
		if (_instance == null) {
			synchronized (AdminConfigPool.class) {
				if (_instance == null) {
					_instance = new AdminConfigPool();
				}
			}
		}

		return _instance;
	}

	private AdminConfigPool() {
		_cacheable = AdminConfig.CACHEABLE;
		_cache = CacheLocator.getCacheAdministrator();
	}

	private void _clear() {
		_cache.flushGroup(primaryGroup);
	}

	private AdminConfig _get(String configId) {
		if (!_cacheable) {
			return null;
		}
		else if (configId == null) {
			return null;
		}
		else {
			AdminConfig obj = null;
			String key = configId.toString();

			if (Validator.isNull(key)) {
				return null;
			}
			try {
				obj = (AdminConfig)_cache.get(key,primaryGroup);
			} catch (DotCacheException e) {
				Logger.debug(this, "Cache Entry not found", e);
			}
			return obj;
		}
	}

	private AdminConfig _put(String configId, AdminConfig obj) {
		if (!_cacheable) {
			return obj;
		}
		else if (configId == null) {
			return obj;
		}
		else {
			String key = configId.toString();
			if (Validator.isNotNull(key)) {
				_cache.put(key, obj,primaryGroup);
			}
			return obj;
		}
	}

	private AdminConfig _remove(String configId) {
		if (!_cacheable) {
			return null;
		}
		else if (configId == null) {
			return null;
		}
		else {
			AdminConfig obj = null;
			String key = configId.toString();

			if (Validator.isNull(key)) {
				return null;
			}
			try {
				obj = (AdminConfig)_cache.get(key,primaryGroup);
			} catch (DotCacheException e) {
				Logger.debug(this, "Cache Entry not found", e);
			}
			_cache.remove(key,primaryGroup);
			return obj;
		}
	}

	private static String primaryGroup = "AdminConfigPool";
    // region's name for the cache
    private static String[] groupNames = {primaryGroup};
    
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
    
    public String[] getGroups() {
    	return groupNames;
    }
	
    private DotCacheAdministrator _cache = CacheLocator.getCacheAdministrator();
	
	private static AdminConfigPool _instance;
	private boolean _cacheable;

	public void clearCache() {
		clear();
	}
}