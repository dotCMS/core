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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

/**
 * <a href="HTMLParser.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class HTMLParser {

	public HTMLParser(Reader reader) throws IOException {
		HTMLEditorKit.Parser parser = new DefaultParser().getParser();

		parser.parse(reader, new HTMLCallback(), true);
	}

	public List getImages() {
		return _images;
	}

	public List getLinks() {
		return _links;
	}

	private List _images = new ArrayList();
	private List _links = new ArrayList();

	private class DefaultParser extends HTMLEditorKit {

		public HTMLEditorKit.Parser getParser() {
			return super.getParser();
		}

	}

	private class HTMLCallback extends HTMLEditorKit.ParserCallback{

		public void handleText(char[] data, int pos) {
		}

		public void handleStartTag(
			HTML.Tag tag, MutableAttributeSet attributes, int pos) {

			if (tag.equals(HTML.Tag.A)) {
				String href = (String)attributes.getAttribute(
					HTML.Attribute.HREF);

				if (href != null) {
					_links.add(href);
				}
			}
			else if (tag.equals(HTML.Tag.IMG)) {
				String src = (String)attributes.getAttribute(
					HTML.Attribute.SRC);

				if (src != null) {
					_images.add(src);
				}
			}
		}

		public void handleEndTag(HTML.Tag tag, int pos) {
		}

		public void handleSimpleTag(
			HTML.Tag tag, MutableAttributeSet attributes, int pos) {

			if (tag.equals(HTML.Tag.A)) {
				String href = (String)attributes.getAttribute(
					HTML.Attribute.HREF);

				if (href != null) {
					_links.add(href);
				}
			}
			else if (tag.equals(HTML.Tag.IMG)) {
				String src = (String)attributes.getAttribute(
					HTML.Attribute.SRC);

				if (src != null) {
					_images.add(src);
				}
			}
		}

		public void handleComment(char[] data, int pos) {
		}

		public void handleError(String errorMsg, int pos) {
		}

	}

}