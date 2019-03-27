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

/**
 * <a href="FastStringBuffer.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class FastStringBuffer {

	public FastStringBuffer() {
		_sb = new StringBuffer();
	}

	public FastStringBuffer(int length) {
		_sb = new StringBuffer(length);
	}

	public FastStringBuffer(String str) {
		_sb = new StringBuffer(str);
	}

	public FastStringBuffer append(boolean value) {
		_sb.append(value);

		return this;
	}

	public FastStringBuffer append(char value) {
		_sb.append(value);

		return this;
	}

	public FastStringBuffer append(char[] value) {
		_sb.append(value);

		return this;
	}

	public FastStringBuffer append(double value) {
		_sb.append(value);

		return this;
	}

	public FastStringBuffer append(float value) {
		_sb.append(value);

		return this;
	}

	public FastStringBuffer append(int value) {
		_sb.append(value);

		return this;
	}

	public FastStringBuffer append(long value) {
		_sb.append(value);

		return this;
	}

	public FastStringBuffer append(short value) {
		_sb.append(value);

		return this;
	}

	public FastStringBuffer append(Object value) {
		_sb.append(value);

		return this;
	}

	public FastStringBuffer append(String value) {
		_sb.append(value);

		return this;
	}

	public FastStringBuffer delete(int start, int end) {
		_sb.delete(start, end);

		return this;
	}

	public FastStringBuffer deleteCharAt(int index) {
		_sb.deleteCharAt(index);

		return this;
	}

	public int indexOf(String str) {
		return indexOf(str, 0);
	}

	public int indexOf(String str, int fromIndex) {
		return _sb.indexOf(str, fromIndex);
	}

	public FastStringBuffer insert(int offset, boolean value) {
		_sb.insert(offset, value);

		return this;
	}

	public FastStringBuffer insert(int offset, char value) {
		_sb.insert(offset, value);

		return this;
	}

	public FastStringBuffer insert(int offset, char[] value) {
		_sb.insert(offset, value);

		return this;
	}

	public FastStringBuffer insert(int offset, double value) {
		_sb.insert(offset, value);

		return this;
	}

	public FastStringBuffer insert(int offset, float value) {
		_sb.insert(offset, value);

		return this;
	}

	public FastStringBuffer insert(int offset, int value) {
		_sb.insert(offset, value);

		return this;
	}

	public FastStringBuffer insert(int offset, long value) {
		_sb.insert(offset, value);

		return this;
	}

	public FastStringBuffer insert(int offset, Object value) {
		_sb.insert(offset, value);

		return this;
	}

	public FastStringBuffer insert(int offset, String value) {
		_sb.insert(offset, value);

		return this;
	}

	public int lastIndexOf(String str) {
		return lastIndexOf(str, length());
	}

	public int lastIndexOf(String str, int fromIndex) {
		return _sb.lastIndexOf(str, fromIndex);
	}

	public int length() {
		return _sb.length();
	}

	public String toString() {
		return _sb.toString();
	}

	// TODO: Finish writing FastStringBufferImpl so that it implements all of
	// the above methods. When we move to JDK 5, we can modify FastStringBuffer
	// to use java.lang.StringBuilder without changing all the places that
	// already reference FastStringBuffer.

	private StringBuffer _sb;

}