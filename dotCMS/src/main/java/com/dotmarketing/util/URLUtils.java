package com.dotmarketing.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLUtils {

	public static class ParsedURL {
		
		private String protocol;
		private String host;
		private int port;
		private String URI;
		private String path;
		private String resource;
		private String queryString;
		private Map<String, String[]> parameters;
		
		public String getProtocol() {
			return protocol;
		}
		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}
		public String getHost() {
			return host;
		}
		public void setHost(String host) {
			this.host = host;
		}
		public int getPort() {
			return port;
		}
		public void setPort(int port) {
			this.port = port;
		}
		public String getPath() {
			return path;
		}
		public void setPath(String path) {
			this.path = path;
		}
		public String getResource() {
			return resource;
		}
		public void setResource(String resource) {
			this.resource = resource;
		}
		public String getQueryString() {
			return queryString;
		}
		public void setQueryString(String queryString) {
			this.queryString = queryString;
		}
		public Map<String, String[]> getParameters() {
			return parameters;
		}
		public void setParameters(Map<String, String[]> parameters) {
			this.parameters = parameters;
		}
		public void setURI(String uRI) {
			URI = uRI;
		}
		public String getURI() {
			return URI;
		}

	}	

	private static Pattern regexPattern = Pattern.compile("((\\w+)://([^/\\p{Cntrl}:]+)(?::([0-9]+))?)?(((?:/[^/\\p{Cntrl}]+)*)(?:/([^/\\p{Cntrl}?]+)?))?\\??(.*)?");
	
	public static ParsedURL parseURL(String url) throws IllegalArgumentException {
		
		Matcher matcher = regexPattern.matcher(url);

		if(!matcher.find())
			return null;
		
		ParsedURL parsedUrl = new ParsedURL();
		parsedUrl.setProtocol(matcher.group(2));
		parsedUrl.setHost(matcher.group(3));
		parsedUrl.setPort(matcher.group(4) != null?Integer.parseInt(matcher.group(4)):0);
		parsedUrl.setURI(matcher.group(5));
		parsedUrl.setPath(matcher.group(6));
		parsedUrl.setResource(matcher.group(7));
		parsedUrl.setQueryString(matcher.group(8));

		Map<String, List<String>> parameters = new HashMap<String, List<String>>();
		String[] queryStringSplitted = parsedUrl.queryString.split("&");
		
		for (int i = 0; i < queryStringSplitted.length; i++) {
			try {
				String[] queryParamTuple = queryStringSplitted[i].split("=");
				String parameterKey = URLDecoder.decode(queryParamTuple[0], "UTF8");
				if(!UtilMethods.isSet(parameterKey))
					continue;
				String parameterValue = queryParamTuple.length > 1?URLDecoder.decode(queryParamTuple[1], "UTF8"):null;
				List<String> parameterValues = parameters.get(parameterKey);
				if(parameterValues == null) {
					parameterValues = new ArrayList<String>(1);
					parameters.put(parameterKey, parameterValues);
				}
				if(parameterValue != null)
					parameterValues.add(parameterValue);
			} catch (UnsupportedEncodingException e) {
				Logger.error(URLUtils.class, e.getMessage(), e);
				throw new IllegalArgumentException(e.getMessage(), e);
			}
		}
		
		Map<String, String[]> parametersToRet = new HashMap<String, String[]>();
		for(Map.Entry<String, List<String>> parameterEntry : parameters.entrySet()) {
			String[] values = parameterEntry.getValue().toArray(new String[0]);
			parametersToRet.put(parameterEntry.getKey(), values);
		}
		
		parsedUrl.setParameters(parametersToRet);
		
		return parsedUrl;
	}
	
}
