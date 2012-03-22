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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * <a href="ListUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.13 $
 *
 */
public class ListUtil {

	public static void distinct(List list) {
		distinct(list, null);
	}
     public static List fromEnumeration(Enumeration enu) {
            List list = new ArrayList();
    
            while (enu.hasMoreElements()) {
               Object obj = enu.nextElement();
   
              list.add(obj);
            }
   
            return list;
         }
	public static void distinct(List list, Comparator comparator) {
		if ((list == null) || (list.size() == 0)) {
			return;
		}

		Set set = null;
		if (comparator == null) {
			set = new TreeSet();
		}
		else {
			set = new TreeSet(comparator);
		}

		Iterator itr = list.iterator();

		while (itr.hasNext()) {
			Object obj = itr.next();

			if (set.contains(obj)) {
				itr.remove();
			}
			else {
				set.add(obj);
			}
		}
	}

	public static List fromArray(Object[] array) {
		if ((array == null) || (array.length == 0)) {
			return new ArrayList();
		}

		List list = new ArrayList(array.length);

		for (int i = 0; i < array.length; i++) {
			list.add(array[i]);
		}

		return list;
	}

	public static List fromCollection(Collection c) {
		if (c != null && c instanceof List) {
			return (List)c;
		}

		if ((c == null) || (c.size() == 0)) {
			return new ArrayList();
		}

		List list = new ArrayList(c.size());

		Iterator itr = c.iterator();

		while (itr.hasNext()) {
			list.add(itr.next());
		}

		return list;
	}

    public static List fromFile(String fileName) throws IOException {
        return fromFile(new File(fileName));
    }

	public static List fromFile(File file) throws IOException {
		List list = new ArrayList();

		BufferedReader br = new BufferedReader(new FileReader(file));

		String s = "";

		while ((s = br.readLine()) != null) {
			list.add(s);
		}

		br.close();

		return list;
	}

	public static List subList(List list, int begin, int end) {
		List newList = new ArrayList();

		int normalizedSize = list.size() - 1;

		if ((begin < 0) || (begin > normalizedSize) || (end < 0) ||
			(begin > end)) {

			return newList;
		}

		for (int i = begin; i < end && i <= normalizedSize; i++) {
			newList.add(list.get(i));
		}

		return newList;
	}

}