package com.dotmarketing.util;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import com.dotcms.enterprise.ClusterThreadProxy;

/**
 * Generic class to get return configuration parameters, and any logic required
 * for those paramenters. This is different from the Config class, which only
 * reads from the config file.
 * 
 * @author andres
 * 
 */
public class ConfigUtils {

	public static String getDynamicContentPath() {
		String realPath = Config.getStringProperty("DYNAMIC_CONTENT_PATH");
		if (UtilMethods.isSet(realPath)) {
			if (!realPath.endsWith(java.io.File.separator)) {
				realPath = realPath + java.io.File.separator;
			}
		} else {
			realPath = Config.CONTEXT.getRealPath("dotsecure");
		}
		return realPath;

	}

	public static String getDynamicVelocityPath() {
		return getDynamicContentPath() + File.separator + "velocity"
				+ File.separator + "dynamic";
	}

	public static String getLucenePath() {
		return getDynamicContentPath() + File.separator + "dotlucene";
	}

	public static String getBackupPath() {
		return getDynamicContentPath() + File.separator + "backup";
	}

	public static String getBundlePath() {
		return getDynamicContentPath() + File.separator + "bundles";
	}

	public static String getServerId(){
		String serverId;
		if (Config.getStringProperty("DIST_INDEXATION_SERVER_ID")==null || Config.getStringProperty("DIST_INDEXATION_SERVER_ID").equalsIgnoreCase("")) {
			serverId=deduceHostName();
		} else {
			serverId= Config.getStringProperty("DIST_INDEXATION_SERVER_ID");
			Logger.info(ConfigUtils.class, "Using configured hostname: " + serverId);
		}
		return serverId;
	}

	private static String deduceHostName() {
		String hostName=null;
		try {
			if (findServerId(java.net.InetAddress.getLocalHost().getHostName())) {
				hostName=java.net.InetAddress.getLocalHost().getHostName() ;
			}
		} catch (UnknownHostException e) {
			Logger.error(ConfigUtils.class, "Couldn't determine hostname: " + e.getMessage());
		}
		if (hostName==null) {
			Logger.info(ConfigUtils.class, "Trying to find hostname by examining network interfaces");
			Enumeration<NetworkInterface> en=null;
			try {
				en=NetworkInterface.getNetworkInterfaces();

			} catch (SocketException e) {
				Logger.error(ConfigUtils.class, "Error getting interfaces: " + e.getMessage());
			}
			if (en!=null) {
				while (en.hasMoreElements()  && hostName==null ) {
					 NetworkInterface intf = en.nextElement();
					 for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
						 InetAddress address= enumIpAddr.nextElement();
						 Logger.info(ConfigUtils.class, "Getting hostname for: " +address );
						 String interfaceName=address.getHostName();
						 if (findServerId(interfaceName)) {
							 hostName=interfaceName;
						 }

					 }
				}
			}
		}
		if (hostName==null) {
			Logger.fatal(ConfigUtils.class, "No valid hostname found. Make sure your correct host name is defined in DIST_INDEXATION_SERVERS_IDS in dotmarketing-config.properties.");
		} else {
			Logger.info(ConfigUtils.class, "Deduced hostname: " + hostName);
		}
		return hostName;
	}

	private static boolean findServerId(String id) {
		if (id!=null) {
				for (String s: ClusterThreadProxy.getClusteredServerIds()) {
					if (s.equalsIgnoreCase(id)) {
				return true;
			}
		}
		}
		return false;
	}

}