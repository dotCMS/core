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
import net.sf.hibernate.mapping.Table;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.LowercaseNamingStrategy;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringUtil;
import com.liferay.util.SystemProperties;
import com.liferay.util.dao.hibernate.SessionConfiguration;

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
			String _dbType = DbConnectionFactory.getDBType();
			if (_dbType!=null && DbConnectionFactory.MYSQL.equals(_dbType)) {
				cfg.setNamingStrategy(new LowercaseNamingStrategy());
				Table liferayTable  = cfg.getClassMapping(com.liferay.portal.ejb.CompanyHBM.class).getTable();
				liferayTable.setName("company");
				liferayTable  = cfg.getClassMapping(com.liferay.portal.ejb.AddressHBM.class).getTable();
				liferayTable.setName("address");
				liferayTable  = cfg.getClassMapping(com.liferay.portal.ejb.ImageHBM.class).getTable();
				liferayTable.setName("image");
				liferayTable  = cfg.getClassMapping(com.liferay.portal.ejb.PasswordTrackerHBM.class).getTable();
				liferayTable.setName("passwordtracker");
				liferayTable  = cfg.getClassMapping(com.liferay.portal.ejb.PortletHBM.class).getTable();
				liferayTable.setName("portlet");
				liferayTable  = cfg.getClassMapping(com.liferay.portal.ejb.PortletPreferencesHBM.class).getTable();
				liferayTable.setName("portletpreferences");
				liferayTable  = cfg.getClassMapping(com.liferay.portal.ejb.ReleaseHBM.class).getTable();
				liferayTable.setName("release_");
				liferayTable  = cfg.getClassMapping(com.liferay.portal.ejb.UserHBM.class).getTable();
				liferayTable.setName("user_");
				liferayTable  = cfg.getClassMapping(com.liferay.portal.ejb.UserTrackerHBM.class).getTable();
				liferayTable.setName("usertracker");
				liferayTable  = cfg.getClassMapping(com.liferay.portal.ejb.UserTrackerPathHBM.class).getTable();
				liferayTable.setName("usertrackerpath");
				liferayTable  = cfg.getClassMapping(com.liferay.portlet.admin.ejb.AdminConfigHBM.class).getTable();
				liferayTable.setName("adminconfig");
				liferayTable  = cfg.getClassMapping(com.liferay.portlet.polls.ejb.PollsChoiceHBM.class).getTable();
				liferayTable.setName("pollschoice");
				liferayTable  = cfg.getClassMapping(com.liferay.portlet.polls.ejb.PollsDisplayHBM.class).getTable();
				liferayTable.setName("pollsdisplay");
				liferayTable  = cfg.getClassMapping(com.liferay.portlet.polls.ejb.PollsQuestionHBM.class).getTable();
				liferayTable.setName("pollsquestion");
				liferayTable  = cfg.getClassMapping(com.liferay.portlet.polls.ejb.PollsVoteHBM.class).getTable();
				liferayTable.setName("pollsvote");
				liferayTable  = cfg.getClassMapping(com.liferay.counter.ejb.CounterHBM.class).getTable();
				liferayTable.setName("counter");
			}

			setSessionFactory(cfg.buildSessionFactory());
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

}