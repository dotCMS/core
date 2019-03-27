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
import com.liferay.portal.model.Address;
import com.liferay.util.Validator;

/**
 * <a href="AddressPool.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.61 $
 *
 */
public class AddressPool implements Cachable{
	public static void clear() {
		_getInstance()._clear();
	}
	
	public void clearCache() {
		clear();
	}

	public static Address get(String addressId) {
		return _getInstance()._get(addressId);
	}

	public static Address put(String addressId, Address obj) {
		return _getInstance()._put(addressId, obj);
	}

	public static Address remove(String addressId) {
		return _getInstance()._remove(addressId);
	}

	private static AddressPool _getInstance() {
		if (_instance == null) {
			synchronized (AddressPool.class) {
				if (_instance == null) {
					_instance = new AddressPool();
				}
			}
		}

		return _instance;
	}

	private AddressPool() {
		_cacheable = Address.CACHEABLE;
		_cache = CacheLocator.getCacheAdministrator();
	}

	private void _clear() {
		_cache.flushGroup(primaryGroup);
	}

	private Address _get(String addressId) {
		if (!_cacheable) {
			return null;
		}
		else if (addressId == null) {
			return null;
		}
		else {
			Address obj = null;
			String key = addressId.toString();

			if (Validator.isNull(key)) {
				return null;
			}
			try {
				obj = (Address)_cache.get(key, primaryGroup);
			} catch (DotCacheException e) {
				Logger.debug(this, "Cache Entry not found", e);
			}
			return obj;
		}
	}

	private Address _put(String addressId, Address obj) {
		if (!_cacheable) {
			return obj;
		}
		else if (addressId == null) {
			return obj;
		}
		else {
			String key = addressId.toString();

			if (Validator.isNotNull(key)) {
				_cache.put(key, obj, primaryGroup);
			}

			return obj;
		}
	}

	private Address _remove(String addressId) {
		if (!_cacheable) {
			return null;
		}
		else if (addressId == null) {
			return null;
		}
		else {
			Address obj = null;
			String key = addressId.toString();

			if (Validator.isNull(key)) {
				return null;
			}
			try {
				obj = (Address)_cache.get(key, primaryGroup);
			} catch (DotCacheException e) {
				Logger.debug(this, "Cache Entry not found", e);
			}
			_cache.remove(key, primaryGroup);
			return obj;
		}
	}

	private static AddressPool _instance;
	
	private static String primaryGroup = "AddressPool";
    // region's name for the cache
    private static String[] groupNames = {primaryGroup};
    
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
    
    public String[] getGroups() {
    	return groupNames;
    }
	
    private DotCacheAdministrator _cache;
	
	private boolean _cacheable;
}