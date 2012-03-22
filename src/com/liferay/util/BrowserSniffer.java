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

import javax.servlet.http.HttpServletRequest;

/**
 * See http://www.zytrax.com/tech/web/browser_ids.htm for examples.
 *
 * <a href="BrowserSniffer.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.11 $
 *
 */
public class BrowserSniffer {

	public static final String ACCEPT = "ACCEPT";

	public static final String USER_AGENT = "USER-AGENT";

	public static boolean is_ie(HttpServletRequest req) {
		if (req == null) {
			return false;
		}

		String agent = req.getHeader(USER_AGENT);

		if (agent == null) {
			return false;
		}

		agent = agent.toLowerCase();

		if (agent.indexOf("msie") != -1) {
			return true;
		}
		else {
			return false;
		}
	}

	public static boolean is_ie_4(HttpServletRequest req) {
		if (req == null) {
			return false;
		}

		String agent = req.getHeader(USER_AGENT);

		if (agent == null) {
			return false;
		}

		agent = agent.toLowerCase();

		if (is_ie(req) && (agent.indexOf("msie 4") != -1)) {
			return true;
		}
		else {
			return false;
		}
	}
	
    public static boolean is_linux(HttpServletRequest req) {
        if (req == null) {
            return false;
        }

        String agent = req.getHeader(USER_AGENT);

        if (agent == null) {
            return false;
        }

        agent = agent.toLowerCase();

        if ((agent.indexOf("linux") != -1)) {
            return true;
        }
        else {
            return false;
        }
    }
	

	public static boolean is_ie_5(HttpServletRequest req) {
		if (req == null) {
			return false;
		}

		String agent = req.getHeader(USER_AGENT);

		if (agent == null) {
			return false;
		}

		agent = agent.toLowerCase();

		if (is_ie(req) && (agent.indexOf("msie 5.0") != -1)) {
			return true;
		}
		else {
			return false;
		}
	}

	public static boolean is_ie_5_5(HttpServletRequest req) {
		if (req == null) {
			return false;
		}

		String agent = req.getHeader(USER_AGENT);

		if (agent == null) {
			return false;
		}

		agent = agent.toLowerCase();

		if (is_ie(req) && (agent.indexOf("msie 5.5") != -1)) {
			return true;
		}
		else {
			return false;
		}
	}

	public static boolean is_ie_5_5_up(HttpServletRequest req) {
		if (is_ie(req) && !is_ie_4(req) && !is_ie_5(req)) {
			return true;
		}
		else {
			return false;
		}
	}

	public static boolean is_mozilla(HttpServletRequest req) {
		if (req == null) {
			return false;
		}

		String agent = req.getHeader(USER_AGENT);

		if (agent == null) {
			return false;
		}

		agent = agent.toLowerCase();

		if ((agent.indexOf("mozilla") != -1) &&
			(agent.indexOf("spoofer") == -1) &&
			(agent.indexOf("compatible") == -1) &&
			(agent.indexOf("opera") == -1) &&
			(agent.indexOf("webtv") == -1) &&
			(agent.indexOf("hotjava") == -1)) {

			return true;
		}
		else {
			return false;
		}
	}

	public static boolean is_mozilla_1_3_up(HttpServletRequest req) {
		if (req == null) {
			return false;
		}

		String agent = req.getHeader(USER_AGENT);

		if (agent == null) {
			return false;
		}

		agent = agent.toLowerCase();

		if (is_mozilla(req)) {
			int pos = agent.indexOf("gecko/");

			if (pos == -1) {
				return false;
			}
			else {
				String releaseDate = agent.substring(pos + 6, agent.length());

				if (releaseDate.compareTo("20030210") > 0) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean is_ns_4(HttpServletRequest req) {
		if (req == null) {
			return false;
		}

		String agent = req.getHeader(USER_AGENT);

		if (agent == null) {
			return false;
		}

		agent = agent.toLowerCase();

		if (!is_ie(req) && (agent.indexOf("mozilla/4.") != -1)) {
			return true;
		}
		else {
			return false;
		}
	}

	public static boolean is_rtf(HttpServletRequest req) {
		if (is_ie_5_5_up(req) || is_mozilla_1_3_up(req)) {
			return true;
		}
		else {
			return false;
		}
	}

	public static boolean is_wml(HttpServletRequest req) {
		if (req == null) {
			return false;
		}

		String accept = req.getHeader(ACCEPT);

		if (accept == null) {
			return false;
		}

		accept = accept.toLowerCase();

		if (accept.indexOf("wap.wml") != -1) {
			return true;
		}
		else {
			return false;
		}
	}

}