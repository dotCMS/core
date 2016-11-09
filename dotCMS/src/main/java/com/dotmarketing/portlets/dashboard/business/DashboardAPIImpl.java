package com.dotmarketing.portlets.dashboard.business;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.FactoryLocator;
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

public class DashboardAPIImpl implements DashboardAPI{
	
	static DashboardFactory dashboardFactory = FactoryLocator.getDashboardFactory();


	public List<DashboardSummary404> get404s(String userId, String hostId,
			boolean showIgnored, Date fromDate, Date toDate, int limit,
			int offset, String sortBy) throws DotDataException,
			DotHibernateException {
		return dashboardFactory.get404s(userId, hostId, showIgnored, fromDate, toDate, limit, offset, sortBy);
	}

	public DashboardSummary getDashboardSummary(String hostId, Date fromDate,
			Date toDate) throws DotDataException, DotHibernateException {
		return dashboardFactory.getDashboardSummary(hostId, fromDate, toDate);
	}

	public List<DashboardSummaryVisits> getDashboardSummaryVisits(
			String hostId, ViewType viewType, Date fromDate, Date toDate) throws DotDataException,
			DotHibernateException {
		return dashboardFactory.getDashboardSummaryVisits(hostId, viewType, fromDate, toDate);
	}

	public List<Host> getHostList(User user, boolean includeArchived,
			Map<String, Object> params, int limit, int offset, String sortBy)
			throws DotDataException, DotHibernateException {
		return dashboardFactory.getHostList(user, includeArchived, params, limit, offset, sortBy);
	}

	
	public List<DashboardSummaryContent> getTopContent(String hostId,
			Date fromDate, Date toDate, int limit, int offset, String sortBy)
			throws DotDataException, DotHibernateException {
		return dashboardFactory.getTopContent(hostId, fromDate, toDate, limit, offset, sortBy);
	}

	

	public List<DashboardSummaryPage> getTopPages(String hostId, Date fromDate,
			Date toDate, int limit, int offset, String sortBy)
			throws DotDataException, DotHibernateException {
		return dashboardFactory.getTopPages(hostId, fromDate, toDate, limit, offset, sortBy);
	}

	

	public List<DashboardSummaryReferer> getTopReferers(String hostId,
			Date fromDate, Date toDate, int limit, int offset, String sortBy)
			throws DotDataException, DotHibernateException {
		return dashboardFactory.getTopReferers(hostId, fromDate, toDate, limit, offset, sortBy);
	}
	

	public List<DashboardWorkStream> getWorkStreamList(User user,
			String hostId, String userId, Date fromDate, Date toDate,
			int limit, int offset, String sortBy) throws DotDataException,
			DotHibernateException {
		return dashboardFactory.getWorkStreamList(user, hostId, userId, fromDate, toDate, limit, offset, sortBy);
	}
	
	public void setIgnored(User user, long id, boolean ignored)
			throws DotDataException, DotHibernateException {
		dashboardFactory.setIgnored(user, id, ignored);
	}

	public long get404Count(String userId, String hostId, boolean showIgnored,
			Date fromDate, Date toDate) throws DotDataException,
			DotHibernateException {
		return dashboardFactory.get404Count(userId, hostId, showIgnored, fromDate, toDate);
	}

	public long getHostListCount(User user, boolean includeArchived,
			Map<String, Object> params) throws DotDataException,
			DotHibernateException {
		return dashboardFactory.getHostListCount(user, includeArchived, params);
	}

	public long getTopContentCount(String hostId, Date fromDate, Date toDate)
			throws DotDataException, DotHibernateException {
	    return dashboardFactory.getTopContentCount(hostId, fromDate, toDate);
	}

	public long getTopPagesCount(String hostId, Date fromDate, Date toDate)
			throws DotDataException, DotHibernateException {
		 return dashboardFactory.getTopPagesCount(hostId, fromDate, toDate);
	}

	public long getTopReferersCount(String hostId, Date fromDate, Date toDate)
			throws DotDataException, DotHibernateException {
		 return dashboardFactory.getTopReferersCount(hostId, fromDate, toDate);
	}

	public long getWorkStreamListCount(User user, String hostId, String userId,
			Date fromDate, Date toDate) throws DotDataException,
			DotHibernateException {
		 return dashboardFactory.getWorkStreamListCount(user, hostId, userId, fromDate, toDate);
	}

	public List<TopAsset> getTopAssets(User user, String hostId)
			throws DotDataException {
		return dashboardFactory.getTopAssets(user, hostId);
	}

	public void populateAnalyticSummaryTables() {
		dashboardFactory.populateAnalyticSummaryTables();
	}

	public int checkPeriodData(int month, int year) {
		
		if(month==0 || year==0){
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			if(month==0){
				month = cal.get(Calendar.MONTH)+1;
			}
			if(year==0){
				year = cal.get(Calendar.YEAR);
			}
		}
		return dashboardFactory.checkPeriodData(month, year);
		
	}



}