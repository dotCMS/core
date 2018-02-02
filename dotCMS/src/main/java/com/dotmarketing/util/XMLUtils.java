package com.dotmarketing.util;


public class XMLUtils {

	/**
	 * This will take the three pre-defined entities in XML 1.0 (used
	 * specifically in XML elements) and convert their character representation
	 * to the appropriate entity reference, suitable for XML element content.
	 * 
	 * @param str
	 *            <code>String</code> input to escape.
	 * @return <code>String</code> with escaped content.
	 */
	public static String xmlEscape(String str) {

	        return org.apache.commons.lang.StringEscapeUtils.escapeXml(str);
	   
	}
}
