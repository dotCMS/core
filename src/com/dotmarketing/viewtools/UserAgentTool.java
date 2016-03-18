package com.dotmarketing.viewtools;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.util.Logger;

import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.DeviceType;
import eu.bitwalker.useragentutils.Manufacturer;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.RenderingEngine;
import eu.bitwalker.useragentutils.UserAgent;
import eu.bitwalker.useragentutils.Version;

public class UserAgentTool implements ViewTool {

	UserAgent userAgent = null;

	public void init(Object obj) {
		if(obj instanceof ViewContext){
			HttpServletRequest req = ((ViewContext) obj).getRequest();

			init( req );
		}
	}

	public void init(HttpServletRequest req) {
		this.userAgent = UserAgent.parseUserAgentString(req.getHeader("User-Agent"));
	}

	public UserAgent getAgent() {

		return userAgent;

	}

	public UserAgent getUserAgent() {

		return userAgent;

	}

	public Browser getBrowser() {
		try {
			return userAgent.getBrowser();
		} catch (Exception e) {
			Logger.error(this.getClass(), "$useragent error :" + e.getMessage());
		}
		return null;

	}

	public Version getVersion() {
		try {
			return userAgent.getBrowserVersion();
		} catch (Exception e) {
			Logger.error(this.getClass(), "$useragent error :" + e.getMessage());
		}
		return null;

	}

	public OperatingSystem getOs() {
		try {
			return userAgent.getOperatingSystem();
		} catch (Exception e) {
			Logger.error(this.getClass(), "$useragent error :" + e.getMessage());
		}
		return null;
	}

	public Manufacturer getManufacturer() {
		try {
			return userAgent.getOperatingSystem().getManufacturer();
		} catch (Exception e) {
			Logger.error(this.getClass(), "$useragent error :" + e.getMessage());
		}
		return null;

	}

	public DeviceType getDeviceType() {
		try {
			return userAgent.getOperatingSystem().getDeviceType();
		} catch (Exception e) {
			Logger.error(this.getClass(), "$useragent error :" + e.getMessage());
		}
		return null;

	}

	public DeviceType getType() {

		return getDeviceType();

	}

	public RenderingEngine getEngine() {
		try {
			return userAgent.getBrowser().getRenderingEngine();
		} catch (Exception e) {
			Logger.error(this.getClass(), "$useragent error :" + e.getMessage());
		}
		return null;

	}
}