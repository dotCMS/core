package com.dotmarketing.util;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.UUID;

import com.dotcms.enterprise.ClusterThreadProxy;
import com.dotmarketing.business.APILocator;

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
			realPath = com.liferay.util.FileUtil.getRealPath("/dotsecure");
		}
		return realPath;

	}

	public static String getDynamicVelocityPath() {
		return getDynamicContentPath() + File.separator + "velocity"
				+ File.separator + "dynamic";
	}

	public static String getACheckerPath() {
		return com.liferay.util.FileUtil.getRealPath("/WEB-INF/achecker_sql");
	}

	public static String getLucenePath() {
		return getDynamicContentPath() + File.separator + "dotlucene";
	}

	public static String getBackupPath() {
		return getDynamicContentPath() + File.separator + "backup";
	}

	public static String getBundlePath() {
		String path=APILocator.getFileAPI().getRealAssetsRootPath() + File.separator + "bundles";
		File pathDir=new File(path);
		if(!pathDir.exists())
		    pathDir.mkdirs();
		return path;
	}

	public static String getIntegrityPath() {
		String path=APILocator.getFileAPI().getRealAssetsRootPath() + File.separator + "integrity";
		File pathDir=new File(path);
		if(!pathDir.exists())
		    pathDir.mkdirs();
		return path;
	}

	public static String getTimeMachinePath(){

		String path = Config.getStringProperty("TIMEMACHINE_PATH", null);

		if(path == null || (path != null && path.equals("null")) ){
			path=APILocator.getFileAPI().getRealAssetsRootPath() + File.separator + "timemachine";
			File pathDir=new File(path);
			if(!pathDir.exists())
			    pathDir.mkdirs();
		}

		return path;
	}

	public static String getServerId(){
		String serverId;
		if (Config.getStringProperty("DIST_INDEXATION_SERVER_ID")==null || Config.getStringProperty("DIST_INDEXATION_SERVER_ID").equalsIgnoreCase("")) {
			serverId = APILocator.getServerAPI().readServerId();

			if(!UtilMethods.isSet(serverId)) {
				serverId = UUID.randomUUID().toString();

			}

			Config.setProperty("DIST_INDEXATION_SERVER_ID", serverId);
//			serverId=deduceHostName();
		} else {
			serverId= Config.getStringProperty("DIST_INDEXATION_SERVER_ID");
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
