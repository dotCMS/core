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

package com.liferay.util.lang;

import java.io.Serializable;

/**
 * <a href="FastStringBufferImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public final class FastStringBufferImpl implements Serializable {

	public FastStringBufferImpl() {
		this(16);
	}

	public FastStringBufferImpl(int length) {
		_buffer = new char[length];
		_shared = false;
	}

	public FastStringBufferImpl(String str) {
		this(str.length() + 16);

		append(str);
	}

	public FastStringBufferImpl append(boolean value) {
		return append(String.valueOf(value));
	}

	public FastStringBufferImpl append(char value) {
		ensureCapacity(_length + 1);
		_copy();
		_buffer[_length++] = value;

		return this;
	}

	public FastStringBufferImpl append(char[] value) {
		ensureCapacity(_length + value.length);
		_copy();

		for (int i = 0; i < value.length; i++) {
			_buffer[_length++] = value[i];
		}

		return this;
	}

	public FastStringBufferImpl append(double value) {
		return append(String.valueOf(value));
	}

	public FastStringBufferImpl append(float value) {
		return append(String.valueOf(value));
	}

	public FastStringBufferImpl append(int value) {
		return append(String.valueOf(value));
	}

	public FastStringBufferImpl append(long value) {
		return append(String.valueOf(value));
	}

	public FastStringBufferImpl append(short value) {
		return append(String.valueOf(value));
	}

	public FastStringBufferImpl append(Object obj) {
		return append(String.valueOf(obj));
	}

	public FastStringBufferImpl append(String str) {
		if (str == null) {
			str = String.valueOf(str);
		}

		int len = str.length();
		ensureCapacity(_length + len);
		_copy();
		str.getChars(0, len, _buffer, _length);
		_length += len;

		return this;
	}

	public char charAt(int index) {
		if ((index < 0) || (index >= _length)) {
			throw new StringIndexOutOfBoundsException(index);
		}

		return _buffer[index];
	}

	public FastStringBufferImpl delete(int start, int end) {
		if (start < 0) {
			throw new StringIndexOutOfBoundsException(start);
		}

		if (end > _length) {
			end = _length;
		}

		if (start > end) {
			throw new StringIndexOutOfBoundsException();
		}

		int delta = end - start;

		if (delta > 0) {
			if (_shared) {
				_copy();
			}

			System.arraycopy(
				_buffer, start + delta, _buffer, start, _length - end);

			_length -= delta;
		}

		return this;
	}

	public FastStringBufferImpl deleteCharAt(int index) {
		if ((index < 0) || (index >= _length)) {
			throw new StringIndexOutOfBoundsException(index);
		}

		System.arraycopy(
			_buffer, index + 1, _buffer, index, _length - index - 1);

		_length--;

		return this;
	}

	public void ensureCapacity(int minCapacity) {
		int maxCapacity = _buffer.length;

		if (minCapacity > maxCapacity) {
			int newCapacity = (maxCapacity + 1) * 2;

			if (minCapacity > newCapacity) {
				newCapacity = minCapacity;
			}

			char newValue[] = new char[newCapacity];

			System.arraycopy(_buffer, 0, newValue, 0, _length);

			_buffer = newValue;
			_shared = false;
		}
	}

	public int indexOf(String str) {
		return indexOf(str, 0);
	}

	public int indexOf(String str, int fromIndex) {
		int len = str.length();

		if (fromIndex < 0) {
			fromIndex = 0;
		}

		int end = _length - len + 1;

		while (fromIndex < end) {
			int i = 0;

			while (i < len) {
				if (_buffer[fromIndex + i] != str.charAt(i)) {
					break;
				}

				i++;
			}

			if (i == len) {
				return fromIndex;
			}

			fromIndex++;
		}

		return -1;
	}

	public int lastIndexOf(String str) {
		return lastIndexOf(str, _length);
	}

	public int lastIndexOf(String str, int fromIndex) {
		int len = str.length();

		if (_length <= fromIndex) {
			fromIndex = _length;
		}

		int end = len;

		while (fromIndex >= end) {
			int i = 0;

			while (i < len) {
				if (_buffer[fromIndex - len + i] != str.charAt(i)) {
					break;
				}

				i++;
			}

			if (i == len) {
				return fromIndex - len;
			}

			fromIndex--;
		}

		return -1;
	}

	public int length() {
		return _length;
	}

	public void setLength(int length) {
		if (length < 0) {
			throw new StringIndexOutOfBoundsException(length);
		}

		ensureCapacity(length);

		if (_length < length) {
			_copy();

			while (_length < length) {
				_buffer[_length++] = '\0';
			}
		}

		_length = length;
	}

	public String toString() {
		_shared = true;

		return new String(_buffer, 0, _length);
	}

	private final void _copy() {
		if (_shared) {
			char newValue[] = new char[_buffer.length];

			System.arraycopy(_buffer, 0, newValue, 0, _length);

			_buffer = newValue;
			_shared = false;
		}
	}

	private static final long serialVersionUID = 1L;

	private char _buffer[];
	private int _length = 0;
	private boolean _shared;

}