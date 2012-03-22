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

package com.liferay.portal.ejb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.ObjectNotFoundException;
import net.sf.hibernate.Query;
import net.sf.hibernate.ScrollableResults;
import net.sf.hibernate.Session;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.HibernateUtil;
import com.liferay.util.dao.hibernate.OrderByComparator;

/**
 * <a href="UserPersistence.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.15 $
 *
 */
public class UserPersistence extends BasePersistence {
	protected com.liferay.portal.model.User create(String userId) {
		return new com.liferay.portal.model.User(userId);
	}

	protected com.liferay.portal.model.User remove(String userId)
		throws NoSuchUserException, SystemException {
		Session session = null;
	
		try {
			session = openSession();

	        User systemUser = null;
	        try {
	             systemUser = APILocator.getUserAPI().getSystemUser();
	        } catch (DotDataException e) {
	            // TODO Auto-generated catch block
	            throw new SystemException("Cannot find System User");
	        }
	        if(systemUser.getUserId().equals(userId)){
	            throw new NoSuchUserException(userId.toString());
	        }
			
			
			
			UserHBM userHBM = (UserHBM)session.load(UserHBM.class, userId);
			com.liferay.portal.model.User user = UserHBMUtil.model(userHBM);
			session.delete(userHBM);
			session.flush();
			UserPool.remove(userId);

			return user;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchUserException(userId.toString());
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.User update(
		com.liferay.portal.model.User user) throws SystemException {
		Session session = null;

		try {
			if (user.isNew() || user.isModified()) {
				session = openSession();

				if (user.isNew()) {
					UserHBM userHBM = new UserHBM(user.getUserId(),
							user.getCompanyId(), user.getPassword(),
							user.getPasswordEncrypted(),
							user.getPasswordExpirationDate(),
							user.getPasswordReset(), user.getFirstName(),
							user.getMiddleName(), user.getLastName(),
							user.getNickName(), user.getMale(),
							user.getBirthday(), user.getEmailAddress(),
							user.getSmsId(), user.getAimId(), user.getIcqId(),
							user.getMsnId(), user.getYmId(),
							user.getFavoriteActivity(),
							user.getFavoriteBibleVerse(),
							user.getFavoriteFood(), user.getFavoriteMovie(),
							user.getFavoriteMusic(), user.getLanguageId(),
							user.getTimeZoneId(), user.getSkinId(),
							user.getDottedSkins(), user.getRoundedSkins(),
							user.getGreeting(), user.getResolution(),
							user.getRefreshRate(), user.getLayoutIds(),
							user.getComments(), user.getCreateDate(),
							user.getLoginDate(), user.getLoginIP(),
							user.getLastLoginDate(), user.getLastLoginIP(),
							user.getFailedLoginAttempts(),
							user.getAgreedToTermsOfUse(), user.getActive());
					session.save(userHBM);
					session.flush();
				}
				else {
					try {
						UserHBM userHBM = (UserHBM)session.load(UserHBM.class,
								user.getPrimaryKey());
						userHBM.setCompanyId(user.getCompanyId());
						userHBM.setPassword(user.getPassword());
						userHBM.setPasswordEncrypted(user.getPasswordEncrypted());
						userHBM.setPasswordExpirationDate(user.getPasswordExpirationDate());
						userHBM.setPasswordReset(user.getPasswordReset());
						userHBM.setFirstName(user.getFirstName());
						userHBM.setMiddleName(user.getMiddleName());
						userHBM.setLastName(user.getLastName());
						userHBM.setNickName(user.getNickName());
						userHBM.setMale(user.getMale());
						userHBM.setBirthday(user.getBirthday());
						userHBM.setEmailAddress(user.getEmailAddress());
						userHBM.setSmsId(user.getSmsId());
						userHBM.setAimId(user.getAimId());
						userHBM.setIcqId(user.getIcqId());
						userHBM.setMsnId(user.getMsnId());
						userHBM.setYmId(user.getYmId());
						userHBM.setFavoriteActivity(user.getFavoriteActivity());
						userHBM.setFavoriteBibleVerse(user.getFavoriteBibleVerse());
						userHBM.setFavoriteFood(user.getFavoriteFood());
						userHBM.setFavoriteMovie(user.getFavoriteMovie());
						userHBM.setFavoriteMusic(user.getFavoriteMusic());
						userHBM.setLanguageId(user.getLanguageId());
						userHBM.setTimeZoneId(user.getTimeZoneId());
						userHBM.setSkinId(user.getSkinId());
						userHBM.setDottedSkins(user.getDottedSkins());
						userHBM.setRoundedSkins(user.getRoundedSkins());
						userHBM.setGreeting(user.getGreeting());
						userHBM.setResolution(user.getResolution());
						userHBM.setRefreshRate(user.getRefreshRate());
						userHBM.setLayoutIds(user.getLayoutIds());
						userHBM.setComments(user.getComments());
						userHBM.setCreateDate(user.getCreateDate());
						userHBM.setLoginDate(user.getLoginDate());
						userHBM.setLoginIP(user.getLoginIP());
						userHBM.setLastLoginDate(user.getLastLoginDate());
						userHBM.setLastLoginIP(user.getLastLoginIP());
						userHBM.setFailedLoginAttempts(user.getFailedLoginAttempts());
						userHBM.setAgreedToTermsOfUse(user.getAgreedToTermsOfUse());
						userHBM.setActive(user.getActive());
						session.flush();
					}
					catch (ObjectNotFoundException onfe) {
						UserHBM userHBM = new UserHBM(user.getUserId(),
								user.getCompanyId(), user.getPassword(),
								user.getPasswordEncrypted(),
								user.getPasswordExpirationDate(),
								user.getPasswordReset(), user.getFirstName(),
								user.getMiddleName(), user.getLastName(),
								user.getNickName(), user.getMale(),
								user.getBirthday(), user.getEmailAddress(),
								user.getSmsId(), user.getAimId(),
								user.getIcqId(), user.getMsnId(),
								user.getYmId(), user.getFavoriteActivity(),
								user.getFavoriteBibleVerse(),
								user.getFavoriteFood(),
								user.getFavoriteMovie(),
								user.getFavoriteMusic(), user.getLanguageId(),
								user.getTimeZoneId(), user.getSkinId(),
								user.getDottedSkins(), user.getRoundedSkins(),
								user.getGreeting(), user.getResolution(),
								user.getRefreshRate(), user.getLayoutIds(),
								user.getComments(), user.getCreateDate(),
								user.getLoginDate(), user.getLoginIP(),
								user.getLastLoginDate(), user.getLastLoginIP(),
								user.getFailedLoginAttempts(),
								user.getAgreedToTermsOfUse(), user.getActive());
						session.save(userHBM);
						session.flush();
					}
				}

				user.setNew(false);
				user.setModified(false);
				user.protect();
				UserPool.remove(user.getPrimaryKey());
				UserPool.put(user.getPrimaryKey(), user);
			}

			return user;
		}
		catch (HibernateException he) {
			throw new SystemException(he);
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected com.liferay.portal.model.User findByPrimaryKey(String userId)
		throws NoSuchUserException, SystemException {
		com.liferay.portal.model.User user = UserPool.get(userId);
		Session session = null;

		try {
			if (user == null) {
				session = openSession();

				UserHBM userHBM = (UserHBM)session.load(UserHBM.class, userId);
				user = UserHBMUtil.model(userHBM);
			}

			return user;
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchUserException(userId.toString());
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
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("firstName ASC").append(", ");
			query.append("middleName ASC").append(", ");
			query.append("lastName ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				UserHBM userHBM = (UserHBM)itr.next();
				list.add(UserHBMUtil.model(userHBM));
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
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("firstName ASC").append(", ");
				query.append("middleName ASC").append(", ");
				query.append("lastName ASC");
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
					UserHBM userHBM = (UserHBM)itr.next();
					list.add(UserHBMUtil.model(userHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						UserHBM userHBM = (UserHBM)sr.get(0);
						list.add(UserHBMUtil.model(userHBM));

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

	protected com.liferay.portal.model.User findByCompanyId_First(
		String companyId, OrderByComparator obc)
		throws NoSuchUserException, SystemException {
		List list = findByCompanyId(companyId, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchUserException();
		}
		else {
			return (com.liferay.portal.model.User)list.get(0);
		}
	}

	protected com.liferay.portal.model.User findByCompanyId_Last(
		String companyId, OrderByComparator obc)
		throws NoSuchUserException, SystemException {
		int count = countByCompanyId(companyId);
		List list = findByCompanyId(companyId, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchUserException();
		}
		else {
			return (com.liferay.portal.model.User)list.get(0);
		}
	}

	protected com.liferay.portal.model.User[] findByCompanyId_PrevAndNext(
		String userId, String companyId, OrderByComparator obc)
		throws NoSuchUserException, SystemException {
		com.liferay.portal.model.User user = findByPrimaryKey(userId);
		int count = countByCompanyId(companyId);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("firstName ASC").append(", ");
				query.append("middleName ASC").append(", ");
				query.append("lastName ASC");
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			com.liferay.portal.model.User[] array = new com.liferay.portal.model.User[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					UserHBM userHBM = (UserHBM)sr.get(0);

					if (userHBM == null) {
						break;
					}

					com.liferay.portal.model.User curUser = UserHBMUtil.model(userHBM);
					int value = obc.compare(user, curUser);

					if (value == 0) {
						if (!user.equals(curUser)) {
							break;
						}

						array[1] = curUser;

						if (sr.previous()) {
							array[0] = UserHBMUtil.model((UserHBM)sr.get(0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = UserHBMUtil.model((UserHBM)sr.get(0));
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

	protected com.liferay.portal.model.User findByC_U(String companyId,
		String userId) throws NoSuchUserException, SystemException {
		Session session = null;

		User u = UserPool.get(userId);
		if(u != null && u.getCompanyId().equals(companyId)){
			return u;
		}
		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("userId = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("firstName ASC").append(", ");
			query.append("middleName ASC").append(", ");
			query.append("lastName ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, userId);

			Iterator itr = q.list().iterator();

			if (!itr.hasNext()) {
				throw new ObjectNotFoundException(null, UserHBM.class);
			}

			UserHBM userHBM = (UserHBM)itr.next();

			return UserHBMUtil.model(userHBM);
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchUserException();
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected List findByC_P(String companyId, String password)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("password_ = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("firstName ASC").append(", ");
			query.append("middleName ASC").append(", ");
			query.append("lastName ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, password);

			Iterator itr = q.list().iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				UserHBM userHBM = (UserHBM)itr.next();
				list.add(UserHBMUtil.model(userHBM));
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

	protected List findByC_P(String companyId, String password, int begin,
		int end) throws SystemException {
		return findByC_P(companyId, password, begin, end, null);
	}

	protected List findByC_P(String companyId, String password, int begin,
		int end, OrderByComparator obc) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("password_ = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("firstName ASC").append(", ");
				query.append("middleName ASC").append(", ");
				query.append("lastName ASC");
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, password);

			List list = new ArrayList();

			if (getDialect().supportsLimit()) {
				q.setMaxResults(end - begin);
				q.setFirstResult(begin);

				Iterator itr = q.list().iterator();

				while (itr.hasNext()) {
					UserHBM userHBM = (UserHBM)itr.next();
					list.add(UserHBMUtil.model(userHBM));
				}
			}
			else {
				ScrollableResults sr = q.scroll();

				if (sr.first() && sr.scroll(begin)) {
					for (int i = begin; i < end; i++) {
						UserHBM userHBM = (UserHBM)sr.get(0);
						list.add(UserHBMUtil.model(userHBM));

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

	protected com.liferay.portal.model.User findByC_P_First(String companyId,
		String password, OrderByComparator obc)
		throws NoSuchUserException, SystemException {
		List list = findByC_P(companyId, password, 0, 1, obc);

		if (list.size() == 0) {
			throw new NoSuchUserException();
		}
		else {
			return (com.liferay.portal.model.User)list.get(0);
		}
	}

	protected com.liferay.portal.model.User findByC_P_Last(String companyId,
		String password, OrderByComparator obc)
		throws NoSuchUserException, SystemException {
		int count = countByC_P(companyId, password);
		List list = findByC_P(companyId, password, count - 1, count, obc);

		if (list.size() == 0) {
			throw new NoSuchUserException();
		}
		else {
			return (com.liferay.portal.model.User)list.get(0);
		}
	}

	protected com.liferay.portal.model.User[] findByC_P_PrevAndNext(
		String userId, String companyId, String password, OrderByComparator obc)
		throws NoSuchUserException, SystemException {
		com.liferay.portal.model.User user = findByPrimaryKey(userId);
		int count = countByC_P(companyId, password);
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("password_ = ?");
			query.append(" ");

			if (obc != null) {
				query.append("ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append("ORDER BY ");
				query.append("firstName ASC").append(", ");
				query.append("middleName ASC").append(", ");
				query.append("lastName ASC");
			}

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, password);

			com.liferay.portal.model.User[] array = new com.liferay.portal.model.User[3];
			ScrollableResults sr = q.scroll();

			if (sr.first()) {
				while (true) {
					UserHBM userHBM = (UserHBM)sr.get(0);

					if (userHBM == null) {
						break;
					}

					com.liferay.portal.model.User curUser = UserHBMUtil.model(userHBM);
					int value = obc.compare(user, curUser);

					if (value == 0) {
						if (!user.equals(curUser)) {
							break;
						}

						array[1] = curUser;

						if (sr.previous()) {
							array[0] = UserHBMUtil.model((UserHBM)sr.get(0));
						}

						sr.next();

						if (sr.next()) {
							array[2] = UserHBMUtil.model((UserHBM)sr.get(0));
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

	protected com.liferay.portal.model.User findByC_EA(String companyId,
		String emailAddress) throws NoSuchUserException, SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("emailAddress = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("firstName ASC").append(", ");
			query.append("middleName ASC").append(", ");
			query.append("lastName ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, emailAddress);

			Iterator itr = q.list().iterator();

			if (!itr.hasNext()) {
				throw new ObjectNotFoundException(null, UserHBM.class);
			}

			UserHBM userHBM = (UserHBM)itr.next();

			return UserHBMUtil.model(userHBM);
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchUserException();
			}
			else {
				throw new SystemException(he);
			}
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
			query.append("FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM ");
			query.append("ORDER BY ");
			query.append("firstName ASC").append(", ");
			query.append("middleName ASC").append(", ");
			query.append("lastName ASC");

			Iterator itr = session.find(query.toString()).iterator();
			List list = new ArrayList();

			while (itr.hasNext()) {
				UserHBM userHBM = (UserHBM)itr.next();
				list.add(UserHBMUtil.model(userHBM));
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
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("firstName ASC").append(", ");
			query.append("middleName ASC").append(", ");
			query.append("lastName ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);

			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				UserHBM userHBM = (UserHBM)itr.next();
				UserPool.remove((String)userHBM.getPrimaryKey());
				session.delete(userHBM);
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

	protected void removeByC_U(String companyId, String userId)
		throws NoSuchUserException, SystemException {
		Session session = null;
        User systemUser = null;
        try {
            systemUser = APILocator.getUserAPI().getSystemUser();
       } catch (DotDataException e) {
           // TODO Auto-generated catch block
           throw new SystemException("Cannot find System User");
       }
		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("userId = ?");
            query.append(" AND ");
            query.append(" userId <> ? ");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("firstName ASC").append(", ");
			query.append("middleName ASC").append(", ");
			query.append("lastName ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, userId);
            q.setString(queryPos++, systemUser.getUserId());
			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				UserHBM userHBM = (UserHBM)itr.next();
				UserPool.remove((String)userHBM.getPrimaryKey());
				session.delete(userHBM);
			}

			session.flush();
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchUserException();
			}
			else {
				throw new SystemException(he);
			}
		}
		finally {
			HibernateUtil.closeSession(session);
		}
	}

	protected void removeByC_P(String companyId, String password)
		throws SystemException {
		Session session = null;
		User systemUser = null;
        try {
            systemUser = APILocator.getUserAPI().getSystemUser();
       } catch (DotDataException e) {
           // TODO Auto-generated catch block
           throw new SystemException("Cannot find System User");
       }
		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("password_ = ?");
            query.append(" AND ");
            query.append("userId <> ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("firstName ASC").append(", ");
			query.append("middleName ASC").append(", ");
			query.append("lastName ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, password);
            q.setString(queryPos++, systemUser.getUserId());
            
			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				UserHBM userHBM = (UserHBM)itr.next();
				UserPool.remove((String)userHBM.getPrimaryKey());
				session.delete(userHBM);
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

	protected void removeByC_EA(String companyId, String emailAddress)
		throws NoSuchUserException, SystemException {
		Session session = null;
		User systemUser = null;
		try {
             systemUser = APILocator.getUserAPI().getSystemUser();
        } catch (DotDataException e) {
            // TODO Auto-generated catch block
            throw new SystemException("Cannot find System User");
        }
		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("emailAddress = ?");
            query.append(" AND ");
            query.append("userId <> ?");
			query.append(" ");
			query.append("ORDER BY ");
			query.append("firstName ASC").append(", ");
			query.append("middleName ASC").append(", ");
			query.append("lastName ASC");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
            q.setString(queryPos++, emailAddress);
            q.setString(queryPos++, systemUser.getUserId());


			Iterator itr = q.list().iterator();

			while (itr.hasNext()) {
				UserHBM userHBM = (UserHBM)itr.next();
				UserPool.remove((String)userHBM.getPrimaryKey());
				session.delete(userHBM);
			}

			session.flush();
		}
		catch (HibernateException he) {
			if (he instanceof ObjectNotFoundException) {
				throw new NoSuchUserException();
			}
			else {
				throw new SystemException(he);
			}
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
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
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

	protected int countByC_U(String companyId, String userId)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("userId = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, userId);

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

	protected int countByC_P(String companyId, String password)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("password_ = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, password);

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

	protected int countByC_EA(String companyId, String emailAddress)
		throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
				"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND ");
			query.append("emailAddress = ?");
			query.append(" ");

			Query q = session.createQuery(query.toString());
			int queryPos = 0;
			q.setString(queryPos++, companyId);
			q.setString(queryPos++, emailAddress);

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