package com.dotcms.achecker.parsing;


import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.commons.logging.Log;
import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.dotcms.achecker.parsing.ACheckerDocument;
import com.dotcms.achecker.parsing.DoctypeBean;
import com.dotcms.achecker.parsing.PositionalHandler;

public class XMLParser {

	private static final Log LOG = org.apache.commons.logging.LogFactory.getLog(XMLParser.class);

	public static Document readXML( String xml ) throws  Exception, SAXException {
		
		Document document = null;
				
		DoctypeBean doctypeBean = null;
		
		try {
			
 			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
 			
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

			document = docBuilder.newDocument();
			
			PositionalHandler handler = new PositionalHandler( document );
			
			Parser ets = new Parser();
						
			DOMResult dr = new DOMResult();

			SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
			
			TransformerHandler th = stf.newTransformerHandler();
			
			th.setResult(dr);

			ets.setContentHandler( handler );

			ets.setProperty(Parser.lexicalHandlerProperty, handler);
			
			ets.parse( new InputSource( new StringReader( xml )));
			
			doctypeBean = handler.getDoctypeBean();
						
		}catch (Exception e) {
			e.printStackTrace();
			LOG.error(e.getMessage() , e);
		} 
	
		ACheckerDocument doc = new ACheckerDocument(document);
		doc.setDoctypeBean(doctypeBean);
		return doc;
 
	}
}
