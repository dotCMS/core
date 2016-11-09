package com.dotmarketing.util.ups;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.sun.net.ssl.HttpsURLConnection;

/**
 * 
 * @author Oswaldo Galango
 *
 */

/**
 * The UPSConnections will transmit an HTTP/HTTPS post with the StringBuffer provided as 
 * the data of the post message.  The UPSConnections must be constructed with a URL or
 * IP address and a protocol to use for transmitting the message.
 */

public class UPSConnections {
	
	private String protocol = Config.getStringProperty("UPS_PROTOCOL").trim();
	private String hostname = Config.getStringProperty("UPS_HOSTNAME").trim();
	private String URLPrefix = Config.getStringProperty("UPS_PREFIX").trim();
	
	
	public UPSConnections() { 		
		
	}	
	
	/**
	 * 
	 * @param service This field indicate the url page tools to acces in this case "Rate" is the value to use
	 * @param xmlRequest is the XML Request
	 * @return the XML Response String of the UPS SERVER
	 * @throws Exception
	 */
	public String contactService(String service, StringBuffer xmlRequest) throws Exception
	{
		//Logger.info("UPS CONNECTIONS ***** Started " + service + " " + new Date().toString() + " *****");
		Logger.debug(UPSConnections.class,"UPS CONNECTIONS ***** Started " + service + " " + new Date().toString() + " *****");
		HttpURLConnection connection;
		URL url;
		String response = "";
		
		try
		{
			//Logger.info("connect to " + protocol + "://" + hostname + "/" + URLPrefix + "/" + service);
			Logger.debug(UPSConnections.class,"connect to " + protocol + "://" + hostname + "/" + URLPrefix + "/" + service);
			// Create new URL and connect
			if (protocol.equalsIgnoreCase("https"))
			{
				//use Sun's JSSE to deal with SSL
				java.security.Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
				System.getProperties().put("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
				url = new URL(protocol + "://" + hostname + "/" + URLPrefix + "/" + service);
				connection = (HttpsURLConnection) url.openConnection();
				
			}
			else	
			{	
				url = new URL(protocol + "://" + hostname + "/" + URLPrefix + "/" + service); 
				connection = (HttpURLConnection) url.openConnection();
			}
			connection.setReadTimeout(15000);
			//Logger.info("Establishing connection with " + url.toString());
			Logger.debug(UPSConnections.class,"Establishing connection with " + url.toString());
			// Setup HTTP POST parameters
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setUseCaches(false);
			
			
			// POST data
			OutputStream out = connection.getOutputStream();
			
			StringBuffer request = new StringBuffer();
			request.append(accessXMLRequest());
			request.append(xmlRequest);
			
			out.write((request.toString()).getBytes());
			//Logger.info("Transmission sent to " + url.toString() + ":\n" + xmlRequest);
			Logger.debug(UPSConnections.class,"Transmission sent to " + url.toString() + ":\n" + xmlRequest);
			out.close();
			
			// get data from URL connection and return the XML document as a StringBuffer
			
			try
			{
				response = readURLConnection(connection);
			}catch (Exception e)
			{
				//Logger.info("Error in reading URL Connection" + e.getMessage());
				Logger.debug(UPSConnections.class,"Error in reading URL Connection" + e.getMessage());
				throw e;
			}
			//Logger.info("Response = " + response);
			Logger.debug(UPSConnections.class,"Response = " + response);
			
		} catch (Exception e1)
		{
			Logger.info(UPSConnections.class, "Error sending data to server" + e1.toString());
			Logger.debug(UPSConnections.class,"Error sending data to server" + e1.toString());
		} finally
		{
			Logger.info(UPSConnections.class, "****** Transmission Finished " + service + " " + new Date().toString() + " *********");
			Logger.debug(UPSConnections.class,"****** Transmission Finished " + service + " " + new Date().toString() + " *********");
		}
		
		return response;
	}
	
	/**
	 * This method read all of the data from a URL conection to a String
	 */
	
	private static String readURLConnection(URLConnection uc) throws Exception
	{
		StringBuffer buffer = new StringBuffer();
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			int letter = 0;
			while ((letter = reader.read()) != -1)
				buffer.append((char) letter);
		} catch (Exception e)
		{
			//Logger.info("Cannot read from URL" + e.toString());
			Logger.debug(UPSConnections.class,"Cannot read from URL" + e.toString());
			throw e;
		} finally
		{
			try
			{
				reader.close();
			} catch (IOException io)
			{
				//Logger.info("Error closing URLReader!");
				Logger.debug(UPSConnections.class,"Error closing URLReader!");
				throw io;
			}
		}
		return buffer.toString();
	}
	
	/**
	 * This xml is obligatory to use. Appended with the xml request to get a result
	 * @return
	 */
	public static StringBuffer accessXMLRequest(){
		
		StringBuffer xml = new StringBuffer();
		/*Access request*/
		xml.append("<?xml version=\"1.0\"?>");
		xml.append("<AccessRequest xml:lang=\"en-US\">");
		xml.append("<AccessLicenseNumber>"+Config.getStringProperty("UPS_ACCESSLICENSENUMBER")+"</AccessLicenseNumber>");
		xml.append("<UserId>"+Config.getStringProperty("UPS_USERID")+"</UserId>");
		xml.append("<Password>"+Config.getStringProperty("UPS_PASSWORD")+"</Password>");
		xml.append("</AccessRequest>");
		
		return xml;
	}
	
}
