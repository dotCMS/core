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

package com.liferay.util;

import java.util.Map;

import com.dotcms.repackage.EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

/**
 * <a href="SimpleCachePool.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class SimpleCachePool {

	public static Object get(String id) {
		return _getInstance()._get(id);
	}

	public static void put(String id, Object obj) {
		_getInstance()._put(id, obj);
	}

	public static Object remove(String id) {
		return _getInstance()._remove(id);
	}

	private static SimpleCachePool _getInstance() {
		if (_instance == null) {
			synchronized (SimpleCachePool.class) {
				if (_instance == null) {
					_instance = new SimpleCachePool();
				}
			}
		}

		return _instance;
	}

	private SimpleCachePool() {
		_scPool = new ConcurrentReaderHashMap(_SIZE);
	}

	private Object _get(String id) {
		return (Object)_scPool.get(id);
	}

	private void _put(String id, Object ds) {
		_scPool.put(id, ds);
	}

	private Object _remove(String id) {
		return _scPool.remove(id);
	}

	private static SimpleCachePool _instance;
	private static int _SIZE = 100000;

	private Map _scPool;

}