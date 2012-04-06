package com.eng.achecker.utility;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.eng.achecker.parsing.EmptyIterable;


/************************************************************************/
/* ACheckerImplImpl                                                             */
/************************************************************************/
/* Copyright (c) 2008 - 2011                                            */
/* Inclusive Design Institute                                           */
/*                                                                      */
/* This program is free software. You can redistribute it and/or        */
/* modify it under the terms of the GNU General Public License          */
/* as published by the Free Software Foundation.                        */
/************************************************************************/
// $Id$

/**
* Utility functions 
* @access	public
* @author	Cindy Qi Li
*/

public class Utility {
	
	public static String getClobContent(Clob clob) {
		if (clob == null)
			return  null;
		StringBuffer str = new StringBuffer();
		String strng;
		try {
			BufferedReader bufferRead = new BufferedReader(clob.getCharacterStream());
			while ((strng = bufferRead .readLine())!=null)
				str.append(strng);
			return str.toString();	
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static int intval(String string) {
		int value = 0;
		int last = 0;
		for ( int i = 1; i <= string.length(); i ++ ) {
			try {
				last = Integer.parseInt(string.substring(0, i));
				value = last;
			}
			catch (Exception e) {
				return value;
			}
		}
		return value;
	}

	public static int row(Node node) {
		try {
			return Integer.parseInt((String) node.getUserData(ParserConst.LINE_NUMBER_KEY_NAME));
		}
		catch (Throwable t) {
		}
		return -1;
	}
	
	public static int col(Node node) {
		try {
			return Integer.parseInt((String) node.getUserData(ParserConst.COL_NUMBER_KEY_NAME));
		}
		catch (Throwable t) {
		}
		return -1;
	}
	
	public static String attr(Node node, String attr) {
		if (node == null)
			return "";
		if (element(node) == null)
			return "";
		return string(element(node).getAttribute(attr));
	}

	public static Element element(Node node) {
		if ( node instanceof Element )
			return (Element) node;
		return null;
	}
	
	public static String string(String value) {
		if ( value == null )
			return "";
		return value;
	}
	
	public static int integer(String value) {
		if ( value == null )
			return 0;
		try {
			return Integer.parseInt(value);
		}
		catch (Throwable t) {
		}
		return 0;
	}
	
	public static String truncate(String value, int length) {
		if ( value.length() <= length )
			return value;
		return value.substring(0, length) + "...";
	}
	
	public static Iterable<Node> nodelist(final NamedNodeMap list) {
		if ( list == null )
			return new EmptyIterable<Node>();
		if ( list.getLength() == 0 )
			return new EmptyIterable<Node>();
		return new Iterable<Node>() {
			public Iterator<Node> iterator() {
				return new Iterator<Node>() {
					private int index = 0;
					private NamedNodeMap list_ = list;
					public boolean hasNext() {
						return ( index < list.getLength() );
					}
					
					public Node next() {
						Node node = list_.item(index);
						index ++;
						return node;
					}

					public void remove() {
						throw new IllegalStateException("Not supported method");
					}
				};
			}
		};
	}

	public static Iterable<Node> nodelist(final NodeList list) {
		if ( list == null )
			return new EmptyIterable<Node>();
		if ( list.getLength() == 0 )
			return new EmptyIterable<Node>();
		return new Iterable<Node>() {
			public Iterator<Node> iterator() {
				return new Iterator<Node>() {
					private int index = 0;
					private NodeList list_ = list;
					public boolean hasNext() {
						return ( index < list.getLength() );
					}
					
					public Node next() {
						Node node = list_.item(index);
						index ++;
						return node;
					}

					public void remove() {
						throw new IllegalStateException("Not supported method");
					}
				};
			}
		};
	}
	
	
	public static Iterable<Node> children(Node node) {
		return nodelist(node.getChildNodes());
	}
	
	public static String truncateTo(String content, int length) {
		if ( content.length() > length ) {
			return content.substring(0, length) + "...";
		}
		return content;
	}

	public static String getPlainNodeContent(NodeList nodes) {
		StringBuffer buffer = new StringBuffer();
		for ( int i = 0; i < nodes.getLength(); i ++ )
			buffer.append(getPlainNodeContent(nodes.item(i)));
		return buffer.toString();
	}

	public static String getPlainNodeContent(Node node) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			StreamResult result = new StreamResult(new StringWriter());
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			DOMSource source = new DOMSource(node);
			transformer.transform(source, result);
			return result.getWriter().toString();
		} catch (TransformerException e1) {
			e1.printStackTrace();
		}
		return null;
	}

	public static String getNodeContent(Node node) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			//initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(node);
			transformer.transform(source, result);
			String xmlString = result.getWriter().toString();
			return xmlString.replace("\n", " ");
		} catch (TransformerException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	
	public static String joinList(String separator, List<?> list) {
		if ( list == null )
			return "";
		if ( list.size() == 0)
			return "";
		if ( list.size() == 1)
			return "" + list.get(0);
		StringBuffer ret = new StringBuffer();
		int count = list.size();
		for ( Object value : list ) {
			ret.append("" + value);
			if ( count > 0 )
				ret.append(",");
		}
		String s = ret.toString();
		s = s.substring(0, s.length() - 1);
		return s;
	}

//	public static String mysql_real_escape_string(Connection link, String str) {
//
//		if (str == null) {
//            return null;
//        }
//                                    
//        if (str.replaceAll("[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/? ]","").length() < 1) {
//            return str;
//        }
//            
//        String clean_string = str;
//        clean_string = clean_string.replaceAll("\\\\", "\\\\\\\\");
//        clean_string = clean_string.replaceAll("\\n","\\\\n");
//        clean_string = clean_string.replaceAll("\\r", "\\\\r");
//        clean_string = clean_string.replaceAll("\\t", "\\\\t");
//        clean_string = clean_string.replaceAll("\\00", "\\\\0");
//        clean_string = clean_string.replaceAll("'", "\\\\'");
//        clean_string = clean_string.replaceAll("\\\"", "\\\\\"");
//                                                            
//        if (clean_string.replaceAll("[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/?\\\\\"' ]"
//          ,"").length() < 1) 
//        {
//            return clean_string;
//        }
//                            
//        java.sql.Statement stmt = link.createStatement();
//        String qry = "SELECT QUOTE('"+clean_string+"')";
//            
//        stmt.executeQuery(qry);
//        java.sql.ResultSet resultSet = stmt.getResultSet();
//        resultSet.first();
//        String r = resultSet.getString(1);
//        return r.substring(1,r.length() - 1);       
//	}
//	
//	/**
//     * Escape data to protected against SQL Injection
//     *
//     * @param link
//     * @param str
//     * @return
//     * @throws Exception 
//     */
//    
//    public static String quote(java.sql.Connection link, String str) throws Exception {
//        if (str == null) {
//            return "NULL";
//        }
//        return "'"+mysql_real_escape_string(link,str)+"'";
//    }
//    
//    /**
//     * Escape identifier to protected against SQL Injection
//     *
//     * @param link
//     * @param str
//     * @return
//     * @throws Exception 
//     */
//    
//    public static String nameQuote(java.sql.Connection link, String str) throws Exception {
//        if (str == null) {
//            return "NULL";
//        }
//        return "`"+mysql_real_escape_string(link,str)+"`";
//    }
//    	
//	public String stripslashes(String value) {
//		
//	}
//
//	/**
//	* return a unique session id based on timestamp
//	* @access  public
//	* @param   none
//	* @return  language code
//	* @author  Cindy Qi Li
//	*/
//	public String getSessionID() {
//		return sha1(mt_rand() . microtime(TRUE));
//	}

	 /**
	 * Return the valid format of given $uri. Otherwise, return FALSE
	 * Return $uri itself if it has valid content, 
	 * otherwise, return the first listed uri that has valid content: 
	 * "http://".$uri
	 * "https://".$uri
	 * "http://www.".$uri
	 * "https://www.".$uri
	 * If none of above has valid content, return FALSE
	 * @access  public
	 * @param   string $uri  The uri address
	 * @return  true: if valid; false: if invalid
	 * @author  Cindy Qi Li
	 */
	public static boolean getValidURI(String uri) {
		try {
			new URI(uri);
			return true;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return false;
	}

//	/**
//	* convert text new lines to html tag <br/>
//	* @access  public
//	* @param   string
//	* @return  converted string
//	* @author  Cindy Qi Li
//	*/
//	public String convertHTMLNewLine(String str) {
//		return str.replaceAll("\\n|\\r|\\n\\r|\\r\\n", "<br />");
//	}
//
//	/**
//	* Return array of seals to display
//	* Some guidelines are in the same group. This is defined in guidelines.subset. 
//	* The format of guidelines.subset is [group_name]-[priority].
//	* When the guidelines in the same group are validated, only the seal for the guideline
//	* with the highest [priority] number is displayed.
//	* @access  public
//	* @param   $guidelines : array of guideline table rows
//	* @return  converted string
//	* @author  Cindy Qi Li
//	*/
//	public static function getSeals($guidelines)
//	{
//		foreach ($guidelines as $guideline)
//		{
//			if ($guideline['subset'] == '0')
//			{
//				$seals[] = array('title' => $guideline['title'],
//				                 'guideline' => $guideline['abbr'], 
//				                 'seal_icon_name' => $guideline['seal_icon_name']);
//			}
//			else
//			{
//				list($group, $priority) = explode('-', $guideline['subset']);
//				
//				if (!isset($highest_priority[$group]['priority']) || $highest_priority[$group]['priority'] < $priority)
//				{
//					$highest_priority[$group]['priority'] = $priority;
//					$highest_priority[$group]['guideline'] = $guideline;
//				}
//			}// end of outer if
//		} // end of foreach
//		
//		if (is_array($highest_priority))
//		{
//			foreach ($highest_priority as $group => $guideline_to_display)
//				$seals[] = array('title' => $guideline_to_display['guideline']['title'], 
//						         'guideline' => $guideline_to_display['guideline']['abbr'], 
//				                 'seal_icon_name' => $guideline_to_display['guideline']['seal_icon_name']);
//		}
//		
//		return $seals;
//	}
	
	/**
	* Sort $inArray in the order of the number presented in the field with name $fieldName
	* @access  public
	* @param   $inArray : input array
	*          $fieldName : the name of the field to sort by
	* @return  sorted array
	* @author  Cindy Qi Li
	*/
	public static List<Map<String, Object>> sortArrayByNumInField(List<Map<String, Object>> inArray, String fieldName) {
		
		List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>(inArray.size());
		
		TreeMap<String, Map<String, Object>> outArray = new TreeMap<String, Map<String, Object>>();
		
		Pattern p = Pattern.compile("[^\\d]*(\\d*(\\.)*(\\d)*(\\.)*(\\d)*)[^\\d]*");
		
		for ( int num = 0; num < inArray.size(); num ++ ) {
			
			Map<String, Object> element = inArray.get(num);
			
			Matcher matches = p.matcher("" + element.get(fieldName));

			if ( matches.group(1) != null ) {
				outArray.put(matches.group(1), element);
			}
			else {
				outArray.put("" + num, element);
			}
			
		}

		for ( String key : outArray.keySet() ) {
			ret.add(outArray.get(key));
		}
		
		return ret;
		
	}

//	/**
//	* This function deletes $dir recrusively without deleting $dir itself.
//	* @access  public
//	* @param   string $charsets_array	The name of the directory where all files and folders under needs to be deleted
//	* @author  Cindy Qi Li
//	*/
//	public static function clearDir($dir) {
//		if(!$opendir = @opendir($dir)) {
//			return false;
//		}
//		
//		while(($readdir=readdir($opendir)) !== false) {
//			if (($readdir !== '..') && ($readdir !== '.')) {
//				$readdir = trim($readdir);
//	
//				clearstatcache(); /* especially needed for Windows machines: */
//	
//				if (is_file($dir.'/'.$readdir)) {
//					if(!@unlink($dir.'/'.$readdir)) {
//						return false;
//					}
//				} else if (is_dir($dir.'/'.$readdir)) {
//					/* calls lib function to clear subdirectories recrusively */
//					if(!Utility::clrDir($dir.'/'.$readdir)) {
//						return false;
//					}
//				}
//			}
//		} /* end while */
//	
//		@closedir($opendir);
//		
//		return true;
//	}
//
//	/**
//	* Enables deletion of directory if not empty
//	* @access  public
//	* @param   string $dir		the directory to delete
//	* @return  boolean			whether the deletion was successful
//	* @author  Joel Kronenberg
//	*/
//	public static function clrDir($dir) {
//		if(!$opendir = @opendir($dir)) {
//			return false;
//		}
//		
//		while(($readdir=readdir($opendir)) !== false) {
//			if (($readdir !== '..') && ($readdir !== '.')) {
//				$readdir = trim($readdir);
//	
//				clearstatcache(); /* especially needed for Windows machines: */
//	
//				if (is_file($dir.'/'.$readdir)) {
//					if(!@unlink($dir.'/'.$readdir)) {
//						return false;
//					}
//				} else if (is_dir($dir.'/'.$readdir)) {
//					/* calls itself to clear subdirectories */
//					if(!Utility::clrDir($dir.'/'.$readdir)) {
//						return false;
//					}
//				}
//			}
//		} /* end while */
//	
//		@closedir($opendir);
//		
//		if(!@rmdir($dir)) {
//			return false;
//		}
//		return true;
//	}
//
//	/**
//	 * This function accepts an array that is supposed to only have integer values.
//	 * The function returns a sanitized array by ensuring all the array values are integers.
//	 * To pervent the SQL injection. 
//	 * @access  public
//	 * @param   $int_array : an array
//	 * @return  $sanitized_int_array : an array that all the values are sanitized to integer
//	 * @author  Cindy Qi Li
//	 */
//	public static function sanitizeIntArray($int_array) {
//		if (!is_array($int_array)) return false;
//		
//		$sanitized_array = array();
//		foreach ($int_array as $i => $value) {
//			$sanitized_array[$i] = intval($value);
//		}
//		return $sanitized_array;
//	}
//	
//	/**
//	 * Return http fail status & message. Used to return error message on ajax call. 
//	 * @access  public
//	 * @param   $errString: error message
//	 * @author  Cindy Qi Li
//	 */
//	public static function returnError($errString)
//	{
//	    header("HTTP/1.0 400 Bad Request");
//	    header("Status: 400");
//	    echo $errString;
//	}
//	
//	/**
//	 * Return http success status & message. Used to return success message on ajax call. 
//	 * @access  public
//	 * @param   $errString: error message
//	 * @author  Cindy Qi Li
//	 */
//	public static function returnSuccess($successString)
//	{
//	    header("HTTP/1.0 200 OK");
//	    header("Status: 200");
//	    echo $successString;
//	}
}

