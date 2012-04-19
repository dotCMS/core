package com.dotmarketing.portlets.containers.ajax.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides some methods for ContainerAjax class that get the container's list
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Apr 18, 2012
 */
public class ContainerAjaxUtil {

	private static final String PATTERN_HTML_START = "^.*<html>.*$";
	private static final String PATTERN_HTML_END = "^.*</html>.*$";	
	private static final String PATTERN_HEAD_START = "^.*<head>.*$";
	private static final String PATTERN_HEAD_END = "^.*</head>.*$";
	private static final String PATTERN_BODY_START = "^.*<body>.*$";
	private static final String PATTERN_BODY_END = "^.*</body>.*$";
	
	private static List<String> PATTERNS = new ArrayList<String>();
	
	static {
		PATTERNS.add(PATTERN_HTML_START);
		PATTERNS.add(PATTERN_HTML_END);
		PATTERNS.add(PATTERN_HEAD_START);
		PATTERNS.add(PATTERN_HEAD_END);
		PATTERNS.add(PATTERN_BODY_START);
		PATTERNS.add(PATTERN_BODY_END);
	}
	
	public static boolean checkContainerCode(StringBuffer containerCode){
		return checkContainerCode(containerCode.toString());
	}
	
	public static boolean checkContainerCode(String containerCode){
		boolean match = false;
		for(String pattern : PATTERNS){
			Pattern patt = Pattern.compile(pattern);
			String _containerCode = containerCode.replaceAll("\n", "").replaceAll("\r", "");
			Matcher matcher = patt.matcher(_containerCode);
			boolean find = matcher.find();
			boolean matches = matcher.matches();
			
			if(matches || find){
				match = true;
			}
		}
		return match;
	}
}
