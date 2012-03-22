package com.dotmarketing.util.ups;

import java.io.StringReader;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.dotmarketing.util.Logger;


/**
 * 
 * @author Oswaldo Gallango
 *
 */

public class UPSResponseParser {

private static Vector<UPSResponseObject> objectList= null;
	
	/*
	 * This parser fill the ObjectCustomerWebService bean with the customer information
	 */
public static Vector getObjectShippingPrices(String uriXML) {
	try {
		
		InputSource is = new InputSource(new StringReader(uriXML));
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse( is );
		objectList = new Vector<UPSResponseObject>();
		
		NodeList list = document.getElementsByTagName("RatingServiceSelectionResponse");
		
		int responseLength = list.getLength();
		for(int i= 0; i < responseLength; i++){
			
			UPSResponseObject object = new UPSResponseObject();
			
			list = document.getElementsByTagName("ResponseStatusCode");
			if(list.item(i) != null && list.item(i).getFirstChild() != null)
				object.setStatusCode(list.item(i).getFirstChild().getNodeValue());
			
			list = document.getElementsByTagName("ResponseStatusDescription");
			if(list.item(i) != null && list.item(i).getFirstChild() != null)
				object.setStatusDescription(list.item(i).getFirstChild().getNodeValue());
			
			list = document.getElementsByTagName("ErrorCode");
			if(list.item(i) != null && list.item(i).getFirstChild() != null)
				object.setErrorCode(list.item(i).getFirstChild().getNodeValue());
			
			list = document.getElementsByTagName("ErrorDescription");
			if(list.item(i) != null && list.item(i).getFirstChild() != null)
				object.setErrorDescription(list.item(i).getFirstChild().getNodeValue());
						
			list = document.getElementsByTagName("TotalCharges");
			if(list.item(i) != null && list.item(i).getFirstChild() != null){
				
				list = document.getElementsByTagName("CurrencyCode");
				if(list.item(i) != null && list.item(i).getFirstChild() != null)
					object.setCurrencyCode(list.item(i).getFirstChild().getNodeValue());
				
				list = document.getElementsByTagName("MonetaryValue");
				if(list.item(i) != null && list.item(i).getFirstChild() != null)
					object.setTotalValue(list.item(i).getFirstChild().getNodeValue());
				
				
			}			
			
			objectList.add(object);
		}
		
	} catch (SAXException e) {
		Logger.error(UPSResponseParser.class,e.getMessage(),e);
	} catch (Exception e) {
		Logger.error(UPSResponseParser.class,e.getMessage(),e);
	}
	
	return objectList;
}

}
