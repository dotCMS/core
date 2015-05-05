package com.dotcms.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import com.dotmarketing.util.UtilMethods;

/**
 * Provides quick access to HTTP request-related operations, such as:
 * <ul>
 * <li>IP address retrieval from the request.</li>
 * <li>Matching IP address and a netmask.</li>
 * <li>etc.</li>
 * </ul>
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-22-2015
 *
 */
public class HttpRequestDataUtil {

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
		InetAddress ipAddress = null;
		String ip = request.getHeader("X-Forwarded-For");
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_FORWARDED_FOR");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_FORWARDED");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_VIA");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("REMOTE_ADDR");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("X-Real-IP");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		if (UtilMethods.isSet(ip)) {
			ipAddress = InetAddress.getByName(ip);
		}
		return ipAddress;
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
		if (UtilMethods.isSet(ipAddress) && UtilMethods.isSet(netmask)) {
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
				SubnetInfo info = subnetUtils.getInfo();
				isMatching = info.isInRange(ipAddress);
			}
		}
		return isMatching;
	}

	/**
	 * Returns the referrer URL where the specified {@link HttpServletRequest}
	 * was generated from.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return The referrer URL.
	 */
	public static String getReferrerUrl(HttpServletRequest request) {
		String referrerUrl = request.getHeader("referer");
		return referrerUrl;
	}

	/**
	 * Returns the request URI.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return The URI of the request.
	 */
	public static String getUri(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri;
	}

}
