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

package com.liferay.portal.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import com.dotmarketing.util.Logger;
import com.liferay.portal.util.WebCacheable;
import com.liferay.portlet.translator.model.Translation;
import com.liferay.portlet.translator.util.TranslationConverter;
import com.liferay.util.FileUtil;
import com.liferay.util.StringUtil;
import com.liferay.util.Validator;

/**
 * <a href="LangBuilder.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.20 $
 *
 */
public class LangBuilder {

	public static void main(String[] args) {
		new LangBuilder();
	}

	public LangBuilder() {
		try {
			String content = _orderProps(
				new File(_LANG_DIR + "Language.properties"));

			_createProps(content, "zh_CN"); // Chinese (China)
			_createProps(content, "zh_TW"); // Chinese (Taiwan)
			_createProps(content, "nl"); // Dutch
			_createProps(content, "fr"); // French
			_createProps(content, "de"); // German
			_createProps(content, "el"); // Greek
			_createProps(content, "it"); // Italian
			_createProps(content, "ja"); // Japanese
			_createProps(content, "ko"); // Korean
			_createProps(content, "pt"); // Portuguese
			_createProps(content, "es"); // Spanish
			_createProps(content, "tr"); // Turkish
			_createProps(content, "vi"); // Vietnamese
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

	private void _createProps(String content, String languageId)
		throws IOException {

		File propsFile = new File(
			_LANG_DIR + "Language_" + languageId + ".properties");

		Properties props = new Properties();

		if (propsFile.exists()) {
			props.load(new FileInputStream(propsFile));
		}

		String translationId = "en_" + languageId;

		if (translationId.equals("en_zh_CN")) {
			translationId = "en_zh";
		}
		else if (translationId.equals("en_zh_TW")) {
			translationId = "en_zt";
		}

		BufferedReader br = new BufferedReader(new StringReader(content));
		BufferedWriter bw = new BufferedWriter(new FileWriter(
			_LANG_DIR + "Language_" + languageId + ".properties.native"));

		String line = null;

		while ((line = br.readLine()) != null) {
			int pos = line.indexOf("=");

			if (pos != -1) {
				String key = line.substring(0, pos);
				String value = line.substring(pos + 1, line.length());

				String translatedText = props.getProperty(key);

				if (translatedText == null || translatedText.equals("")) {
					Logger.info(this, languageId + " " + key);

					if (line.indexOf("{") != -1 || line.indexOf("<") != -1) {
						translatedText = value;
					}
					else {
						translatedText = _translate(translationId, value);
					}
				}

				if (Validator.isNotNull(translatedText)) {
					if (translatedText.indexOf("&#39;") != -1) {
						translatedText =
							StringUtil.replace(translatedText, "&#39;", "\'");
					}

					bw.write(key + "=" + translatedText);

					bw.newLine();
					bw.flush();
				}
			}
			else {
				bw.write(line);

				bw.newLine();
				bw.flush();
			}
		}

		br.close();
		bw.close();
	}

	private String _orderProps(File propsFile) throws IOException {
		String content = FileUtil.read(propsFile);

		BufferedReader br = new BufferedReader(new StringReader(content));
		BufferedWriter bw = new BufferedWriter(new FileWriter(propsFile));

		Set messages = new TreeSet();

		boolean begin = false;

		String line = null;

		while ((line = br.readLine()) != null) {
			int pos = line.indexOf("=");

			if (pos != -1) {
				String key = line.substring(0, pos);
				String value = line.substring(pos + 1, line.length());

				messages.add(key + "=" + value);
			}
			else {
				if (begin == true && line.equals("")) {
					_sortAndWrite(bw, messages);
				}

				if (line.equals("")) {
					begin = !begin;
				}

				bw.write(line);
				bw.newLine();
			}

			bw.flush();
		}

		if (messages.size() > 0) {
			_sortAndWrite(bw, messages);
		}

		br.close();
		bw.close();

		return FileUtil.read(propsFile);
	}

	private void _sortAndWrite(BufferedWriter bw, Set messages)
		throws IOException {

		String[] messagesArray = (String[])messages.toArray(new String[0]);

		for (int i = 0; i < messagesArray.length; i++) {
			bw.write(messagesArray[i]);
			bw.newLine();
		}

		messages.clear();
	}

	private String _translate(String translationId, String fromText) {
		if (translationId.equals("en_tr") ||
			translationId.equals("en_vi")) {

			// Automatic translator does not support Turkish or Vietnamese

			return null;
		}

		String toText = null;

		try {
			WebCacheable wc =
				new TranslationConverter(translationId, fromText);

			Translation translation = (Translation)wc.convert("");

			toText = translation.getToText();

			if ((toText != null) &&  (toText.indexOf("Babel") != -1)) {
				Logger.info(this, "Please manually check for errors.");
			}
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}

		// Keep trying

		if (toText == null) {
			return _translate(translationId, fromText);
		}

		return toText;
	}

	private String _LANG_DIR = "../portal-ejb/classes/content/";

}