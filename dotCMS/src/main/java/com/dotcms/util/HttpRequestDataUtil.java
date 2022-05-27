package com.dotcms.util;

import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.netty.util.NetUtil;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.Query;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.elasticsearch.common.network.InetAddresses;

/**
 * Provides quick access to information that can be obtained from the HTTP
 * request, including, for example:
 * <ul>
 * <li>IP address retrieval from the request.</li>
 * <li>Matching IP address and a netmask.</li>
 * <li>Retrieval of URL parameters.</li>
 * <li>Retrieval of the request URI.</li>
 * </ul>
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-22-2015
 *
 */
public class HttpRequestDataUtil {

	public static final String SERVER_PORT = createServerPort();

	public static final String DEFAULT_REMOTE_ADDRESS = "0.0.0.0";

	/**
	 * Get the remote address, if the request is null will return 0.0.0.0.
	 * @param request {@link HttpServletRequest}
	 * @return String
     */
	public static String getRemoteAddress (final HttpServletRequest request) {

		return null != request?request.getRemoteAddr(): DEFAULT_REMOTE_ADDRESS;
	} // getRemoteAddress.

	/**
	 * Retrieves the client's IP address from the {@link HttpServletRequest}
	 * object based on the different available approaches. It's worth noting
	 * that, depending on the server startup parameters (as described in the
	 * Javadoc for this class), the resulting IP address can be either IPv4 or
	 * IPv6.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return The client's IP address (either IPv4 or IPv6.).
	 * @throws UnknownHostException
	 *             The host name or IP address does not exist.
	 */
	public static InetAddress getIpAddress(HttpServletRequest request)
			throws UnknownHostException {
		byte[] remoteAddr = Try.of(()->InetAddresses.isInetAddress(request.getRemoteAddr())).getOrElse(false)
				? NetUtil.createByteArrayFromIpAddressString(request.getRemoteAddr())
				: new byte[]{127, 0, 0, 1};
		return InetAddress.getByAddress(remoteAddr);
	}

	/**
	 * Determines whether an IP address matches a specific netmask.
	 * 
	 * @param ipAddress
	 *            - The IP address to validate.
	 * @param netmask
	 *            - The netmask address. This can be either a classic netmask (
	 *            {@code "192.168.1.2/255.255.255.0"}) or CIDR-notation netmask
	 *            ({@code "192.168.1.2/24"}).
	 * @return If the IP address matches the given netmask, returns {@code true}
	 *         . Otherwise, returns {@code false}.
	 */
	public static boolean isIpMatchingNetmask(String ipAddress, String netmask) {
		boolean isMatching = false;
		// function only handles IPv4 addresses - this check for period prevents exceptions until IPv6 functionality is added
		if (UtilMethods.isSet(ipAddress) && ipAddress.contains(".") && UtilMethods.isSet(netmask) && netmask.contains(".")) {
			String[] netmaskParts = netmask.split("/");
			if (netmaskParts != null && netmaskParts.length == 2) {
				SubnetUtils subnetUtils = null;
				Pattern pattern = Pattern
						.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
				Matcher matcher = pattern.matcher(netmaskParts[1]);
				if (matcher.find()) {
					subnetUtils = new SubnetUtils(netmaskParts[0],
							netmaskParts[1]);
				} else {
					subnetUtils = new SubnetUtils(netmask);
				}
				subnetUtils.setInclusiveHostCount(true); // Necessary to get proper resolution of CIDRs like 127.0.0.1/32
				SubnetInfo info = subnetUtils.getInfo();
				isMatching = info.isInRange(ipAddress);
			}
			else {
				isMatching = netmask.equals(ipAddress);
			}
		}
		else {
			isMatching = netmask.equals(ipAddress); // to handle direct IPv6 IP matches even though IPv6 CIDRs not yet supported
		}
		Logger.debug(HttpRequestDataUtil.class, "ipAddress=" + ipAddress + "; netmask=" + netmask + "; isMatching=" + isMatching);
		return isMatching;
	}

	/**
	 * Returns the decoded URI of the request.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return The URI of the request.
	 * @throws UnsupportedEncodingException
	 *             If character encoding needs to be consulted, but named
	 *             character encoding is not supported.
	 */
	public static String getUri(HttpServletRequest request)
			throws UnsupportedEncodingException {
		String uri = URLDecoder.decode(request.getRequestURI(),
				UtilMethods.getCharsetConfiguration());
		return uri;
	}

	/**
	 * Retrieves the value of the specified parameter sent through the query
	 * String.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @param name
	 *            - The name of the query String parameter.
	 * @return The value of the URL parameter. If the request or the parameter
	 *         name are null/empty, or if the parameter is not present in the
	 *         request, it returns {@code null}.
	 */
	public static String getUrlParameterValue(HttpServletRequest request,
			String name) {
		if (request == null || !UtilMethods.isSet(name)) {
			return null;
		}
		String value = request.getParameter(name);
		return value;
	}

	/**
	 * Returns the host name in the specified {@link HttpServletRequest} object.
	 * <b>Important:</b> This method retrieves the authority part of the URL in
	 * order to get the host name.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return The host name, or an empty String if it could not be retrieved.
	 */
	public static String getHostname(final HttpServletRequest request) {
		String hostName = StringUtils.EMPTY;
		try {
			URL requestUrl = new URL(request.getRequestURL().toString());
			hostName = requestUrl.getAuthority();
		} catch (MalformedURLException e) {
			// URL is not valid, just return an empty String
		}
		return hostName;
	}

	/**
	 * Convenience method to create the server port and cache it to use it must likely in logs.
	 *
	 * @return String representing the server port
	 */
    private static String createServerPort() {
		final MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();

		try {
			final Set<ObjectName> objectNames = beanServer.queryNames(
					new ObjectName("*:type=Connector,*"),
					Query.match(Query.attr("protocol"), Query.value("HTTP/1.1")));
			final Iterator<ObjectName> iterator = objectNames.iterator();
			return iterator.hasNext() ? objectNames.iterator().next().getKeyProperty("port") : null;
		} catch (MalformedObjectNameException e) {
			return null;
		}
    }

	/**
	 * Returns the value of a given attribute in the HTTP Request object, or the default value if not present.
	 *
	 * @param request       An instance of the {@link HttpServletRequest} object.
	 * @param attributeName The name of the attribute that should be present in the request.
	 * @param defaultValue  The default value that will be returned in case the expected one is NOT present.
	 * @return The value of the specified request attribute.
	 */
	public static <T> T getAttribute(final HttpServletRequest request, final String attributeName, final T defaultValue) {
		if (null != request.getAttribute(attributeName)) {
			return (T) request.getAttribute(attributeName);
		}
		return defaultValue;
	}

	/**
	 * Convenience method to get the server port and cache it to use it must likely in logs.
	 *
	 * @return String representing the server port
	 */
	public static Optional<String> getServerPort() {
		return Optional.ofNullable(SERVER_PORT);
	}

	public static Optional<String> getParamCaseInsensitive(final HttpServletRequest request,
			final String paramNameToLookFor) {
		Enumeration<String> parameterNames = request.getParameterNames();

		Optional<String> valueFromParam = Optional.empty();

		while (parameterNames.hasMoreElements()) {
			final String paramName = parameterNames.nextElement();

			if(UtilMethods.isSet(paramName) && paramName.toLowerCase().equals(paramNameToLookFor)) {
				valueFromParam = Optional.ofNullable(request.getParameter(paramName));
				break;
			}
		}
		return valueFromParam;
	}

	public static Optional<String> getHeaderCaseInsensitive(final HttpServletRequest request,
			final String headerNameToLookFor) {
		Enumeration<String> headerNames = request.getHeaderNames();

		Optional<String> valueFromParam = Optional.empty();

		while (headerNames.hasMoreElements()) {
			final String headerName = headerNames.nextElement();

			if(UtilMethods.isSet(headerName) && headerName.toLowerCase().equals(headerNameToLookFor)) {
				valueFromParam = Optional.ofNullable(request.getHeader(headerName));
				break;
			}
		}
		return valueFromParam;
	}
}
