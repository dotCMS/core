package com.dotmarketing.viewtools;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class RSSWebAPI {

	private static String RSS_VTL_PATH;
	private static String SHORT_RSS_VTL_PATH;
	private static int TTL;

	static {
		String velocityRootPath =ConfigUtils.getDynamicVelocityPath() +java.io.File.separator;
		RSS_VTL_PATH = velocityRootPath +  "rss" + java.io.File.separator;
		SHORT_RSS_VTL_PATH = velocityRootPath;

		TTL = Config.getIntProperty("RSS_TTL", 60);

		try {
			java.io.File file = new java.io.File(RSS_VTL_PATH);
			if (!file.exists()) {
				file.mkdir();
			}
			file = new java.io.File(SHORT_RSS_VTL_PATH);
			if (!file.exists()) {
				file.mkdir();
			}
		} catch (Exception ex) {
			String message = ex.toString();
			Logger.error(RSSWebAPI.class, message);
		}
	}
	
	/**
	 * Convert a rss feed to a html list
	 * @param	uri String with rss feed uri
	 * @param	ingesterName String
	 * @param	userAgent String with a specific user agent. Specify when the rss feed site is filtering the user agent request header. Example: "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)"
	 * @return	String with the html code
	 */
	public static String RSSParse(String uri, String ingesterName, String userAgent) {
		StringBuffer sb = new StringBuffer();
		String returnValue = "";
		try {
			XMLIngester ingester = locator(ingesterName);
			ArrayList<HashMap<String, String>> RSSEntries = ingester.ingest(uri, userAgent);

			for (HashMap<String, String> entry : RSSEntries) {
				try {
					String titleValue = entry.get("title");
					String linkValue = entry.get("link");
					String descriptionValue = entry.get("description");

					// Create the link value
					sb.append("<ul><li><a href=\"" + linkValue + "\">"
							+ titleValue + "</a><br>" + descriptionValue
							+ "</li></ul>\n\r");
				} catch (Exception ex) {
					Logger.error(RSSWebAPI.class, ex.toString());
				}
			}
			returnValue = sb.toString();
		} catch (Exception ex) {
			Logger.error(RSSWebAPI.class, ex.toString());
			returnValue = "";
		} finally {
			return returnValue;
		}
	}

	/*
	 * public static ArrayList<HashMap<String, String>> RSSIngester(String
	 * uri) { ArrayList<HashMap<String, String>> returnValue = new ArrayList<HashMap<String,String>>();
	 * try { DocumentBuilderFactory builderFactory =
	 * org.apache.xerces.jaxp.DocumentBuilderFactoryImpl.newInstance();
	 * DocumentBuilder builder = builderFactory.newDocumentBuilder(); Document
	 * doc = builder.parse(uri); NodeList items =
	 * doc.getElementsByTagName("item"); for(int i = 0; i <
	 * items.getLength();i++) { try { Element item = (Element) items.item(i); //
	 * Get the title NodeList titles = item.getElementsByTagName("title");
	 * String titleValue = ""; if(titles.getLength() > 0) { Element title =
	 * (Element) titles.item(0); NodeList children = title.getChildNodes();
	 * titleValue = children.item(0).getNodeValue(); } // Get the link NodeList
	 * links = item.getElementsByTagName("link"); String linkValue = "";
	 * if(links.getLength() > 0) { Element link = (Element) links.item(0);
	 * NodeList children = link.getChildNodes(); linkValue =
	 * children.item(0).getNodeValue(); } // Get the description NodeList
	 * descriptions = item.getElementsByTagName("description"); String
	 * descriptionValue = ""; if(descriptions.getLength() > 0) { Element
	 * description = (Element) descriptions.item(0); NodeList children =
	 * description.getChildNodes(); descriptionValue =
	 * children.item(0).getNodeValue(); }
	 * 
	 * //Get the pub date NodeList pubDates =
	 * item.getElementsByTagName("pubDate"); String pubDateValue = "";
	 * if(descriptions.getLength() > 0) { Element description = (Element)
	 * pubDates.item(0); NodeList children = description.getChildNodes();
	 * pubDateValue = children.item(0).getNodeValue(); }
	 * 
	 * HashMap<String, String> entry = new HashMap<String, String>();
	 * entry.put("title", titleValue); entry.put("link", linkValue);
	 * entry.put("description", descriptionValue);
	 * entry.put("pubDate",pubDateValue); returnValue.add(entry); }
	 * catch(Exception ex) { Logger.debug(RSSWebAPI.class,ex.toString()); } } }
	 * catch(Exception ex) { Logger.debug(RSSWebAPI.class,ex.toString()); }
	 * finally { return returnValue; } }
	 */

	private static boolean filterRSS(String title, ArrayList<String> filters) {
		boolean returnValue = false;
		try {
			title = title.toLowerCase();
			for (String filter : filters) {
				filter = filter.toLowerCase();
				if (title.contains(filter)) {
					returnValue = true;
					break;
				}
			}
		} catch (Exception ex) {
			Logger.error(RSSWebAPI.class, ex.toString());
		} finally {
			return returnValue;
		}
	}
	
	/**
	 * Get the full path of the cached rss feed file in dotcms from a specific uri.
	 * @param	uri String with rss feed uri
	 * @return	String with the full path of the rss feed file
	 */
	public static String getFile(String uri) {
		String ingesterName = "RSSIngester";
		return getFile(uri, ingesterName, null);
	}
	
	/**
	 * Get the full path of the cached rss feed file in dotcms from a specific uri.
	 * @param	uri String with rss feed uri
	 * @param	userAgent String with a specific user agent. Specify when the rss feed site is filtering the user agent request header. Example: "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)"
	 * @return	String with the full path of the rss feed file
	 */
	public static String getFile(String uri, String userAgent) {
		String ingesterName = "RSSIngester";
		return getFile(uri, ingesterName, userAgent);
	}
	
	/**
	 * Get the full path of the cached rss feed file in dotcms from a specific uri.
	 * @param	uri String with rss feed uri
	 * @param	ingesterName String
	 * @param	userAgent String with a specific user agent. Specify when the rss feed site is filtering the user agent request header. Example: "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)"
	 * @return	String with the full path of the rss feed file
	 */
	public static String getFile(String uri, String ingesterName, String userAgent) {
		int id = uri.hashCode();
		if (id < 0) {
			id *= -1;
		}
		String rssString = "rss_" + id + ".rss";
		String fullPath = RSS_VTL_PATH + rssString;
		try {
			java.io.File file;
			file = new java.io.File(fullPath);
			boolean requireWriteFile = false;
			if (!file.exists()) {
				file.createNewFile();
				requireWriteFile = true;
			} else {
				/*
				 * HttpClient client; client = new HttpClient(new
				 * MultiThreadedHttpConnectionManager()); HeadMethod headers =
				 * new HeadMethod(uri); headers.setFollowRedirects(true); int
				 * iHeadResultCode = client.executeMethod(headers); Header[]
				 * headersResponse = headers.getRequestHeaders();
				 */

				GregorianCalendar gc = new GregorianCalendar();
				gc.add(Calendar.MINUTE, -TTL);
				Date now = gc.getTime();
				long lastModifiedThreshold = now.getTime();

				long lastModifiedFile = file.lastModified();
				if (lastModifiedThreshold > lastModifiedFile) {
					requireWriteFile = true;
				}
			}
			if (requireWriteFile) {
				writeFile(file, uri, ingesterName, userAgent,fullPath);				
			}
		} catch (Exception ex) {
			Logger.error(RSSWebAPI.class, ex.toString());
		} finally {
			return fullPath;
		}
	}
	
	/**
	 * Write in the cached rss feed file in dotcms from a specific uri.
	 * @param	file Dotcms rss feed cached file
	 * @param	uri String with rss feed uri
	 * @param	ingesterName String
	 * @param	userAgent String with a specific user agent. Specify when the rss feed site is filtering the user agent request header. Example: "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)"
	 * @return	String with the full path of the rss feed file
	 */
	private static void writeFile(File file, String uri, String ingesterName, String userAgent,String fullPath)
			throws Exception {
		try {
			StringBuffer sb = new StringBuffer();

			sb.append("##" + uri + "\n");
			sb.append("#set($list = ${contents.getEmptyList()})\n\n");

			XMLIngester ingester = locator(ingesterName);
			ArrayList<HashMap<String, String>> RSSEntries = ingester.ingest(uri, userAgent);
			for (HashMap<String, String> entry : RSSEntries) {
				try {
					sb.append("#set($content = ${contents.getEmptyMap()})\n");

					Set<String> keys = entry.keySet();
					for (String key : keys) {
						String entryValue = UtilMethods.espaceForVelocity(entry
								.get(key));
						sb.append("$!content.put(\"" + key + "\", \""
								+ entryValue + "\")\n");
					}

					sb.append("#set($aux = $!list.add($content))\n\n");
				} catch (Exception ex) {
					Logger.error(RSSWebAPI.class, ex.toString());
				}
			}

			// Write to the file
			FileWriter fw = new FileWriter(file);
			fw.write(sb.toString());
			fw.close();
			CacheLocator.getVeloctyResourceCache().remove(fullPath);
		} catch (Exception ex) {
			Logger.error(RSSWebAPI.class, ex.toString());
			throw ex;
		}		
	}

	/**
	 * filterAndTop
	 * @param	list
	 * @param	filters
	 * @param	top
	 * @return	List<HashMap<String, String>>
	 */
	public List<HashMap<String, String>> filterAndTop(
			List<HashMap<String, String>> list, ArrayList<String> filters,
			int top) {
		int actualTop = 0;
		ArrayList<HashMap<String, String>> returnValue = new ArrayList<HashMap<String, String>>();
		for (HashMap<String, String> content : list) {
			String titleValue = content.get("title");

			if (filterRSS(titleValue, filters) || (filters.size() > 0 && filters.get(0).equals(""))) {
				returnValue.add(content);
				actualTop++;
			}
			if (top != -1 && actualTop >= top) {
				break;
			}
		}
		return returnValue;
	}

	/**
	 * XMLIngester locator
	 * @param	className
	 * @return	XMLIngester
	 */
	private static XMLIngester locator(String className) {
		XMLIngester ingester = null;
		if (className.equals("RSSIngester")) {
			ingester = new RSSWebAPI().new RSSIngester();		
		}else{
			ingester = new RSSWebAPI().new RSSIngester();
		}
		return ingester;
	}

	/**
	 * XMLIngester Interface
	 */
	public interface XMLIngester {
		
		/**
		 * ingest
		 * @param	uri String with rss feed uri
		 * @param	userAgent String with a specific user agent. Specify when the rss feed site is filtering the user agent request header. Example: "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)"
		 * @return	ArrayList<HashMap<String, String>>
		 */
		public ArrayList<HashMap<String, String>> ingest(String uri, String userAgent);
	}

	/**
	 * RSSIngester implements RSSWebAPI.XMLIngester
	 */
	public class RSSIngester implements RSSWebAPI.XMLIngester {
		
		private String extractData (NodeList nodeList) {
			String ret="";
			if (nodeList.getLength() > 0) {
				Element title = (Element) nodeList.item(0);
				NodeList children = title.getChildNodes();
				Node item=children.item(0);
				if (item!=null) {
					ret = item.getNodeValue();
				}
			}
			return ret;
		}
		
		/**
		 * ingest
		 * @param	uri String with rss feed uri
		 * @param	userAgent String with a specific user agent. Specify when the rss feed site is filtering the user agent request header. Example: "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)"
		 * @return	ArrayList<HashMap<String, String>>
		 */
		public ArrayList<HashMap<String, String>> ingest(String uri, String userAgent) {
			ArrayList<HashMap<String, String>> returnValue = new ArrayList<HashMap<String, String>>();
			try {
				DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
				builderFactory.setValidating(false);
				DocumentBuilder builder = builderFactory.newDocumentBuilder();
				Document doc;
				if (UtilMethods.isSet(userAgent)) {
					URL urlObject = new URL(uri);
					URLConnection con = urlObject.openConnection();
					con.setReadTimeout(15000);
					con.setRequestProperty("User-Agent", userAgent);
					InputStream st = con.getInputStream();
					doc  = builder.parse(st);
				} else {
					doc = builder.parse(uri);
				}
				
				NodeList items = doc.getElementsByTagName("item");
				for (int i = 0; i < items.getLength(); i++) {
					try {
						Element item = (Element) items.item(i);
						// Get the title
						NodeList titles = item.getElementsByTagName("title");
						String titleValue =extractData(titles);
						
						// Get the link
						NodeList links = item.getElementsByTagName("link");
						String linkValue = extractData(links);
						
						// Get the description
						NodeList descriptions = item
								.getElementsByTagName("description");
						String descriptionValue = extractData(descriptions);
						
						// Get the pub date
						NodeList pubDates = item
								.getElementsByTagName("pubDate");
						String pubDateValue = extractData(pubDates);
						
						HashMap<String, String> entry = new HashMap<String, String>();
						entry.put("title", titleValue);
						entry.put("link", linkValue);
						entry.put("description", descriptionValue);
						entry.put("pubDate", pubDateValue);
						returnValue.add(entry);
					} catch (Exception ex) {
						Logger.error(RSSWebAPI.class, ex.toString());
					}
				}
			} catch (Exception ex) {
				Logger.error(RSSWebAPI.class, ex.toString());
			} finally {
				return returnValue;
			}
		}
	}
}
