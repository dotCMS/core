/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util;

import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;

import javax.portlet.PortletRequest;
import javax.servlet.ServletRequest;

import com.dotmarketing.util.Logger;

/**
 * <a href="ParamUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.8 $
 *
 */
public class ParamUtil {

	// Servlet Request

	public static boolean getBoolean(ServletRequest req, String param) {
		return GetterUtil.getBoolean(req.getParameter(param));
	}

	public static boolean getBoolean(ServletRequest req,
									 String param, boolean defaultValue) {

		return get(req, param, defaultValue);
	}

	public static Date getDate(ServletRequest req, String param,
							   DateFormat df) {

		return GetterUtil.getDate(req.getParameter(param), df);
	}

	public static Date getDate(ServletRequest req,
							   String param, DateFormat df, Date defaultValue) {

		return get(req, param, df, defaultValue);
	}

	public static double getDouble(ServletRequest req, String param) {
		return GetterUtil.getDouble(req.getParameter(param));
	}

	public static double getDouble(ServletRequest req,
								   String param, double defaultValue) {

		return get(req, param, defaultValue);
	}

	public static float getFloat(ServletRequest req, String param) {
		return GetterUtil.getFloat(req.getParameter(param));
	}

	public static float getFloat(ServletRequest req,
								 String param, float defaultValue) {

		return get(req, param, defaultValue);
	}

	public static int getInteger(ServletRequest req, String param) {
		return GetterUtil.getInteger(req.getParameter(param));
	}

	public static int getInteger(ServletRequest req,
								 String param, int defaultValue) {

		return get(req, param, defaultValue);
	}

	public static long getLong(ServletRequest req, String param) {
		return GetterUtil.getLong(req.getParameter(param));
	}

	public static long getLong(ServletRequest req,
							   String param, long defaultValue) {

		return get(req, param, defaultValue);
	}

	public static short getShort(ServletRequest req, String param) {
		return GetterUtil.getShort(req.getParameter(param));
	}

	public static short getShort(ServletRequest req,
								 String param, short defaultValue) {

		return get(req, param, defaultValue);
	}

	public static String getString(ServletRequest req, String param) {
		return GetterUtil.getString(req.getParameter(param));
	}

	public static String getString(ServletRequest req,
								   String param, String defaultValue) {

		return get(req, param, defaultValue);
	}

	public static boolean get(ServletRequest req,
							  String param, boolean defaultValue) {

		return GetterUtil.get(req.getParameter(param), defaultValue);
	}

	public static Date get(ServletRequest req,
						   String param, DateFormat df, Date defaultValue) {

		return GetterUtil.get(req.getParameter(param), df, defaultValue);
	}

	public static double get(ServletRequest req,
							 String param, double defaultValue) {

		return GetterUtil.get(req.getParameter(param), defaultValue);
	}

	public static float get(ServletRequest req,
							String param, float defaultValue) {

		return GetterUtil.get(req.getParameter(param), defaultValue);
	}

	public static int get(ServletRequest req, String param, int defaultValue) {
		return GetterUtil.get(req.getParameter(param), defaultValue);
	}

	public static long get(ServletRequest req,
						   String param, long defaultValue) {

		return GetterUtil.get(req.getParameter(param), defaultValue);
	}

	public static short get(ServletRequest req,
							String param, short defaultValue) {

		return GetterUtil.get(req.getParameter(param), defaultValue);
	}

	public static String get(ServletRequest req,
							 String param, String defaultValue) {

		String returnValue =
			GetterUtil.get(req.getParameter(param), defaultValue);

		if (returnValue != null) {
			return returnValue.trim();
		}

		return null;
	}

	public static void print(ServletRequest req) {
		Enumeration e = req.getParameterNames();

		while (e.hasMoreElements()) {
			String param = (String)e.nextElement();

			String[] values = req.getParameterValues(param);

			for (int i = 0; i < values.length; i++) {
				Logger.info(ParamUtil.class, param + "[" + i + "] = " + values[i]);
			}
		}
	}

	// Portlet Request

	public static boolean getBoolean(PortletRequest req, String param) {
		return GetterUtil.getBoolean(req.getParameter(param));
	}

	public static boolean getBoolean(PortletRequest req,
									 String param, boolean defaultValue) {

		return get(req, param, defaultValue);
	}

	public static Date getDate(PortletRequest req, String param,
							   DateFormat df) {

		return GetterUtil.getDate(req.getParameter(param), df);
	}

	public static Date getDate(PortletRequest req,
							   String param, DateFormat df, Date defaultValue) {

		return get(req, param, df, defaultValue);
	}

	public static double getDouble(PortletRequest req, String param) {
		return GetterUtil.getDouble(req.getParameter(param));
	}

	public static double getDouble(PortletRequest req,
								   String param, double defaultValue) {

		return get(req, param, defaultValue);
	}

	public static float getFloat(PortletRequest req, String param) {
		return GetterUtil.getFloat(req.getParameter(param));
	}

	public static float getFloat(PortletRequest req,
								 String param, float defaultValue) {

		return get(req, param, defaultValue);
	}

	public static int getInteger(PortletRequest req, String param) {
		return GetterUtil.getInteger(req.getParameter(param));
	}

	public static int getInteger(PortletRequest req,
								 String param, int defaultValue) {

		return get(req, param, defaultValue);
	}

	public static long getLong(PortletRequest req, String param) {
		return GetterUtil.getLong(req.getParameter(param));
	}

	public static long getLong(PortletRequest req,
							   String param, long defaultValue) {

		return get(req, param, defaultValue);
	}

	public static short getShort(PortletRequest req, String param) {
		return GetterUtil.getShort(req.getParameter(param));
	}

	public static short getShort(PortletRequest req,
								 String param, short defaultValue) {

		return get(req, param, defaultValue);
	}

	public static String getString(PortletRequest req, String param) {
		return GetterUtil.getString(req.getParameter(param));
	}

	public static String getString(PortletRequest req,
								   String param, String defaultValue) {

		return get(req, param, defaultValue);
	}

	public static boolean get(PortletRequest req,
							  String param, boolean defaultValue) {

		return GetterUtil.get(req.getParameter(param), defaultValue);
	}

	public static Date get(PortletRequest req,
						   String param, DateFormat df, Date defaultValue) {

		return GetterUtil.get(req.getParameter(param), df, defaultValue);
	}

	public static double get(PortletRequest req,
							 String param, double defaultValue) {

		return GetterUtil.get(req.getParameter(param), defaultValue);
	}

	public static float get(PortletRequest req,
							String param, float defaultValue) {

		return GetterUtil.get(req.getParameter(param), defaultValue);
	}

	public static int get(PortletRequest req, String param, int defaultValue) {
		return GetterUtil.get(req.getParameter(param), defaultValue);
	}

	public static long get(PortletRequest req,
						   String param, long defaultValue) {

		return GetterUtil.get(req.getParameter(param), defaultValue);
	}

	public static short get(PortletRequest req,
							String param, short defaultValue) {

		return GetterUtil.get(req.getParameter(param), defaultValue);
	}

	public static String get(PortletRequest req,
							 String param, String defaultValue) {

		String returnValue =
			GetterUtil.get(req.getParameter(param), defaultValue);

		if (returnValue != null) {
			return returnValue.trim();
		}

		return null;
	}

	public static void print(PortletRequest req) {
		Enumeration e = req.getParameterNames();

		while (e.hasMoreElements()) {
			String param = (String)e.nextElement();

			String[] values = req.getParameterValues(param);

			for (int i = 0; i < values.length; i++) {
				Logger.info(ParamUtil.class, param + "[" + i + "] = " + values[i]);
			}
		}
	}

}