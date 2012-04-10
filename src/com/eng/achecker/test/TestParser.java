package com.eng.achecker.test;


import java.io.IOException;
import java.io.OutputStream;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.eng.achecker.parsing.XMLParser;
import com.eng.achecker.utility.ContentDownloader;
import com.eng.achecker.utility.ParserConst;
import com.eng.achecker.utility.URLConnectionInputStream;

public class TestParser {

	public static void serialize(Document doc, OutputStream out) throws Exception {
        OutputFormat format = new OutputFormat(doc);
        format.setLineWidth(65);
        format.setIndenting(true);
        format.setIndent(2);
        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.serialize(doc);
    }

	/**
	 * @param args
	 * @throws Exception 
	 * @throws SAXException 
	 */
	public static void main(String[] args) throws SAXException, Exception {
		
		URLConnectionInputStream url = new URLConnectionInputStream("http://www.apple.com");
		url.setupProxy("proxy.eng.it", "3128", "micmastr", "apriti80");
		String content = ContentDownloader.getContent(url);

		XMLParser parser = new XMLParser();
		Document document = parser.readXML(content);
		
		serialize(document, System.out);
		printLineNumber( document );
	}

	public static void  printLineNumber(Document document ){
		NodeList lista = document.getChildNodes();
		if( lista!= null ){

			for(int i = 0; i<lista.getLength(); i++){
				Node child = lista.item(i);
				System.out.println("document " + child.getNodeName() +" line="+ child.getUserData(ParserConst.LINE_NUMBER_KEY_NAME ) );				 
				printNodeNumber( child );
			}
		}
		
	}
	
	public static void  printNodeNumber(Node child ){
		NodeList lista = child.getChildNodes();
		if( lista!= null ){

			for(int i = 0; i<lista.getLength(); i++){
				System.out.println("name " + lista.item(i).getNodeName() +" line= "+ lista.item(i).getUserData(ParserConst.LINE_NUMBER_KEY_NAME ) );
				printNodeNumber( lista.item(i) );
			}
		}	 
			 
		 
		
	}
}
