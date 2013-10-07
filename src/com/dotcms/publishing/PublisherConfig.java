package com.dotcms.publishing;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.*;

public class PublisherConfig implements Map<String, Object> {

	private enum Config {
		START_DATE, END_DATE, HOSTS, FOLDERS, STRUCTURES, INCLUDE_PATTERN, 
		EXCLUDE_PATTERN, LANGUAGE, USER, PUBLISHER, MAKE_BUNDLE, LUCENE_QUERY, 
		THREADS, ID, TIMESTAMP, BUNDLERS, INCREMENTAL, DESTINATION_BUNDLE,
		UPDATED_HTML_PAGE_IDS, LUCENE_QUERIES, ENDPOINT, GROUP_ID, ASSETS, FOLDERS_PENDING_DEFAULT
	}

	public void PublisherConfig(Map<String, Object> map){
		params = map;
	}
	
	Map<String, Object> params;
	private boolean liveOnly = true;

	@SuppressWarnings("unchecked")
	public Set<String> getFolders() {
		if(get(Config.FOLDERS.name()) == null){
			Set<String> foldersToBuild =   new HashSet<String>();
			params.put(Config.FOLDERS.name(), foldersToBuild);
		}
		return (Set<String>) params.get(Config.FOLDERS.name());
	}

	public void setFolders(Set<String> folders) {
		params.put(Config.FOLDERS.name(), folders);
	}

    /**
     * Get the list of pending folders to apply a given default type (Structure inode)
     *
     * @param structureInode
     * @return
     */
    public ArrayList<Folder> getPendingFoldersForDefaultType ( String structureInode ) {

        if ( get( Config.FOLDERS_PENDING_DEFAULT.name() ) != null ) {

            Map<String, ArrayList<Folder>> pendingRecords = (Map<String, ArrayList<Folder>>) get( Config.FOLDERS_PENDING_DEFAULT.name() );
            return pendingRecords.get( structureInode );
        }

        return null;
    }

    /**
     * Adds a given Folder to a list of pending folders to apply a given default structure type as
     * soon as the structure is created in the end point server.
     *
     * @param structureInode
     * @param folder
     */
    public void addPendingFolderForDefaultType ( String structureInode, Folder folder ) {

        if ( get( Config.FOLDERS_PENDING_DEFAULT.name() ) == null ) {

            Map<String, ArrayList<Folder>> pendingRecords = new HashMap<String, ArrayList<Folder>>();

            ArrayList<Folder> foldersToModify = new ArrayList<Folder>();
            foldersToModify.add( folder );
            pendingRecords.put( structureInode, foldersToModify );

            params.put( Config.FOLDERS_PENDING_DEFAULT.name(), pendingRecords );
        } else {

            Map<String, ArrayList<Folder>> pendingRecords = (Map<String, ArrayList<Folder>>) get( Config.FOLDERS_PENDING_DEFAULT.name() );

            ArrayList<Folder> foldersToModify;
            if ( pendingRecords.containsKey( structureInode ) ) {
                foldersToModify = pendingRecords.get( structureInode );
                foldersToModify.add( folder );
            } else {
                foldersToModify = new ArrayList<Folder>();
                foldersToModify.add( folder );
            }

            pendingRecords.put( structureInode, foldersToModify );
        }
    }

    @SuppressWarnings("unchecked")
	public Set<String> getHostSet() {
		if(get(Config.HOSTS.name()) == null){
			Set<String> hostsToBuild =   new HashSet<String>();
			params.put(Config.HOSTS.name(), hostsToBuild);
		}
		return (Set<String>) params.get(Config.HOSTS.name());
	}

	public void setHostSet(Set<String> hosts) {
		params.put(Config.HOSTS.name(), hosts);
	}

	@SuppressWarnings("unchecked")
	public Set<String> getStructures() {
		if(get(Config.STRUCTURES.name()) == null){
			Set<String> structsToBuild =   new HashSet<String>();
			params.put(Config.STRUCTURES.name(), structsToBuild);
		}
		return (Set<String>) params.get(Config.STRUCTURES.name());
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
	
	public void setStructures(Set<String> structures) {
		params.put(Config.STRUCTURES.name(), structures);
	}
	
	public String getLuceneQuery() {
		return (String) params.get(Config.LUCENE_QUERY.name());
	}

	public void setLuceneQuery(String luceneQuery) {
		params.put(Config.LUCENE_QUERY.name(), luceneQuery);
	}
	
	public String getEndpoint() {
		return (String) params.get(Config.ENDPOINT.name());
	}

	public void setEndpoint(String endpoint) {
		params.put(Config.ENDPOINT.name(), endpoint);
	}
	
	public String getGroupId() {
		return (String) params.get(Config.GROUP_ID.name());
	}

	public void setGroupId(String groupId) {
		params.put(Config.GROUP_ID.name(), groupId);
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
			List<String> ids = new ArrayList<String>();
			for(Host h : getHosts())
			    ids.addAll(
			        APILocator.getHTMLPageAPI().findUpdatedHTMLPageIds(h, getStartDate(), getEndDate()));
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
		params = java.util.Collections.synchronizedMap(new LinkedHashMap<String, Object>());
		setId(UtilMethods.dateToJDBC(new Date()).replace(':', '-').replace(' ', '_'));

		setLanguage(APILocator.getLanguageAPI().getDefaultLanguage().getId());

		setTimeStamp(new Date());

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

		List<String> bs = new ArrayList<String>();
		for(IBundler clazz : bundlers){
			bs.add(clazz.getClass().getName());
		}

		params.put(Config.BUNDLERS.name(), bs);
		
		
		
		
		
	}
	
	
	public List<IBundler> getBundlers() {

		 List<String> x = (List<String>) params.get(Config.BUNDLERS.name());
		 List<IBundler> bs = new ArrayList<IBundler>();
		 for(String name : x){
			 try {
				bs.add((IBundler) Class.forName(name).newInstance());
			} catch (Exception e) {
				Logger.error(this.getClass(), "Cannont get bundler:" + e.getMessage(), e);
			}
			 
		 }
		 return bs;
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
	
	@SuppressWarnings("unchecked")
	public List<PublishQueueElement> getAssets() {
		
		return (List<PublishQueueElement>) params.get(Config.ASSETS.name());
	}
	
	public void setAssets(List<PublishQueueElement> pageIds) {
		params.put(Config.ASSETS.name(), pageIds);
	}
}
