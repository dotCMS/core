package com.dotmarketing.portlets.calendar.business;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryParser.ParseException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import edu.emory.mathcs.backport.java.util.Arrays;

public class EventFactoryImpl extends EventFactory {


	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private LanguageAPI languageAPI = APILocator.getLanguageAPI();
	private HostAPI hostAPI = APILocator.getHostAPI();
	private CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	private PermissionAPI perAPI = APILocator.getPermissionAPI();
	
	/**
	 * Search implemented using lucene
	 * @throws DotSecurityException 
	 * @throws NumberFormatException 
	 */
	@Override
	protected List<Event> find(Date fromDate, Date toDate, String[] tags,
			String[] keywords, List<Category> categories, boolean liveOnly, boolean includeArchived, int offset, int limit,
			User user, boolean respectFrontendRoles) throws DotDataException, NumberFormatException, DotSecurityException {
		return find(null, fromDate, toDate, tags, keywords, categories, liveOnly, includeArchived, offset, limit, user, respectFrontendRoles);
	}
	
	/**
	 * Search implemented using lucene
	 * @throws DotSecurityException 
	 * @throws NumberFormatException 
	 */
	@Override
	protected List<Event> find(String hostId, Date fromDate, Date toDate, String[] tags,
			String[] keywords, List<Category> categories, boolean liveOnly, boolean includeArchived, int offset, int limit,
			User user, boolean respectFrontendRoles) throws DotDataException, NumberFormatException, DotSecurityException {
		if(keywords == null)
			keywords = new String[0];
		if(tags == null)
			tags = new String[0];
		Structure eventStructure = getEventStructure();
		Field startDateF = eventStructure.getFieldVar("startDate");
		Field endDateF = eventStructure.getFieldVar("endDate");
		Field titleF = eventStructure.getFieldVar("title");
		Field descriptionF = eventStructure.getFieldVar("description");
		Field tagsF = eventStructure.getFieldVar("tags");
		Field recurEndF = eventStructure.getFieldVar("recurrenceEnd");
		Field recurNoEndF = eventStructure.getFieldVar("noRecurrenceEnd");
		Field recurs = eventStructure.getFieldVar("recurs");

		String fromDateQuery = new SimpleDateFormat("yyyyMMddHHmmss").format(fromDate)+"*";
		String fromDateQueryRec = new SimpleDateFormat("yyyy").format(fromDate)+"*";
		String toDateQuery = new SimpleDateFormat("yyyyMMddHHmmss").format(toDate);
		
		StringBuffer query = new StringBuffer ("+type:content +structureInode:" + eventStructure.getInode() +
			" +" + startDateF.getFieldContentlet() + ":[19000101000000" + " TO " + toDateQuery + "] " +
			" +(" + endDateF.getFieldContentlet() + ":[" + fromDateQuery + " TO 30000101000000] " + "(+("+
			        recurEndF.getFieldContentlet() + ":[" + fromDateQueryRec + " TO 30000101000000] " +
			        recurNoEndF.getFieldContentlet() + ":true ) "+ "-"+recurs.getFieldContentlet()+":false))"
			
		);
		
		Host systemHost = hostAPI.findSystemHost(user, false);
		if (UtilMethods.isSet(hostId)) {
			query.append("+(conHost:" + hostId + " conHost:" + systemHost.getIdentifier() + ")");
		}
		
		if(liveOnly)
			query.append(" +live:true");
		else if(includeArchived)
			query.append(" +(working:true deleted:true)");
		else
			query.append(" +working:true +deleted:false");
		
		String[] keywordTokens;
		for (String keyword : keywords) {
			if (UtilMethods.isSet(keyword)) {
				keywordTokens = keyword.trim().split(" ");
				for (String keywordToken : keywordTokens) {
					if (UtilMethods.isSet(keywordToken)) {
						keywordToken = keywordToken.trim();
						query.append(" +(" + titleF.getFieldContentlet() + ": " + keywordToken.replaceAll("\"", "").trim() + "* " +
						descriptionF.getFieldContentlet() + ": " + keywordToken.replaceAll("\"", "").trim() + "* " +
						tagsF.getFieldContentlet() + ": " + keywordToken.replaceAll("\"", "").trim() + "*)");
					}
				}
			}
		}
		
		for(String tag : tags) {
			tag=tag.trim();
			if(UtilMethods.isSet(tag)) {
				query.append(" +(" + tagsF.getFieldContentlet() + ":" + tag.replaceAll("\"", "").replaceAll(":", "").trim() + "*)"); 
			}
		}
		
		if(categories != null) {
			query.append(categoriesQueryFilter(categories));
		}
		
		return findInLucene(query.toString(), fromDate, toDate, liveOnly, offset, limit, user, respectFrontendRoles);
		
	}

	
	private String categoriesQueryFilter (List<Category> categories) {
		StringBuffer luceneQuery = new StringBuffer();
		if(categories.size() > 0) {
			luceneQuery.append(" +(");
			for(Category cat : categories) {
				luceneQuery.append("categories:" + cat.getCategoryVelocityVarName()+ " ");
			}
			luceneQuery.append(") ");
		}
		return luceneQuery.toString();
	}
	
	
	@Override
	protected Event find(String identifier, boolean live, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		
		Event ev = null;
		
		Language lang = languageAPI.getDefaultLanguage();
		Contentlet cont = conAPI.findContentletByIdentifier(identifier, live, lang.getId(), user, respectFrontendRoles);
		if(cont == null)
			return null;
		ev = convertToEvent(cont);
		
		return ev;
		
	}
	
	@Override
	protected Event findbyInode(String inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		
		Event ev = null;
		
		Contentlet cont = conAPI.find(inode, user, respectFrontendRoles);
		if(cont == null)
			return null;
		ev = convertToEvent(cont);
		
		return ev;
		
	}

	//Structure creation constants
	/**
	 * Returns the event structure
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected Structure getBuildingStructure() {
		Structure eventStructure = StructureCache.getStructureByName(BUILDING_STRUCTURE_NAME);
		return eventStructure;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Structure getEventStructure() {
		Structure eventStructure = StructureCache.getStructureByName(EVENT_STRUCTURE_NAME);
		return eventStructure;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Structure getLocationStructure() {
		Structure eventStructure = StructureCache.getStructureByName(FACILITY_STRUCTURE_NAME);
		return eventStructure;
	}	
	
	private static final String EVENT_STRUCTURE_NAME = "Event";
	private static final String EVENT_STRUCTURE_DESCRIPTION = "Calendar Events";
	private static final String BUILDING_STRUCTURE_NAME = "Building";
	private static final String BUILDING_STRUCTURE_DESCRIPTION = "Buildings";
	private static final String FACILITY_STRUCTURE_NAME = "Facility";
	private static final String FACILITY_STRUCTURE_DESCRIPTION = "Facilities";

	private static void initEventEventRelation(Structure eventStructure) throws DotHibernateException {
		
		if(true)return;
		Relationship relationship = RelationshipFactory.getRelationshipByRelationTypeValue("Event-Event");
		
		if (relationship == null) {

			// Create the relationship
			relationship = new Relationship();
			relationship.setCardinality(0);
			relationship.setChildRelationName("Event");
			relationship.setParentRelationName("Event");
			relationship.setChildStructureInode(eventStructure.getInode());
			relationship.setParentStructureInode(eventStructure.getInode());
			relationship.setRelationTypeValue("Event-Event");
			relationship.setParentRequired(false);
			relationship.setChildRequired(false);
			RelationshipFactory.saveRelationship(relationship);

		}
	}	
	
	private static void initBuidlingFacilityRelation(Structure buildingStructure, Structure facilityStructure) throws DotHibernateException {
		if(true)return;
		Relationship relationship = RelationshipFactory.getRelationshipByRelationTypeValue("Building-Facility");
		
		if (relationship == null || !InodeUtils.isSet(relationship.getInode())) {

			// Create the relationship
			relationship = new Relationship();
			relationship.setCardinality(0);
			relationship.setParentRelationName("Building");
			relationship.setChildRelationName("Facility");
			relationship.setParentStructureInode(buildingStructure.getInode());
			relationship.setChildStructureInode(facilityStructure.getInode());
			relationship.setRelationTypeValue("Building-Facility");
			relationship.setParentRequired(true);
			relationship.setChildRequired(false);
			RelationshipFactory.saveRelationship(relationship);

		}
	}

	protected Event convertToEvent (Contentlet cont) throws DotDataException, DotContentletStateException, DotSecurityException {
		
		Event ev = new Event();
		ev.setStructureInode(getEventStructure().getInode());
		Map<String, Object> contentletMap = cont.getMap();
		conAPI.copyProperties(ev, contentletMap);		
		return ev;
	}
	
	private List<Event> findInLucene(String query, Date dateFrom, Date dateTo, boolean liveOnly, int offset, int limit, User user, boolean respectFrontendRoles) throws DotDataException, NumberFormatException, DotSecurityException {

		Structure eventStructure = getEventStructure();
		Field startDate = eventStructure.getFieldVar("startDate");
		if(offset>0){
			offset-=1;
		}
		try {
						
			boolean done = false;
			int countLimit = 100;
			int internalLimit = limit<=0?0:500;
			int internalOffset = 0;
			int size = 0;
            List<Contentlet> hits = null;
            List<Event> events = new ArrayList<Event>();
            PaginatedArrayList<Event> toReturn = new PaginatedArrayList<Event>();
			GregorianCalendar dateFromCal = new GregorianCalendar();
			dateFromCal.setTime(dateFrom);
			dateFromCal.set(Calendar.HOUR_OF_DAY, 0);
			dateFromCal.set(Calendar.MINUTE, 0);
			dateFromCal.set(Calendar.SECOND, 0);
			dateFromCal.set(Calendar.MILLISECOND, 0);
			GregorianCalendar dateToCal = new GregorianCalendar();
			dateToCal.setTime(dateTo);
			dateToCal.set(Calendar.HOUR_OF_DAY, 23);
			dateToCal.set(Calendar.MINUTE, 59);
			dateToCal.set(Calendar.SECOND, 59);
			dateToCal.set(Calendar.MILLISECOND, 0);
		
			while(!done) { 
				hits = conAPI.search(query, internalLimit, internalOffset, eventStructure.getVelocityVarName() + "." + startDate.getVelocityVarName(), user, respectFrontendRoles);
				List<String> recurrentInodes = new ArrayList<String> (); 
				for(Contentlet con: hits) {	
					recurrentInodes.add(con.getInode());
				}
				List<Contentlet> recurrentConts = conAPI.findContentlets(recurrentInodes);
				for(Contentlet con : recurrentConts) {
					Event event = convertToEvent(con);

					if(UtilMethods.isSet(event.getDisconnectedFrom())){
						String disconnectedId = event.getDisconnectedFrom();
						Event baseEvent = null;
						try{
							baseEvent = find(disconnectedId, false, APILocator.getUserAPI().getSystemUser(), false);
						}catch(Exception e){}
						if(baseEvent==null){
							event.setDisconnectedFrom("");
							try{
								conAPI.checkin(event, categoryAPI.getParents(event, user, true), perAPI.getPermissions(event),  APILocator.getUserAPI().getSystemUser(),false);
							}catch(Exception e){}
						}
					}
					if(event.isRecurrent()){
						buildRecurrenceForRange(event,events,dateFrom, dateTo, offset, internalLimit);
					}else{
						events.add(event);
					}
				}
				
				if(limit<=0)done=true;
				
				if(countLimit > 0 && events.size() >= countLimit + offset)
					done = true;
				else if(events.size() < internalLimit)
					done = true;

				internalOffset += internalLimit;
			}
			
			Collections.sort(events, new Comparator<Event>() {
				public int compare(Event event1, Event event2) {
					return event1.getStartDate().compareTo(event2.getStartDate());
				}
			});

			if(limit<=0){
				toReturn.addAll(events);
				toReturn.setTotalResults(events.size());
			}else{
				if(offset > events.size()) {
					size = 0;
				} else if(countLimit > 0) {
					int toIndex = offset + countLimit > events.size()?events.size():offset + countLimit;
					size = events.subList(offset, toIndex).size();
				} else if (offset > 0) {
					size = events.subList(offset, events.size()).size();
				}
				toReturn.setTotalResults(size);
				
				
				int from = offset<events.size()?offset:0;
				int pageLimit = 0;
				for(int i=from;i<events.size();i++){
					if(limit>0 && pageLimit>=limit){
						break;
					}
					toReturn.add((Event) events.get(i));
					pageLimit++;
				}
			}
			
			return toReturn;
			
		} catch (Exception e) {
			throw new DotDataException("Unable to search on lucene index. query = " + query, e);
		}

	}
	
	private List<Event> buildRecurrenceForRange(Event baseEvent, List<Event> events, Date dateFrom, Date dateTo, int offset, int limit) throws DotDataException, DotContentletStateException, DotSecurityException{
 
		if(baseEvent.isRecurrent()){
			if(!UtilMethods.isSet(dateFrom)){
				throw new IllegalArgumentException("dateFrom cannot be null");
			}
			
			if(!UtilMethods.isSet(dateTo)){
				throw new IllegalArgumentException("dateTo cannot be null");
			}

			int interval = baseEvent.getRecurrenceInterval()<=0?1:baseEvent.getRecurrenceInterval();
			
			GregorianCalendar startDate = new GregorianCalendar();
			GregorianCalendar endDate = new GregorianCalendar();
			GregorianCalendar startTime = new GregorianCalendar();
			GregorianCalendar endTime = new GregorianCalendar();
			
			
			startDate.setTime(baseEvent.getRecurrenceStart());
			startTime.setTime(baseEvent.getStartDate());
			endTime.setTime(baseEvent.getEndDate());
			
			
			GregorianCalendar dateToCal = new GregorianCalendar();
			GregorianCalendar dateFromCal = new GregorianCalendar();
		
			dateToCal.setTime(dateTo);
			dateToCal.set(Calendar.HOUR_OF_DAY, 23);
			dateToCal.set(Calendar.MINUTE, 59);
			dateToCal.set(Calendar.SECOND, 59);
			
			dateFromCal.setTime(dateFrom);
			dateFromCal.set(Calendar.HOUR_OF_DAY, 23);
			dateFromCal.set(Calendar.MINUTE, 59);
			dateFromCal.set(Calendar.SECOND, 59);
			
		
			if(startDate.getTime().before(dateFromCal.getTime())){
				startDate.setTime(dateFromCal.getTime());
			}
			
			GregorianCalendar end = null;
			if(UtilMethods.isSet(baseEvent.getRecurrenceEnd())){
				endDate.setTime(baseEvent.getRecurrenceEnd());
				endDate.set(Calendar.HOUR_OF_DAY, 23);
				endDate.set(Calendar.MINUTE, 59);
				endDate.set(Calendar.SECOND, 59);
				end = endDate;
				if(dateToCal.getTime().before(endDate.getTime())){
					end = dateToCal;	
				}
			}else{
				end = dateToCal;
			}
			String[] datestoIgnoreArr = null;
			List<String> datesToIgnore = new ArrayList<String>();
			
			if(UtilMethods.isSet(baseEvent.getRecurrenceDatesToIgnore())){
				datestoIgnoreArr = baseEvent.getRecurrenceDatesToIgnore().split(" ");
				datesToIgnore = new ArrayList<String>(Arrays.asList(datestoIgnoreArr));
			}
						
			int count = 0;
			while (!startDate.getTime().after(end.getTime())) {
			
				Event recurrentEvent = copyEvent(baseEvent);
			    recurrentEvent.setRecurrenceDatesToIgnore("");

				if(count>0 && UtilMethods.isSet(baseEvent.getDisconnectedFrom())){
					recurrentEvent.setDisconnectedFrom("");
			    }
				switch(baseEvent.getOccursEnum()) {
				case DAILY:
					
					//build the start time/date
					GregorianCalendar cal = new GregorianCalendar();
					cal.setTime(startDate.getTime());
					cal.set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY));
					cal.set(Calendar.MINUTE, startTime.get(Calendar.MINUTE));
					cal.set(Calendar.SECOND, startTime.get(Calendar.SECOND));
					recurrentEvent.setStartDate(cal.getTime());
	
					//build end date/time
					cal = new GregorianCalendar();
					cal.setTime(startDate.getTime());
					cal.set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY));
					cal.set(Calendar.MINUTE, endTime.get(Calendar.MINUTE));
					cal.set(Calendar.SECOND, endTime.get(Calendar.SECOND));
					recurrentEvent.setEndDate(cal.getTime());
					
				
					//if this event is after, die
					if (cal.getTime().after(end.getTime()))
						break;
					
					GregorianCalendar calToIgnore = new GregorianCalendar();
					calToIgnore.setTime(recurrentEvent.getStartDate());
					calToIgnore.set(Calendar.SECOND, 0);
					calToIgnore.set(Calendar.MILLISECOND, 0);
					
					if(!datesToIgnore.contains(String.valueOf(calToIgnore.getTime().getTime()))){
						GregorianCalendar firstOccurence = new GregorianCalendar();
						firstOccurence.setTime(baseEvent.getStartDate());
						long numberOfDays = (long)( (calToIgnore.getTime().getTime() - firstOccurence.getTime().getTime()) / (1000 * 60 * 60 * 24));
						if(numberOfDays%interval==0){
							recurrentEvent.setStartDate(calToIgnore.getTime());
							//http://jira.dotmarketing.net/browse/DOTCMS-6303
							recurrentEvent.setIdentifier(RecurrenceUtil.getRecurrentEventIdentifier(recurrentEvent));
							events.add(recurrentEvent);
							count++;		
						}
					}
					
					//add to start date
					startDate.add(Calendar.DAY_OF_MONTH, 1);
				
					break;
	 
				case WEEKLY:
					
					if (baseEvent.getRecurrenceDaysOfWeek() == null) {
						break;
					}
					for (int j = 1; j < 8; j++) {
						int x = startDate.get(Calendar.DAY_OF_WEEK);
						if (baseEvent.getRecurrenceDaysOfWeek().contains(String.valueOf(x))) {
							
							recurrentEvent = copyEvent(baseEvent);
							recurrentEvent.setRecurrenceDatesToIgnore("");
							if(count>0 && UtilMethods.isSet(baseEvent.getDisconnectedFrom())){
								recurrentEvent.setDisconnectedFrom("");
						    }
							
							//build the start time/date
							cal = new GregorianCalendar();
							cal.setTime(startDate.getTime());
							cal.set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY));
							cal.set(Calendar.MINUTE, startTime.get(Calendar.MINUTE));
							recurrentEvent.setStartDate(cal.getTime());
	
							//build end date/time
							cal = new GregorianCalendar();
							cal.setTime(startDate.getTime());
							cal.set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY));
							cal.set(Calendar.MINUTE, endTime.get(Calendar.MINUTE));
							recurrentEvent.setEndDate(cal.getTime());
	
						
							//if this event is after, die
							if (cal.getTime().after(end.getTime()))
								break;
							
							calToIgnore = new GregorianCalendar();
							calToIgnore.setTime(recurrentEvent.getStartDate());
							calToIgnore.set(Calendar.SECOND, 0);
							calToIgnore.set(Calendar.MILLISECOND, 0);
													
							if(!datesToIgnore.contains(String.valueOf(calToIgnore.getTime().getTime()))){
								
								GregorianCalendar baseCal = new GregorianCalendar();
								baseCal.setTime(baseEvent.getStartDate());    
								GregorianCalendar firstOccurence = calculateFirstOccurence(baseEvent);
								GregorianCalendar c1 = baseCal;
								GregorianCalendar c2 = new GregorianCalendar();
								c2.setTime(recurrentEvent.getStartDate());
								long numberOfWeeks = 0;
								for( long i=1; ; i++ ) {           
									c1.add( Calendar.WEEK_OF_YEAR, 1 );     
									if( c1.after(c2) ) { 
										numberOfWeeks =  i-1; 
										break;
									}
								} 
								if(baseCal.get(Calendar.WEEK_OF_YEAR)<firstOccurence.get(Calendar.WEEK_OF_YEAR)){
									c1 = firstOccurence;
									c2 = new GregorianCalendar();
									c2.setTime(recurrentEvent.getStartDate());;
									for( long i=1; ; i++ ) {           
										c1.add( Calendar.WEEK_OF_YEAR, 1 );   
										if( c1.after(c2) )  {
											numberOfWeeks =  i-1; 
											break;
										}
									} 
								}
								if((numberOfWeeks%(interval)==0)){
									recurrentEvent.setStartDate(calToIgnore.getTime());
									//http://jira.dotmarketing.net/browse/DOTCMS-6303
									recurrentEvent.setIdentifier(RecurrenceUtil.getRecurrentEventIdentifier(recurrentEvent));
									events.add(recurrentEvent);
									count++;		
								}
							}
							
							
							
						}
						startDate.add(Calendar.DAY_OF_MONTH, 1);
					}
					break;
					
				case MONTHLY:

						while (true) {

							recurrentEvent = copyEvent(baseEvent);
							recurrentEvent.setRecurrenceDatesToIgnore("");
							if(count>0 && UtilMethods.isSet(baseEvent.getDisconnectedFrom())){
								recurrentEvent.setDisconnectedFrom("");
							}

							boolean isInRange = false;
							cal = new GregorianCalendar();
							cal.setTime(startDate.getTime());	
							cal.set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY));
							cal.set(Calendar.MINUTE, startTime.get(Calendar.MINUTE));

							int weekOfMonth = baseEvent.getRecurrenceWeekOfMonth();
							int dayOfWeek = baseEvent.getRecurrenceDayOfWeek();
							if(UtilMethods.isSet(baseEvent.getRecurrenceDayOfMonth()) && (baseEvent.getRecurrenceDayOfMonth() > 0)){
								GregorianCalendar baseCal = new GregorianCalendar();
								baseCal.set(Calendar.MONTH, cal.get(Calendar.MONTH));
								baseCal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
								GregorianCalendar baseCal2 = new GregorianCalendar();
								baseCal2.set(Calendar.MONTH, cal.get(Calendar.MONTH));
								baseCal2.set(Calendar.YEAR, cal.get(Calendar.YEAR));

								int dayOfMonth = baseEvent.getRecurrenceDayOfMonth();
								baseCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
								while(baseCal.get(Calendar.MONTH) != baseCal2.get(Calendar.MONTH)){
									baseCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
									dayOfMonth--;
								}

								weekOfMonth = baseCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
								dayOfWeek = baseCal.get(Calendar.DAY_OF_WEEK);
							}
							
							GregorianCalendar auxCal = new GregorianCalendar();
							auxCal.set(Calendar.MONTH, cal.get(Calendar.MONTH));
							auxCal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
							auxCal.set(Calendar.DAY_OF_WEEK, dayOfWeek); 
							if(weekOfMonth==1){
								auxCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, 1);      
							}else{
								if(weekOfMonth<=4){
									auxCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, weekOfMonth);
								}else{
									auxCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, -1);                       	
								}
							}
							recurrentEvent.setStartDate(cal.getTime());
							if((cal.get(Calendar.WEEK_OF_MONTH)==auxCal.get(Calendar.WEEK_OF_MONTH)) && (cal.get(Calendar.DAY_OF_WEEK)==auxCal.get(Calendar.DAY_OF_WEEK))){
							
								GregorianCalendar baseCal = new GregorianCalendar();
								baseCal.setTime(baseEvent.getStartDate());    
								GregorianCalendar firstOccurence = calculateFirstOccurence(baseEvent);
					            int numberOfMonths =((baseCal.get(Calendar.YEAR) - auxCal.get(Calendar.YEAR)) * 12) + (baseCal.get(Calendar.MONTH) - auxCal.get(Calendar.MONTH));
							    if(baseCal.get(Calendar.MONTH)<firstOccurence.get(Calendar.MONTH)){
							    	numberOfMonths =((firstOccurence.get(Calendar.YEAR) - auxCal.get(Calendar.YEAR)) * 12) + (firstOccurence.get(Calendar.MONTH) - auxCal.get(Calendar.MONTH));       
								}
							    if(numberOfMonths==0 || (numberOfMonths%interval==0)){
							 	  isInRange = true;
								}
							
							}
							
						

							//build end date/time
							cal = new GregorianCalendar();
							cal.setTime(startDate.getTime());
							cal.set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY));
							cal.set(Calendar.MINUTE, endTime.get(Calendar.MINUTE));
							recurrentEvent.setEndDate(cal.getTime());

							//if this event is after, die
							if (cal.getTime().after(end.getTime()))
								break;


							if(isInRange){
								calToIgnore = new GregorianCalendar();
								calToIgnore.setTime(recurrentEvent.getStartDate());
								calToIgnore.set(Calendar.SECOND, 0);
								calToIgnore.set(Calendar.MILLISECOND, 0);
								if(!datesToIgnore.contains(String.valueOf(calToIgnore.getTime().getTime()))){
									recurrentEvent.setStartDate(calToIgnore.getTime());
									//http://jira.dotmarketing.net/browse/DOTCMS-6303
									recurrentEvent.setIdentifier(RecurrenceUtil.getRecurrentEventIdentifier(recurrentEvent));
									events.add(recurrentEvent);
									count++;
								}
							}
							startDate.add(Calendar.DAY_OF_MONTH, 1);
						}


						//add to start date

						startDate.add(Calendar.MONTH, interval);
					
					
				    break;
				case ANNUALLY:
					
					 while (true) {
							recurrentEvent = copyEvent(baseEvent);
							recurrentEvent.setRecurrenceDatesToIgnore("");
							if(count>0 && UtilMethods.isSet(baseEvent.getDisconnectedFrom())){
								recurrentEvent.setDisconnectedFrom("");
						    }
							
							boolean isInRange = false;
							cal = new GregorianCalendar();
							cal.setTime(startDate.getTime());	
							cal.set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY));
							cal.set(Calendar.MINUTE, startTime.get(Calendar.MINUTE));

							int weekOfMonth = baseEvent.getRecurrenceWeekOfMonth();
							int dayOfWeek = baseEvent.getRecurrenceDayOfWeek();
							int monthOfYear = baseEvent.getRecurrenceMonthOfYear()>0?baseEvent.getRecurrenceMonthOfYear()-1:0;
							if(UtilMethods.isSet(baseEvent.getRecurrenceDayOfMonth()) && (baseEvent.getRecurrenceDayOfMonth() > 0)){
								GregorianCalendar baseCal = new GregorianCalendar();
								baseCal.set(Calendar.MONTH, monthOfYear);
								baseCal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
								GregorianCalendar baseCal2 = new GregorianCalendar();
								baseCal2.set(Calendar.MONTH, monthOfYear);
								baseCal2.set(Calendar.YEAR, cal.get(Calendar.YEAR));

								int dayOfMonth = baseEvent.getRecurrenceDayOfMonth();
								baseCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
								while(baseCal.get(Calendar.MONTH) != baseCal2.get(Calendar.MONTH)){
									baseCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
									dayOfMonth--;
								}

								weekOfMonth = baseCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
								dayOfWeek = baseCal.get(Calendar.DAY_OF_WEEK);
							}
							GregorianCalendar auxCal = new GregorianCalendar();
	                    	auxCal.set(Calendar.MONTH, monthOfYear);
	                    	auxCal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
	                    	auxCal.set(Calendar.DAY_OF_WEEK, dayOfWeek); 
	                    	if(weekOfMonth==1){
	                    		auxCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, 1);      
	                    	}else{
	                    		if(weekOfMonth<=4){
	                    			auxCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, weekOfMonth);
	                    		}else{
	                    			auxCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, -1);                       	
	                    		}
	                    	}
							recurrentEvent.setStartDate(cal.getTime());
							if((cal.get(Calendar.MONTH)==auxCal.get(Calendar.MONTH)) && (cal.get(Calendar.WEEK_OF_MONTH)==auxCal.get(Calendar.WEEK_OF_MONTH)) && (cal.get(Calendar.DAY_OF_WEEK)==auxCal.get(Calendar.DAY_OF_WEEK))){
								
								GregorianCalendar baseCal = new GregorianCalendar();
								baseCal.setTime(baseEvent.getStartDate());    
								GregorianCalendar firstOccurence = calculateFirstOccurence(baseEvent);
					            int numberOfYears =(baseCal.get(Calendar.YEAR) - auxCal.get(Calendar.YEAR));
							    if(baseCal.get(Calendar.YEAR)<firstOccurence.get(Calendar.YEAR)){
							    	numberOfYears =(firstOccurence.get(Calendar.YEAR) - auxCal.get(Calendar.YEAR));
								}
							    if(numberOfYears==0 || (numberOfYears%interval==0)){
							 	  isInRange = true;
								}
							    
							}

							//build end date/time
							cal = new GregorianCalendar();
							cal.setTime(startDate.getTime());
							cal.set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY));
							cal.set(Calendar.MINUTE, endTime.get(Calendar.MINUTE));
							recurrentEvent.setEndDate(cal.getTime());

							//if this event is after, die
							if (cal.getTime().after(end.getTime()))
								break;


							if(isInRange){
								calToIgnore = new GregorianCalendar();
								calToIgnore.setTime(recurrentEvent.getStartDate());
								calToIgnore.set(Calendar.SECOND, 0);
								calToIgnore.set(Calendar.MILLISECOND, 0);
								if(!datesToIgnore.contains(String.valueOf(calToIgnore.getTime().getTime()))){
									recurrentEvent.setStartDate(calToIgnore.getTime());
									//http://jira.dotmarketing.net/browse/DOTCMS-6303
									recurrentEvent.setIdentifier(RecurrenceUtil.getRecurrentEventIdentifier(recurrentEvent));
									events.add(recurrentEvent);
									count++;
								}
							}
							startDate.add(Calendar.DAY_OF_MONTH, 1);
						}
					//add to start date
					startDate.add(Calendar.YEAR, interval);
					break;
					
				}
				
				if(limit>0 && events.size()==limit-1){
					break;
				}
				
			}
		}
		return events;
	}
	
	
	private Event copyEvent(Event baseEvent) throws DotContentletStateException, DotSecurityException, DotDataException {
		Event newEvent = new Event();
		conAPI.copyProperties(newEvent, baseEvent.getMap());
		newEvent.setInode(baseEvent.getInode());
		newEvent.setIdentifier(baseEvent.getIdentifier());
		return newEvent;
	}
	
	private GregorianCalendar calculateFirstOccurence(Event baseEvent){

		GregorianCalendar auxFo = new GregorianCalendar();
		switch(baseEvent.getOccursEnum()) {
		case WEEKLY:
			auxFo.setTime(baseEvent.getStartDate());
			for (int j = 1; j < 8; j++) {
				int x = auxFo.get(Calendar.DAY_OF_WEEK);
				if (baseEvent.getRecurrenceDaysOfWeek().contains(String.valueOf(x))) {
				  break;	
				}
				auxFo.add(Calendar.DAY_OF_MONTH, 1);
			}
			break;
		case MONTHLY:	
			GregorianCalendar fo = new GregorianCalendar();
			fo.setTime(baseEvent.getStartDate());	
			int weekOfMonth = baseEvent.getRecurrenceWeekOfMonth();
			int dayOfWeek = baseEvent.getRecurrenceDayOfWeek();
			if(UtilMethods.isSet(baseEvent.getRecurrenceDayOfMonth()) && (baseEvent.getRecurrenceDayOfMonth() > 0)){
				GregorianCalendar baseCal = new GregorianCalendar();
				baseCal.set(Calendar.MONTH, fo.get(Calendar.MONTH));
				baseCal.set(Calendar.YEAR, fo.get(Calendar.YEAR));
				GregorianCalendar baseCal2 = new GregorianCalendar();
				baseCal2.set(Calendar.MONTH, fo.get(Calendar.MONTH));
				baseCal2.set(Calendar.YEAR, fo.get(Calendar.YEAR));

				int dayOfMonth = baseEvent.getRecurrenceDayOfMonth();
				baseCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
				while(baseCal.get(Calendar.MONTH) != baseCal2.get(Calendar.MONTH)){
					baseCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
					dayOfMonth--;
				}

				weekOfMonth = baseCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
				dayOfWeek = baseCal.get(Calendar.DAY_OF_WEEK);
			}

			auxFo = new GregorianCalendar();
			auxFo.set(Calendar.MONTH, fo.get(Calendar.MONTH));
			auxFo.set(Calendar.YEAR, fo.get(Calendar.YEAR));
			auxFo.set(Calendar.DAY_OF_WEEK, dayOfWeek); 
			if(weekOfMonth==1){
				auxFo.set(Calendar.DAY_OF_WEEK_IN_MONTH, 1);      
			}else{
				if(weekOfMonth<=4){
					auxFo.set(Calendar.DAY_OF_WEEK_IN_MONTH, weekOfMonth);
				}else{
					auxFo.set(Calendar.DAY_OF_WEEK_IN_MONTH, -1);                       	
				}
			}
			break;
		case ANNUALLY:
			fo = new GregorianCalendar();
			fo.setTime(baseEvent.getStartDate());		
			weekOfMonth = baseEvent.getRecurrenceWeekOfMonth();
			dayOfWeek = baseEvent.getRecurrenceDayOfWeek();
			int monthOfYear = baseEvent.getRecurrenceMonthOfYear()>0?baseEvent.getRecurrenceMonthOfYear()-1:0;
			if(UtilMethods.isSet(baseEvent.getRecurrenceDayOfMonth()) && (baseEvent.getRecurrenceDayOfMonth() > 0)){
				GregorianCalendar baseCal = new GregorianCalendar();
				baseCal.set(Calendar.MONTH, monthOfYear);
				baseCal.set(Calendar.YEAR, fo.get(Calendar.YEAR));
				GregorianCalendar baseCal2 = new GregorianCalendar();
				baseCal2.set(Calendar.MONTH, monthOfYear);
				baseCal2.set(Calendar.YEAR, fo.get(Calendar.YEAR));

				int dayOfMonth = baseEvent.getRecurrenceDayOfMonth();
				baseCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
				while(baseCal.get(Calendar.MONTH) != baseCal2.get(Calendar.MONTH)){
					baseCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
					dayOfMonth--;
				}

				weekOfMonth = baseCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
				dayOfWeek = baseCal.get(Calendar.DAY_OF_WEEK);
			}
		    auxFo = new GregorianCalendar();
			auxFo.set(Calendar.MONTH, monthOfYear);
			auxFo.set(Calendar.YEAR, fo.get(Calendar.YEAR));
			auxFo.set(Calendar.DAY_OF_WEEK, dayOfWeek); 
        	if(weekOfMonth==1){
        		auxFo.set(Calendar.DAY_OF_WEEK_IN_MONTH, 1);      
        	}else{
        		if(weekOfMonth<=4){
        			auxFo.set(Calendar.DAY_OF_WEEK_IN_MONTH, weekOfMonth);
        		}else{
        			auxFo.set(Calendar.DAY_OF_WEEK_IN_MONTH, -1);                       	
        		}
        	}
			break;
		}

		return auxFo;
		
	}
	
}
