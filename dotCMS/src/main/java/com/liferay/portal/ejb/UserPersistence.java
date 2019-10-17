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

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.user.LiferayUserTransformer;
import com.dotcms.util.user.UserTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.util.dao.hibernate.OrderByComparator;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;
import com.dotcms.repackage.net.sf.hibernate.ObjectNotFoundException;
import com.dotcms.repackage.net.sf.hibernate.Query;
import com.dotcms.repackage.net.sf.hibernate.ScrollableResults;
import com.dotcms.repackage.net.sf.hibernate.Session;

/**
 * <a href="UserPersistence.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.15 $
 *
 */
public class UserPersistence extends BasePersistence {

	private static final String SEARCH_USER_BY_ID="select * from user_ where userid=?";
	private static final UserTransformer userTransformer = new LiferayUserTransformer();

	protected com.liferay.portal.model.User create(String userId) {
		return new com.liferay.portal.model.User(userId);
	}

	@WrapInTransaction
	protected com.liferay.portal.model.User remove(String userId)
			throws NoSuchUserException, SystemException {

		try {


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



			UserHBM userHBM = (UserHBM) HibernateUtil.load(UserHBM.class, userId);
			com.liferay.portal.model.User user = UserHBMUtil.model(userHBM);
			HibernateUtil.delete(userHBM);
			UserPool.remove(userId);

			return user;
		}
		catch (DotHibernateException he) {
		    throw new SystemException(he);
			
		}

	}
	@WrapInTransaction
	protected com.liferay.portal.model.User update(
			com.liferay.portal.model.User user) throws SystemException {
	


			if (user.isNew() || user.isModified()) {
	
				UserHBM userHBM = null;
				if (user.isNew()) {
					userHBM = new UserHBM(user.getUserId(),
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
							user.getAgreedToTermsOfUse(), user.getActive(),
							user.getDeleteInProgress(), user.getDeleteDate());


				}
				else {
					try {
						userHBM = (UserHBM)HibernateUtil.load(UserHBM.class,
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
						userHBM.setDeleteInProgress(user.getDeleteInProgress());
						userHBM.setDeleteDate(user.getDeleteDate());

	
					}
					catch (DotHibernateException onfe) {
						userHBM = new UserHBM(user.getUserId(),
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
								user.getAgreedToTermsOfUse(), user.getActive(),
								user.getDeleteInProgress(), user.getDeleteDate());


	
					} 
				}
                userHBM.setModDate(new Date());
                try {
                    com.dotmarketing.db.HibernateUtil.save(userHBM);
                }
                catch(Exception e) {
                    throw new DotStateException(e);
                }
				user.setNew(false);
				user.setModified(false);
				user.protect();
				UserPool.remove(user.getPrimaryKey());
				UserPool.put(user.getPrimaryKey(), user);
			}

			return user;
		
	

	}

	@CloseDBIfOpened
	protected com.liferay.portal.model.User findByPrimaryKey(String userId)
			throws NoSuchUserException {
		com.liferay.portal.model.User user = UserPool.get(userId);

		try {
			if (user == null) {
				DotConnect dc = new DotConnect();
				dc.setSQL(SEARCH_USER_BY_ID);
				dc.addParam(userId);
				List<Map<String, Object>> list = dc.loadObjectResults();
				if(list.isEmpty()) {
					throw new NoSuchUserException(userId);
				}else{
					user = userTransformer.fromMap(list.get(0));
					UserPool.put(user.getPrimaryKey(), user);
				}
			}

			return user;
		}
		catch(DotDataException e){
			throw new NoSuchUserException(userId);
		}
	}

	@CloseDBIfOpened
	protected List findByCompanyId(String companyId) throws SystemException {


		try {


			StringBuffer query = new StringBuffer();
			query.append(
					"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");

			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());

			query.append(" ORDER BY ");
			query.append("firstName ASC").append(", ");
			query.append("middleName ASC").append(", ");
			query.append("lastName ASC");

			HibernateUtil util = new HibernateUtil();
			util.setQuery(query.toString());
			util.setParam(companyId);
			
			


			return util.list();
		}
		catch (DotHibernateException e) {
		    throw new SystemException(e);
        }

	}

	protected List findByCompanyId(String companyId, int begin, int end)
			throws SystemException {
		return findByCompanyId(companyId, begin, end, null);
	}
	@CloseDBIfOpened
	protected List findByCompanyId(String companyId, int begin, int end,
			OrderByComparator obc) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
					"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");

			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());

			if (obc != null) {
				query.append(" ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append(" ORDER BY ");
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

	@CloseDBIfOpened
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

			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());

			if (obc != null) {
				query.append(" ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append(" ORDER BY ");
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

	}
	@CloseDBIfOpened
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

			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());

			query.append(" ORDER BY ");
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

	}
	@CloseDBIfOpened
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
			query.append(getPasswordCriteria());
			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());
			query.append(" ORDER BY ");
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

	}

	protected List findByC_P(String companyId, String password, int begin,
			int end) throws SystemException {
		return findByC_P(companyId, password, begin, end, null);
	}
	@CloseDBIfOpened
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
			query.append(getPasswordCriteria());

			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());

			if (obc != null) {
				query.append(" ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append(" ORDER BY ");
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
	@CloseDBIfOpened
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
			query.append(getPasswordCriteria());
			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());

			if (obc != null) {
				query.append(" ORDER BY " + obc.getOrderBy());
			}
			else {
				query.append(" ORDER BY ");
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

	}
	@CloseDBIfOpened
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
			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());

			query.append(" ORDER BY ");
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

	}
	@CloseDBIfOpened
	protected List findAll() throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM ");
			query.append(" WHERE delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());
			query.append(" ORDER BY ");
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

	}
	@WrapInTransaction
	protected void removeByCompanyId(String companyId)
			throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append(
					"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());
			query.append(" ORDER BY ");
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

	}
	@WrapInTransaction
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
			query.append(" userId <> ?");
			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());
			query.append(" ORDER BY ");
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

	}
	@WrapInTransaction
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
			query.append(getPasswordCriteria());
			query.append(" AND ");
			query.append("userId <> ?");
			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());
			query.append(" ORDER BY ");
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

	}
	@WrapInTransaction
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
			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());
			query.append(" ORDER BY ");
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

	}
	@CloseDBIfOpened
	protected int countByCompanyId(String companyId) throws SystemException {
		Session session = null;

		try {
			session = openSession();

			StringBuffer query = new StringBuffer();
			query.append("SELECT COUNT(*) ");
			query.append(
					"FROM User_ IN CLASS com.liferay.portal.ejb.UserHBM WHERE ");
			query.append("companyId = ?");
			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());

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

	}
	@CloseDBIfOpened
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
			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());

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

	}
	@CloseDBIfOpened
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

			query.append(getPasswordCriteria());

			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());

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

	}
	@CloseDBIfOpened
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
			query.append(" AND delete_in_progress = ");
			query.append(DbConnectionFactory.getDBFalse());

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

	}
	@CloseDBIfOpened
	private String getPasswordCriteria(){
		if (DbConnectionFactory.isOracle()){
			return "dbms_lob.compare(password_, ?) = 0";
		}else if (DbConnectionFactory.isMsSql()){
			return "cast(password_ AS varchar(max)) = ?";
		} else{
			return "password_ = ?";
		}
	}
}