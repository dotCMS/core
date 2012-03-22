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
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.liferay.util.FileUtil;
import com.liferay.util.StringUtil;
import com.liferay.util.Validator;
import com.liferay.util.xml.descriptor.XMLDescriptor;

/**
 * <a href="XMLMergerRunner.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Jorge Ferrer
 * @version $Revision: 1.6 $
 *
 */
public class XMLMergerRunner {

	public static void main(String[] args)
		throws ClassNotFoundException, DocumentException,
			   IllegalAccessException, InstantiationException, IOException {

		if ((args != null) && (args.length == 4)) {
			XMLMergerRunner runner = new XMLMergerRunner(args[3]);

			runner.mergeAndSave(args[0], args[1], args[2]);
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	public XMLMergerRunner(String descriptorClassName) {
		if (Validator.isNotNull(descriptorClassName)) {
			_descriptorClassName = descriptorClassName;
		}
	}

	public void mergeAndSave(
			String masterFile, String slaveFile, String mergedFile)
		throws ClassNotFoundException, DocumentException,
			   IllegalAccessException, InstantiationException, IOException {

		mergeAndSave(
			new File(masterFile), new File(slaveFile), new File(mergedFile));
	}

	public void mergeAndSave(File masterFile, File slaveFile, File mergedFile)
		throws ClassNotFoundException, DocumentException,
			   IllegalAccessException, InstantiationException, IOException {

		String xml1 = FileUtil.read(masterFile);
		String xml2 = FileUtil.read(slaveFile);

		String mergedXml = _merge(xml1, xml2);

		FileUtil.write(mergedFile, mergedXml);
	}

	private String _documentToString(Document doc, String docType)
		throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		OutputFormat format = OutputFormat.createPrettyPrint();

		format.setIndent("\t");
		format.setLineSeparator("\n");

		XMLWriter writer = new XMLWriter(baos, format);

		writer.write(doc);

		String xml = baos.toString();

		int pos = xml.indexOf("<?");

		String header = xml.substring(pos, xml.indexOf("?>", pos) + 2);

		xml = StringUtil.replace(xml, header, "");
		xml = header + "\n" + docType + "\n" + xml;

		return xml;
	}

	private String _merge(String masterXml, String slaveXml)
		throws ClassNotFoundException, DocumentException,
			   IllegalAccessException, InstantiationException, IOException {

		int pos = masterXml.indexOf("<!DOCTYPE");
		String masterDoctype = "";
		if (pos >= 0) {
			masterDoctype = masterXml.substring(
				pos, masterXml.indexOf(">", pos) + 1);
			masterXml = StringUtil.replace(masterXml, masterDoctype, "");
		}

		pos = slaveXml.indexOf("<!DOCTYPE");
		String slaveDoctype = "";
		if (pos >= 0) {
			slaveDoctype = slaveXml.substring(
				pos, slaveXml.indexOf(">", pos) + 1);
			slaveXml = StringUtil.replace(slaveXml, slaveDoctype, "");
		}

		String doctype = null;
		if (Validator.isNotNull(masterDoctype)) {
			doctype = masterDoctype;
		}
		else {
			doctype = slaveDoctype;
		}

		SAXReader reader = new SAXReader();

		Document masterDoc = reader.read(new StringReader(masterXml));
		Document slaveDoc = reader.read(new StringReader(slaveXml));

		XMLDescriptor descriptor = null;
		if (_descriptorClassName.equals(_AUTO_DESCRIPTOR)) {
			descriptor = XMLTypeDetector.determineType(doctype, masterDoc);
		}
		else {
			descriptor = (XMLDescriptor)Class.forName(
				_descriptorClassName).newInstance();
		}

		XMLMerger merger = new XMLMerger(descriptor);

		Document mergedDoc = merger.merge(masterDoc, slaveDoc);

		return _documentToString(mergedDoc, doctype);
	}

	private static final String _AUTO_DESCRIPTOR = "auto";

	private String _descriptorClassName = _AUTO_DESCRIPTOR;

}