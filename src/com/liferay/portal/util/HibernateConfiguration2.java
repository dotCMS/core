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

import java.io.InputStream;

import net.sf.hibernate.cfg.Configuration;

import com.dotmarketing.util.Logger;
import com.liferay.util.dao.hibernate.SessionConfiguration;

/**
 * <a href="HibernateConfiguration2.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class HibernateConfiguration2 extends SessionConfiguration {

	public void init() {
		try {
			ClassLoader classLoader = getClass().getClassLoader();

			Configuration cfg = new Configuration();

			InputStream is =
				classLoader.getResourceAsStream("META-INF/sample-hbm.xml");

			if (is != null) {
				cfg = cfg.addInputStream(is);

				is.close();
			}

			cfg.setProperty(
				"hibernate.connection.datasource",
				"jdbc/SamplePool");

			cfg.setProperty(
				"hibernate.statement_cache.size",
				"0");

			cfg.setProperty(
				"hibernate.dialect",
				"com.liferay.util.dao.hibernate.DynamicDialect");

			cfg.setProperty(
				"hibernate.jdbc.batch_size",
				"0");

			cfg.setProperty(
				"hibernate.jdbc.use_scrollable_resultset",
				"true");

			cfg.setProperty(
				"hibernate.cglib.use_reflection_optimizer",
				"false");

			cfg.setProperty(
				"hibernate.connection.provider_class",
				"com.liferay.util.dao.hibernate.DSConnectionProvider");

			cfg.setProperty(
				"hibernate.cache.provider_class",
				"net.sf.hibernate.cache.EhCacheProvider");

			cfg.setProperty(
				"hibernate.show_sql",
				"false");

			setSessionFactory(cfg.buildSessionFactory());
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

}