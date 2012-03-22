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

package com.liferay.util.velocity;

import java.util.Map;

import javax.servlet.ServletContext;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

/**
 * <a href="VelocityContextPool.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.7 $
 *
 */
public class VelocityContextPool {

	public static ServletContext get(String name) {
		return _instance._get(name);
	}

	public static void put(String name, ServletContext ctx) {
		_instance._put(name, ctx);
	}

	public static ServletContext remove(String name) {
		return _instance._remove(name);
	}

	private VelocityContextPool() {
		_pool = new ConcurrentReaderHashMap();
	}

	private ServletContext _get(String name) {
		return (ServletContext)_pool.get(name);
	}

	private void _put(String name, ServletContext ctx) {
		_pool.put(name, ctx);
	}

	private ServletContext _remove(String name) {
		return (ServletContext)_pool.remove(name);
	}

	private static VelocityContextPool _instance = new VelocityContextPool();

	private Map _pool;

}