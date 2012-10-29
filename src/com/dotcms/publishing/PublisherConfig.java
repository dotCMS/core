package com.dotcms.publishing;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class PublisherConfig implements Map<String, Object> {

	private enum Config {
		START_DATE, END_DATE, HOSTS, FOLDERS, STRUCTURES, INCLUDE_PATTERN, 
		EXCLUDE_PATTERN, LANGUAGE, USER, PUBLISHER, MAKE_BUNDLE, LUCENE_QUERY, 
		THREADS, ID, TIMESTAMP, BUNDLERS, INCREMENTAL, DESTINATION_BUNDLE,
		UPDATED_HTML_PAGE_IDS, LUCENE_QUERIES;
	}
	
	public void PublisherConfig(Map<String, Object> map){
		params = map;
	}
	
	Map<String, Object> params;
	private boolean liveOnly = true;

	@SuppressWarnings("unchecked")
	public List<Folder> getFolders() {
		return (List<Folder>) params.get(Config.FOLDERS.name());
	}

	public void setFolders(List<Folder> folders) {
		params.put(Config.FOLDERS.name(), folders);
	}

	@SuppressWarnings("unchecked")
	public List<Structure> getStructures() {
		return (List<Structure>) params.get(Config.STRUCTURES.name());
	}
	public boolean makeBundle() {
		return (Boolean) params.get(Config.MAKE_BUNDLE.name());
	}
	
	public void setMakeBundle(boolean bundle) {
		params.put(Config.MAKE_BUNDLE.name(), bundle);
	}
	
	/**
	 * Defaults to live. This handles most cause.  Is set to false bundlers should bundle both working and live
	 * @return
	 */
	public boolean liveOnly(){
		return liveOnly;
	}
	
	public void setLiveOnly(boolean liveOnly){
		this.liveOnly = liveOnly;
	}
	
	public void setStructures(List<Structure> structures) {
		params.put(Config.STRUCTURES.name(), structures);
	}
	
	public String getLuceneQuery() {
		return (String) params.get(Config.LUCENE_QUERY.name());
	}

	public void setLuceneQuery(String luceneQuery) {
		params.put(Config.LUCENE_QUERY.name(), luceneQuery);
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getLuceneQueries() {
		return (List<String>) params.get(Config.LUCENE_QUERIES.name());
	}

	public void setLuceneQueries(List<String> luceneQueries) {
		params.put(Config.LUCENE_QUERIES.name(), luceneQueries);
	}

	public void clear() {
		this.params = new LinkedHashMap<String, Object>();
	}

	public boolean containsKey(Object key) {
		// TODO Auto-generated method stub
		return this.params.containsKey(key);
	}

	public boolean containsValue(Object value) {
		// TODO Auto-generated method stub
		return this.params.containsValue(value);
	}

	public Set<Entry<String, Object>> entrySet() {
		// TODO Auto-generated method stub
		return this.params.entrySet();
	}

	public Object get(Object key) {
		return this.params.get(key);
	}

	public boolean isEmpty() {
		return this.params.isEmpty();
	}

	public Set<String> keySet() {
		return this.params.keySet();
	}

	public void putAll(Map<? extends String, ? extends Object> m) {
		this.params.putAll(m);

	}

	public Object remove(Object key) {
		return this.params.remove(key);
	}

	public int size() {
		return this.params.size();
	}

	public Collection<Object> values() {
		return this.params.values();
	}

	public Object put(String key, Object val) {
		return params.put(key, val);
	}

	public Object get(String key) {
		return params.get(key);
	}

	public Date getStartDate() {
		return (Date) params.get(Config.START_DATE.name());
	}

	public void setStartDate(Date startDate) {
		params.put(Config.START_DATE.name(), startDate);
	}

	public Date getEndDate() {

		return (Date) params.get(Config.END_DATE.name());
	}

	public void setEndDate(Date endDate) {
		params.put(Config.END_DATE.name(), endDate);
	}

	public List<Host> getHosts() {
		return (List<Host>) params.get(Config.HOSTS.name());
	}

	public void setHosts(List<Host> hosts) {
		params.put(Config.HOSTS.name(), hosts);
	}

	public List<String> getIncludePatterns() {
		return (List<String>) params.get(Config.INCLUDE_PATTERN.name());
	}
	
	public List<String> getUpdatedHTMLPageIds() {
		
		// lazy load
		if(params.get(Config.UPDATED_HTML_PAGE_IDS.name()) ==null){
			List<String> ids = BundlerUtil.getUpdatedHTMLPageIds(getStartDate(), getEndDate());
			params.put(Config.UPDATED_HTML_PAGE_IDS.name(), ids);
		}
		
		
		
		
		return (List<String>) params.get(Config.UPDATED_HTML_PAGE_IDS.name());
	}
	
	public void setUpdatedHTMLPageIds(List<String> pageIds) {
		params.put(Config.UPDATED_HTML_PAGE_IDS.name(), pageIds);
	}
	
	
	public void setIncludePatterns(List<String> includePatterns) {
		params.put(Config.INCLUDE_PATTERN.name(), includePatterns);
	}

	public List<String> getExcludePatterns() {
		return (List<String>) params.get(Config.EXCLUDE_PATTERN.name());
	}

	public void setExcludePatterns(List<String> excludePatterns) {
		params.put(Config.EXCLUDE_PATTERN.name(), excludePatterns);
	}

	public PublisherConfig() {
		params = new HashMap<String, Object>();
		setId(UtilMethods.dateToJDBC(new Date()).replace(':', '-').replace(' ', '_'));

		Date startDate = new java.util.Date();
		startDate.setTime(0);
		setStartDate(startDate);
		setEndDate(new java.util.Date());

		setLanguage(APILocator.getLanguageAPI().getDefaultLanguage().getId());

		setTimeStamp(new Date());

		params = new LinkedHashMap<String, Object>();

	}

	public long getLanguage() {
		Long x=  (Long) params.get(Config.LANGUAGE.name());
		if(x == null){
			x= APILocator.getLanguageAPI().getDefaultLanguage().getId();
		}
		return x;
	}

	public void setLanguage(long language) {
		params.put(Config.LANGUAGE.name(), language);
	}

	public User getUser() {
		return (User) params.get(Config.USER.name());
	}

	public void setUser(User user) {
		params.put(Config.USER.name(), user);
	}

	public int getAdditionalThreads() {
		Integer x =  (Integer) params.get(Config.THREADS.name());
		if(x == null){
			x= 0;
		}
		return x;
	}

	public void setAdditionalThreads(int additionalThreads) {
		params.put(Config.THREADS.name(), additionalThreads);
	}

	public Date getTimeStamp() {
		return (Date) params.get(Config.TIMESTAMP.name());
	}

	public void setTimeStamp(Date timeStamp) {
		params.put(Config.TIMESTAMP.name(), timeStamp);
	}

	public String getId() {

		return (String) params.get(Config.ID.name());
	}

	public void setId(String id) {
		params.put(Config.ID.name(), id);
	}

	public String getDestinationBundle() {
		return (String) params.get(Config.DESTINATION_BUNDLE.name());
	}
	
	public void setDestinationBundle(String bundle) {
		params.put(Config.DESTINATION_BUNDLE.name(), bundle);
	}
	
	public List<Class> getPublishers() {
		return (List<Class>) params.get(Config.PUBLISHER.name());
	}

	public void setPublishers(List<Class> publishers) {
		System.out.println(publishers);
		params.put(Config.PUBLISHER.name(), publishers);
	}

	@Override
	public String toString() {
		return "PublisherConfig [params=" + params + "]";
	}
	public void setBundlers(List<IBundler> bundlers) {

		params.put(Config.BUNDLERS.name(), bundlers);
	}
	
	
	public List<IBundler> getBundlers() {

		return (List<IBundler>) params.get(Config.BUNDLERS.name());
	}
	public boolean isIncremental(){
		return (params.get(Config.INCREMENTAL.name()) !=null);
		
	}
	public void setIncremental(boolean inc){
		if(inc){
			params.put(Config.INCREMENTAL.name(), true);
		}
		else{
			params.remove(Config.INCREMENTAL.name());
		}
	}
}
