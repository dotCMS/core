package com.dotmarketing.portlets.htmlpageviews.factories;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;

/**
 * 
 * @author Rocco
 */
public class HTMLPageViewFactory {

	public static final String STAT_TOTAL_PAGE_VIEWS = "Total-Page-Views";
	public static final String STAT_UNIQUE_VISITORS = "Unique-Vistitors";
	
    private static String GET_TOTAL_HTML_PAGE_VIEWS = "select count(*) as num_views from clickstream_request where request_uri = ? and timestampper between ? and ? and host_id = ?";
    private static String GET_UNIQUE_VISITORS = "select count(distinct cookie_id) as num_views from clickstream_request, clickstream where clickstream.clickstream_id = clickstream_request.clickstream_id and request_uri = ? and timestampper between ? and ? and clickstream_request.host_id = ?";
    private static String GET_TOTAL_HTML_PAGE_VIEWS_BY_LANGUAGE = "select language, count(clickstream_request_id) as pageviews from clickstream_request, language where language.id = clickstream_request.language_id and request_uri = ? and timestampper between ? and ? and clickstream_request.host_id = ? group by language order by pageviews desc";
    private static String GET_TOP_INTERNAL_REFERRING_PAGES = "select cr2.request_uri, cr2.associated_identifier, count(cr.request_uri) as num_referring from clickstream_request cr left join clickstream_request cr2 on (cr2.request_order = cr.request_order - 1 and cr2.clickstream_id = cr.clickstream_id ) where cr2.request_uri is not null and cr2.request_uri <> ? and cr.request_uri = ? and cr.timestampper between ? and ? and cr.host_id = ? group by cr2.request_uri, cr2.associated_identifier order by num_referring desc";
    private static String GET_TOP_INTERNAL_EXIT_PAGES = "select cr2.request_uri, cr2.associated_identifier, count(cr.request_uri) as num_referring from clickstream_request cr left join clickstream_request cr2 on (cr2.request_order = cr.request_order + 1 and cr2.clickstream_id = cr.clickstream_id) where cr2.request_uri is not null and cr.request_uri = ? and cr2.request_uri <> ? and cr.timestampper between ? and ? and cr.host_id = ? group by cr2.request_uri, cr2.associated_identifier order by num_referring desc";
    private static String GET_TOP_EXTERNAL_REFERRING_PAGES = "select referer, count(request_uri) as num_referring from clickstream_request, clickstream where clickstream.clickstream_id = clickstream_request.clickstream_id and request_order = 1 and request_uri = ? and timestampper between ? and ? and clickstream_request.host_id = ? group by referer order by num_referring desc";
    private static String GET_TOP_USERS = "select user_id, count(request_uri) as num_views from clickstream_request, clickstream where clickstream.clickstream_id = clickstream_request.clickstream_id and request_uri = ? and timestampper between ? and ? and clickstream_request.host_id = ? group by user_id order by num_views desc";
    private static String GET_ALL_USERS = "select distinct user_id, User_.* from clickstream_request, clickstream, User_ where clickstream.clickstream_id = clickstream_request.clickstream_id and clickstream.user_id = User_.userid and  request_uri = ? and timestampper between ? and ? and clickstream_request.host_id = ?" ;
    
    private static String GET_CONTENTS_INODES_VIEWS = "select query_string as num_views from clickstream_request where request_uri = ? and timestampper between ? and ? and host_id = ?";
    private static String GET_CONTENTS_INODES_UNIQUE_VISITORS = "select query_string, clickstream.clickstream_id as clickstream_id from clickstream_request, clickstream where clickstream.clickstream_id = clickstream_request.clickstream_id and request_uri = ? and timestampper between ? and ? and clickstream_request.host_id = ?";
    
    private static String GET_TIME_ON_PAGE = "select clickstream_id, request_order, timestampper, request_uri from (select clickstream_id, request_order, timestampper, request_uri from clickstream_request a where request_uri = ? and " +
			"timestampper between ? and ? and host_id = ? " +
			"union select a.clickstream_id, a.request_order, a.timestampper, a.request_uri from clickstream_request a " +
			"join clickstream_request i on a.clickstream_id = i.clickstream_id and  "+
			"a.request_order = (i.request_order + 1) where i.request_uri = ? and " +
			"i.timestampper between ? and ? and i.host_id = ?) results order by clickstream_id, request_order";
    
    private static String GET_PAGE_CLICKSTREAM_BOUNCES = "select r1.clickstream_id from clickstream_request r1 join clickstream_request r2 on r1.clickstream_id = r2.clickstream_id " +
			"where r2.request_uri = ? and r2.timestampper between ? and ? and r2.host_id = ?  group by r1.clickstream_id having count(*) = 1";
    
    private static String GET_TOTAL_PAGE_CLICKSTREAMS = "select distinct clickstream_id from clickstream_request r2 where " +
    		"r2.request_uri = ? and r2.timestampper between ? and ? and r2.host_id = ?";  

    private static String GET_PAGES_VISIT = "select r1.clickstream_id, count(*) as count from clickstream_request r1 join clickstream_request r2 on r1.clickstream_id = r2.clickstream_id " +
			" where r2.request_uri = ? and r2.timestampper between ? and ? and r2.host_id = ? group by r1.clickstream_id";

    private static String GET_PAGE_EXIT_COUNT = "select count(*) as count from clickstream_request a where not exists (select * from clickstream_request i where  " +
    		"a.clickstream_id = i.clickstream_id and i.request_order = (a.request_order + 1)) and request_uri = ? " +
    		"and timestampper between ? and ? and host_id = ? ";
    
    private static String GET_SEARCH_ENGINE_VISITS = "select count(*) as count from clickstream where exists (select * from clickstream_request where " +
    	"request_uri = ? and timestampper between ? and ? and host_id = ? and clickstream_request.clickstream_id = clickstream.clickstream_id) and (";

    private static String GET_REFERRING_SITE_VISITS = "select count(*) as count from clickstream where exists (select * from clickstream_request where " +
		"request_uri = ? and timestampper between ? and ? and host_id = ? and clickstream_request.clickstream_id = clickstream.clickstream_id) and referer is not null and referer <> '' and not (";

    private static String GET_DIRECT_TRAFFIC_VISITS = "select count(*) as count from clickstream where exists (select * from clickstream_request where " +
		"request_uri = ? and timestampper between ? and ? and host_id = ? and clickstream_request.clickstream_id = clickstream.clickstream_id) and (referer is null or referer = '')";

    public static int getDirectTrafficVisits(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {

    	DotConnect db = new DotConnect();
        db.setSQL(GET_DIRECT_TRAFFIC_VISITS);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        return db.getInt("count");
        
    }
    
    public static int getReferringSiteVisits(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {

    	String[] searchEngineExpressions = Config.getStringArrayProperty("SEARCH_ENGINES");
    	StringBuilder sql = new StringBuilder(GET_REFERRING_SITE_VISITS);
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
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        return db.getInt("count");
        
    }
    
    public static int getSearchEngineVisits(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {

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
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        return db.getInt("count");
        
    }    
    public static int getPageExitRate(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {
        
    	DotConnect db = new DotConnect();
        db.setSQL(GET_PAGE_EXIT_COUNT);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        
        int exitCount = db.getInt("count");
        
        db.setSQL(GET_TOTAL_PAGE_CLICKSTREAMS);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        
        int totalCount = db.loadObjectResults().size();
        
        return totalCount == 0?0:exitCount * 100 / totalCount;

    }

    public static int getPageBounceRate(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {
        
    	DotConnect db = new DotConnect();
        db.setSQL(GET_PAGE_CLICKSTREAM_BOUNCES);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        
        int bouncesCount = db.loadObjectResults().size();
        
        db.setSQL(GET_TOTAL_PAGE_CLICKSTREAMS);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        
        int totalCount = db.loadObjectResults().size();
        
        return totalCount == 0?0:bouncesCount * 100 / totalCount;

    }
    
   public static long getPagesVisit(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {
        
    	DotConnect db = new DotConnect();
        db.setSQL(GET_PAGES_VISIT);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        
        List<Map<String, Object>> results = db.loadObjectResults();
        long totalVisits = 0;
        for(Map<String, Object> entry : results) {
        	totalVisits += entry.get("count") instanceof BigDecimal?((BigDecimal)entry.get("count")).intValue():entry.get("count") instanceof Integer?(Integer) entry.get("count"):(Long) entry.get("count");
        }
        return results.size() == 0?0:totalVisits / results.size();

    }
   
    public static long getTimeOnPage(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {
        
    	long totalTimeElapsed = 0;
    	
    	DotConnect db = new DotConnect();
        db.setSQL(GET_TIME_ON_PAGE);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        
        
        List<Map<String, Object>> results = db.loadObjectResults();
        for(int i = 0; i < results.size() - 1; i++) {
        	
        	Map<String, Object> entry1 = results.get(i);
        	Map<String, Object> entry2 = results.get(i + 1);
        	
        	long clickstreamId1 = entry1.get("clickstream_id") instanceof Long?(Long) entry1.get("clickstream_id"):((BigDecimal) entry1.get("clickstream_id")).longValue();
        	int requestOrder1 = entry1.get("request_order") instanceof Integer?(Integer) entry1.get("request_order"):((BigDecimal) entry1.get("request_order")).intValue();
        	Date requestTime1 = (Date) entry1.get("timestampper");
        	String requestURI1 = (String) entry1.get("request_uri");
        	
        	long clickstreamId2 = entry2.get("clickstream_id") instanceof Long?(Long) entry2.get("clickstream_id"):((BigDecimal) entry2.get("clickstream_id")).longValue();
        	int requestOrder2 = entry2.get("request_order") instanceof Integer?(Integer) entry2.get("request_order"):((BigDecimal) entry2.get("request_order")).intValue();
        	Date requestTime2 = (Date) entry2.get("timestampper");
        	//
        	if(clickstreamId1 != clickstreamId2 || requestOrder1 + 1 != requestOrder2 || !requestURI1.equals(uri)) 
        		continue;
        	
        	totalTimeElapsed += (requestTime2.getTime() - requestTime1.getTime()) / 1000L; 
        	
        }
        return totalTimeElapsed;
    }
    
    public static int getTotalHTMLPageViewsBetweenDates(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) {
        DotConnect db = new DotConnect();
        db.setSQL(GET_TOTAL_HTML_PAGE_VIEWS);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        return db.getInt("num_views");
    }

    public static class StatisticBetweenDates {
    	
    	private String statistic;
    	private Date startDate;
    	private Date endDate;
    	private Object data;
    	
		public StatisticBetweenDates(String statistic, Date startDate, Date endDate, Object data) {
			super();
			this.statistic = statistic;
			this.startDate = startDate;
			this.endDate = endDate;
			this.data = data;
		}
		
		public String getStatistic() {
			return statistic;
		}
		public void setStatistic(String statistic) {
			this.statistic = statistic;
		}
		public Date getStartDate() {
			return startDate;
		}
		public void setStartDate(Date startDate) {
			this.startDate = startDate;
		}
		public Date getEndDate() {
			return endDate;
		}
		public void setEndDate(Date endDate) {
			this.endDate = endDate;
		}
		public Object getData() {
			return data;
		}
		public void setData(Object data) {
			this.data = data;
		}
    	
    }
    
    public static List<StatisticBetweenDates> getTotalHTMLPageViewsBetweenDatesGroupByMonth(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) {
    	
    	List<StatisticBetweenDates> data = new ArrayList<StatisticBetweenDates>();
    	
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(startDate);
    	cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
    	Date currentMonthStart = cal.getTime();
    	while(currentMonthStart.before(endDate)) {
    		cal.setTime(currentMonthStart);
    		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
    		cal.set(Calendar.HOUR, 23);
    		cal.set(Calendar.MINUTE, 59);
    		cal.set(Calendar.SECOND, 59);
    		Date currentMonthEnd = cal.getTime();
            DotConnect db = new DotConnect();
            db.setSQL(GET_TOTAL_HTML_PAGE_VIEWS);
            db.addParam(uri);
            db.addParam(currentMonthStart);
            db.addParam(currentMonthEnd);
            db.addParam(hostId);
            int pageViews = db.getInt("num_views");            
            data.add(new StatisticBetweenDates(STAT_TOTAL_PAGE_VIEWS, currentMonthStart, currentMonthEnd, pageViews));
            cal.setTime(currentMonthStart);
            cal.add(Calendar.MONTH, 1);
            currentMonthStart = cal.getTime();
    	}
    		
    	return data;
    	
    }

    public static List<StatisticBetweenDates> getTotalHTMLPageViewsBetweenDatesGroupByDay(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) {
    	
    	List<StatisticBetweenDates> data = new ArrayList<StatisticBetweenDates>();
    	
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(startDate);    	
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
    	Date currentDayStart = cal.getTime();
    	while(currentDayStart.before(endDate)) {
    		cal.setTime(currentDayStart);    		
    		cal.set(Calendar.HOUR, 23);
    		cal.set(Calendar.MINUTE, 59);
    		cal.set(Calendar.SECOND, 59);
    		Date currentDayEnd = cal.getTime();
            DotConnect db = new DotConnect();
            db.setSQL(GET_TOTAL_HTML_PAGE_VIEWS);
            db.addParam(uri);
            db.addParam(currentDayStart);
            db.addParam(currentDayEnd);
            db.addParam(hostId);
            int pageViews = db.getInt("num_views");           
            data.add(new StatisticBetweenDates(STAT_TOTAL_PAGE_VIEWS, currentDayStart, currentDayEnd, pageViews));
            cal.setTime(currentDayStart);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            currentDayStart = cal.getTime();
    	}
    		
    	return data;
    	
    }
    
    public static List<StatisticBetweenDates> getTotalHTMLPageViewsBetweenDatesGroupByWeek(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) {
    	
    	List<StatisticBetweenDates> data = new ArrayList<StatisticBetweenDates>();
    	
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(startDate);    	
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
    	Date currentWeekStart = cal.getTime();
    	while(currentWeekStart.before(endDate)) {
    		cal.setTime(currentWeekStart);
    		cal.add(Calendar.DAY_OF_MONTH, 7);    		
    		cal.set(Calendar.HOUR, 23);
    		cal.set(Calendar.MINUTE, 59);
    		cal.set(Calendar.SECOND, 59);
    		Date currentWeekEnd = cal.getTime();
            DotConnect db = new DotConnect();
            db.setSQL(GET_TOTAL_HTML_PAGE_VIEWS);
            db.addParam(uri);
            db.addParam(currentWeekStart);
            db.addParam(currentWeekEnd);
            db.addParam(hostId);
            int pageViews = db.getInt("num_views");
            data.add(new StatisticBetweenDates(STAT_TOTAL_PAGE_VIEWS, currentWeekStart, currentWeekEnd, pageViews));
            cal.setTime(currentWeekStart);
            cal.add(Calendar.DAY_OF_MONTH, 7);
            currentWeekStart = cal.getTime();
    	}
    		
    	return data;
    	
    }    
    
    public static int getUniqueVisitorsBetweenDates(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) {
        DotConnect db = new DotConnect();
        db.setSQL(GET_UNIQUE_VISITORS);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        return db.getInt("num_views");
    }

    
    public static List<StatisticBetweenDates> getUniqueVisitorsBetweenDatesGroupByMonth(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) {
    	
    	List<StatisticBetweenDates> data = new ArrayList<StatisticBetweenDates>();
    	
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(startDate);
    	cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
    	Date currentMonthStart = cal.getTime();
    	while(currentMonthStart.before(endDate)) {
    		cal.setTime(currentMonthStart);
    		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
    		cal.set(Calendar.HOUR, 23);
    		cal.set(Calendar.MINUTE, 59);
    		cal.set(Calendar.SECOND, 59);
    		Date currentMonthEnd = cal.getTime();
            DotConnect db = new DotConnect();
            db.setSQL(GET_UNIQUE_VISITORS);
            db.addParam(uri);
            db.addParam(currentMonthStart);
            db.addParam(currentMonthEnd);
            db.addParam(hostId);
            int pageViews = db.getInt("num_views");
            data.add(new StatisticBetweenDates(STAT_UNIQUE_VISITORS, currentMonthStart, currentMonthEnd, pageViews));
            cal.setTime(currentMonthStart);
            cal.add(Calendar.MONTH, 1);
            currentMonthStart = cal.getTime();
    	}
    		
    	return data;
    	
    }
    
    public static List<StatisticBetweenDates> getUniqueVisitorsBetweenDatesGroupByWeek(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) {
    	
    	List<StatisticBetweenDates> data = new ArrayList<StatisticBetweenDates>();
    	
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(startDate);    	
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
    	Date currentWeekStart = cal.getTime();
    	while(currentWeekStart.before(endDate)) {
    		cal.setTime(currentWeekStart);
    		cal.add(Calendar.DAY_OF_MONTH, 5);
    		cal.set(Calendar.HOUR, 23);
    		cal.set(Calendar.MINUTE, 59);
    		cal.set(Calendar.SECOND, 59);
    		Date currentWeekEnd = cal.getTime();
            DotConnect db = new DotConnect();
            db.setSQL(GET_UNIQUE_VISITORS);
            db.addParam(uri);
            db.addParam(currentWeekStart);
            db.addParam(currentWeekEnd);
            db.addParam(hostId);
            int pageViews = db.getInt("num_views");
            data.add(new StatisticBetweenDates(STAT_UNIQUE_VISITORS, currentWeekStart, currentWeekEnd, pageViews));
            cal.setTime(currentWeekStart);
            cal.add(Calendar.DAY_OF_MONTH, 7);
            currentWeekStart = cal.getTime();
    	}
    		
    	return data;
    	
    }
    
    public static List<StatisticBetweenDates> getUniqueVisitorsBetweenDatesGroupByDay(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) {
    	
    	List<StatisticBetweenDates> data = new ArrayList<StatisticBetweenDates>();
    	
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(startDate);    	
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
    	Date currentDayStart = cal.getTime();
    	while(currentDayStart.before(endDate)) {
    		cal.setTime(currentDayStart);    		
    		cal.set(Calendar.HOUR, 23);
    		cal.set(Calendar.MINUTE, 59);
    		cal.set(Calendar.SECOND, 59);
    		Date currentDayEnd = cal.getTime();
            DotConnect db = new DotConnect();
            db.setSQL(GET_UNIQUE_VISITORS);
            db.addParam(uri);
            db.addParam(currentDayStart);
            db.addParam(currentDayEnd);
            db.addParam(hostId);
            int pageViews = db.getInt("num_views");
            data.add(new StatisticBetweenDates(STAT_UNIQUE_VISITORS, currentDayStart, currentDayEnd, pageViews));
            cal.setTime(currentDayStart);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            currentDayStart = cal.getTime();
    	}
    		
    	return data;
    	
    }
    
    @SuppressWarnings("unchecked")
	public static List<Map<String, String>> getTotalHTMLPageViewsByLanguageBetweenDates(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {
        DotConnect db = new DotConnect();
        db.setSQL(GET_TOTAL_HTML_PAGE_VIEWS_BY_LANGUAGE);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        return db.loadResults();
    }

    @SuppressWarnings("unchecked")
	public static List<Map<String, String>> getTopInternalReferringPages(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {
        DotConnect db = new DotConnect();
        db.setSQL(GET_TOP_INTERNAL_REFERRING_PAGES);
        db.addParam(uri);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        db.setMaxRows(5);
        return db.loadResults();
    }
    
    @SuppressWarnings("unchecked")
	public static List<Map<String, String>> getTopInternalOutgoingPages(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {
        DotConnect db = new DotConnect();
        db.setSQL(GET_TOP_INTERNAL_EXIT_PAGES);
        db.addParam(uri);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        db.setMaxRows(5);
        return db.loadResults();
    }
    
    @SuppressWarnings("unchecked")
	public static List<Map<String, String>> getTopExternalReferringPages(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {
        DotConnect db = new DotConnect();
        db.setSQL(GET_TOP_EXTERNAL_REFERRING_PAGES);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        db.setMaxRows(5);
        return db.loadResults();
    }

    @SuppressWarnings("unchecked")
	public static List<Map<String, String>> getTopUsers(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {
        DotConnect db = new DotConnect();
        db.setSQL(GET_TOP_USERS);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        db.setMaxRows(10);
        return db.loadResults();
    }
    
    @SuppressWarnings("unchecked")
	public static List<Map<String, String>> getAllUsers(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {
        DotConnect db = new DotConnect();
        db.setSQL(GET_ALL_USERS);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        return db.loadResults();
    }
    
    @SuppressWarnings("unchecked")
	public static List<String> getContentsInodesViewsBetweenDates(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {
        DotConnect db = new DotConnect();
        db.setMaxRows(-1);
        
        db.setSQL(GET_CONTENTS_INODES_VIEWS);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        List<HashMap<String, String>> result = db.loadResults();
        List<String> inodes = new ArrayList<String>();
        
        java.util.StringTokenizer parameters;
        String parameter;
        String inode;
        String inodeStr="";
        
        for (HashMap<String, String> row: result) {
        	inode = row.get("num_views");
        	
        	if ((inode != null) && (!inode.trim().equals(""))) {
        		parameters = new java.util.StringTokenizer(inode, "&");
        		
        		for (; parameters.hasMoreTokens();) {
        			parameter = parameters.nextToken();
        			
        			if (parameter.indexOf("inode=") == 0) {
        				try {
        					if (inodes.size() == 0)
        						inodes.add(parameter.substring(parameter.indexOf("=")+1));
        					else {
        						inodeStr = parameter.substring(parameter.indexOf("=")+1);
        						
    							inodes.add(inodeStr);
        					}
        				} catch (NumberFormatException e) {
        				}
        			}
        		}
        	}
        }

        Collections.sort(inodes);
        
        return inodes;
    }
    
    @SuppressWarnings("unchecked")
	public static List<String> getContentsInodesUniqueVisitorsBetweenDates(String uri, java.util.Date startDate, java.util.Date endDate, String hostId) throws DotDataException {
        DotConnect db = new DotConnect();
        db.setMaxRows(-1);
        
        db.setSQL(GET_CONTENTS_INODES_UNIQUE_VISITORS);
        db.addParam(uri);
        db.addParam(startDate);
        db.addParam(endDate);
        db.addParam(hostId);
        List<HashMap<String, String>> result = db.loadResults();
        ArrayList<String> inodes = new ArrayList<String>();
        ArrayList<String> uniqueVisits = new ArrayList<String>();
        
        java.util.StringTokenizer parameters;
        String parameter;
        String inode;
        String clickstreamId;
        String inodeStr="";
        
        for (HashMap<String, String> row: result) {
        	inode = row.get("query_string");
        	clickstreamId = row.get("clickstream_id");
        	
        	if ((inode != null) && (!inode.trim().equals(""))) {
        		parameters = new java.util.StringTokenizer(inode, "&");
        		
        		for (; parameters.hasMoreTokens();) {
        			parameter = parameters.nextToken();
        			
        			if (parameter.indexOf("inode=") == 0) {
        				try {
    						inodeStr = parameter.substring(parameter.indexOf("=")+1);
    						//Filtering unique visits
    						if(!uniqueVisits.contains(inodeStr + "-" + clickstreamId)) {
    							inodes.add(inodeStr);
    							uniqueVisits.add(inodeStr + "-" + clickstreamId);
    						}
        				} catch (NumberFormatException e) {
        				}
        			}
        		}
        	}
        }
        Collections.sort(inodes);

        return inodes;
    }
}