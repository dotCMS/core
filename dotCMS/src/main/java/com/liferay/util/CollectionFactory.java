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

import com.dotcms.repackage.gnu.trove.THashMap;
import com.dotcms.repackage.gnu.trove.THashSet;
import com.dotcms.repackage.gnu.trove.TLinkedList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.repackage.EDU.oswego.cs.dl.util.concurrent.SyncMap;
import com.dotcms.repackage.EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

/**
 * <a href="CollectionFactory.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.12 $
 *
 */
public class CollectionFactory {

	public static final boolean TROVE = GetterUtil.get(
		SystemProperties.get("trove"), true);

	public static Map getHashMap() {
		if (TROVE) {
			return new THashMap();
		}
		else {
			return new HashMap();
		}
	}

	public static Map getHashMap(int capacity) {
		if (TROVE) {
			return new THashMap(capacity);
		}
		else {
			return new HashMap(capacity);
		}
	}

	public static Set getHashSet() {
		if (TROVE) {
			return new THashSet();
		}
		else {
			return new HashSet();
		}
	}

	public static Set getHashSet(int capacity) {
		if (TROVE) {
			return new THashSet(capacity);
		}
		else {
			return new HashSet(capacity);
		}
	}

	public static List getLinkedList() {
		if (TROVE) {
			return new TLinkedList();
		}
		else {
			return new LinkedList();
		}
	}

	public static Map getSyncHashMap() {
		return new SyncMap(getHashMap(), new WriterPreferenceReadWriteLock());
	}

	public static Map getSyncHashMap(int capacity) {
		return new SyncMap(
			getHashMap(capacity), new WriterPreferenceReadWriteLock());
	}

}