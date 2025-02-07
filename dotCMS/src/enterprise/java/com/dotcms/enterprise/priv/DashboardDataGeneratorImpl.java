/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.priv;

import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.dashboard.business.DashboardDataGenerator;
import com.dotmarketing.portlets.dashboard.model.DashboardSummary;
import com.dotmarketing.portlets.dashboard.model.DashboardSummary404;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryContent;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryPage;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryPeriod;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryReferer;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryVisits;
import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DashboardDataGeneratorImpl extends DashboardDataGenerator{

	private volatile boolean flag = true;

	private volatile boolean finished = false;

	private int monthFrom = 0;

	private int yearFrom = 0;

	private int monthTo = 0;

	private int yearTo = 0;

	double currentProgress = 0;

	long rowCount = 0;

	private List<String> errors =  new ArrayList<>();

	public void setFlag(boolean flag){
		this.flag = flag;
	}

	public boolean isFinished(){
		return finished;
	}

	public double getProgress(){
		return currentProgress;
	}

	public List<String> getErrors(){
		return this.errors;
	}

	public long getRowCount(){
		return rowCount;
	}

	public int getMonthFrom() {
		return monthFrom;
	}

	public int getYearFrom() {
		return yearFrom;
	}

	public int getMonthTo() {
		return monthTo;
	}

	public int getYearTo() {
		return yearTo;
	}


	public DashboardDataGeneratorImpl(int monthFrom, int yearFrom, int monthTo, int yearTo){
		if(System.getProperty("dotcms_level") !=null &&
				Integer.parseInt(System.getProperty("dotcms_level"))!= LicenseLevel.COMMUNITY.level){
			if((yearTo>yearFrom) || (yearTo==yearFrom && monthTo>monthFrom)){
				this.monthFrom = monthFrom;
				this.yearFrom = yearFrom;
				this.monthTo = monthTo;
				this.yearTo = yearTo;
			}else{
				throw new IllegalArgumentException("Please provide a valid date interval");
			}
		}else{
			throw new RuntimeException("Not a valid license");
		}

	}



	private static Map<String, ArrayList<Map<String, Object>>> results404 = new HashMap<>();

	private static Map<String, ArrayList<Map<String, Object>>> content = new HashMap<>();

	private static Map<String, ArrayList<Map<String, Object>>> pages = new HashMap<>();


	public synchronized void start(){
		Runnable dataGeneratorRunnable = new Runnable(){
			public void run(){	
				try{
					while(flag){
						generate();
					}
				}catch(Exception e){
					flag = false;
				}finally{
					flag = false;
				}
			}
		};
		Thread dataGeneratorThread = new Thread(dataGeneratorRunnable);
		dataGeneratorThread.start();

	}

	private synchronized void generate() throws DotDataException, DotSecurityException{


		List<Host> hosts = APILocator.getHostAPI().findAll(APILocator.getUserAPI().getSystemUser(), false);

		Map<Integer, List<Integer>> yearMonths = new HashMap<>();
		List<Integer> months = null;
		for(int year=yearFrom;year<=yearTo;year++){
			if(year==yearFrom){
				months = new ArrayList<>();
				for(int i = monthFrom-1; i<=11 ;i++ ){
					months.add(i);
				}
				yearMonths.put(year, months);	
			}else if(year == yearTo){
				months = new ArrayList<>();
				for(int i = 0; i<=monthTo-1 ;i++ ){
					months.add(i);
				}
				yearMonths.put(year, months);	
			}else{
				months = new ArrayList<>();
				for(int i = 0; i<=11 ;i++ ){
					months.add(i);
				}
				yearMonths.put(year, months);	
			}
		}



		List<Calendar> calendars = new ArrayList<>();

		for(int year=yearFrom;year<=yearTo;year++){
			for(Integer month : yearMonths.get(year)){
				Calendar calendar = Calendar.getInstance();
				calendar.set(Calendar.YEAR, year);
				calendar.set(Calendar.MONTH, month);
				int k = calendar.getMinimum(Calendar.DAY_OF_MONTH);
				int m = calendar.getMaximum(Calendar.DAY_OF_MONTH);
				for(int day=k;day<=m;day++){
					Calendar calendar2 = Calendar.getInstance(); 
					calendar2.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
					calendar2.set(Calendar.MONTH,calendar.get(Calendar.MONTH));
					calendar2.set(Calendar.DAY_OF_MONTH,day);
					calendar2.set(Calendar.HOUR_OF_DAY,0);
					calendar2.set(Calendar.MINUTE, 0);
					calendar2.set(Calendar.SECOND,0);
					calendar2.set(Calendar.MILLISECOND,0);	 
					if(!calendars.contains(calendar2)){
						calendars.add(calendar2);
					}
				}

			}

		}
		try{

			HibernateUtil.startTransaction();
			if(!calendars.isEmpty()){
				List<DashboardSummaryPeriod> summaryPeriods = new ArrayList<>();
				for(Calendar calendar:calendars){
					if(flag){
						DashboardSummaryPeriod summaryPeriod = null;
						try{
							summaryPeriod = insertSummaryPeriod(calendar);
						}catch(Exception e){
							if(e.getMessage().contains("com.dotcms.repackage.net.sf.hibernate.UnresolvableObjectException")){
								errors.add("Data for period  = "+ (calendar.get(Calendar.MONTH)+1) + " - " + (calendar.get(Calendar.DAY_OF_MONTH))  + " - " + calendar.get(Calendar.YEAR) + " already exists and has been ignored.");		
							}else{
								errors.add(e.getMessage());
							}
						}
						if(summaryPeriod!=null){
							rowCount+=1;
							summaryPeriods.add(summaryPeriod);
							currentProgress += 20.0/(double)calendars.size();
						}
					}
				}
				if(!summaryPeriods.isEmpty()){
					for(Host host : hosts){
						if(flag){
							if(!host.isSystemHost()){
								for(DashboardSummaryPeriod summaryPeriod : summaryPeriods){
									if(flag){
										DotConnect dc = null;
										if(content.get(host.getIdentifier())==null){
											dc = new DotConnect();
											dc.setSQL(getContentQuery());
											dc.addParam(host.getIdentifier());
											content.put(host.getIdentifier(), dc.loadResults());
										}

										insertSummaryVisits(summaryPeriod, host);
										insertSummary404(summaryPeriod,host);
										DashboardSummary summary = insertSummary(summaryPeriod, host);
										if(summary!=null){
											insertSummaryPages(summary);
											insertSummaryContent(summary);
											insertSummaryReferers(summary);

										}
									} 
									currentProgress+= 75.0/(double)(summaryPeriods.size() * (hosts.size()-1));
								}

							}

						}

					}
				}
			}
			if(flag){
				insertWorkStreams();
				DotConnect dc = new DotConnect();
				dc.setSQL("select count(*) as totalrows from analytic_summary_workstream");
				rowCount+=dc.getInt("totalrows");
				currentProgress+= 5.0;
			}

		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			Logger.error(DashboardDataGeneratorImpl.class, "generate failed:" + e, e);
			errors.add("Process failed :" + e);
			rowCount = 0;
			throw new DotRuntimeException(e.toString());
		}finally{
			if(flag){
				HibernateUtil.closeSession();
			}else{
				HibernateUtil.rollbackTransaction();
			}
			DbConnectionFactory.closeConnection();
			flag = false;
			finished = true;
			currentProgress = (double)100;
		}
	}


	private DashboardSummaryPeriod insertSummaryPeriod(Calendar calendar) {

		if(calendar!=null){
			String[] strDays = new String[] { "Sunday", "Monday", "Tuesday", "Wednesday", "Thusday",
					"Friday", "Saturday" };
			String[] monthName = {"January", "February",
					"March", "April", "May", "June", "July",
					"August", "September", "October", "November","December"};		
			try{
				DashboardSummaryPeriod summaryPeriod = new DashboardSummaryPeriod();
				summaryPeriod.setFullDate(calendar.getTime());
				summaryPeriod.setDay(calendar.get(Calendar.DAY_OF_MONTH));
				summaryPeriod.setMonth(calendar.get(Calendar.MONTH)+1);
				summaryPeriod.setYear(String.valueOf(calendar.get(Calendar.YEAR)));
				summaryPeriod.setWeek(calendar.get(Calendar.WEEK_OF_MONTH));
				summaryPeriod.setDayName(strDays[calendar.get(Calendar.DAY_OF_WEEK) - 1]);
				summaryPeriod.setMonthName(monthName[calendar.get(Calendar.MONTH)]);
				HibernateUtil.save(summaryPeriod);
				HibernateUtil.getSession().refresh(summaryPeriod);
				return summaryPeriod;
			} catch (Exception e) {
				throw new DotRuntimeException(e.toString());
			}
		}

		return null;
	}


	private void insertSummaryVisits(DashboardSummaryPeriod summaryPeriod, Host host){

		if(summaryPeriod!=null){

			Calendar calendar = Calendar.getInstance();
			calendar.setTime(summaryPeriod.getFullDate());
			try {
				int minHour  = calendar.getMinimum(Calendar.HOUR_OF_DAY);
				int maxHour  = calendar.getMaximum(Calendar.HOUR_OF_DAY);
				int count = 0;
				for (int hour = minHour; hour <=maxHour ; hour++) {
					Random random = new Random();
					int randomInt = random.nextInt(10000);
					Long totalVisits = (long)randomInt;
					calendar.set(Calendar.HOUR_OF_DAY, hour);
					calendar.set(Calendar.MINUTE, 0);
					calendar.set(Calendar.SECOND, 0);
					calendar.set(Calendar.MILLISECOND,0);
					DashboardSummaryVisits summaryVisit = new DashboardSummaryVisits();
					summaryVisit.setHostId(host.getIdentifier());
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
					rowCount+=1;
				}

			} catch (Exception e) {
				Logger.error(DashboardFactoryImpl.class, "insertSummaryVisits failed:" + e, e);
				throw new DotRuntimeException(e.toString());
			}

		}

	}

	private  void insertSummary404(DashboardSummaryPeriod summaryPeriod, Host host){
		String[] referers = new String[] { "http://www.dzone.com", "http://www.ycombinator.com",
				"http://www.javaranch.com","http://www.dotcms.com",
				"http://www.cnn.com", "http://www.linkedin.com",
				"http://www.oracle.com","http://www.cmswire.com",
				"http://www.youtube.com","http://www.amazon.com",
				"http://www.skype.com", "http://www.stackoverflow.com",
				"http://www.wikipedia.org", "http://www.java.com","http://www.google.com", "http://www.bing.com",
				"http://www.yahoo.com","http://www.ask.com","http://www.altavista.com"};
		List<String> refererList = new ArrayList<>(Arrays.asList(referers));

		try {
			ArrayList<Map<String, Object>> results404List = results404.get(host.getIdentifier());

			int count = 0;
			for (int i = 0; i < results404List.size(); i++) {
				Map<String, Object> hash = (Map<String, Object>) results404List.get(i);
				if(!hash.isEmpty()){
					String uri = (String) hash.get("uri");
					Random random = new Random();
					int randomInt = random.nextInt(refererList.size());
					String refererUri = refererList.get(randomInt);
					DashboardSummary404 summary404 = new DashboardSummary404();
					summary404.setHostId(host.getIdentifier());
					summary404.setRefererUri(refererUri);
					summary404.setSummaryPeriod(summaryPeriod);
					summary404.setUri(uri);
					HibernateUtil.save(summary404);
					HibernateUtil.getSession().refresh(summaryPeriod);
					count++;
					if(count % 20 == 0){ 
						HibernateUtil.getSession().flush(); 
						HibernateUtil.getSession().clear(); 
					}
					rowCount+=1;
				}
			}


		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "insertSummary404 failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}


	}

	private DashboardSummary insertSummary(DashboardSummaryPeriod summaryPeriod, Host host){

		String hostId = host.getIdentifier();
		try {
			int count = 0;
			DashboardSummary summary = new DashboardSummary();
			summary.setSummaryPeriod(summaryPeriod);
			summary.setHostId(hostId);
			Random random = new Random();
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.YEAR, 1970);
			calendar.set(Calendar.MONTH, 0);
			calendar.set(Calendar.DAY_OF_MONTH, 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, random.nextInt(59));
			calendar.set(Calendar.SECOND, random.nextInt(59));
			calendar.set(Calendar.MILLISECOND,0);
			Date timeOnSite = calendar.getTime();
			summary.setAvgTimeOnSite(timeOnSite);
			summary.setBounceRate(random.nextInt(100));
			summary.setDirectTraffic(random.nextInt(10000));
			summary.setSearchEngines(random.nextInt(10000));
			summary.setReferringSites(random.nextInt(10000));
			summary.setNewVisits(random.nextInt(10000));
			summary.setUniqueVisits(random.nextInt(10000));
			summary.setPageViews(random.nextInt(10000));
			summary.setVisits(random.nextInt(10000));
			HibernateUtil.save(summary);
			HibernateUtil.getSession().refresh(summaryPeriod);
			count++;
			if(count % 20 == 0){ 
				HibernateUtil.getSession().flush(); 
				HibernateUtil.getSession().clear(); 
			}
			rowCount+=1;
			return summary;


		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "insertSummary failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}


	private void insertSummaryPages(DashboardSummary summary){

		ArrayList<Map<String, Object>> pagesList = pages.get(summary.getHostId());

		try {
			int count =0;
			for (int i = 0; i < pagesList.size(); i++) {
				Map<String, Object> hash = (Map<String, Object>) pagesList.get(i);
				if(!hash.isEmpty() && flag){
					Random random = new Random();
					long hits = random.nextInt(10000);
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
					rowCount+=1;

				}
			}
		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "insertSummaryPages failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

	}

	private void insertSummaryContent(DashboardSummary summary){

		ArrayList<Map<String, Object>> contentList = content.get(summary.getHostId());
		try {
			int count =0;
			for (int i = 0; i < contentList.size(); i++) {
				Map<String, Object> hash = (Map<String, Object>) contentList.get(i);
				if(!hash.isEmpty() && flag){
					Random random = new Random();
					long hits = random.nextInt(10000);
					String inode = (String) hash.get("inode");
					String title = (String) hash.get("title");
					DashboardSummaryContent summaryContent = new DashboardSummaryContent();
					summaryContent.setHits(hits);
					summaryContent.setInode(inode);
					summaryContent.setSummary(summary);
					summaryContent.setUri("");
					summaryContent.setTitle(title);
					HibernateUtil.save(summaryContent);
					HibernateUtil.getSession().refresh(summary);
					count++;
					if(count % 20 == 0){ 
						HibernateUtil.getSession().flush(); 
						HibernateUtil.getSession().clear(); 
					}
					rowCount+=1;

				}
			}
		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "insertSummaryContent failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

	}

	private void insertSummaryReferers(DashboardSummary summary){

		String[] referers = new String[] { "http://www.dzone.com", "http://www.ycombinator.com",
				"http://www.javaranch.com","http://www.dotcms.com",
				"http://www.cnn.com", "http://www.linkedin.com",
				"http://www.oracle.com","http://www.cmswire.com",
				"http://www.youtube.com","http://www.amazon.com",
				"http://www.skype.com", "http://www.stackoverflow.com",
				"http://www.wikipedia.org", "http://www.java.com","http://www.google.com", "http://www.bing.com",
				"http://www.yahoo.com","http://www.ask.com","http://www.altavista.com"};
		List<String> refererList = new ArrayList<>(Arrays.asList(referers));
		try {
			int count =0;
			for(String referer : refererList){
				Random random = new Random();
				long hits = random.nextInt(10000);
				DashboardSummaryReferer summaryReferer= new DashboardSummaryReferer();
				summaryReferer.setSummary(summary);
				summaryReferer.setUri(referer);
				summaryReferer.setHits(hits);
				HibernateUtil.save(summaryReferer);
				HibernateUtil.getSession().refresh(summary);
				count++;
				if(count % 20 == 0){ 
					HibernateUtil.getSession().flush(); 
					HibernateUtil.getSession().clear(); 
				}
				rowCount+=1;

			}



		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "insertSummaryReferers failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

	}


	private void insertWorkStreams(){

		DotConnect dc = new DotConnect();
		try {
			if(DbConnectionFactory.isOracle()){
				dc.setSQL("insert into analytic_summary_workstream(id, inode, asset_type, mod_user_id, host_id, mod_date, action, name) select workstream_seq.NEXTVAL, t.* from ( select "+getWorkstreamQuery() +") t ");	
			}else if(DbConnectionFactory.isPostgres()){
				dc.setSQL("insert into analytic_summary_workstream(id, inode, asset_type, mod_user_id, host_id, mod_date, action, name) select nextval('workstream_seq'), "+getWorkstreamQuery());
			}else{
				dc.setSQL("insert into analytic_summary_workstream(inode, asset_type, mod_user_id, host_id, mod_date, action, name) select "+getWorkstreamQuery());
			}
			dc.loadResult();
		} catch (Exception e) {
			Logger.error(DashboardFactoryImpl.class, "insertSummaryReferers failed:" + e, e);
			throw new DotRuntimeException(e.toString(),e);
		}

	}

}
