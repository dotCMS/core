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

package com.liferay.util.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.liferay.util.StringUtil;

/**
 * <a href="XMLFormatter.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Alan Zimmerman
 * @version $Revision: 1.9 $
 *
 */
public class XMLFormatter {

	public static final String INDENT = "\t";

	public static String toString(String xml)
		throws DocumentException, IOException {

		return toString(xml, INDENT);
	}

	public static String toString(String xml, String indent)
		throws DocumentException, IOException {

		SAXReader reader = new SAXReader();

		Document doc = reader.read(new StringReader(xml));

		return toString(doc, indent);
	}

	public static String toString(Document doc) throws IOException {
		return toString(doc, INDENT);
	}

	public static String toString(Document doc, String indent)
		throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		OutputFormat format = OutputFormat.createPrettyPrint();

		format.setIndent(indent);
		format.setLineSeparator("\n");

		XMLWriter writer = new XMLWriter(baos, format);

		writer.write(doc);

		String content = baos.toString();

		content = StringUtil.replace(
			content,
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
			"<?xml version=\"1.0\"?>");

		int x = content.indexOf("<!DOCTYPE");
		if (x != -1) {
			x = content.indexOf(">", x) + 1;
			content = content.substring(0, x) + "\n" +
				content.substring(x, content.length());
		}

		content = StringUtil.replace(content, "\n\n\n", "\n\n");

		if (content.endsWith("\n\n")) {
			content = content.substring(0, content.length() - 2);
		}

		if (content.endsWith("\n")) {
			content = content.substring(0, content.length() - 1);
		}

		return content;
	}

}