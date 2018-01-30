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

package com.liferay.portlet.admin.ejb;

import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.BasePersistence;
import com.liferay.portal.util.HibernateUtil;
import com.liferay.portlet.admin.NoSuchConfigException;
import com.liferay.util.dao.hibernate.OrderByComparator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;
import com.dotcms.repackage.net.sf.hibernate.ObjectNotFoundException;
import com.dotcms.repackage.net.sf.hibernate.Query;
import com.dotcms.repackage.net.sf.hibernate.ScrollableResults;
import com.dotcms.repackage.net.sf.hibernate.Session;

/**
 * <a href="AdminConfigPersistence.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.13 $
 *
 */
public class AdminConfigPersistence extends BasePersistence {
	protected com.liferay.portlet.admin.model.AdminConfig create(
		String configId) {
		return new com.liferay.portlet.admin.model.AdminConfig(configId);
	}

	protected com.liferay.portlet.admin.model.AdminConfig remove(
		String configId) throws NoSuchConfigException, SystemException {
		Session session = null;

		try {
			session = openSession();

			AdminConfigHBM adminConfigHBM = (AdminConfigHBM)session.load(AdminConfigHBM.class,
					configId);
			com.liferay.portlet.admin.model.AdminConfig adminConfig = AdminConfigHBMUtil.model(adminConfigHBM);
			session.delete(adminConfigHBM);
			session.flush();
			AdminConfigPool.remove(configId);

			return adminConfig;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchConfigException(configId.toString());
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portlet.admin.model.AdminConfig update(
		com.liferay.portlet.admin.model.AdminConfig adminConfig)
		throws SystemException {
		Session session = null;

		try {
			if (adminConfig.isNew() || adminConfig.isModified()) {
				session = openSession();

				if (adminConfig.isNew()) {
					AdminConfigHBM adminConfigHBM = new AdminConfigHBM(adminConfig.getConfigId(),
							adminConfig.getCompanyId(), adminConfig.getType(),
							adminConfig.getName(), adminConfig.getConfig());
					session.save(adminConfigHBM);
					session.flush();
				}
				else {
					try {
						AdminConfigHBM adminConfigHBM = (AdminConfigHBM)session.load(AdminConfigHBM.class,
								adminConfig.getPrimaryKey());
						adminConfigHBM.setCompanyId(adminConfig.getCompanyId());
						adminConfigHBM.setType(adminConfig.getType());
						adminConfigHBM.setName(adminConfig.getName());
						adminConfigHBM.setConfig(adminConfig.getConfig());
						session.flush();
					}
					catch (ObjectNotFoundException onfe) {
						AdminConfigHBM adminConfigHBM = new AdminConfigHBM(adminConfig.getConfigId(),
								adminConfig.getCompanyId(),
								adminConfig.getType(), adminConfig.getName(),
								adminConfig.getConfig());
						session.save(adminConfigHBM);
						session.flush();
					}
				}

				adminConfig.setNew(false);
				adminConfig.setModified(false);
				adminConfig.protect();
				AdminConfigPool.remove(adminConfig.getPrimaryKey());
				AdminConfigPool.put(adminConfig.getPrimaryKey(), adminConfig);
			}

			return adminConfig;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portlet.admin.model.AdminConfig findByPrimaryKey(
		String configId) throws NoSuchConfigException, SystemException {
		com.liferay.portlet.admin.model.AdminConfig adminConfig = AdminConfigPool.get(configId);
		Session session = null;

		try {
			if (adminConfig == null) {
				session = openSession();

				AdminConfigHBM adminConfigHBM = (AdminConfigHBM)session.load(AdminConfigHBM.class,
						configId);
				adminConfig = AdminConfigHBMUtil.model(adminConfigHBM);
			}

			return adminConfig;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchConfigException(configId.toString());
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findByCompanyId(String companyId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM AdminConfig IN CLASS com.liferay.portlet.admin.ejb.AdminConfigHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				AdminConfigHBM adminConfigHBM = (AdminConfigHBM)itr.next();
				list.add(AdminConfigHBMUtil.model(adminConfigHBM));
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findByCompanyId(String companyId, int begin, int end)
		throws SystemException {
		return findByCompanyId(companyId, begin, end, null);
	}

	protected List findByCompanyId(String companyId, int begin, int end,
		OrderByComparator obc) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM AdminConfig IN CLASS com.liferay.portlet.admin.ejb.AdminConfigHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			List list = new ArrayList();

			if (getDialect().supportsLimit()) {
				q.setMaxResults(end - begin);
				q.setFirstResult(begin);

				Iterator itr = q.list().iterator();

				while (itr.hasNext()) {
					AdminConfigHBM adminConfigHBM = (AdminConfigHBM)itr.next();
					list.add(AdminConfigHBMUtil.model(adminConfigHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						AdminConfigHBM adminConfigHBM = (AdminConfigHBM)sr.get(0);
						list.add(AdminConfigHBMUtil.model(adminConfigHBM));

						if (!sr.next()) {
							break;
						}
					}
				}
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portlet.admin.model.AdminConfig findByCompanyId_First(
		String companyId, OrderByComparator obc)
		throws NoSuchConfigException, SystemException {
		List list = findByCompanyId(companyId, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchConfigException();
		}
		else {
			return (com.liferay.portlet.admin.model.AdminConfig)list.get(0);
		}
	}

	protected com.liferay.portlet.admin.model.AdminConfig findByCompanyId_Last(
		String companyId, OrderByComparator obc)
		throws NoSuchConfigException, SystemException {
		int count = countByCompanyId(companyId);
		List list = findByCompanyId(companyId, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchConfigException();
		}
		else {
			return (com.liferay.portlet.admin.model.AdminConfig)list.get(0);
		}
	}

	protected com.liferay.portlet.admin.model.AdminConfig[] findByCompanyId_PrevAndNext(
		String configId, String companyId, OrderByComparator obc)
		throws NoSuchConfigException, SystemException {
		com.liferay.portlet.admin.model.AdminConfig adminConfig = findByPrimaryKey(configId);
		int count = countByCompanyId(companyId);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM AdminConfig IN CLASS com.liferay.portlet.admin.ejb.AdminConfigHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			com.liferay.portlet.admin.model.AdminConfig[] array = new com.liferay.portlet.admin.model.AdminConfig[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					AdminConfigHBM adminConfigHBM = (AdminConfigHBM)sr.get(0);

					if (adminConfigHBM == null) {
						break;
					}

					com.liferay.portlet.admin.model.AdminConfig curAdminConfig = AdminConfigHBMUtil.model(adminConfigHBM);
					int value = obc.compare(adminConfig, curAdminConfig);

					if (value == 0) {
						if (!adminConfig.equals(curAdminConfig)) {
							break;
						}

						array[1] = curAdminConfig;

						if (sr.previous()) {
							array[0] = AdminConfigHBMUtil.model((AdminConfigHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = AdminConfigHBMUtil.model((AdminConfigHBM)sr.get(
										0));
						}

						break;
					}

					if (count == 1) {
						break;
					}

					count = (int)Math.ceil(count / 2.0);

					if (value < 0) {
						if (!sr.scroll(count * -1)) {
							break;
						}
					}
					else {
						if (!sr.scroll(count)) {
							break;
						}
					}
				}
			}

			return array;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findByC_T(String companyId, String type)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM AdminConfig IN CLASS com.liferay.portlet.admin.ejb.AdminConfigHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("type_ = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, type);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				AdminConfigHBM adminConfigHBM = (AdminConfigHBM)itr.next();
				list.add(AdminConfigHBMUtil.model(adminConfigHBM));
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findByC_T(String companyId, String type, int begin, int end)
		throws SystemException {
		return findByC_T(companyId, type, begin, end, null);
	}

	protected List findByC_T(String companyId, String type, int begin, int end,
		OrderByComparator obc) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM AdminConfig IN CLASS com.liferay.portlet.admin.ejb.AdminConfigHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("type_ = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, type);

			List list = new ArrayList();

			if (getDialect().supportsLimit()) {
				q.setMaxResults(end - begin);
				q.setFirstResult(begin);

				Iterator itr = q.list().iterator();

				while (itr.hasNext()) {
					AdminConfigHBM adminConfigHBM = (AdminConfigHBM)itr.next();
					list.add(AdminConfigHBMUtil.model(adminConfigHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						AdminConfigHBM adminConfigHBM = (AdminConfigHBM)sr.get(0);
						list.add(AdminConfigHBMUtil.model(adminConfigHBM));

						if (!sr.next()) {
							break;
						}
					}
				}
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portlet.admin.model.AdminConfig findByC_T_First(
		String companyId, String type, OrderByComparator obc)
		throws NoSuchConfigException, SystemException {
		List list = findByC_T(companyId, type, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchConfigException();
		}
		else {
			return (com.liferay.portlet.admin.model.AdminConfig)list.get(0);
		}
	}

	protected com.liferay.portlet.admin.model.AdminConfig findByC_T_Last(
		String companyId, String type, OrderByComparator obc)
		throws NoSuchConfigException, SystemException {
		int count = countByC_T(companyId, type);
		List list = findByC_T(companyId, type, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchConfigException();
		}
		else {
			return (com.liferay.portlet.admin.model.AdminConfig)list.get(0);
		}
	}

	protected com.liferay.portlet.admin.model.AdminConfig[] findByC_T_PrevAndNext(
		String configId, String companyId, String type, OrderByComparator obc)
		throws NoSuchConfigException, SystemException {
		com.liferay.portlet.admin.model.AdminConfig adminConfig = findByPrimaryKey(configId);
		int count = countByC_T(companyId, type);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM AdminConfig IN CLASS com.liferay.portlet.admin.ejb.AdminConfigHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("type_ = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, type);

			com.liferay.portlet.admin.model.AdminConfig[] array = new com.liferay.portlet.admin.model.AdminConfig[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					AdminConfigHBM adminConfigHBM = (AdminConfigHBM)sr.get(0);

					if (adminConfigHBM == null) {
						break;
					}

					com.liferay.portlet.admin.model.AdminConfig curAdminConfig = AdminConfigHBMUtil.model(adminConfigHBM);
					int value = obc.compare(adminConfig, curAdminConfig);

					if (value == 0) {
						if (!adminConfig.equals(curAdminConfig)) {
							break;
						}

						array[1] = curAdminConfig;

						if (sr.previous()) {
							array[0] = AdminConfigHBMUtil.model((AdminConfigHBM)sr.get(
										0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = AdminConfigHBMUtil.model((AdminConfigHBM)sr.get(
										0));
						}

						break;
					}

					if (count == 1) {
						break;
					}

					count = (int)Math.ceil(count / 2.0);

					if (value < 0) {
						if (!sr.scroll(count * -1)) {
							break;
						}
					}
					else {
						if (!sr.scroll(count)) {
							break;
						}
					}
				}
			}

			return array;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findAll() throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM AdminConfig IN CLASS com.liferay.portlet.admin.ejb.AdminConfigHBM ");

			Iterator itr = session.find(query.toString()).iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				AdminConfigHBM adminConfigHBM = (AdminConfigHBM)itr.next();
				list.add(AdminConfigHBMUtil.model(adminConfigHBM));
			}

			return list;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected void removeByCompanyId(String companyId)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM AdminConfig IN CLASS com.liferay.portlet.admin.ejb.AdminConfigHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				AdminConfigHBM adminConfigHBM = (AdminConfigHBM)itr.next();
				AdminConfigPool.remove((String)adminConfigHBM.getPrimaryKey());
				session.delete(adminConfigHBM);
			}

			session.flush();
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected void removeByC_T(String companyId, String type)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM AdminConfig IN CLASS com.liferay.portlet.admin.ejb.AdminConfigHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("type_ = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, type);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				AdminConfigHBM adminConfigHBM = (AdminConfigHBM)itr.next();
				AdminConfigPool.remove((String)adminConfigHBM.getPrimaryKey());
				session.delete(adminConfigHBM);
			}

			session.flush();
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected int countByCompanyId(String companyId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
				"FROM AdminConfig IN CLASS com.liferay.portlet.admin.ejb.AdminConfigHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			Iterator itr = q.list().iterator();

			if (itr.hasNext()) {
				Integer count = (Integer)itr.next();

				if (count != null) {
					return count.intValue();
				}
			}

			return 0;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected int countByC_T(String companyId, String type)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
				"FROM AdminConfig IN CLASS com.liferay.portlet.admin.ejb.AdminConfigHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("type_ = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, type);

			Iterator itr = q.list().iterator();

			if (itr.hasNext()) {
				Integer count = (Integer)itr.next();

				if (count != null) {
					return count.intValue();
				}
			}

			return 0;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}
}