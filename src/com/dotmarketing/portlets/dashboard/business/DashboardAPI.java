package com.dotmarketing.portlets.dashboard.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.dashboard.model.DashboardSummary;
import com.dotmarketing.portlets.dashboard.model.DashboardSummary404;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryContent;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryPage;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryReferer;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryVisits;
import com.dotmarketing.portlets.dashboard.model.DashboardWorkStream;
import com.dotmarketing.portlets.dashboard.model.TopAsset;
import com.dotmarketing.portlets.dashboard.model.ViewType;
import com.liferay.portal.model.User;

public interface DashboardAPI {

		
		/**
		 * 
		 * @param user
		 * @param includeArchived
		 * @param params
		 * @param limit
		 * @param offset
		 * @param sortBy
		 * @return
		 * @throws DotDataException
		 * @throws DotHibernateException
		 */
		public List<Host> getHostList(User user, boolean includeArchived, Map<String, Object> params, int limit, int offset, String sortBy) throws DotDataException, DotHibernateException;
		
		
	    /**
	     * 
	     * @param user
	     * @param includeArchived
	     * @param params
	     * @return
	     * @throws DotDataException
	     * @throws DotHibernateException
	     */
		public long getHostListCount(User user, boolean includeArchived, Map<String, Object> params) throws DotDataException, DotHibernateException;
		
		/**
		 * 
		 * @param user
		 * @param hostId
		 * @param userId
		 * @param fromDate
		 * @param toDate
		 * @param limit
		 * @param offset
		 * @param sortBy
		 * @return
		 * @throws DotDataException
		 * @throws DotHibernateException
		 */
		public List<DashboardWorkStream> getWorkStreamList(User user, String hostId, String userId, Date fromDate, Date toDate, int limit, int offset, String sortBy)throws DotDataException,DotHibernateException;
		
		
	   /**
	    * 
	    * @param user
	    * @param hostId
	    * @param userId
	    * @param fromDate
	    * @param toDate
	    * @return
	    * @throws DotDataException
	    * @throws DotHibernateException
	    */
		public long getWorkStreamListCount(User user, String hostId, String userId, Date fromDate, Date toDate)throws DotDataException,DotHibernateException;
		
		/**
		 * 
		 * @param hostId
		 * @param fromDate
		 * @param toDate
		 * @return
		 * @throws DotDataException
		 * @throws DotHibernateException
		 */
		public DashboardSummary getDashboardSummary(String hostId, Date fromDate, Date toDate) throws DotDataException, DotHibernateException;
		
		/**
		 * 
		 * @param hostId
		 * @param fromDate
		 * @param toDate
		 * @return
		 * @throws DotDataException
		 * @throws DotHibernateException
		 */
		public List<DashboardSummaryVisits> getDashboardSummaryVisits(String hostId, ViewType viewType,  Date fromDate, Date toDate) throws DotDataException, DotHibernateException;
		
		
		/**
		 * 
		 * @param hostId
		 * @param fromDate
		 * @param toDate
		 * @param limit
		 * @param offset
		 * @param sortBy
		 * @return
		 * @throws DotDataException
		 * @throws DotHibernateException
		 */
		public List<DashboardSummaryReferer> getTopReferers(String hostId, Date fromDate, Date toDate, int limit, int offset, String sortBy) throws DotDataException, DotHibernateException;
		
		
		/**
		 * 
		 * @param hostId
		 * @param fromDate
		 * @param toDate
		 * @return
		 * @throws DotDataException
		 * @throws DotHibernateException
		 */
		public long getTopReferersCount(String hostId, Date fromDate, Date toDate) throws DotDataException, DotHibernateException;
		
		/**
		 * 
		 * @param hostId
		 * @param fromDate
		 * @param toDate
		 * @param limit
		 * @param offset
		 * @param sortBy
		 * @return
		 * @throws DotDataException
		 * @throws DotHibernateException
		 */
		public List<DashboardSummaryPage> getTopPages(String hostId, Date fromDate, Date toDate, int limit, int offset, String sortBy) throws DotDataException, DotHibernateException;
		
		/**
		 * 
		 * @param hostId
		 * @param fromDate
		 * @param toDate
		 * @return
		 * @throws DotDataException
		 * @throws DotHibernateException
		 */
		public long getTopPagesCount(String hostId, Date fromDate, Date toDate) throws DotDataException, DotHibernateException;
		
		/**
		 * 
		 * @param hostId
		 * @param fromDate
		 * @param toDate
		 * @param limit
		 * @param offset
		 * @param sortBy
		 * @return
		 * @throws DotDataException
		 * @throws DotHibernateException
		 */
		public List<DashboardSummaryContent> getTopContent(String hostId, Date fromDate, Date toDate, int limit, int offset, String sortBy) throws DotDataException, DotHibernateException;
		
        /**
         * 
         * @param hostId
         * @param fromDate
         * @param toDate
         * @return
         * @throws DotDataException
         * @throws DotHibernateException
         */
		public long getTopContentCount(String hostId, Date fromDate, Date toDate) throws DotDataException, DotHibernateException;
		
		/**
		 * 
		 * @param user
		 * @param hostId
		 * @param showIgnored
		 * @param fromDate
		 * @param toDate
		 * @param limit
		 * @param offset
		 * @param sortBy
		 * @return
		 * @throws DotDataException
		 * @throws DotHibernateException
		 */
		public List<DashboardSummary404> get404s(String userId, String hostId, boolean showIgnored, Date fromDate, Date toDate, int limit, int offset, String sortBy) throws DotDataException, DotHibernateException;
		
		
		/**
		 * 
		 * @param user
		 * @param hostId
		 * @param showIgnored
		 * @param fromDate
		 * @param toDate
		 * @return
		 * @throws DotDataException
		 * @throws DotHibernateException
		 */
		public long get404Count(String userId, String hostId, boolean showIgnored, Date fromDate, Date toDate) throws DotDataException, DotHibernateException;
		
		/**
		 * 
		 * @param user
		 * @param id
		 * @param ignored
		 * @throws DotDataException
		 * @throws DotHibernateException
		 */
		public void setIgnored(User user, long id, boolean ignored) throws DotDataException, DotHibernateException;
		


		/**
		 * 
		 * @param user
		 * @param hostId
		 * @return
		 * @throws DotDataException
		 * @throws DotSecurityException
		 */
		public List<TopAsset> getTopAssets(User user,String hostId) throws DotDataException;
		
		
		/**
		 * 
		 */
		public void populateAnalyticSummaryTables();
		
		
		/**
		 * 
		 * @param month
		 * @param year
		 */
		public int checkPeriodData(int month, int year);
		
		
	}


