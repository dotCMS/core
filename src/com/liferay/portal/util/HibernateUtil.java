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

package com.liferay.portal.util;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.dialect.Dialect;

import com.dotmarketing.util.Logger;
import com.liferay.util.InstancePool;
import com.liferay.util.dao.hibernate.SessionConfiguration;

/**
 * <a href="HibernateUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.11 $
 *
 */
public class HibernateUtil {

	public static void closeSession(Session session) {
		try {
			if (session != null) {
				session.close();
			}
		}
		catch (HibernateException he) {
			Logger.error(HibernateException.class,he.getMessage(),he);
		}
	}

	public static Dialect getDialect(String className) {
		SessionConfiguration config =
			_getSessionConfigurationInstance(className);

		return config.getDialect();
	}

	public static Session openSession() throws HibernateException {
		return openSession(null);
	}

	public static Session openSession(String className)
		throws HibernateException {

		SessionConfiguration config =
			_getSessionConfigurationInstance(className);

		return config.openSession();
	}

	private static String _getSessionConfigurationClassName(String className) {
		if (className == null) {
			className = HibernateConfiguration.class.getName();
		}

		return className;
	}

	private static SessionConfiguration _getSessionConfigurationInstance(
		String className) {

		className = _getSessionConfigurationClassName(className);

		return (SessionConfiguration)InstancePool.get(
			_getSessionConfigurationClassName(className));
	}

}