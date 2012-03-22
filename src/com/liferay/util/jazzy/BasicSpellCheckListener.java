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

package com.liferay.util.jazzy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.swabunga.spell.engine.Word;
import com.swabunga.spell.event.SpellCheckEvent;
import com.swabunga.spell.event.SpellCheckListener;

/**
 * <a href="BasicSpellCheckListener.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class BasicSpellCheckListener implements SpellCheckListener {

	public BasicSpellCheckListener(String text) {
		_text = text;
		_textCharArray = text.toCharArray();
		_invalidWords = new ArrayList();
	}

	public void spellingError(SpellCheckEvent event) {
		List suggestions = new ArrayList();

		Iterator itr = event.getSuggestions().iterator();

		while (itr.hasNext()) {
			Word word = (Word)itr.next();

			suggestions.add(word.getWord());
		}

		int pos = event.getWordContextPosition();

		if (pos >= 0) {
			if ((pos == 0) ||
				((pos > 0) &&
				 //(_text.charAt(pos - 1) != '<') &&
				 (!_isInsideHtmlTag(pos)) &&
				 (_text.charAt(pos - 1) != '&') &&
				 (event.getInvalidWord().length() > 1))) {

				_invalidWords.add(
					new InvalidWord(
						event.getInvalidWord(), suggestions,
						event.getWordContext(), pos));
			}
		}
	}

	public List getInvalidWords() {
		return _invalidWords;
	}

	private boolean _isInsideHtmlTag(int pos) {
		boolean insideHtmlTag = false;

		for (int i = pos; i >= 0; i--) {
			if (_textCharArray[i] == '<') {
				insideHtmlTag = true;

				break;
			}
			else if (_textCharArray[i] == '>') {
				break;
			}
		}

		if (insideHtmlTag) {
			for (int i = pos; i < _textCharArray.length; i++) {
				if (_textCharArray[i] == '<') {
					insideHtmlTag = false;

					break;
				}
				else if (_textCharArray[i] == '>') {
					break;
				}
			}
		}

		return insideHtmlTag;
	}

	private String _text;
	private char[] _textCharArray;
	private List _invalidWords;

}