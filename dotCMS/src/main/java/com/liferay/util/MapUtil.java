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

import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <a href="MapUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.13 $
 *
 */
public class MapUtil {

	public static void copy(Map master, Map slave) {
		slave.clear();

		Iterator itr = master.entrySet().iterator();

		while (itr.hasNext()) {
			Map.Entry entry = (Map.Entry)itr.next();

			Object key = entry.getKey();
			Object value = entry.getValue();

			slave.put(key, value);
		}
	}

    /**
     * Returns a new map with keys and values from the given map swapped.
     * <p>
     * For each entry (k, v) in the input map, the returned map will contain an entry (v, k).
     * The input map is not modified.
     * </p>
     *
     * <p><strong>Important notes and constraints:</strong></p>
     * <ul>
     *   <li>Null handling: The provided map must not be null. In addition, none of the values in the
     *   input map may be null because values become keys in the returned map, and null keys are not
     *   permitted by the collector used here; a null value would result in a NullPointerException.</li>
     *   <li>Duplicate values: If the input map contains duplicate values (which would become duplicate
     *   keys in the inverted map), this implementation will throw an IllegalStateException due to key
     *   collisions. Ensure values are unique if inversion is required.</li>
     *   <li>Return type: The returned map is a new map instance; changes to it do not affect the input
     *   map.</li>
     * </ul>
     *
     * @param map the source map to invert; must not be null and must not contain null values
     * @return a new map where each entry's key is the original value and the value is the original key
     * @throws NullPointerException if the map is null or if any value in the map is null
     * @throws IllegalStateException if the map contains duplicate values leading to key collisions
     */
    public static Map<String, String> invertMap(Map<String, String> map) {
        return map.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getValue,
                        Map.Entry::getKey
                ));
    }
}