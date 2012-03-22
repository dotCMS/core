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

package com.liferay.portlet.words.util;

import java.util.Set;
import java.util.TreeSet;

import com.liferay.portlet.words.ScramblerException;

/**
 * <a href="Scrambler.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class Scrambler {

	public Scrambler(String word) throws ScramblerException {
		if (word == null || word.length() < 3) {
			throw new ScramblerException();
		}

		_word = word;
		_words = new TreeSet(new WordComparator());
	}

	public String[] scramble() {
		if (_word == null) {
			return new String[0];
		}

		_scramble(0, _word.length(), _word.toCharArray());

		return (String[])_words.toArray(new String[0]);
	}

	private void _rotate(char[] charArray, int start) {
		char temp = charArray[start];

		for (int i = charArray.length - start -1; i > 0; i--) {
			charArray[start] = charArray[++start];
		}

		charArray[start] = temp;
	}

	private void _scramble(int start, int length, char[] charArray) {
		if (length == 0) {
			String word = new String(charArray);

			for (int i = 3; i <= charArray.length; i++) {
				_words.add(word.substring(0, i));
			}
		}
		else {
			for (int i = 0; i < length; i++) {
				_scramble(start + 1, length - 1, charArray);
				_rotate(charArray, start);
			}
		}
	}

	private String _word;
	private Set _words;

}