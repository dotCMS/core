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

package com.liferay.portlet.polls.ejb;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.liferay.portlet.polls.model.PollsChoice;
import com.liferay.util.Validator;

/**
 * <a href="PollsChoicePool.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.49 $
 *
 */
public class PollsChoicePool implements Cachable {
	public static void clear() {
		_getInstance()._clear();
	}

	public static PollsChoice get(PollsChoicePK pollsChoicePK) {
		return _getInstance()._get(pollsChoicePK);
	}

	public static PollsChoice put(PollsChoicePK pollsChoicePK, PollsChoice obj) {
		return _getInstance()._put(pollsChoicePK, obj);
	}

	public static PollsChoice remove(PollsChoicePK pollsChoicePK) {
		return _getInstance()._remove(pollsChoicePK);
	}

	private static PollsChoicePool _getInstance() {
		if (_instance == null) {
			synchronized (PollsChoicePool.class) {
				if (_instance == null) {
					_instance = new PollsChoicePool();
				}
			}
		}

		return _instance;
	}

	private PollsChoicePool() {
		_cacheable = PollsChoice.CACHEABLE;
		_cache = CacheLocator.getCacheAdministrator();
	}

	private void _clear() {
		_cache.flushGroup(primaryGroup);
	}

	private PollsChoice _get(PollsChoicePK pollsChoicePK) {
		if (!_cacheable) {
			return null;
		}
		else if (pollsChoicePK == null) {
			return null;
		}
		else {
			PollsChoice obj = null;
			String key = pollsChoicePK.toString();

			if (Validator.isNull(key)) {
				return null;
			}
			try {
				obj = (PollsChoice)_cache.get(key,primaryGroup);
			} catch (DotCacheException e) {
				Logger.debug(this, "Cache Entry not found", e);
			}
			return obj;
		}
	}

	private PollsChoice _put(PollsChoicePK pollsChoicePK, PollsChoice obj) {
		if (!_cacheable) {
			return obj;
		}
		else if (pollsChoicePK == null) {
			return obj;
		}
		else {
			String key = pollsChoicePK.toString();

			if (Validator.isNotNull(key)) {
				_cache.put(key, obj,primaryGroup);
			}

			return obj;
		}
	}

	private PollsChoice _remove(PollsChoicePK pollsChoicePK) {
		if (!_cacheable) {
			return null;
		}
		else if (pollsChoicePK == null) {
			return null;
		}
		else {
			PollsChoice obj = null;
			String key = pollsChoicePK.toString();

			if (Validator.isNull(key)) {
				return null;
			}
			try {
				obj = (PollsChoice)_cache.get(key,primaryGroup);
			} catch (DotCacheException e) {
				Logger.debug(this, "Cache Entry not found", e);
			}
			_cache.remove(key,primaryGroup);
			return obj;
		}
	}

	private static String primaryGroup = "PollsChoicePool";
    // region's name for the cache
    private static String[] groupNames = {primaryGroup};
    
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
    
    public String[] getGroups() {
    	return groupNames;
    }
	
    private DotCacheAdministrator _cache = CacheLocator.getCacheAdministrator();
	
	private static PollsChoicePool _instance;
	private boolean _cacheable;
	public void clearCache() {
		clear();
	}
}