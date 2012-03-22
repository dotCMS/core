package com.dotmarketing.util;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.dotmarketing.beans.GoogleMiniSearch;
import com.dotmarketing.beans.GoogleMiniSearchResult;
/**
 * Utility class (Infrastructure Layer) used to make searches using GoogleMini engine
 * @author Edgar De Sousa
 * @version 1.0
 *
 */
public class GoogleMiniUtils {
		
	
	/**
	 * Searches under google mini and return the results in a special object
	 * 
	 * @param client A valid setup client in google mini
	 * @param collection The collection to search on
	 * @param subcollection Subcollection to search on
	 * @param query Search query as entered by the user
	 * @param metaquery Meta tags query, filter to the search by meta tags names and values I.E. key:value.key:value.key:value|key:value. Use . as AND. User | as OR
	 * @param start Start index use -1 if want all results
	 * @param num Number of results to show use -1 to show all
	 * @param autoFilter Let google mini auto filter results, google mini automatic filtering does Duplicate Snippet Filter and Duplicate Directory Filter
	 * @return
	 * @throws Exception
	 */
	public static GoogleMiniSearch searchGoogleMini(String client, String collection, String subcollection, String query, 
			String metaquery, int start, int num, boolean autoFilter)
			throws Exception {

		if (UtilMethods.isSet(client)) {
			StringBuffer searchURL = new StringBuffer(512);
			searchURL.ensureCapacity(128);
			String url = Config.getStringProperty("GOOGLE_MINI_SEARCH_URL");
			if (!url.endsWith("/"))
				url += "/";
			searchURL.append(url + "search?output=xml_no_dtd");
			searchURL.append("&numgm=3");
			if(!autoFilter)
				searchURL.append("&filter=0");
			searchURL.append("&client="+UtilMethods.encodeURL(client));
			searchURL.append("&getfields=*");
			if (-1 < start) {
				searchURL.append("&start=");
				searchURL.append(start);
				if (-1 < num) {
					searchURL.append("&num=");
					searchURL.append(num);
				}
			}
			if (UtilMethods.isSet(query)) {
				searchURL.append("&q=" + UtilMethods.encodeURL(query));
			}
			if (UtilMethods.isSet(metaquery)) {
				searchURL.append("&partialfields=" + metaquery);
			}
			
			if (UtilMethods.isSet(subcollection)) {
				searchURL.append("&restrict=");
				searchURL.append(UtilMethods.encodeURL(subcollection));
			}
			
			if (UtilMethods.isSet(collection)) {
				searchURL.append("&site=");
				searchURL.append(UtilMethods.encodeURL(collection));
			}

			return parseGoogleMiniResults(searchURL.toString());
		}
		return null;
	}
	
	/**
	 * Searches under google custom and return the results in a special object
	 * 
	 * @param client A valid setup client in google mini (Null can be used if you want to use the default value 'GOOGLE_CUSTOM_SEARCH_CSEID' configured in 'dotmarketing-config.properties')
	 * @param query Search query as entered by the user
	 * @param start Start index use -1 if want all results
	 * @param num Number of results to show use -1 to show all (Maximum value is 20. If you request more than 20 results, only 20 results will be returned)
	 * @param autoFilter Let google mini auto filter results, google mini automatic filtering does Duplicate Snippet Filter and Duplicate Directory Filter
	 * @return
	 * @throws Exception
	 */
	public static GoogleMiniSearch searchGoogleCustom(String client, String query, int start, int num, boolean autoFilter)
			throws Exception {

		if (UtilMethods.isSet(client)) {
			StringBuffer searchURL = new StringBuffer(512);
			searchURL.ensureCapacity(128);
			String url = Config.getStringProperty("GOOGLE_CUSTOM_SEARCH_URL");
			String cx = Config.getStringProperty("GOOGLE_CUSTOM_SEARCH_CSEID");
			if (!url.endsWith("/"))
				url += "/";
			searchURL.append(url + "cse?output=xml_no_dtd");
			if(UtilMethods.isSet(cx))
				searchURL.append("&cx=" + UtilMethods.encodeURL(cx));
			if(!autoFilter)
				searchURL.append("&filter=0");
			searchURL.append("&client=" + UtilMethods.encodeURL(client));
			if (-1 < start) {
				searchURL.append("&start=");
				searchURL.append(start);
				if (-1 < num) {
					searchURL.append("&num=");
					searchURL.append(num);
				}
			}
			if (UtilMethods.isSet(query)) {
				searchURL.append("&q=" + UtilMethods.encodeURL(query));
			}
			
			return parseGoogleMiniResults(searchURL.toString());
		}
		return null;
	}
	
	private static GoogleMiniSearch parseGoogleMiniResults(String searchURL) throws Exception {
		
		GoogleMiniSearch result = null;
		InputStream stream = null;

		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			docBuilderFactory.setValidating(false);
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			stream = new URL(searchURL.toString()).openStream();
			Document doc = docBuilder.parse(stream);
			if ((doc != null) && (doc.getChildNodes() != null)) {
				result = new GoogleMiniSearch();
				Element nodeGSP = (Element) doc.getChildNodes().item(0);
				NodeList GSPChildNodes = nodeGSP.getElementsByTagName("TM");
				if ((GSPChildNodes != null) && (0 < GSPChildNodes.getLength())) {
					Element nodeTM = (Element) GSPChildNodes.item(0);
					result.setSearchTime(nodeTM.getChildNodes().item(0)
							.getNodeValue());
				}
				GSPChildNodes = nodeGSP.getElementsByTagName("Q");
				if ((GSPChildNodes != null) && (0 < GSPChildNodes.getLength())) {
					Element nodeQ = (Element) GSPChildNodes.item(0);
					if (nodeQ.getChildNodes().item(0) != null)
						result.setQuery(nodeQ.getChildNodes().item(0)
								.getNodeValue());
				}
				GSPChildNodes = nodeGSP.getElementsByTagName("PARAM");
				if ((GSPChildNodes != null) && (0 < GSPChildNodes.getLength())) {
					HashMap<String, HashMap<String, String>> params = new HashMap<String, HashMap<String, String>>();
					HashMap<String, String> paramValues;
					for (int i=0; i < GSPChildNodes.getLength(); ++i) {
						paramValues = new HashMap<String, String>();
						Element nodeRES = (Element) GSPChildNodes.item(i);
						paramValues.put("value", nodeRES.getAttribute("value"));
						paramValues.put("original_value", nodeRES.getAttribute("original_value"));
						params.put(nodeRES.getAttribute("name"), paramValues);
					}
					result.setParams(params);
				}
				GSPChildNodes = nodeGSP.getElementsByTagName("Context");
				if ((GSPChildNodes != null) && (0 < GSPChildNodes.getLength())) {
					NodeList ContextChildNodes = nodeGSP.getElementsByTagName("title");
					if ((ContextChildNodes != null) && (0 < ContextChildNodes.getLength())) {
						Element nodeRES = (Element) ContextChildNodes.item(0);
						result.setContextTitle(nodeRES.getChildNodes().item(0)
								.getNodeValue());
					}
				}
				GSPChildNodes = nodeGSP.getElementsByTagName("RES");
				if ((GSPChildNodes != null) && (0 < GSPChildNodes.getLength())) {
					Element nodeRES = (Element) GSPChildNodes.item(0);
					result.setFromIndex(Integer.parseInt(nodeRES
							.getAttribute("SN")));
					result.setToIndex(Integer.parseInt(nodeRES
							.getAttribute("EN")));
				}
				GSPChildNodes = nodeGSP.getElementsByTagName("M");
				if ((GSPChildNodes != null) && (0 < GSPChildNodes.getLength())) {
					Element nodeM = (Element) GSPChildNodes.item(0);
					result.setEstimatedTotal(Integer.parseInt(nodeM
							.getChildNodes().item(0).getNodeValue()));
				}
				GSPChildNodes = nodeGSP.getElementsByTagName("NB");
				if ((GSPChildNodes != null) && (0 < GSPChildNodes.getLength())) {
					Element nodeNB = (Element) GSPChildNodes.item(0);
					NodeList NBChildNodes = nodeNB.getElementsByTagName("PU");
					if ((NBChildNodes != null)
							&& (0 < NBChildNodes.getLength())) {
						Element nodePU = (Element) NBChildNodes.item(0);
						result.setPreviousResultPageRelativeURL(nodePU
								.getChildNodes().item(0).getNodeValue());
					}
					NBChildNodes = nodeNB.getElementsByTagName("NU");
					if ((NBChildNodes != null)
							&& (0 < NBChildNodes.getLength())) {
						Element nodeNU = (Element) NBChildNodes.item(0);
						result.setNextResultPageRelativeURL(nodeNU
								.getChildNodes().item(0).getNodeValue());
					}
				}
				/*** KeyMatches ****/
				GSPChildNodes = nodeGSP.getElementsByTagName("GM");
				if (GSPChildNodes != null) {
					ArrayList<GoogleMiniSearchResult> keyMatchResults = new ArrayList<GoogleMiniSearchResult>();
					GoogleMiniSearchResult googleSearchResult;
					Element nodeRKeyMatch;
					Element nodeGD;
					Element nodeGL;
					NodeList RChildNodes;
					for (int i = 0; i < GSPChildNodes.getLength(); ++i) {
						googleSearchResult = new GoogleMiniSearchResult();
						nodeRKeyMatch = (Element) GSPChildNodes.item(i);
						RChildNodes = nodeRKeyMatch.getElementsByTagName("GD");
						if ((RChildNodes != null)
								&& (0 < RChildNodes.getLength())) {
							nodeGD = (Element) RChildNodes.item(0);
							googleSearchResult.setTitle(nodeGD
									.getChildNodes().item(0).getNodeValue());
						}
						RChildNodes = nodeRKeyMatch.getElementsByTagName("GL");
						if ((RChildNodes != null)
								&& (0 < RChildNodes.getLength())) {
							nodeGL = (Element) RChildNodes.item(0);
							googleSearchResult.setResultURL(nodeGL
									.getChildNodes().item(0).getNodeValue());
						}
						googleSearchResult.setSnippet("keyMatch");
						keyMatchResults.add(googleSearchResult);
					}
					result.setKeyMatchResults(keyMatchResults);
				}
				/**********/
				GSPChildNodes = nodeGSP.getElementsByTagName("R");
				if (GSPChildNodes != null) {
					GoogleMiniSearchResult googleSearchResult;
					HashMap<String, String> additionalSearchDetails;
					ArrayList<GoogleMiniSearchResult> results = new ArrayList<GoogleMiniSearchResult>(
							100);
					results.ensureCapacity(100);
					Element nodeR;
					NodeList RChildNodes;
					Element nodeU;
					Element nodeUE;
					Element nodeT;
					Element nodeRK;
					Element nodeFS;
					Element nodeMT;
					String fieldName;
					String fieldValue;
					List<String> fieldValues;
					HashMap<String, List<String>> fields;
					HashMap<String, String[]> metaTagsFields;
					Element nodeS;
					Element nodeHAS;
					NodeList HASChildNodes;
					Element nodeL;
					Element nodeHN;
					for (int i = 0; i < GSPChildNodes.getLength(); ++i) {
						googleSearchResult = new GoogleMiniSearchResult();
						nodeR = (Element) GSPChildNodes.item(i);
						googleSearchResult.setResultIndex(Integer
								.parseInt(nodeR.getAttribute("N")));
						if (UtilMethods.isSet(nodeR.getAttribute("L")))
							googleSearchResult.setIdentationLevel(Integer
									.parseInt(nodeR.getAttribute("L")));
						googleSearchResult.setMimeType(nodeR
								.getAttribute("MIME"));
						RChildNodes = nodeR.getElementsByTagName("U");
						if ((RChildNodes != null)
								&& (0 < RChildNodes.getLength())) {
							nodeU = (Element) RChildNodes.item(0);
							googleSearchResult.setResultURL(nodeU
									.getChildNodes().item(0).getNodeValue());
						}
						RChildNodes = nodeR.getElementsByTagName("UE");
						if ((RChildNodes != null)
								&& (0 < RChildNodes.getLength())) {
							nodeUE = (Element) RChildNodes.item(0);
							googleSearchResult.setResultURLEnconded(nodeUE
									.getChildNodes().item(0).getNodeValue());
						}
						RChildNodes = nodeR.getElementsByTagName("T");
						if ((RChildNodes != null)
								&& (0 < RChildNodes.getLength())) {
							nodeT = (Element) RChildNodes.item(0);
							googleSearchResult.setTitle(nodeT.getChildNodes()
									.item(0).getNodeValue());
						}
						RChildNodes = nodeR.getElementsByTagName("RK");
						if ((RChildNodes != null)
								&& (0 < RChildNodes.getLength())) {
							nodeRK = (Element) RChildNodes.item(0);
							googleSearchResult
									.setGeneralRatingRelevance(Integer
											.parseInt(nodeRK.getChildNodes()
													.item(0).getNodeValue()));
						}
						RChildNodes = nodeR.getElementsByTagName("FS");
						if ((RChildNodes != null)
								&& (0 < RChildNodes.getLength())) {
							additionalSearchDetails = new HashMap<String, String>(
									10);
							for (int j = 0; j < RChildNodes.getLength(); ++j) {
								nodeFS = (Element) RChildNodes.item(j);
								additionalSearchDetails.put(nodeFS
										.getAttribute("NAME"), nodeFS
										.getAttribute("VALUE"));
							}
							googleSearchResult
									.setAdditionalSearchDetails(additionalSearchDetails);
						}
						RChildNodes = nodeR.getElementsByTagName("MT");

						if ((RChildNodes != null)
								&& (0 < RChildNodes.getLength())) {
							fields = new HashMap<String, List<String>>(10);
							for (int j = 0; j < RChildNodes.getLength(); ++j) {
								nodeMT = (Element) RChildNodes.item(j);
								fieldName = nodeMT.getAttribute("N");
								fieldValue = nodeMT.getAttribute("V");
								// Verify if the field is a date
								if (fieldValue.startsWith("D:")) {
									fieldValue = fieldValue.substring(2);
									Date dateValue = UtilMethods
											.googleDateToDate(fieldValue);
									fieldValue = UtilMethods
											.dateToGoogleDate(dateValue);
								}
								fieldValues = fields.get(fieldName);
								if (fieldValues == null) {
									fieldValues = new ArrayList<String>(10);
									fields.put(fieldName, fieldValues);
								}
								fieldValues.add(fieldValue);
							}
							metaTagsFields = new HashMap<String, String[]>(
									fields.size());

							Iterator<String> names = fields.keySet().iterator();
							for (; names.hasNext();) {
								fieldName = names.next();
								metaTagsFields.put(fieldName, fields.get(
										fieldName).toArray(new String[0]));
							}
							googleSearchResult
									.setMetaTagsFields(metaTagsFields);
						}
						RChildNodes = nodeR.getElementsByTagName("S");
						if ((RChildNodes != null)
								&& (0 < RChildNodes.getLength())) {
							nodeS = (Element) RChildNodes.item(0);
							if(nodeS.getChildNodes().getLength() > 0)
								googleSearchResult.setSnippet(nodeS.getChildNodes()
									.item(0).getNodeValue());
						}
						RChildNodes = nodeR.getElementsByTagName("LANG");
						if ((RChildNodes != null)
								&& (0 < RChildNodes.getLength())) {
							nodeS = (Element) RChildNodes.item(0);
							if(nodeS.getChildNodes().getLength() > 0)
								googleSearchResult.setLanguage(nodeS.getChildNodes()
									.item(0).getNodeValue());
						}
						RChildNodes = nodeR.getElementsByTagName("Label");
						if ((RChildNodes != null)
								&& (0 < RChildNodes.getLength())) {
							nodeS = (Element) RChildNodes.item(0);
							if(nodeS.getChildNodes().getLength() > 0)
								googleSearchResult.setLabel(nodeS.getChildNodes()
									.item(0).getNodeValue());
						}
						RChildNodes = nodeR.getElementsByTagName("HAS");
						if ((RChildNodes != null)
								&& (0 < RChildNodes.getLength())) {
							nodeHAS = (Element) RChildNodes.item(0);
							HASChildNodes = nodeHAS.getElementsByTagName("L");
							if ((HASChildNodes != null)
									&& (0 < HASChildNodes.getLength())) {
								nodeL = (Element) HASChildNodes.item(0);
								googleSearchResult.setSpecialQueryTerm(nodeL
										.getAttribute("TAG"));
							}
							HASChildNodes = nodeHAS.getElementsByTagName("C");
							if ((HASChildNodes != null)
									&& (0 < HASChildNodes.getLength())) {
								nodeL = (Element) HASChildNodes.item(0);
								googleSearchResult.setDocumentCacheSize(nodeL
										.getAttribute("SZ"));
								googleSearchResult.setDocumentCacheId(nodeL
										.getAttribute("CID"));
							}
						}
						RChildNodes = nodeR.getElementsByTagName("HN");
						if ((RChildNodes != null)
								&& (0 < RChildNodes.getLength())) {
							nodeHN = (Element) RChildNodes.item(0);
							googleSearchResult.setMoreResultsDirectory(nodeHN
									.getAttribute("U"));
						}
						results.add(googleSearchResult);
					}
					result.setSearchResults(results);
				}
			}
		} catch (Exception e) {
			if (stream != null)
				stream.close();
			throw e;
		}
		return result;
	}
}
