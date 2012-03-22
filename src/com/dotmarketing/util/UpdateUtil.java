package com.dotmarketing.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.util.ReleaseInfo;

public class UpdateUtil {

	/**
	 * 
	 * @return the new version if found. Null if up to date.
	 * @throws DotDataException if an error is encountered
	 */
	public static String getNewVersion() throws DotDataException{

		String fileUrl = "https://www.dotcms.org/servlets/upgrade/";
		Map<String, String> pars = new HashMap<String, String>();
		pars.put("version", ReleaseInfo.getVersion()); //$NON-NLS-1$
		pars.put("minor", ReleaseInfo.getBuildNumber() + ""); //$NON-NLS-1$
		pars.put("check_version", "true");
		pars.put("level", System.getProperty("dotcms_level"));
		if (System.getProperty("dotcms_license_serial") != null) {
			pars.put("license", System.getProperty("dotcms_license_serial"));
		}

		HttpClient client = new HttpClient();

		PostMethod method = new PostMethod(fileUrl);
		Object[] keys = (Object[]) pars.keySet().toArray();
		NameValuePair[] data = new NameValuePair[keys.length];
		for (int i = 0; i < keys.length; i++) {
			String key = (String) keys[i];
			NameValuePair pair = new NameValuePair(key, pars.get(key));
			data[i] = pair;
		}

		method.setRequestBody(data);
		String ret = null;

		try {
			client.executeMethod(method);
			int retCode = method.getStatusCode();
			if (retCode == 204) {
				Logger.info(UpdateUtil.class, "No new updates found");
			} else {
				if (retCode == 200) {
					String newMinor = method.getResponseHeader("Minor-Version")
							.getValue();
					String newPrettyName = null;
					if (method.getResponseHeader("Pretty-Name") != null) {
						newPrettyName = method.getResponseHeader("Pretty-Name")
								.getValue();
					}

					if (newPrettyName == null) {
						Logger.info(UpdateUtil.class, "New Version: "
								+ newMinor);
						ret = newMinor;
					} else {
						Logger.info(UpdateUtil.class, "New Version: "
								+ newPrettyName + "/" + newMinor);
						ret = newPrettyName;
					}

				} else {
					throw new DotDataException("Unknown return code: " + method.getStatusCode() + " (" +method.getStatusText()+")");
				} 
			} 
		} catch (HttpException e) {
			Logger.error(UpdateUtil.class, "HttpException: " + e.getMessage(),
					e);
			throw new DotDataException("HttpException: " + e.getMessage(),e);
			
		} catch (IOException e) {
			Logger.error(UpdateUtil.class, "IOException: " + e.getMessage(), e);
			throw new DotDataException("IOException: " + e.getMessage(),e);
		}

		return ret;
	}

}
