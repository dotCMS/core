package com.dotcms.achecker.utility;


import java.io.OutputStream;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;

import com.dotcms.achecker.parsing.XMLParser;

public class PrettyDump {

	public static void dump(String page, OutputStream out) {
		try {
			XMLParser parser = new XMLParser();
			Document document = parser.readXML(page);
			serialize(document, out);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	private static void serialize(Document doc, OutputStream out) throws Exception {
        OutputFormat format = new OutputFormat(doc);
        format.setLineWidth(65);
        format.setIndenting(true);
        format.setIndent(2);
        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.serialize(doc);
    }



}
