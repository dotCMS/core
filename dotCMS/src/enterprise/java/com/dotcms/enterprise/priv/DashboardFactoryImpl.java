/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.priv;

import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.repackage.org.apache.commons.httpclient.util.URIUtil;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.dashboard.business.DashboardFactory;
import com.dotmarketing.portlets.dashboard.model.DashboardSummary;
import com.dotmarketing.portlets.dashboard.model.DashboardSummary404;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryContent;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryPage;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryPeriod;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryReferer;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryVisits;
import com.dotmarketing.portlets.dashboard.model.DashboardUserPreferences;
import com.dotmarketing.portlets.dashboard.model.DashboardWorkStream;
import com.dotmarketing.portlets.dashboard.model.HostWrapper;
import com.dotmarketing.portlets.dashboard.model.TopAsset;
import com.dotmarketing.portlets.dashboard.model.ViewType;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class DashboardFactoryImpl extends DashboardFactory {

	String runDashboardFieldContentlet = "";

	public DashboardFactoryImpl(){
		Structure hostStructure = null;
		try{
		  hostStructure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
		  if(hostStructure!=null && UtilMethods.isSet(hostStructure.getInode())){
			  Field runDashboardField =  FieldFactory.getFieldByVariableName(hostStructure.getInode(), "runDashboard");
			  if(runDashboardField!=null){
				  runDashboardFieldContentlet = runDashboardField.getFieldContentlet();
			  }
		  }
		}catch(Exception e){
			Logger.error(DashboardFactoryImpl.class, "unable to get runDashboard field contentlet:" + e, e);
		}
	}

	private static final SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");


	private static final String DASHBOARD_SUMMARY_DATES  = (DbConnectionFactory.isPostgres()|| DbConnectionFactory.isOracle())?
			" SELECT dd,mm,yyyy from ( select  extract (day from timestampper) as dd, extract (month from timestampper) as mm, extract (year from timestampper) as yyyy from clickstream_404 "+
			" UNION ALL select  extract (day from timestampper) as dd, extract (month from timestampper) as mm, extract (year from timestampper) as yyyy from clickstream_request )dates "+
			" where (yyyy > (select coalesce(extract (year from max(full_date)),0) from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) or (yyyy = (select coalesce(extract (year from max(full_date)),0) from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) "+
			" and (mm > (select coalesce(extract (month from max(full_date)),0) from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) or (mm = (select coalesce(extract (month from max(full_date)),0) from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) "+
		    " and (dd > (select coalesce(extract (day from max(full_date)),0) from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) ))))) group by dd,mm,yyyy order by yyyy,mm,dd asc "
			:DbConnectionFactory.isMySql()?
					" SELECT dd,mm,yyyy from ( select  DAY (timestampper) as dd, MONTH( timestampper) as mm, YEAR(timestampper) as yyyy from clickstream_404 "+
					" UNION ALL select  DAY (timestampper) as dd, MONTH( timestampper) as mm, YEAR(timestampper) as yyyy from clickstream_request)dates "+
					" where (yyyy > (select coalesce(YEAR(max(full_date)),0) from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) or (yyyy = (select coalesce(YEAR(max(full_date)),0) from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) "+
					" and (mm > (select coalesce(MONTH(max(full_date)),0) from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) or (mm = (select coalesce(MONTH(max(full_date)),0) from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) "+
				    " and (dd > (select coalesce(DAY(max(full_date)),0) from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) ))))) group by dd,mm,yyyy order by yyyy,mm,dd asc "
					:DbConnectionFactory.isMsSql()?
							" SELECT dd,mm,yyyy from ( select  DATEPART(day , timestampper) as dd, DATEPART(month, timestampper) as mm, DATEPART(year,timestampper) as yyyy from clickstream_404 "+
							" UNION ALL select  DATEPART(day,timestampper) as dd, DATEPART(month,timestampper) as mm, DATEPART(year, timestampper) as yyyy from clickstream_request )dates "+
							" where (yyyy > (select coalesce(DATEPART(year,max(full_date)),0)  from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) or (yyyy = (select coalesce(DATEPART(year,max(full_date)),0)  from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) "+
							" and (mm > (select coalesce(DATEPART(month,max(full_date)),0)  from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) or (mm = (select coalesce(DATEPART(month,max(full_date)),0) from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) "+
						    " and (dd > (select coalesce(DATEPART(day,max(full_date)),0) from analytic_summary_period, analytic_summary where analytic_summary.summary_period_id = analytic_summary_period.id and analytic_summary.host_id = ?) ))))) group by dd,mm,yyyy order by yyyy,mm,dd asc ":"";


	private static final String DASHBOARD_SUMMARY_VISITS = (DbConnectionFactory.isPostgres() || DbConnectionFactory.isOracle())?
			" select count(distinct clickstream_id) as totalvisits, extract (hour from timestampper ) as visithour, host_id  from clickstream_request "+
			" where extract(day from timestampper) = ? and extract(month from timestampper) = ? "+
			" and extract(year from timestampper) = ? and host_id = ? group by extract (hour from timestampper ), host_id "+
			" order by extract (hour from timestampper ) asc"
			:DbConnectionFactory.isMySql()?
					" select count(distinct clickstream_id) as totalvisits, HOUR(timestampper ) as visithour, host_id  from clickstream_request "+
					" where DAY(timestampper) = ? and MONTH(timestampper) = ? "+
					" and YEAR(timestampper) = ? and host_id = ? group by HOUR(timestampper ), host_id "+
					" order by HOUR(timestampper ) asc "
					:DbConnectionFactory.isMsSql()?
							" select count(distinct clickstream_id) as totalvisits, DATEPART(hour,timestampper ) as visithour, host_id  from clickstream_request "+
							" where DATEPART(day,timestampper) = ? and DATEPART(month,timestampper) = ? "+
							" and DATEPART(year,timestampper) = ? and host_id = ? group by DATEPART(hour,timestampper ), host_id "+
							" order by DATEPART(hour,timestampper ) asc":"";

	private static final String DASHBOARD_SUMMARY_404 = " select request_uri, referer_uri, host_id  from clickstream_404 "+
			" where timestampper > ? and timestampper < ? " +
			" and not exists( "+
            "  select analytic_summary_404.id from analytic_summary_404,analytic_summary_period summaryPeriod "+
            "  where coalesce(analytic_summary_404.uri,'no_data') = coalesce(clickstream_404.request_uri,'no_data')  "+
            "  and coalesce(analytic_summary_404.referer_uri, 'no_data') = coalesce(clickstream_404.referer_uri, 'no_data') "+
            "  and analytic_summary_404.host_id = clickstream_404.host_id " +
            "  and summary_period_id=summaryPeriod.id " +
            "  and summaryPeriod.full_date > ? " +
            "  and summaryPeriod.full_date < ?) " +
            "  and host_id = ? " +
			" group by request_uri,referer_uri,host_id ";


    private static final String DASHBOARD_SUMMARY_REFERERS = DbConnectionFactory.isOracle() ?//In Oracle empty strings are treated as nulls, so a "<> ''" will cause problem...
            " select coalesce(count(*),0) as hits, referer as uri from clickstream where clickstream_id in( " +
                    " select distinct clickstream_id from clickstream_request where timestampper > ? and timestampper < ?) and host_id = ? and referer IS NOT NULL group by referer "
            :
            " select coalesce(count(*),0) as hits, referer as uri from clickstream where clickstream_id in( " +
                    " select distinct clickstream_id from clickstream_request where timestampper > ? and timestampper < ?) and host_id = ? and referer <> '' group by referer ";


	private static final String GET_TIME_ON_SITE = DbConnectionFactory.isPostgres() ?
			" select coalesce(avg(time_on_site),0) as avg_time_on_site from (" +
			" select extract(epoch from age(max(timestampper), min(timestampper))) as time_on_site from clickstream_request "+
			" where extract(day from timestampper) = ? and extract(month from timestampper) = ? "+
			" and extract(year from timestampper) = ? and host_id = ? group by clickstream_id )total_time where total_time.time_on_site > 0 "
			:DbConnectionFactory.isOracle()?
					" select coalesce(avg(time_on_site),0) as avg_time_on_site from (" +
					" select ROUND(TO_NUMBER(SYSDATE + (max(timestampper) - min(timestampper))*86400 - SYSDATE),6) as time_on_site "+
					" from clickstream_request where extract(day from timestampper) = ? and extract(month from timestampper) = ? "+
					" and extract(year from timestampper) = ? and host_id = ? group by clickstream_id )total_time where total_time.time_on_site > 0 "
					:DbConnectionFactory.isMySql()?
							" select coalesce(avg(time_on_site),0) as avg_time_on_site from (" +
							" select (unix_timestamp(max(timestampper)) - unix_timestamp(min(timestampper))) as time_on_site from clickstream_request "+
							" where DAY(timestampper) = ? and MONTH(timestampper) = ? and YEAR(timestampper) = ? "+
							" and host_id = ? group by clickstream_id )total_time where total_time.time_on_site > 0 "
							:DbConnectionFactory.isMsSql()?
									" select coalesce(avg(time_on_site),0) as avg_time_on_site from (" +
									" select DATEDIFF(S, min(timestampper), max(timestampper)) as time_on_site from clickstream_request "+
									" where DATEPART(day,timestampper) = ? and DATEPART(month,timestampper) = ? and DATEPART(year,timestampper) = ? "+
									" and host_id = ? group by clickstream_id )total_time where total_time.time_on_site > 0 "
								:"";


	private static final String GET_CLICKSTREAM_BOUNCES = " select case when b.total_count = 0 then 0 else ((a.bounces_count * 100)/b.total_count) end as bounces from "+
                                                          "(select count(*) as bounces_count from( "+
                                                            "select coalesce(count(clickstream_id),0) as bounces_count "+
                                                            "from clickstream_request r1 "+
                                                            "where  r1.timestampper > ? and r1.timestampper < ? "+
                                                            "and  r1.host_id = ?  group by r1.clickstream_id having count(clickstream_id) = 1 "+
                                                            ")x "+
                                                           ")a, "+
														  "(select coalesce(y.total_count,0) as total_count from( "+
														    "select coalesce(count(clickstream_id),0) as total_count "+
														    "from clickstream_request r2 "+
														    "where r2.timestampper > ? and r2.timestampper < ?  and r2.host_id = ? "+
														    ")y "+
														  ")b";

	private static final String GET_SEARCH_ENGINE_VISITS = " select coalesce(count(*),0) as count from clickstream where exists (select * from clickstream_request where " +
			" timestampper > ? and timestampper < ? and host_id = ? and clickstream_request.clickstream_id = clickstream.clickstream_id) and (";


    private static final String GET_REFERRING_SITE_VISITS = DbConnectionFactory.isOracle() ?//In Oracle empty strings are treated as nulls, so a "<> ''" will cause problem...
            " select coalesce(count(*),0) as count from clickstream where exists (select * from clickstream_request where timestampper > ? and timestampper < ?  and host_id = ? " +
                    " and clickstream_request.clickstream_id = clickstream.clickstream_id) and referer IS NOT NULL and not ("
            :
            " select coalesce(count(*),0) as count from clickstream where exists (select * from clickstream_request where timestampper > ? and timestampper < ?  and host_id = ? " +
                    " and clickstream_request.clickstream_id = clickstream.clickstream_id) and referer IS NOT NULL and referer <> '' and not (";


	private static final String GET_DIRECT_TRAFFIC_VISITS = " select coalesce(count(distinct clickstream.clickstream_id),0) as count from clickstream_request, clickstream where clickstream_request.clickstream_id = clickstream.clickstream_id and "+
	        " (referer is null or referer = '') and clickstream_request.timestampper > ? and clickstream_request.timestampper < ? and clickstream_request.host_id = ? ";


	private static final String GET_TOTAL_HTML_PAGE_VIEWS = " select coalesce(count(*),0) as num_views from clickstream_request where timestampper > ? and timestampper < ? and host_id = ? ";


	private static final String GET_TOTAL_VISITS = " select coalesce(count(distinct r1.clickstream_id),0) as count from clickstream_request r1 where " +
			" r1.timestampper > ? and r1.timestampper < ? "+
			" and r1.host_id = ? ";


	private static final String  GET_UNIQUE_VISITORS = " select coalesce(count(distinct remote_address),0) as num_views from clickstream_request, clickstream " +
			" where clickstream.clickstream_id = clickstream_request.clickstream_id and timestampper > ?  and timestampper < ? and clickstream_request.host_id = ?";


	private static final String GET_NEW_VISITORS = " select coalesce(count(distinct cs1.remote_address),0) as new_visits from clickstream_request r1, clickstream  cs1 "+
			" where cs1.clickstream_id = r1.clickstream_id and r1.timestampper > ? and r1.timestampper < ? and r1.host_id = ? and remote_address not in( select cs2.remote_address from clickstream_request r2, clickstream cs2 "+
			" where cs2.clickstream_id = r2.clickstream_id and r2.timestampper < ? and r2.host_id = ? group by cs2.remote_address )";



	public List<DashboardSummary404> get404s(String userId, String hostId,
			boolean showIgnored, Date fromDate, Date toDate, int limit,
			int offset, String sortBy) throws DotDataException,
			DotHibernateException {

		List<DashboardSummary404> results = new ArrayList<>();
		HibernateUtil dh = new HibernateUtil(DashboardSummary404.class);
		StringBuffer queryString = new StringBuffer();

		queryString.append("select new "+DashboardSummary404.class.getName() +"(summary404.id,summary404.hostId,summary404.uri," +
					" summary404.refererUri, userPreferences.ignored) from "+
					" summaryPeriod in class " + DashboardSummaryPeriod.class.getName()+ ", summary404 in class " +DashboardSummary404.class.getName() +
					" left join summary404.userPreferences as userPreferences where "+
					" summary404.summaryPeriod.id = summaryPeriod.id "+
			        " and summaryPeriod.fullDate > ? and summaryPeriod.fullDate < ? and summary404.hostId = ? "+
					" and (userPreferences.userId is null or userPreferences.userId = ?)" +
					(!showIgnored?" and userPreferences.ignored is null or userPreferences.ignored = 0 ":""));

		if(!UtilMethods.isSet(sortBy)){
			sortBy = " order by summary404.uri desc, summary404.refererUri asc ";
		}else{
			sortBy = " order by " + sortBy;
		}
		try {
			dh.setQuery(queryString.toString() + sortBy);

			if(UtilMethods.isSet(fromDate)){
				dh.setParam(fromDate);
			}
			if(UtilMethods.isSet(toDate)){
				dh.setParam(toDate);
			}
			if(UtilMethods.isSet(hostId)){
				dh.setParam(hostId);
			}

			if(UtilMethods.isSet(userId)){
				dh.setParam(userId);
			}

			if(offset < 0) offset = 0;
			dh.setFirstResult(offset);
			if(limit>0){
			 dh.setMaxResults(limit);
			}
			results = dh.list();
		} catch (Exception e) {

			Logger.error(DashboardFactoryImpl.class, "get404s failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		return results;
	}

	public DashboardSummary getDashboardSummary(String hostId, Date fromDate,
			Date toDate) throws DotDataException, DotHibernateException {

		StringBuffer query = new StringBuffer();

		DashboardSummary summary = new DashboardSummary();

		String avgTime = "";
		if(DbConnectionFactory.isPostgres()){
			avgTime = "to_timestamp(avg(extract(epoch from avg_time_on_site)))::TIMESTAMP WITHOUT TIME ZONE as avg_time_on_site";
		}else if(DbConnectionFactory.isMySql()){
			avgTime = "FROM_UNIXTIME(avg(unix_timestamp(avg_time_on_site))) as avg_time_on_site";
		}else if(DbConnectionFactory.isOracle()){
			avgTime = "TO_CHAR((numtodsinterval(AVG((sysdate  + (avg_time_on_site - TO_TIMESTAMP('01-01-1970 00:00:00', 'DD-MM-YYYY HH24:MI:SS'))- sysdate) * 86400),'SECOND') + TO_TIMESTAMP('01-01-1970 00:00:00', 'DD-MM-YYYY HH24:MI:SS')),'DD-MM-YYYY HH24:MI:SS') as avg_time_on_site";
		}else if(DbConnectionFactory.isMsSql()){
			avgTime = "DATEADD(s, avg(DATEDIFF(s, '1970-01-01 00:00:00',avg_time_on_site)), '1970-01-01 00:00:00') as avg_time_on_site";
		}

		query.append("select sum(visits) as visits, " +
				            "sum(page_views) as page_views, " +
				            "sum(unique_visits) as unique_visits, " +
				            "sum(new_visits) as new_visits, " +
				            "sum(direct_traffic) as direct_traffic, " +
				            "sum(referring_sites) as referring_sites, " +
				            "sum(search_engines) as search_engines, "+
				            avgTime +", "+
				            "avg(bounce_rate) as bounce_rate "+
                     "from analytic_summary  join analytic_summary_period on " +
				     "analytic_summary.summary_period_id = analytic_summary_period.id "+
	                 "and analytic_summary_period.full_date > ? and analytic_summary_period.full_date < ? "+
	                 "and host_id = ?");

		DotConnect dc = new DotConnect();
		try {
			dc.setSQL(query.toString());
			if(UtilMethods.isSet(query)){
				dc.addParam(fromDate);
				dc.addParam(toDate);
				dc.addParam(hostId);
			}

			List<Map<String,Object>> results =  dc.loadObjectResults();
			Map<String, Object> resultsMap = results.get(0);
			long visits = resultsMap.get("visits")!=null?((BigDecimal)resultsMap.get("visits")).longValue():0;
			long pageViews = resultsMap.get("page_views")!=null?((BigDecimal)resultsMap.get("page_views")).longValue():0;
			long uniqueVisits = resultsMap.get("unique_visits")!=null?((BigDecimal)resultsMap.get("unique_visits")).longValue():0;
			long newVisits = resultsMap.get("new_visits")!=null?((BigDecimal)resultsMap.get("new_visits")).longValue():0;
			long directTraffic = resultsMap.get("direct_traffic")!=null?((BigDecimal)resultsMap.get("direct_traffic")).longValue():0;
			long referringSites = resultsMap.get("referring_sites")!=null?((BigDecimal)resultsMap.get("referring_sites")).longValue():0;
			long searchEngines =resultsMap.get("search_engines")!=null?((BigDecimal) resultsMap.get("search_engines")).longValue():0;
			Date avgTimeOnSite = null;
			Object avg = resultsMap.get("avg_time_on_site");
			if(avg!=null){
				if(avg instanceof String){
					avgTimeOnSite = df.parse((String)avg);
				}else if(avg instanceof Date){
					avgTimeOnSite = ((Date) avg);
				}
			}
			int bounceRate = 0;
			Object bounceRateObj = resultsMap.get("bounce_rate");
			if(bounceRateObj!=null){
				if(bounceRateObj instanceof BigDecimal){
					bounceRate = ((BigDecimal)resultsMap.get("bounce_rate")).intValue();
				}else if(bounceRateObj instanceof java.lang.Integer){
					bounceRate = (Integer)bounceRateObj;
				}

			}
			summary.setAvgTimeOnSite(avgTimeOnSite);
			summary.setVisits(visits);
			summary.setBounceRate(bounceRate);
			summary.setDirectTraffic(directTraffic);
			summary.setNewVisits(newVisits);
			summary.setPageViews(pageViews);
			summary.setReferringSites(referringSites);
			summary.setSearchEngines(searchEngines);
			summary.setUniqueVisits(uniqueVisits);

			return summary;

		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "getDashboardSummary failed:" + e, e);
		}

		return null;
	}

	public List<DashboardSummaryVisits> getDashboardSummaryVisits(
			String hostId, ViewType viewType, Date fromDate, Date toDate) throws DotDataException,
			DotHibernateException {

		List<DashboardSummaryVisits> visits = new ArrayList<>();
		HibernateUtil dh = new HibernateUtil(DashboardSummaryVisits.class);
		StringBuffer query = new StringBuffer();

		if(viewType.equals(ViewType.DAY)){
			query.append("select visit from visit in class "+DashboardSummaryVisits.class.getName()+"  where visit.visitTime > ? and visit.visitTime < ? and visit.hostId = ? order by visit.visitTime asc ");
		}else{
			query.append("select new "+DashboardSummaryVisits.class.getName()+ "(sum(visit.visits), summaryPeriod.fullDate) from  visit in class "+ DashboardSummaryVisits.class.getName() +"" +
					" , summaryPeriod in class " + DashboardSummaryPeriod.class.getName() + " where " +
					        " summaryPeriod.id = visit.summaryPeriod.id and "+
							" visit.visitTime > ? and visit.visitTime < ? and visit.hostId = ? " +
							" group by summaryPeriod.day, summaryPeriod.fullDate order by summaryPeriod.fullDate asc ");
		}

		try {
			dh.setQuery(query.toString());
			if(UtilMethods.isSet(fromDate)){
				dh.setParam(fromDate);
			}
			if(UtilMethods.isSet(toDate)){
				dh.setParam(toDate);
			}
			if(UtilMethods.isSet(hostId)){
				dh.setParam(hostId);
			}
			visits = dh.list();
		} catch (Exception e) {

			Logger.error(DashboardFactoryImpl.class, "getDashboardSummaryVisits failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		return visits;
	}

	public List<Host> getHostList(User user, boolean includeArchived, Map<String, Object> params,
			int limit, int offset, String sortBy) throws DotDataException,
			DotHibernateException {

		List<Host> hosts = new ArrayList<>();

		Date now = new Date();
		Calendar c1 = Calendar.getInstance();
		c1.setTime(now);
		c1.add(Calendar.DATE,-1);
		Date interVal1To = c1.getTime();

		Calendar c2 = Calendar.getInstance();
		c2.setTime(now);
		c2.add(Calendar.DATE,-8);
		Date interVal1From = c2.getTime();

		Calendar c3 = Calendar.getInstance();
		c3.setTime(now);
		c3.add(Calendar.DATE,-2);
		Date interVal2To = c3.getTime();

		Calendar c4 = Calendar.getInstance();
		c4.setTime(now);
		c4.add(Calendar.DATE,-9);
		Date interVal2From = c4.getTime();


		StringBuffer conditionBuffer = new StringBuffer();


		List<Object> paramValues =null;
		boolean hasCategory = false;
		boolean close = true;
		String selectedCategories = "";
		if(params!=null && params.size()>0){
			conditionBuffer.append(" and (");
			paramValues = new ArrayList<>();
			int counter = 0;
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				if(entry.getKey().equalsIgnoreCase("categories")){
					hasCategory = true;
					selectedCategories = (String)entry.getValue();
					if(params.size()==1){
						conditionBuffer  = new StringBuffer();
						close = false;
					}
				}else{
					if(counter==0){
						if(entry.getValue() instanceof String){
							if(entry.getKey().indexOf("like")!=-1){
								conditionBuffer.append(" " + (entry.getKey().equalsIgnoreCase("hostId")?getIdentifierColumn():entry.getKey())+ " ");
								paramValues.add("%"+ ((String)entry.getValue()).toLowerCase()+"%");
							}else{
							  conditionBuffer.append(" " + (entry.getKey().equalsIgnoreCase("hostId")?getIdentifierColumn():entry.getKey())+ " = '" + entry.getValue() + "'");
							}
						}else if(entry.getValue() instanceof Date){
							conditionBuffer.append(" " + entry.getKey()+ " = ?");
							paramValues.add((Date)entry.getValue());
						}else{
							conditionBuffer.append(" " + entry.getKey()+ " = " + entry.getValue() + "");
						}
					}else{
						if(entry.getValue() instanceof String){
							if(entry.getKey().indexOf("like")!=-1){
								conditionBuffer.append(" and " + (entry.getKey().equalsIgnoreCase("hostId")?getIdentifierColumn():entry.getKey())+ " ");
								paramValues.add("%"+ ((String)entry.getValue()).toLowerCase()+"%");
							}else{
							  conditionBuffer.append(" and " + (entry.getKey().equalsIgnoreCase("hostId")?getIdentifierColumn():entry.getKey())+ " = '" + entry.getValue() + "'");
							}
						}else if(entry.getValue() instanceof Date){
							conditionBuffer.append(" and " + entry.getKey()+ " = ?");
							paramValues.add((Date)entry.getValue());
						}else{
							conditionBuffer.append(" and " + entry.getKey()+ " = " + entry.getValue() + "");
						}
					}

					counter+=1;
				}
			}
			if(close){
				 conditionBuffer.append(" ) ");
			}
		}
		conditionBuffer.append(!includeArchived?" and contentinfo.deleted = " +DbConnectionFactory.getDBFalse():" ");
		StringBuffer query = getHostListQuery(hasCategory, selectedCategories,runDashboardFieldContentlet);
		if(!UtilMethods.isSet(sortBy)){
			sortBy = "totalpageviews desc";
		}

		List<Contentlet> hostList;
		try {
		    final DotConnect dotConnect = new DotConnect();
		    String q = query.toString() + conditionBuffer.toString()  +  " order by " + sortBy;
            dotConnect.setSQL(q);
			if(UtilMethods.isSet(query)){
                dotConnect.addParam(interVal1From);
                dotConnect.addParam(interVal1To);
				if(paramValues!=null && !paramValues.isEmpty()){
					for(Object o : paramValues){
						if(o instanceof Date){
                            dotConnect.addParam((Date)o);
						}else if(o instanceof String){
                            dotConnect.addParam((String)o);
						}
					}
				}
			}
			if(offset < 0) offset = 0;
            dotConnect.setStartRow(offset);
            dotConnect.setMaxRows(limit);
			hostList = TransformerLocator
                    .createContentletTransformer(dotConnect.loadObjectResults()).asList();
		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "getHostList failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		if(!hostList.isEmpty()){
			for(Contentlet c : hostList){
				BigInteger pageViews1 = BigInteger.valueOf(getHostPageViews(c.getIdentifier(), interVal1From, interVal1To));
				BigInteger pageViews2 =  BigInteger.valueOf(getHostPageViews(c.getIdentifier(), interVal2From, interVal2To));
				Double pageViewsDiff = 0.0;
				if(pageViews2.longValue()>0){
					pageViewsDiff =  (((pageViews1.doubleValue() - pageViews2.doubleValue())/pageViews2.doubleValue())*100);
				}
				try{
					HostWrapper hw = new HostWrapper(c, pageViews1.longValue(),pageViewsDiff.longValue());
					hosts.add(hw);
				}catch(Exception e){
					Logger.error(DashboardFactoryImpl.class, "getHostList failed:" + e, e);
					throw new DotRuntimeException(e.toString());
				}
			}
		}

		return hosts;
	}

	@SuppressWarnings("unchecked")
	private Long getHostPageViews(String hostId, Date from, Date to) throws DotDataException{
		StringBuffer query = new StringBuffer();
		long views = 0;
		query.append("select coalesce(sum(page_views),0) as page_views from analytic_summary join "+
	               "analytic_summary_period on analytic_summary.summary_period_id = analytic_summary_period.id "+
	               "and analytic_summary_period.full_date > ? and analytic_summary_period.full_date < ? "+
	               "and host_id = ?");
		 DotConnect dc = new DotConnect();
		 dc.setSQL(query.toString());
		 dc.addParam(from);
		 dc.addParam(to);
		 dc.addParam(hostId);
		 ArrayList pageViews = dc.loadResults();
		 views = Long.valueOf((String) ((Map)pageViews.get(0)).get("page_views"));
		return views;

	}


	public List<DashboardSummaryContent> getTopContent(String hostId,
			Date fromDate, Date toDate, int limit, int offset, String sortBy)
			throws DotDataException, DotHibernateException {

		List<DashboardSummaryContent> contentList = new ArrayList<>();
		HibernateUtil dh = new HibernateUtil(DashboardSummaryContent.class);
		if(!UtilMethods.isSet(sortBy)){
			sortBy = " order by sum(summaryContent.hits) desc ";
		}else{
			sortBy = " order by " + sortBy;
		}
		try {
			dh.setQuery("select new "+ DashboardSummaryContent.class.getName() +"(summaryContent.inode, sum(summaryContent.hits), summaryContent.uri, summaryContent.title) " +
					"from summaryContent in class " + DashboardSummaryContent.class.getName() + ", " +
					"summary in class " + DashboardSummary.class.getName()+", summaryPeriod in class " + DashboardSummaryPeriod.class.getName()+" " +
					"where summaryContent.summary.id = summary.id and summary.summaryPeriod.id = summaryPeriod.id "+
					"and summaryPeriod.fullDate > ? and  summaryPeriod.fullDate < ? and summary.hostId = ? " +
					"group by summaryContent.inode, summaryContent.uri, summaryContent.title  " + sortBy);
			if(UtilMethods.isSet(fromDate)){
				dh.setParam(fromDate);
			}
			if(UtilMethods.isSet(toDate)){
				dh.setParam(toDate);
			}
			if(UtilMethods.isSet(hostId)){
				dh.setParam(hostId);
			}
			if(offset < 0) offset = 0;
			dh.setFirstResult(offset);
			if(limit>0){
				 dh.setMaxResults(limit);
			}
			contentList = dh.list();
		} catch (Exception e) {

			Logger.error(DashboardFactoryImpl.class, "getTopContent failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		return contentList;
	}

	public List<DashboardSummaryPage> getTopPages(String hostId, Date fromDate,
			Date toDate, int limit, int offset, String sortBy)
			throws DotDataException, DotHibernateException {

		List<DashboardSummaryPage> pageList = new ArrayList<>();
		HibernateUtil dh = new HibernateUtil(DashboardSummaryPage.class);
		if(!UtilMethods.isSet(sortBy)){
			sortBy = " order by sum(summaryPage.hits) desc ";
		}else{
			sortBy = " order by " + sortBy;
		}
		try {
			dh.setQuery("select new "+ DashboardSummaryPage.class.getName() +"(summaryPage.uri, sum(summaryPage.hits)) " +
					"from summaryPage in class " + DashboardSummaryPage.class.getName() + ", " +
					"summary in class " + DashboardSummary.class.getName()+", summaryPeriod in class " + DashboardSummaryPeriod.class.getName()+" " +
					"where summaryPage.summary.id = summary.id and summary.summaryPeriod.id = summaryPeriod.id "+
					"and summaryPeriod.fullDate > ? and  summaryPeriod.fullDate < ? and summary.hostId = ? " +
					"group by summaryPage.uri  " + sortBy);
			if(UtilMethods.isSet(fromDate)){
				dh.setParam(fromDate);
			}
			if(UtilMethods.isSet(toDate)){
				dh.setParam(toDate);
			}
			if(UtilMethods.isSet(hostId)){
				dh.setParam(hostId);
			}
			if(offset < 0) offset = 0;
			dh.setFirstResult(offset);
			if(limit>0){
				 dh.setMaxResults(limit);
			}
			pageList = dh.list();
		} catch (Exception e) {

			Logger.error(DashboardFactoryImpl.class, "getTopPages failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}


		return pageList;
	}

	public List<DashboardSummaryReferer> getTopReferers(String hostId,
			Date fromDate, Date toDate, int limit, int offset, String sortBy)
			throws DotDataException, DotHibernateException {

		List<DashboardSummaryReferer> pageList = new ArrayList<>();
		HibernateUtil dh = new HibernateUtil(DashboardSummaryReferer.class);
		if(!UtilMethods.isSet(sortBy)){
			sortBy = " order by sum(summaryRef.hits) desc ";
		}else{
			sortBy = " order by " + sortBy;
		}
		try {
            String condition = "";
            Host host = APILocator.getHostAPI().find( hostId, APILocator.getUserAPI().getSystemUser(), false );
            ArrayList<String> aliasesArr = new ArrayList<>();
            aliasesArr.add( host.getHostname() );
            if ( host.getAliases() != null ) {//In Oracle empty strings are treated as nulls
                aliasesArr.addAll( new ArrayList<String>( Arrays.asList( host.getAliases().split( "\n" ) ) ) );
            }
            for ( String alias : aliasesArr ) {
                if ( alias.trim().length() > 0 ) {
                    if ( !alias.startsWith( "http://" ) ) {
                        alias = "http://" + alias.trim();
                    }
                    try {
                        URI uri = new URI( alias );
                        condition += " and summaryRef.uri not like 'http://%" + uri.getHost().replaceAll( "www.", "" ) + "%' ";
                    } catch ( Exception e ) {
                        Logger.error( DashboardFactoryImpl.class, "Error parsing uri : " + e, e );
                    }
                }
            }

            dh.setQuery("select new "+ DashboardSummaryReferer.class.getName() +"(summaryRef.uri, sum(summaryRef.hits)) " +
					"from summaryRef in class " + DashboardSummaryReferer.class.getName() + ", " +
					"summary in class " + DashboardSummary.class.getName()+", summaryPeriod in class " + DashboardSummaryPeriod.class.getName()+" " +
					"where summaryRef.summary.id = summary.id and summary.summaryPeriod.id = summaryPeriod.id "+
					"and summaryPeriod.fullDate > ? and  summaryPeriod.fullDate < ? and summary.hostId = ? " +
					condition  + " group by summaryRef.uri " + sortBy);

			if(UtilMethods.isSet(fromDate)){
				dh.setParam(fromDate);
			}
			if(UtilMethods.isSet(toDate)){
				dh.setParam(toDate);
			}
			if(UtilMethods.isSet(hostId)){
				dh.setParam(hostId);
			}
			if(offset < 0) offset = 0;
			dh.setFirstResult(offset);
			if(limit>0){
				 dh.setMaxResults(limit);
			}
			pageList = dh.list();
		} catch (Exception e) {

			Logger.error(DashboardFactoryImpl.class, "getTopReferers failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}


		return pageList;
	}



	public List<DashboardWorkStream> getWorkStreamList(User user,
			String hostId, String userId, Date fromDate, Date toDate,
			int limit, int offset, String sortBy) throws DotDataException,
			DotHibernateException {

		String orderBy = "";
		String queryStr = "";

		if(UtilMethods.isSet(hostId)){
	        queryStr += " and analytic_summary_workstream.host_id = '"+ hostId +"'";
		}
		if(UtilMethods.isSet(userId)){
			queryStr += " and analytic_summary_workstream.mod_user_id= ? ";
		}

	    if(UtilMethods.isSet(fromDate)){
	    	queryStr += " and analytic_summary_workstream.mod_date > ? ";
		}

        if(UtilMethods.isSet(toDate)){
        	queryStr += " and analytic_summary_workstream.mod_date < ? ";
		}

        if(UtilMethods.isSet(sortBy)){
        	orderBy = " order by " + sortBy;
		}else{
			orderBy = " order by analytic_summary_workstream.mod_date desc ";
		}

		List<DashboardWorkStream> workStreamList = new ArrayList<>();
		HibernateUtil hu = new HibernateUtil(DashboardWorkStream.class);
		try {
			hu.setSQLQuery(getWorkstreamListQuery()+ queryStr + orderBy );

			if(UtilMethods.isSet(userId)){
				hu.setParam(userId);
			}
		    if(UtilMethods.isSet(fromDate)){
		    	hu.setParam(fromDate);
			}
	        if(UtilMethods.isSet(toDate)){
	        	hu.setParam(toDate);
			}
			if(offset < 0) offset = 0;
			hu.setFirstResult(offset);
			hu.setMaxResults(limit);
			workStreamList = hu.list();
		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "getWorkStreamList failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
		return workStreamList;
	}

	public void setIgnored(User user, long id, boolean ignored)
	throws DotDataException, DotHibernateException {

		if(ignored){
			DashboardSummary404 summary404 =  (DashboardSummary404) HibernateUtil.load(DashboardSummary404.class, id);
			if(summary404!=null){
				DashboardUserPreferences prefs = new DashboardUserPreferences();
				prefs.setIgnored(ignored);
				prefs.setSummary404(summary404);
				prefs.setUserId(user.getUserId());
				prefs.setModDate(new Date());
				HibernateUtil.save(prefs);
			}
		}else{
			 DotConnect dc = new DotConnect();
			 dc.setSQL("update dashboard_user_preferences set ignored = false, mod_date = ? where summary_404_id = "+id+" and user_id = '"+ user.getUserId()+"'");
			 dc.addParam(new Date());
			 dc.loadResult();

		}

	}

	public long get404Count(String userId, String hostId, boolean showIgnored,
			Date fromDate, Date toDate)
			throws DotDataException, DotHibernateException {

		StringBuffer queryString = new StringBuffer();
		queryString.append("select count(*) as summaryCount " +
				" from analytic_summary_404, analytic_summary_period " +
				" where analytic_summary_404.summary_period_id = analytic_summary_period.id " +
				" and analytic_summary_period.full_date > ?  and analytic_summary_period.full_date < ?" +
				" and analytic_summary_404.host_id = ? ");
		if(!showIgnored){
			queryString.append(" and analytic_summary_404.id not in (select summary_404_id from dashboard_user_preferences where ignored = 1 and user_id = ?) ");
		}
		int count = 0;
		DotConnect dc = new DotConnect();
		try {
			dc.setSQL(queryString.toString());
			dc.addParam(fromDate);
			dc.addParam(toDate);
			dc.addParam(hostId);
			if(!showIgnored && UtilMethods.isSet(userId)){
			   dc.addParam(userId);
			}

			count =  dc.getInt("summaryCount");

		} catch (Exception e) {

			Logger.error(DashboardFactoryImpl.class, "get404Count failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		return count;
	}

	public long getHostListCount(User user, boolean includeArchived, Map<String, Object> params)
			throws DotDataException, DotHibernateException {

		long hostCount = 0;
		StringBuffer conditionBuffer = new StringBuffer();
		List<Object> paramValues =null;
		boolean hasCategory = false;
		boolean close = true;
		String selectedCategories = "";
		if(params!=null && params.size()>0){
			conditionBuffer.append(" and (");
			paramValues = new ArrayList<>();
			int counter = 0;
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				if(entry.getKey().equalsIgnoreCase("categories")){
					hasCategory = true;
					selectedCategories = (String)entry.getValue();
					if(params.size()==1){
						conditionBuffer  = new StringBuffer();
						close = false;
					}
				}else{
					if(counter==0){
						if(entry.getValue() instanceof String){
							if(entry.getKey().indexOf("?")!=-1){
								conditionBuffer.append(" " + entry.getKey()+ " ");
								paramValues.add("%"+ ((String)entry.getValue()).toLowerCase()+"%");
							}else{
							  conditionBuffer.append(" " + (entry.getKey().equalsIgnoreCase("hostId")?getIdentifierColumn():entry.getKey())+ " = '" + entry.getValue() + "'");
							}
						}else if(entry.getValue() instanceof Date){
							conditionBuffer.append(" " + entry.getKey()+ " = ?");
							paramValues.add((Date)entry.getValue());
						}else{
							conditionBuffer.append(" " + entry.getKey()+ " = " + entry.getValue() + "");
						}
					}else{
						if(entry.getValue() instanceof String){
							if(entry.getKey().indexOf("?")!=-1){
								conditionBuffer.append(" and " + entry.getKey()+ " ");
								paramValues.add("%"+ ((String)entry.getValue()).toLowerCase()+"%");
							}else{
							  conditionBuffer.append(" and " + (entry.getKey().equalsIgnoreCase("hostId")?getIdentifierColumn():entry.getKey())+ " = '" + entry.getValue() + "'");
							}
						}else if(entry.getValue() instanceof Date){
							conditionBuffer.append(" and " + entry.getKey()+ " = ?");
							paramValues.add((Date)entry.getValue());
						}else{
							conditionBuffer.append(" and " + entry.getKey()+ " = " + entry.getValue() + "");
						}
					}

					counter+=1;
				}
			}
			if(close){
			 conditionBuffer.append(" ) ");
			}
		}
		//conditionBuffer.append(!includeArchived?" and versioninfo.deleted = " +DbConnectionFactory.getDBFalse()+ " ":" ");
		conditionBuffer.append(!includeArchived?" and contentlet.identifier in(select identifier from contentlet_version_info where identifier = contentlet.identifier and deleted = " +DbConnectionFactory.getDBFalse()+ ")":" ");
		StringBuffer query = getHostListCountQuery(hasCategory, selectedCategories,runDashboardFieldContentlet);


		DotConnect dc = new DotConnect();
		try {
			dc.setSQL(query.toString() + conditionBuffer.toString());
			if(UtilMethods.isSet(query)){
				if(paramValues!=null && !paramValues.isEmpty()){
					for(Object o : paramValues){
						if(o instanceof Date){
							dc.addParam((Date)o);
						}else if(o instanceof String){
							dc.addParam((String)o);
						}
					}
				}
			}
			List<Map<String, String>> results = dc.loadResults();
			if(!results.isEmpty()){
				String c = results.get(0).get("total");
				if (!UtilMethods.isSet(c)) {
					hostCount = 0;
				} else if (c.equals("")) {
					hostCount = 0;
				}else{
				  hostCount = Long.valueOf(c);
				}
			}
		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "getHostListCount failed:" + e, e);
		}

		return hostCount;
	}

	public long getTopContentCount(String hostId, Date fromDate, Date toDate) throws DotDataException,
			DotHibernateException {

		StringBuffer queryString = new StringBuffer();
		queryString.append("select count(*) as summaryCount from (select  sum(analytic_summary_content.hits) hits" +
				" from analytic_summary_content , analytic_summary, analytic_summary_period " +
				" where analytic_summary_content.summary_id = analytic_summary.id and analytic_summary.summary_period_id = analytic_summary_period.id  " +
				" and analytic_summary_period.full_date > ?  and analytic_summary_period.full_date < ?" +
				" and analytic_summary.host_id = ? group by analytic_summary_content.inode, analytic_summary_content.uri, analytic_summary_content.title) sc");

		int count = 0;
		DotConnect dc = new DotConnect();
		try {
			dc.setSQL(queryString.toString());
			dc.addParam(fromDate);
			dc.addParam(toDate);
			dc.addParam(hostId);
			count =  dc.getInt("summaryCount");

		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "getTopContentCount failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		return count;
	}

	public long getTopPagesCount(String hostId, Date fromDate, Date toDate) throws DotDataException,
			DotHibernateException {

		StringBuffer queryString = new StringBuffer();
		queryString.append("select count(*) as summaryCount from (select  sum(analytic_summary_pages.hits) hits " +
				" from analytic_summary_pages , analytic_summary, analytic_summary_period " +
				" where analytic_summary_pages.summary_id = analytic_summary.id and analytic_summary.summary_period_id = analytic_summary_period.id  " +
				" and analytic_summary_period.full_date > ?  and analytic_summary_period.full_date < ?" +
				" and analytic_summary.host_id = ? group by analytic_summary_pages.uri) sc");

		int count = 0;
		DotConnect dc = new DotConnect();
		try {
			dc.setSQL(queryString.toString());
			dc.addParam(fromDate);
			dc.addParam(toDate);
			dc.addParam(hostId);
			count =  dc.getInt("summaryCount");

		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "getTopPagesCount failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		return count;
	}

	public long getTopReferersCount(String hostId, Date fromDate, Date toDate) throws DotDataException,
			DotHibernateException {

        String condition = "";
        try {
            Host host = APILocator.getHostAPI().find( hostId, APILocator.getUserAPI().getSystemUser(), false );
            ArrayList<String> aliasesArr = new ArrayList<>();
            aliasesArr.add( host.getHostname() );
            if ( host.getAliases() != null ) {//In Oracle empty strings are treated as nulls
                aliasesArr.addAll( new ArrayList<String>( Arrays.asList( host.getAliases().split( "\n" ) ) ) );
            }
            for ( String alias : aliasesArr ) {

                if ( alias != null && !alias.isEmpty() ) {

                    if ( !alias.startsWith( "http://" ) ) {
                        alias = "http://" + alias;
                    }
                    try {
                        URI uri = new URI( alias );
                        condition += " and analytic_summary_referer.uri not like 'http://%" + uri.getHost().replaceAll( "www.", "" ) + "%' ";
                    } catch ( Exception e ) {
                        Logger.error( DashboardFactoryImpl.class, "Error parsing uri : " + e, e );
                    }
                }
            }
        } catch ( Exception e ) {
            Logger.error( DashboardFactoryImpl.class, "getTopReferersCount failed:" + e, e );
        }

		StringBuffer queryString = new StringBuffer();
		queryString.append("select count(*) as summaryCount from (select  sum(analytic_summary_referer.hits) hits " +
				" from analytic_summary_referer , analytic_summary, analytic_summary_period " +
				" where analytic_summary_referer.summary_id = analytic_summary.id and analytic_summary.summary_period_id = analytic_summary_period.id  " +
				" and analytic_summary_period.full_date > ?  and analytic_summary_period.full_date < ?" +
				" and analytic_summary.host_id = ? "+ condition  +" group by analytic_summary_referer.uri) sc");

        int count;
        DotConnect dc = new DotConnect();
        try {
            dc.setSQL( queryString.toString() );
            dc.addParam( fromDate );
            dc.addParam( toDate );
            dc.addParam( hostId );
            count = dc.getInt( "summaryCount" );

        } catch ( Exception e ) {
            Logger.error( DashboardFactoryImpl.class, "getTopReferersCount failed:" + e, e );
            throw new DotRuntimeException( e.toString() );
        }

        return count;
    }

	public long getWorkStreamListCount(User user, String hostId, String userId,Date fromDate, Date toDate)
			throws DotDataException, DotHibernateException {

		String queryStr = "";

		if(UtilMethods.isSet(hostId)){
	        queryStr += " and analytic_summary_workstream.host_id = '"+ hostId +"'";
		}
		if(UtilMethods.isSet(userId)){
			queryStr += " and analytic_summary_workstream.mod_user_id= ? ";
		}

	    if(UtilMethods.isSet(fromDate)){
	    	queryStr += " and analytic_summary_workstream.mod_date > ? ";
		}

        if(UtilMethods.isSet(toDate)){
        	queryStr += " and analytic_summary_workstream.mod_date < ? ";
		}

        int count = 0;
		DotConnect dc = new DotConnect();
		try {
			dc.setSQL(getWorkstreamCountQuery() + queryStr);
			 if(UtilMethods.isSet(userId)){
				   dc.addParam(userId);
			 }
			 if(UtilMethods.isSet(fromDate)){
				   dc.addParam(fromDate);
		     }
			 if(UtilMethods.isSet(toDate)){
					 dc.addParam(toDate);
			 }

			count =  dc.getInt("summaryCount");

		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "getWorkStreamListCount failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		return count;

	}

	public List<TopAsset> getTopAssets(User user, String hostId)
			throws DotDataException {

		List<TopAsset> topAssets = new ArrayList<>();
		DotConnect dc = new DotConnect();
		try {
			dc.setSQL(getTopAssetsQuery());
			dc.addParam(hostId);
			ArrayList<Map<String, Object>> results = dc.loadResults();

			for (int i = 0; i < results.size(); i++) {
				TopAsset topAsset = new TopAsset();
				Map<String, Object> hash = (Map<String, Object>) results.get(i);
				Long count = ConversionUtils.toLong(hash.get("count"), 0L);
				String assetType = (String) hash.get("asset_type");
				String hostInode = (String) hash.get("host_inode");
				topAsset.setAssetType(assetType);
				topAsset.setCount(count);
				topAsset.setHostId(hostInode);
				topAssets.add(topAsset);
			}


		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "getTopAssets failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}


		return topAssets;
	}


	public void populateAnalyticSummaryTables() {
		AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Starting to populate Dashboard Tables");
		long before = System.nanoTime();

		DotConnect dc = new DotConnect();
		//Get available hosts
		List<String> hostIds = new ArrayList<>();
		dc.setSQL(getHostQueryForClickstream(runDashboardFieldContentlet));
		try{
			ArrayList<Map<String, Object>> results = dc.loadResults();
			for (int i = 0; i < results.size(); i++) {
				Map<String, Object> hash = (Map<String, Object>) results.get(i);
				if(!hash.isEmpty()){
					String hostId = (String) hash.get("host_id");
					if(UtilMethods.isSet(hostId)){
						hostIds.add(hostId);
					}

				}
			}
		}catch(Exception e){
			Logger.error(DashboardFactoryImpl.class, "populateAnalyticSummaryTables failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		if(!hostIds.isEmpty()){
			AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Found " + hostIds.size() + " hosts");
			for(String hostId:hostIds){
				AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Starting to populate data for host: " + hostId );
				long beforeHost = System.nanoTime();
				List<Calendar> summaryPeriodDates = new ArrayList<>();
				try {
					AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Pulling the dashboard dates");
					dc.setSQL(DASHBOARD_SUMMARY_DATES);
					dc.addParam(hostId);
					dc.addParam(hostId);
					dc.addParam(hostId);
					dc.addParam(hostId);
					dc.addParam(hostId);
					ArrayList<Map<String, Object>> results = dc.loadResults();
					for (int i = 0; i < results.size(); i++) {
						Map<String, Object> hash = (Map<String, Object>) results.get(i);
						String day = (String) hash.get("dd");
						String month = (String) hash.get("mm");
						String year = (String) hash.get("yyyy");

						if(UtilMethods.isSet(day) && UtilMethods.isSet(month) && UtilMethods.isSet(year)){
							Calendar calendar = Calendar.getInstance();
							calendar.set(Calendar.YEAR, Integer.parseInt(year));
							calendar.set(Calendar.MONTH,Integer.parseInt(month)-1);
							calendar.set(Calendar.DAY_OF_MONTH,Integer.parseInt(day));
							calendar.set(Calendar.HOUR_OF_DAY,0);
							calendar.set(Calendar.MINUTE, 0);
							calendar.set(Calendar.SECOND,0);
							calendar.set(Calendar.MILLISECOND,0);
							summaryPeriodDates.add(calendar);
						}

					}
				}catch(Exception e){
					Logger.error(DashboardFactoryImpl.class, "populateAnalyticSummaryTables failed for host = " + hostId + " :", e);
					//throw new DotRuntimeException(e.toString());
				}

				try{
					if(!summaryPeriodDates.isEmpty()){
						HibernateUtil.startTransaction();
						int count = 0;
						for(Calendar calendar:summaryPeriodDates){
							AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Starting to insert summary period dashboard data for " + calendar.getTime().toString() + " and host = " + hostId);
							long beforeDate = System.nanoTime();
							DashboardSummaryPeriod summaryPeriod = insertSummaryPeriod(calendar);
							AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Finished inserting summary period dashboard data for " + calendar.getTime().toString() + " and host = " + hostId );
							if(summaryPeriod!=null){
								AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Starting to insert summary visits dashboard data for " + calendar.getTime().toString() + " and host = " + hostId );
								insertSummaryVisits(summaryPeriod, hostId);
								AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Finished inserting summary visits dashboard data for " + calendar.getTime().toString() + " and host = " + hostId );
								AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Starting to insert summary 404 dashboard data for " + calendar.getTime().toString() + " and host = " + hostId );
								insertSummary404(summaryPeriod,hostId);
								AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Finished inserting summary 404 dashboard data for " + calendar.getTime().toString() + " and host = " + hostId );
								AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Starting to insert summary data for " + calendar.getTime().toString() + " and host = " + hostId );
								DashboardSummary summary = insertSummary(summaryPeriod, hostId);
								AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Finished inserting summary dashboard data for " + calendar.getTime().toString() + " and host = " + hostId);
								if(summary!=null){
									AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Starting to insert summary pages dashboard data for " + calendar.getTime().toString() + " and host = " + hostId);
									insertSummaryPages(summary);
									AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Finished inserting summary pages dashboard data for " + calendar.getTime().toString() + " and host = " + hostId);
									AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Starting to insert summary content dashboard data for " + calendar.getTime().toString() + " and host = " + hostId);
									insertSummaryContent(summary);
									AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Finished inserting summary content dashboard data for " + calendar.getTime().toString() + " and host = " + hostId);
									AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Starting to insert summary referers dashboard data for " + calendar.getTime().toString() + " and host = " + hostId );
									insertSummaryReferers(summary);
									AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Finished inserting summary referers dashboard data for " + calendar.getTime().toString() + " and host = " + hostId);
								}

							}
							AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Finished inserting summary period dashboard data for " + calendar.getTime().toString() + " and host = " + hostId);
							long afterDate = System.nanoTime();
							AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Total time for date "+ calendar.getTime().toString() + " and host "+ hostId + " was "  + ((float)(afterDate - beforeDate) / 1000000F));

						}
						HibernateUtil.commitTransaction();
						count++;
						if(count % 20 == 0){
							HibernateUtil.getSession().flush();
							HibernateUtil.getSession().clear();
						}
					}

				} catch (Exception e) {
					try {
						HibernateUtil.rollbackTransaction();
					} catch (DotHibernateException e1) {
						Logger.error(DashboardFactoryImpl.class, "populateAnalyticSummaryTables failed host = " + hostId + " :" + e, e);
						//throw new DotRuntimeException(e.toString());
					}
					Logger.error(DashboardFactoryImpl.class, "populateAnalyticSummaryTables failed host = " + hostId + " :" + e, e);
					//throw new DotRuntimeException(e.toString());
				}finally{
					try {
						HibernateUtil.closeSession();
					} catch (DotHibernateException e) {
						Logger.error(DashboardFactoryImpl.class, "populateAnalyticSummaryTables failed host = " + hostId + " :"+ e, e);
						//throw new DotRuntimeException(e.toString());
					}
					AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Finished populating Dashboard Tables for host = " + hostId);
				}



				AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Starting to insert workstream data for host = " + hostId);
				long beforeWorkstream = System.nanoTime();
				try{
					HibernateUtil.startTransaction();
					insertWorkStreams(hostId);
					HibernateUtil.commitTransaction();
				} catch (Exception e) {
					try {
						HibernateUtil.rollbackTransaction();
					} catch (DotHibernateException e1) {
						Logger.error(DashboardFactoryImpl.class, "populateAnalyticSummaryTables failed inserting workstream data for host = " + hostId + " :" + e, e);
						//throw new DotRuntimeException(e.toString());
					}
					Logger.error(DashboardFactoryImpl.class, "populateAnalyticSummaryTables failed inserting workstream data for host = " + hostId + " :" + e, e);
					//throw new DotRuntimeException(e.toString());
				}finally{
					try {
						HibernateUtil.closeSession();
					} catch (DotHibernateException e) {
						Logger.error(DashboardFactoryImpl.class, "populateAnalyticSummaryTables  failed inserting workstream data for host = " + hostId + " :"+ e, e);
						//throw new DotRuntimeException(e.toString());
					}
					AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "finished inserting workstream data for host = " + hostId);
					long afterWorkstream = System.nanoTime();
					AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables",  "Total time for workstream of host "+ hostId + " was "  + ((float)(afterWorkstream - beforeWorkstream ) / 1000000F));
				}
				long afterHost = System.nanoTime();
				AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables",  "Total time for host "+ hostId + " was "  + ((float)(afterHost - beforeHost) / 1000000F));

			}

		}else{
			AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "No hosts found");
		}

		AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables", "Finished populating Dashboard Tables for all hosts");
		long after = System.nanoTime();
		AdminLogger.log(DashboardFactoryImpl.class, "populateAnalyticSummaryTables",  "Total time was "  + ((float)(after - before) / 1000000F));
	}


	private DashboardSummaryPeriod insertSummaryPeriod(Calendar calendar) {

		if(calendar!=null){
			String[] strDays = new String[] { "Sunday", "Monday", "Tuesday", "Wednesday", "Thusday",
					"Friday", "Saturday" };
			String[] monthName = {"January", "February",
					"March", "April", "May", "June", "July",
					"August", "September", "October", "November","December"};
			DashboardSummaryPeriod summaryPeriod = new DashboardSummaryPeriod();
			summaryPeriod.setFullDate(calendar.getTime());
			summaryPeriod.setDay(calendar.get(Calendar.DAY_OF_MONTH));
			summaryPeriod.setMonth(calendar.get(Calendar.MONTH)+1);
			summaryPeriod.setYear(String.valueOf(calendar.get(Calendar.YEAR)));
			summaryPeriod.setWeek(calendar.get(Calendar.WEEK_OF_MONTH));
			summaryPeriod.setDayName(strDays[calendar.get(Calendar.DAY_OF_WEEK) - 1]);
			summaryPeriod.setMonthName(monthName[calendar.get(Calendar.MONTH)]);
			try{
				HibernateUtil hu = new HibernateUtil(DashboardSummaryPeriod.class);
				hu.setQuery("from com.dotmarketing.portlets.dashboard.model.DashboardSummaryPeriod period where period.day = ? and period.month = ? and period.year= ?");
				hu.setParam(summaryPeriod.getDay());
				hu.setParam(summaryPeriod.getMonth());
				hu.setParam(summaryPeriod.getYear());
				DashboardSummaryPeriod period = (DashboardSummaryPeriod) hu.load();
				if(period!=null && period.getFullDate()!=null){
					summaryPeriod = period;
				}else{
					HibernateUtil.save(summaryPeriod);
					HibernateUtil.getSession().refresh(summaryPeriod);
				}
				return summaryPeriod;
			} catch (Exception e) {
				Logger.error(DashboardFactoryImpl.class, "insertSummaryPeriod failed:" + e, e);
				throw new DotRuntimeException(e.toString());
			}
		}

		return null;
	}


	private void insertSummaryVisits(DashboardSummaryPeriod summaryPeriod, String hostId){

		if(summaryPeriod!=null){

			Calendar calendar = Calendar.getInstance();
			calendar.setTime(summaryPeriod.getFullDate());
			DotConnect dc = new DotConnect();
			try {
				String query = DASHBOARD_SUMMARY_VISITS;
				dc.setSQL(query);
				dc.addParam(summaryPeriod.getDay());
				dc.addParam(summaryPeriod.getMonth());
				dc.addParam(Integer.parseInt(summaryPeriod.getYear()));
				dc.addParam(hostId);
				ArrayList<Map<String, Object>> results = dc.loadResults();
				int count = 0;
				for (int i = 0; i < results.size(); i++) {
					Map<String, Object> hash = (Map<String, Object>) results.get(i);
					if(!hash.isEmpty()){
						Long totalVisits = Long.parseLong((String) hash.get("totalvisits"));
						String visitHour = (String) hash.get("visithour");
						calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(visitHour));
						calendar.set(Calendar.MINUTE, 0);
						calendar.set(Calendar.SECOND, 0);
						calendar.set(Calendar.MILLISECOND,0);
						DashboardSummaryVisits summaryVisit = new DashboardSummaryVisits();
						summaryVisit.setHostId(hostId);
						summaryVisit.setVisits(totalVisits);
						summaryVisit.setSummaryPeriod(summaryPeriod);
						summaryVisit.setVisitTime(calendar.getTime());
						HibernateUtil.save(summaryVisit);
						HibernateUtil.getSession().refresh(summaryPeriod);
						count++;
						if(count % 20 == 0){
							HibernateUtil.getSession().flush();
							HibernateUtil.getSession().clear();
				        }
					}
				}


			} catch (Exception e) {
				Logger.error(DashboardFactoryImpl.class, "insertSummaryVisits failed:" + e, e);
				throw new DotRuntimeException(e.toString());
			}



		}

	}

    private void insertSummary404(DashboardSummaryPeriod summaryPeriod, String hostId){
    	DotConnect dc = new DotConnect();
    	try {
    		String query = DASHBOARD_SUMMARY_404;
			dc.setSQL(query);
	    	Calendar cal1 = Calendar.getInstance();
	    	cal1.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
	    	cal1.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
	    	cal1.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
	    	cal1.add(Calendar.DAY_OF_MONTH, -1);
	    	cal1.set(Calendar.MINUTE, 59);
	    	cal1.set(Calendar.HOUR_OF_DAY, 23);
	    	cal1.set(Calendar.SECOND, 59);
	    	cal1.set(Calendar.MILLISECOND, -1);

	    	Calendar cal2 = Calendar.getInstance();
	    	cal2.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
	    	cal2.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
	    	cal2.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
	    	cal2.add(Calendar.DAY_OF_MONTH, 1);
	      	cal2.set(Calendar.MINUTE, 0);
	    	cal2.set(Calendar.HOUR_OF_DAY, 0);
	    	cal2.set(Calendar.SECOND, 0);
	    	cal2.set(Calendar.MILLISECOND, 0);

	    	Calendar cal3 = Calendar.getInstance();
	    	cal3.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
	    	cal3.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
	    	cal3.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
	    	cal3.add(Calendar.DATE, -31);
	    	cal3.set(Calendar.MINUTE, 0);
	    	cal3.set(Calendar.HOUR_OF_DAY, 0);
	    	cal3.set(Calendar.SECOND,0);
	    	cal3.set(Calendar.MILLISECOND, 0);

	    	Calendar cal4 = Calendar.getInstance();
	    	cal4.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
	    	cal4.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
	    	cal4.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
	    	cal4.set(Calendar.MINUTE, 0);
	    	cal4.set(Calendar.HOUR_OF_DAY, 0);
	    	cal4.set(Calendar.SECOND,0);
	    	cal4.set(Calendar.MILLISECOND, 0);

			dc.addParam(cal1.getTime());
			dc.addParam(cal2.getTime());
			dc.addParam(cal3.getTime());
			dc.addParam(cal4.getTime());
			dc.addParam(hostId);

			ArrayList<Map<String, Object>> results = dc.loadResults();
			int count = 0;
			for (int i = 0; i < results.size(); i++) {
				Map<String, Object> hash = (Map<String, Object>) results.get(i);
				if(!hash.isEmpty()){
					String uri = (String) hash.get("request_uri");
					String refererUri = (String) hash.get("referer_uri");
					DashboardSummary404 summary404 = new DashboardSummary404();
					summary404.setHostId(hostId);
					summary404.setRefererUri(UtilMethods.isSet(refererUri)?refererUri:null);
					summary404.setSummaryPeriod(summaryPeriod);
					summary404.setUri(uri);
					HibernateUtil.save(summary404);
					HibernateUtil.getSession().refresh(summaryPeriod);
					count++;
					if(count % 20 == 0){
						HibernateUtil.getSession().flush();
						HibernateUtil.getSession().clear();
			        }
				}
			}


		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "insertSummary404 failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}


	}

    private DashboardSummary insertSummary(DashboardSummaryPeriod summaryPeriod, String hostId){

    	try{
    		DashboardSummary summary = new DashboardSummary();
    		summary.setSummaryPeriod(summaryPeriod);
    		summary.setHostId(hostId);
    		summary.setAvgTimeOnSite(getTimeOnsite(summaryPeriod,hostId));
    		summary.setBounceRate(getBounceRate(summaryPeriod,hostId));
    		summary.setDirectTraffic(getDirectTrafficVisits(summaryPeriod,hostId));
    		summary.setSearchEngines(getSearchEngineVisits(summaryPeriod,hostId));
    		summary.setReferringSites(getReferringSiteVisits(summaryPeriod,hostId));
    		summary.setNewVisits(getNewVisits(summaryPeriod,hostId));
    		summary.setUniqueVisits(getUniqueVisits(summaryPeriod,hostId));
    		summary.setPageViews(getPageViews(summaryPeriod,hostId));
    		summary.setVisits(getVisits(summaryPeriod,hostId));
    		HibernateUtil.save(summary);
    		HibernateUtil.getSession().refresh(summaryPeriod);
    		return summary;
    	} catch (Exception e) {
    		Logger.error(DashboardFactoryImpl.class, "insertSummary failed:" + e, e);
    		throw new DotRuntimeException(e.toString());
    	}

    }

    private Date getTimeOnsite(DashboardSummaryPeriod summaryPeriod, String hostId){


    	DotConnect dc = new DotConnect();
    	try {
    		dc.setSQL(GET_TIME_ON_SITE);
    		dc.addParam(summaryPeriod.getDay());
    		dc.addParam(summaryPeriod.getMonth());
    		dc.addParam(Integer.parseInt(summaryPeriod.getYear()));
    		dc.addParam(hostId);
    		ArrayList<Map<String, Object>> results = dc.loadResults();
    		for (int i = 0; i < results.size(); i++) {
    			Map<String, Object> hash = (Map<String, Object>) results.get(i);
    			if(!hash.isEmpty()){
    				String timeStr = (String) hash.get("avg_time_on_site");
    				int minutes = 0;
    				int seconds = 0;
    				int hours =  0;
    				if(UtilMethods.isSet(timeStr)){
    					double timeOnSite = Double.parseDouble(timeStr);
    					minutes = (int) Math.floor(timeOnSite/60);
    					seconds = (int) timeOnSite - (minutes*60);
    					hours =  (int) Math.floor(minutes/60);
    					minutes = minutes - (hours*60);
    				}
    				Calendar calendar = Calendar.getInstance();
    				calendar.set(Calendar.YEAR, 1970);
    				calendar.set(Calendar.MONTH, 0);
    				calendar.set(Calendar.DAY_OF_MONTH, 1);
    				calendar.set(Calendar.HOUR_OF_DAY, hours);
    				calendar.set(Calendar.MINUTE, minutes);
    				calendar.set(Calendar.SECOND, seconds);
    				calendar.set(Calendar.MILLISECOND,0);
    				return calendar.getTime();

    			}
    		}
    	} catch (Exception e) {
    		Logger.error(DashboardFactoryImpl.class, "getTimeOnsite failed:" + e, e);
    		throw new DotRuntimeException(e.toString());
    	}

    	return null;
    }

    private int getBounceRate(DashboardSummaryPeriod summaryPeriod, String hostId) throws DotDataException{
    	DotConnect db = new DotConnect();
        db.setSQL(GET_CLICKSTREAM_BOUNCES);
    	Calendar cal1 = Calendar.getInstance();
    	cal1.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal1.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal1.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal1.add(Calendar.DAY_OF_MONTH, -1);
    	cal1.set(Calendar.MINUTE, 59);
    	cal1.set(Calendar.HOUR_OF_DAY, 23);
    	cal1.set(Calendar.SECOND, 59);
    	cal1.set(Calendar.MILLISECOND, -1);

    	Calendar cal2 = Calendar.getInstance();
    	cal2.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal2.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal2.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal2.add(Calendar.DAY_OF_MONTH, 1);
      	cal2.set(Calendar.MINUTE, 0);
    	cal2.set(Calendar.HOUR_OF_DAY, 0);
    	cal2.set(Calendar.SECOND, 0);
    	cal2.set(Calendar.MILLISECOND, 0);
    	db.addParam(cal1.getTime());
    	db.addParam(cal2.getTime());
        db.addParam(hostId);
    	db.addParam(cal1.getTime());
    	db.addParam(cal2.getTime());
        db.addParam(hostId);

        String bounces = db.getString("bounces");
        return Double.valueOf( bounces ).intValue();
    }

    private long getSearchEngineVisits(DashboardSummaryPeriod summaryPeriod, String hostId){
    	String[] searchEngineExpressions = Config.getStringArrayProperty("SEARCH_ENGINES");
    	StringBuilder sql = new StringBuilder(GET_SEARCH_ENGINE_VISITS);
    	boolean first = true;
    	for(String engine : searchEngineExpressions) {
    		if(first) {
    			first = false;
    		} else {
    			sql.append(" or ");
    		}
    		sql.append(" referer like '" + engine + "' ");
    	}
		sql.append(" ) ");

    	DotConnect db = new DotConnect();
        db.setSQL(sql.toString());
    	Calendar cal1 = Calendar.getInstance();
    	cal1.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal1.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal1.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal1.add(Calendar.DAY_OF_MONTH, -1);
    	cal1.set(Calendar.MINUTE, 59);
    	cal1.set(Calendar.HOUR_OF_DAY, 23);
    	cal1.set(Calendar.SECOND, 59);
    	cal1.set(Calendar.MILLISECOND, -1);

    	Calendar cal2 = Calendar.getInstance();
    	cal2.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal2.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal2.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal2.add(Calendar.DAY_OF_MONTH, 1);
      	cal2.set(Calendar.MINUTE, 0);
    	cal2.set(Calendar.HOUR_OF_DAY, 0);
    	cal2.set(Calendar.SECOND, 0);
    	cal2.set(Calendar.MILLISECOND, 0);
    	db.addParam(cal1.getTime());
    	db.addParam(cal2.getTime());
        db.addParam(hostId);
        return db.getInt("count");

    }

    private long getReferringSiteVisits(DashboardSummaryPeriod summaryPeriod, String hostId){

        String[] searchEngineExpressions = Config.getStringArrayProperty( "SEARCH_ENGINES" );
        StringBuilder sql = new StringBuilder( GET_REFERRING_SITE_VISITS );

        boolean first = true;
    	for(String engine : searchEngineExpressions) {
    		if(first) {
    			first = false;
    		} else {
    			sql.append(" or ");
    		}
    		sql.append(" referer like '" + engine + "' ");
    	}
		sql.append(" ) ");

    	DotConnect db = new DotConnect();
        db.setSQL(sql.toString());
    	Calendar cal1 = Calendar.getInstance();
    	cal1.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal1.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal1.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal1.add(Calendar.DAY_OF_MONTH, -1);
    	cal1.set(Calendar.MINUTE, 59);
    	cal1.set(Calendar.HOUR_OF_DAY, 23);
    	cal1.set(Calendar.SECOND, 59);
    	cal1.set(Calendar.MILLISECOND, -1);

    	Calendar cal2 = Calendar.getInstance();
    	cal2.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal2.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal2.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal2.add(Calendar.DAY_OF_MONTH, 1);
      	cal2.set(Calendar.MINUTE, 0);
    	cal2.set(Calendar.HOUR_OF_DAY, 0);
    	cal2.set(Calendar.SECOND, 0);
    	cal2.set(Calendar.MILLISECOND, 0);
    	db.addParam(cal1.getTime());
    	db.addParam(cal2.getTime());
        db.addParam(hostId);
        return db.getInt("count");
    }

    private long getDirectTrafficVisits(DashboardSummaryPeriod summaryPeriod, String hostId){
    	DotConnect db = new DotConnect();
        db.setSQL(GET_DIRECT_TRAFFIC_VISITS);
    	Calendar cal1 = Calendar.getInstance();
    	cal1.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal1.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal1.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal1.add(Calendar.DAY_OF_MONTH, -1);
    	cal1.set(Calendar.MINUTE, 59);
    	cal1.set(Calendar.HOUR_OF_DAY, 23);
    	cal1.set(Calendar.SECOND, 59);
    	cal1.set(Calendar.MILLISECOND, -1);

    	Calendar cal2 = Calendar.getInstance();
    	cal2.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal2.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal2.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal2.add(Calendar.DAY_OF_MONTH, 1);
      	cal2.set(Calendar.MINUTE, 0);
    	cal2.set(Calendar.HOUR_OF_DAY, 0);
    	cal2.set(Calendar.SECOND, 0);
    	cal2.set(Calendar.MILLISECOND, 0);
    	db.addParam(cal1.getTime());
    	db.addParam(cal2.getTime());
        db.addParam(hostId);
        return db.getInt("count");
    }

    private long getNewVisits(DashboardSummaryPeriod summaryPeriod, String hostId){
    	DotConnect db = new DotConnect();
    	db.setSQL(GET_NEW_VISITORS);
    	Calendar cal1 = Calendar.getInstance();
    	cal1.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal1.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal1.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal1.add(Calendar.DAY_OF_MONTH, -1);
    	cal1.set(Calendar.MINUTE, 59);
    	cal1.set(Calendar.HOUR_OF_DAY, 23);
    	cal1.set(Calendar.SECOND, 59);
    	cal1.set(Calendar.MILLISECOND, -1);

    	Calendar cal2 = Calendar.getInstance();
    	cal2.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal2.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal2.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal2.add(Calendar.DAY_OF_MONTH, 1);
      	cal2.set(Calendar.MINUTE, 0);
    	cal2.set(Calendar.HOUR_OF_DAY, 0);
    	cal2.set(Calendar.SECOND, 0);
    	cal2.set(Calendar.MILLISECOND, 0);

     	Calendar cal3 = Calendar.getInstance();
    	cal3.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal3.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal3.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
      	cal3.set(Calendar.MINUTE, 0);
    	cal3.set(Calendar.HOUR_OF_DAY, 0);
    	cal3.set(Calendar.SECOND, 0);
    	cal3.set(Calendar.MILLISECOND, 0);

    	db.addParam(cal1.getTime());
    	db.addParam(cal2.getTime());
    	db.addParam(hostId);
    	db.addParam(cal3.getTime());
    	db.addParam(hostId);


    	return db.getInt("new_visits");
    }

    private long getUniqueVisits(DashboardSummaryPeriod summaryPeriod, String hostId){
    	DotConnect db = new DotConnect();
    	db.setSQL(GET_UNIQUE_VISITORS);
    	Calendar cal1 = Calendar.getInstance();
    	cal1.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal1.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal1.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal1.add(Calendar.DAY_OF_MONTH, -1);
    	cal1.set(Calendar.MINUTE, 59);
    	cal1.set(Calendar.HOUR_OF_DAY, 23);
    	cal1.set(Calendar.SECOND, 59);
    	cal1.set(Calendar.MILLISECOND, -1);

    	Calendar cal2 = Calendar.getInstance();
    	cal2.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal2.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal2.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal2.add(Calendar.DAY_OF_MONTH, 1);
      	cal2.set(Calendar.MINUTE, 0);
    	cal2.set(Calendar.HOUR_OF_DAY, 0);
    	cal2.set(Calendar.SECOND, 0);
    	cal2.set(Calendar.MILLISECOND, 0);
    	db.addParam(cal1.getTime());
    	db.addParam(cal2.getTime());
    	db.addParam(hostId);
    	return db.getInt("num_views");
    }

    private long getPageViews(DashboardSummaryPeriod summaryPeriod, String hostId){
    	DotConnect db = new DotConnect();
    	db.setSQL(GET_TOTAL_HTML_PAGE_VIEWS);
    	Calendar cal1 = Calendar.getInstance();
    	cal1.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal1.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal1.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal1.add(Calendar.DAY_OF_MONTH, -1);
    	cal1.set(Calendar.MINUTE, 59);
    	cal1.set(Calendar.HOUR_OF_DAY, 23);
    	cal1.set(Calendar.SECOND, 59);
    	cal1.set(Calendar.MILLISECOND, -1);

    	Calendar cal2 = Calendar.getInstance();
    	cal2.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal2.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal2.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal2.add(Calendar.DAY_OF_MONTH, 1);
      	cal2.set(Calendar.MINUTE, 0);
    	cal2.set(Calendar.HOUR_OF_DAY, 0);
    	cal2.set(Calendar.SECOND, 0);
    	cal2.set(Calendar.MILLISECOND, 0);
    	db.addParam(cal1.getTime());
    	db.addParam(cal2.getTime());
    	db.addParam(hostId);
    	return db.getInt("num_views");
    }

    private long getVisits(DashboardSummaryPeriod summaryPeriod, String hostId) throws DotDataException{
    	DotConnect db = new DotConnect();
    	db.setSQL(GET_TOTAL_VISITS);
    	Calendar cal1 = Calendar.getInstance();
    	cal1.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal1.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal1.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal1.add(Calendar.DAY_OF_MONTH, -1);
    	cal1.set(Calendar.MINUTE, 59);
    	cal1.set(Calendar.HOUR_OF_DAY, 23);
    	cal1.set(Calendar.SECOND, 59);
    	cal1.set(Calendar.MILLISECOND, -1);

    	Calendar cal2 = Calendar.getInstance();
    	cal2.set(Calendar.YEAR, Integer.parseInt(summaryPeriod.getYear()));
    	cal2.set(Calendar.MONTH, summaryPeriod.getMonth()-1);
    	cal2.set(Calendar.DAY_OF_MONTH, summaryPeriod.getDay());
    	cal2.add(Calendar.DAY_OF_MONTH, 1);
      	cal2.set(Calendar.MINUTE, 0);
    	cal2.set(Calendar.HOUR_OF_DAY, 0);
    	cal2.set(Calendar.SECOND, 0);
    	cal2.set(Calendar.MILLISECOND, 0);
    	db.addParam(cal1.getTime());
    	db.addParam(cal2.getTime());
    	db.addParam(hostId);
    	return db.getInt("count");

    }


    private void insertSummaryPages(DashboardSummary summary){

    	DotConnect dc = new DotConnect();
    	try {
    		dc.setSQL(getSummaryPagesQuery());
    		dc.addParam(summary.getSummaryPeriod().getDay());
    		dc.addParam(summary.getSummaryPeriod().getMonth());
    		dc.addParam(Integer.parseInt(summary.getSummaryPeriod().getYear()));
    		dc.addParam(summary.getHostId());
    		ArrayList<Map<String, Object>> results = dc.loadResults();
    		int count =0;
    		for (int i = 0; i < results.size(); i++) {
    			Map<String, Object> hash = (Map<String, Object>) results.get(i);
    			if(!hash.isEmpty()){
    				long hits = Long.parseLong((String) hash.get("hits"));
    				String inode = (String) hash.get("inode");
    				String uri = (String) hash.get("uri");
    				DashboardSummaryPage summaryPage = new DashboardSummaryPage();
    				summaryPage.setHits(hits);
    				summaryPage.setInode(inode);
    				summaryPage.setSummary(summary);
    				summaryPage.setUri(uri);
    				HibernateUtil.save(summaryPage);
    				HibernateUtil.getSession().refresh(summary);
    				count++;
					if(count % 20 == 0){
						HibernateUtil.getSession().flush();
						HibernateUtil.getSession().clear();
			        }

    			}
    		}
    	} catch (Exception e) {
    		Logger.error(DashboardFactoryImpl.class, "insertSummaryPages failed:" + e, e);
    		throw new DotRuntimeException(e.toString());
    	}

    }

    private void insertSummaryContent(DashboardSummary summary){

      	DotConnect dc = new DotConnect();
    	try {
    		dc.setSQL(getSummaryContentQuery());
    		dc.addParam(summary.getSummaryPeriod().getDay());
    		dc.addParam(summary.getSummaryPeriod().getMonth());
    		dc.addParam(Integer.parseInt(summary.getSummaryPeriod().getYear()));
    		dc.addParam(summary.getHostId());
    		ArrayList<Map<String, Object>> results = dc.loadResults();
    		int count =0;
    		for (int i = 0; i < results.size(); i++) {
    			Map<String, Object> hash = (Map<String, Object>) results.get(i);
    			if(!hash.isEmpty()){
    				long hits = Long.parseLong((String) hash.get("hits"));
    				String inode = (String) hash.get("inode");
    				String uri = (String) hash.get("uri");
    				String title = (String) hash.get("title");
    				DashboardSummaryContent summaryContent = new DashboardSummaryContent();
    				summaryContent.setHits(hits);
    				summaryContent.setInode(inode);
    				summaryContent.setSummary(summary);
    				summaryContent.setUri(uri);
    				summaryContent.setTitle(title);
    				HibernateUtil.save(summaryContent);
    				HibernateUtil.getSession().refresh(summary);
    				count++;
					if(count % 20 == 0){
						HibernateUtil.getSession().flush();
						HibernateUtil.getSession().clear();
			        }

    			}
    		}
    	} catch (Exception e) {
    		Logger.error(DashboardFactoryImpl.class, "insertSummaryContent failed:" + e, e);
    		throw new DotRuntimeException(e.toString());
    	}

    }

    private void insertSummaryReferers(DashboardSummary summary){

      	DotConnect dc = new DotConnect();
    	try {
            dc.setSQL( DASHBOARD_SUMMARY_REFERERS );

    		Calendar cal1 = Calendar.getInstance();
	    	cal1.set(Calendar.YEAR, Integer.parseInt( summary.getSummaryPeriod().getYear() ));
	    	cal1.set( Calendar.MONTH, summary.getSummaryPeriod().getMonth() - 1 );
	    	cal1.set(Calendar.DAY_OF_MONTH, summary.getSummaryPeriod().getDay());
	    	cal1.add( Calendar.DAY_OF_MONTH, -1 );
	    	cal1.set(Calendar.MINUTE, 59);
	    	cal1.set(Calendar.HOUR_OF_DAY, 23);
	    	cal1.set(Calendar.SECOND, 59);
	    	cal1.set(Calendar.MILLISECOND, -1);

	    	Calendar cal2 = Calendar.getInstance();
	    	cal2.set(Calendar.YEAR, Integer.parseInt( summary.getSummaryPeriod().getYear() ));
	    	cal2.set( Calendar.MONTH, summary.getSummaryPeriod().getMonth() - 1 );
	    	cal2.set(Calendar.DAY_OF_MONTH, summary.getSummaryPeriod().getDay());
	    	cal2.add( Calendar.DAY_OF_MONTH, 1 );
	     	cal2.set(Calendar.MINUTE, 0);
	    	cal2.set(Calendar.HOUR_OF_DAY, 0);
	    	cal2.set( Calendar.SECOND, 0 );
	    	cal2.set( Calendar.MILLISECOND, 0 );

			dc.addParam(cal1.getTime());
			dc.addParam(cal2.getTime());
    		dc.addParam(summary.getHostId());
    		ArrayList<Map<String, Object>> results = dc.loadResults();
    		Host host = APILocator.getHostAPI().find(summary.getHostId(), APILocator.getUserAPI().getSystemUser(), false);
    		int count =0;
    		for (int i = 0; i < results.size(); i++) {
    			Map<String, Object> hash = (Map<String, Object>) results.get(i);
    			if(!hash.isEmpty()){
    				String uri = (String) hash.get("uri");
    				if(UtilMethods.isSet(uri)){
    					URI refUri;
    					boolean validReferer  = true;
    					try{
    						refUri = new URI(URIUtil.encodeQuery(uri,"UTF-8"));
    						String domainName = refUri.getHost();
    						if(UtilMethods.isSet(domainName)){
    							ArrayList<String> aliasesArr = new ArrayList<>();
    							aliasesArr.add(host.getHostname());
                                if ( host.getAliases() != null ) {//In Oracle empty strings are treated as nulls
                                    aliasesArr.addAll( new ArrayList<String>( Arrays.asList( host.getAliases().split( "\n" ) ) ) );
                                }
                                for(String alias: aliasesArr){
    								if(UtilMethods.isSet(alias)){
    									if(!alias.startsWith("http://")){
    										alias = "http://"+alias;
    									}
    									URI aliasUri = new URI(alias);
    									String uriHostName = aliasUri.getHost();
    									if(UtilMethods.isSet(uriHostName) && domainName.equals(uriHostName)){
    										validReferer = false;
    										break;

    									}
    								}
    							}
    						}
    					}catch(Exception e){
    						Logger.error(DashboardFactoryImpl.class, e.getLocalizedMessage(), e);
    					}
    					if(validReferer){
    						long hits = Long.parseLong((String) hash.get("hits"));
    						DashboardSummaryReferer summaryReferer= new DashboardSummaryReferer();
    						summaryReferer.setSummary(summary);
    						summaryReferer.setUri(uri);
    						summaryReferer.setHits(hits);
    						HibernateUtil.save(summaryReferer);
    						HibernateUtil.getSession().refresh(summary);
    						count++;
    						if(count % 20 == 0){
    							HibernateUtil.getSession().flush();
    							HibernateUtil.getSession().clear();
    						}
    					}
    				}

    			}
    		}
    	} catch (Exception e) {
    		Logger.error(DashboardFactoryImpl.class, "insertSummaryReferers failed:" + e, e);
    		throw new DotRuntimeException(e.toString());
    	}

    }


    private void insertWorkStreams(String hostId){

    	DotConnect dc = new DotConnect();
    	try {

    		if(DbConnectionFactory.isOracle()){
    			dc.setSQL("insert into analytic_summary_workstream(id, inode, asset_type, mod_user_id, host_id, mod_date, action, name) select workstream_seq.NEXTVAL, t.* from ( select "+getWorkstreamQuery(hostId) +") t ");
    		}else if(DbConnectionFactory.isPostgres()){
    			dc.setSQL("insert into analytic_summary_workstream(id, inode, asset_type, mod_user_id, host_id, mod_date, action, name) select nextval('workstream_seq'), "+getWorkstreamQuery(hostId));
    		}else{
    			dc.setSQL("insert into analytic_summary_workstream(inode, asset_type, mod_user_id, host_id, mod_date, action, name) select "+getWorkstreamQuery(hostId));
    		}
    		dc.loadResult();
    	} catch (Exception e) {
    		Logger.error(DashboardFactoryImpl.class, "insertWorkStreams failed:" + e, e);
    		throw new DotRuntimeException(e.toString());
    	}
    }


	public int checkPeriodData(int month, int year) {
		int ret = 0;
		DotConnect dc = new DotConnect();
    	try {
    		dc.setSQL("select count(*) as total from analytic_summary_period where month = ? and year = ?");
    		dc.addParam(month);
    		dc.addParam(String.valueOf(year));
    		ret = dc.getInt("total");
    	}catch (Exception e) {
    		Logger.error(DashboardFactoryImpl.class, "checkPeriodData failed:" + e, e);
    		throw new DotRuntimeException(e.toString());
    	}
		return ret;
	}


}
