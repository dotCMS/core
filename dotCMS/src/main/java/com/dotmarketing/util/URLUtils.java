package com.dotmarketing.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLUtils {

	private URLUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static class ParsedURL {

		private String protocol;
		private String host;
		private int port;
		private String uri;
		private String path;
		private String resource;
		private String queryString;
		private Map<String, String[]> parameters;
		private String fragment;

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
			uri = uRI;
		}
		public String getURI() {
			return uri;
		}
		public String getFragment() {
			return fragment;
		}
		public void setFragment(String fragment) {
			this.fragment = fragment;
		}
	}

	private static final Pattern protocolHostPortPattern = Pattern.compile(
    "^(?<protocol>[^:/?#]+)://(?<host>[^/\\p{Cntrl}:]+)(?::(?<port>\\d+))?");

    private static final Pattern pathQueryFragmentPattern = Pattern.compile(
    "(?<uri>(?<path>/[^?#]*)?/(?<resource>[^/?#]*))?(?:\\?(?<query>[^#]*))?(?:#(?<fragment>.*))?");

	public static ParsedURL parseURL(final String url) throws IllegalArgumentException {
		final ParsedURL parsedUrl = new ParsedURL();

		final int pathIndex = parseProtocolHostPort(url, parsedUrl);
		final boolean foundMatch = parsePathQueryFragment(url, parsedUrl, pathIndex);
		if (!foundMatch) {
			return null;
		}
		processQueryString(parsedUrl);

		return parsedUrl;
	}

	private static int parseProtocolHostPort(final String url, final ParsedURL parsedUrl) {
		final Matcher matcher = protocolHostPortPattern.matcher(url);
		if (matcher.find()) {
			parsedUrl.setProtocol(matcher.group("protocol"));
			parsedUrl.setHost(matcher.group("host"));
			parsedUrl.setPort(matcher.group("port") != null ?
					Integer.parseInt(matcher.group("port")) : 0);
			return matcher.end();
		} else {
			return 0;
		}
	}

	private static boolean parsePathQueryFragment(final String url,
											   final ParsedURL parsedUrl, final int pathIndex) {
		final Matcher matcher = pathQueryFragmentPattern.matcher(url);
		if (matcher.find(pathIndex)) {
			parsedUrl.setURI(matcher.group("uri"));
			parsedUrl.setPath(matcher.group("path"));
			parsedUrl.setResource(matcher.group("resource"));
			parsedUrl.setQueryString(matcher.group("query"));
			parsedUrl.setFragment(matcher.group("fragment"));
			return true;
		} else {
			return false;
		}
	}

	private static void processQueryString(final ParsedURL parsedUrl) {
		final Map<String, List<String>> parameters = new HashMap<>();
		if (parsedUrl.getQueryString() != null) {
			final String[] queryStringSplitted = parsedUrl.getQueryString().split("&");
			for (String queryParam : queryStringSplitted) {
                final String[] queryParamTuple = queryParam.split("=");
                final String parameterKey = URLDecoder.decode(
                        queryParamTuple[0], StandardCharsets.UTF_8);
                if (!UtilMethods.isSet(parameterKey)) continue;
                final String parameterValue = queryParamTuple.length > 1 ?
                        URLDecoder.decode(queryParamTuple[1], StandardCharsets.UTF_8) : null;
                parameters.computeIfAbsent(parameterKey,
                        k -> new ArrayList<>()).add(parameterValue);
            }
		}

		final Map<String, String[]> parametersToRet = new HashMap<>();
		for (Map.Entry<String, List<String>> parameterEntry : parameters.entrySet()) {
			parametersToRet.put(parameterEntry.getKey(),
					parameterEntry.getValue().toArray(new String[0]));
		}
		parsedUrl.setParameters(parametersToRet);
	}

}
