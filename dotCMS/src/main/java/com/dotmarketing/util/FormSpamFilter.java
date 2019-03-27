package com.dotmarketing.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

public class FormSpamFilter {

	
	/**
	 * This method receive a request and check all the parameters to know if anyone have three or more href
	 * to external pages
	 *
	 * @param request the request to be checked
	 * @return true if there is spam in the request
	 */
	
	@SuppressWarnings("unchecked")
	public static boolean isSpamRequest(HttpServletRequest request) 
	{
		Enumeration<String> params = request.getParameterNames();		
		return isSpamRequest(request,params);
	}
	
	/**
	 * This method receive a request and check a list of parameters to know if anyone have three or more href
	 * to external pages
	 *
	 * @param request the request to be checked
	 * @param params A enumeration with the names of the parameters to be checked
	 * @return true if there is spam in the request
	 */
	
	public static boolean isSpamRequest(HttpServletRequest request,Enumeration<String> params) {

		StringBuffer sb = new StringBuffer();		
		while (params.hasMoreElements()) 
		{
			String value = request.getParameter(params.nextElement());
			sb.append(value + " ");
		}
		return isSpamField(sb.toString());
	}
	
	public static boolean isSpamRequest(Map<String, Object> parameters, Set<String> paramsToCheck) {

		StringBuffer sb = new StringBuffer();		
		for (String param : paramsToCheck) 
		{
			Object ovalue = parameters.get(param);
			if(ovalue instanceof String)
				sb.append(((String)ovalue) + " ");
			else if(ovalue instanceof String[] && ((String[])ovalue).length > 0) {
				sb.append(((String[])ovalue)[0] + " ");
			}
		}
		return isSpamField(sb.toString());
	}
	
	/**
	 * This method receive a map and check a list of parameters to know if anyone have three or more href
	 * to external pages
	 *
	 * @param parameters A map with key/value sets to be checked
	 * @param params A enumeration with the names of the parameters to be checked
	 * @return true if there is spam in the request
	 */
	
	public static boolean isSpamParameters(Map<String,String> parameters) 
	{
		StringBuffer sb = new StringBuffer();
		Set<Entry<String, String>> es = parameters.entrySet();
		for (Entry<String, String> entry : es) 
		{
			if(UtilMethods.isSet(entry.getValue()))
				sb.append(entry.getValue() + " ");
		}
		return isSpamField(sb.toString());
	}

	/**
	 * This method receive a map and check a list of parameters to know if anyone have three or more href
	 * to external pages
	 *
	 * @param parameters A map with key/value sets to be checked
	 * @param params A enumeration with the names of the parameters to be checked
	 * @return true if there is spam in the request
	 */
	public static boolean isSpamParameters(Map<String,String> parameters,Enumeration<String> params) 
	{
		StringBuffer sb = new StringBuffer();
		while (params.hasMoreElements()) 
		{
			String nextKey = params.nextElement();
			String value = (String) (UtilMethods.isSet(parameters.get(nextKey)) ? parameters.get(nextKey) : "");
			sb.append(value + " ");
		}
		return isSpamField(sb.toString());
	}
	
	/**
	 * This method receive a string to be checked if has three or more href
	 * to external pages
	 *
	 * @param fieldValue represents the value of the parameters to be checked 
	 * @return true if there is spam in the request
	 */
	
	public static boolean isSpamField(String fieldValue)
	{
		//find a list of URLs in the 
		Pattern p = Pattern
				.compile("([A-Za-z][A-Za-z0-9+.-]{1,120}:[A-Za-z0-9/](([A-Za-z0-9$_.+!*,;/?:@&~=-])|%[A-Fa-f0-9]{2}){1,333}(#([a-zA-Z0-9][a-zA-Z0-9$_.+!*,;/?:@&~=%-]{0,1000}))?)");

		Map<String, Integer> map = new HashMap<String, Integer>();
		Matcher m = p.matcher(fieldValue);

		boolean result = m.find();

		// if any url is referenced more than 3 times
		while (result) {
			String url = m.group();

			Integer i = map.get(url);
			if (i == null){
				i = 0;
			}
			i++;
			map.put(url, i);
			if (i > 2)
				return true;

			result = m.find();
		}
		
		//if exist a <br>, <br/> <a href
		Pattern p2 = Pattern.compile(".*<br>.*|.*<br(\\s)*/>.*",Pattern.CASE_INSENSITIVE );
		Matcher m2 = p2.matcher(fieldValue);
		result = m2.find();

		return result;
	}

}
