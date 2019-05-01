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

import com.dotcms.repackage.net.sf.hibernate.mapping.RootClass;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.LowercaseNamingStrategy;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringUtil;
import com.liferay.util.SystemProperties;
import com.liferay.util.dao.hibernate.SessionConfiguration;
import java.io.InputStream;
import com.dotcms.repackage.net.sf.hibernate.cfg.Configuration;
import com.dotcms.repackage.net.sf.hibernate.mapping.Table;
import java.util.Iterator;

/**
 * <a href="HibernateConfiguration.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class HibernateConfiguration extends SessionConfiguration {

	public void init() {
		try {
			ClassLoader classLoader = getClass().getClassLoader();

			Configuration cfg = new Configuration();

			String[] configs = StringUtil.split(
				SystemProperties.get("hibernate.configs"));

			for (int i = 0; i < configs.length; i++) {
				try {
					InputStream is =
						classLoader.getResourceAsStream(configs[i]);

					if (is != null) {
						cfg = cfg.addInputStream(is);

						is.close();
					}
				}
				catch (Exception e) {
					Logger.error(this,e.getMessage(),e);
				}
			}

			cfg.setProperties(SystemProperties.getProperties());
			//http://jira.dotmarketing.net/browse/DOTCMS-4937
			if (DbConnectionFactory.isMySql()) {
				cfg.setNamingStrategy(new LowercaseNamingStrategy());
				Iterator it = cfg.getClassMappings();
				while (it.hasNext()) {
					RootClass c = (RootClass) it.next();
					Table liferayTable = c.getRootTable();
					liferayTable.setName(liferayTable.getName().toLowerCase());

				}
			}

			setSessionFactory(cfg.buildSessionFactory());
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

}