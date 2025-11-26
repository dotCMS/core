/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.parsing;

import java.util.Iterator;

public class EmptyIterable<T> implements Iterable<T> {
	
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			public boolean hasNext() {
				return false;
			}

			public T next() {
				throw new IllegalStateException("Not supported method");
			}

			public void remove() {
				throw new IllegalStateException("Not supported method");
			}
		};
	}
}
