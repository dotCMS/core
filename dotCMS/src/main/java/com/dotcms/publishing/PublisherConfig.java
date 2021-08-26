package com.dotcms.publishing;

import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.manifest.CSVManifestBuilder;
import com.dotcms.publishing.manifest.ManifestBuilder;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides the main configuration parameters of a bundle sent via Push Publish.
 * 
 * @author Jason Tesser
 * @version N/A
 * @since Mar 23, 2012
 *
 */
public class PublisherConfig implements Map<String, Object>, Cloneable {

	static public String STATIC_SUFFIX = "-static";

	public enum Config {
		START_DATE, END_DATE, HOSTS, HOST_SET, LANGUAGES, FOLDERS, STRUCTURES, INCLUDE_PATTERN,
		EXCLUDE_PATTERN, LANGUAGE, USER, PUBLISHER, MAKE_BUNDLE, LUCENE_QUERY, 
		THREADS, ID, TIMESTAMP, BUNDLERS, INCREMENTAL, NOT_NEW_NOT_INCREMENTAL, DESTINATION_BUNDLE,
		UPDATED_HTML_PAGE_IDS, LUCENE_QUERIES, ENDPOINT, GROUP_ID, ASSETS, FOLDERS_PENDING_DEFAULT,
		MAPPED_REMOTE_LANGUAGES
	}

	public enum Operation {
		PUBLISH,
		UNPUBLISH
	}

	public enum MyConfig {
		RUN_NOW,INDEX_NAME
	}

	/**
	 * Specifies the scope of this publisher configuration in terms of
	 * end-points that failed or succeeded in receiving contents for a given
	 * Environment. Depending on user-specified settings, a failed-to-publish or
	 * successful bundle can be re-sent (for the "Re-Try" option) with the
	 * following configuration:
	 * <ul>
	 * <li>{@code ALL_ENDPOINTS}: The bundle will be sent to ALL end-points,
	 * even if some of them successfully installed the bundles.</li>
	 * <li>{@code FAILED_ENDPOINTS}: The bundle will be sent to FAILED
	 * end-points only. This will improve the performance as the publisher will
	 * not re-install a successful bundle.</li>
	 * </ul>
	 */
	public enum DeliveryStrategy {
		ALL_ENDPOINTS, FAILED_ENDPOINTS
	}

	private Operation operation;
	private DeliveryStrategy deliveryStrategy = null;
	private PublishAuditStatus publishAuditStatus;

	public void PublisherConfig(Map<String, Object> map){
		params = map;
	}
	
	Map<String, Object> params;
	private boolean liveOnly = true;
	private boolean isStatic = false;

	private Map<String, String> existingContent = new HashMap<>();

	protected ManifestBuilder manifestBuilder;

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
		if(get(Config.HOST_SET.name()) == null){
			Set<String> hostsToBuild =   new HashSet<String>();
			params.put(Config.HOST_SET.name(), hostsToBuild);
		}
		return (Set<String>) params.get(Config.HOST_SET.name());
	}

	public void setHostSet(Set<String> hosts) {
		params.put(Config.HOST_SET.name(), hosts);
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
		return this.params.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return this.params.containsValue(value);
	}

	public Set<Entry<String, Object>> entrySet() {
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

	public boolean isStatic() {
		return isStatic;
	}

	/**
	 * Specifies if the configuration wants the bundler to manage each dependency. For example to check the
	 * Force Push feature as part of the bundle process.
	 */
	public boolean shouldManageDependencies(){
		return isStatic();
	}

	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	public String getName(){
		String result = getId();

		if (isStatic()){
			result += STATIC_SUFFIX;
		}

		return result;
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

	public boolean isSameIndexNotIncremental() {
		return (params.get(Config.NOT_NEW_NOT_INCREMENTAL.name()) !=null);
	}

	public void setSameIndexNotIncremental(boolean sameIndexNoIncremental) {
		if (sameIndexNoIncremental) {
			params.put(Config.NOT_NEW_NOT_INCREMENTAL.name(), true);
		} else {
			params.remove(Config.NOT_NEW_NOT_INCREMENTAL.name());
		}
	}

	public Set<String> getLanguages() {
		if(get(Config.LANGUAGES.name()) == null){
			Set<String> languagesToBuild =   new HashSet<>();
			put(Config.LANGUAGES.name(), languagesToBuild);
		}
		return (Set<String>) get(Config.LANGUAGES.name());
	}

	public void setLanguages(Set<String> languages) {
		put(Config.LANGUAGES.name(), languages);
	}

	/**
	 * Returns the type of operation we will apply to the bundle (PUBLISH/UNPUBLISH).
	 *
	 * @return
	 */
	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}

	public boolean runNow(){
		return this.get(PushPublisherConfig.MyConfig.RUN_NOW.toString()) !=null && new Boolean((String) this.get(
			PushPublisherConfig.MyConfig.RUN_NOW.toString()));
	}

	public void setRunNow(boolean once){
		this.put(MyConfig.RUN_NOW.toString(), once);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	/**
	 * 
	 * @return
	 */
	public DeliveryStrategy getDeliveryStrategy() {
		return null != this.deliveryStrategy ? this.deliveryStrategy : DeliveryStrategy.ALL_ENDPOINTS;
	}

	/**
	 * 
	 * @param deliveryStrategy
	 */
	public void setDeliveryStrategy(DeliveryStrategy deliveryStrategy) {
		this.deliveryStrategy = deliveryStrategy;
	}

	public PublishAuditStatus getPublishAuditStatus() {
		return publishAuditStatus;
	}

	public void setPublishAuditStatus(PublishAuditStatus publishAuditStatus) {
		this.publishAuditStatus = publishAuditStatus;
	}


	/**
	 * Convenience method to get access to the mapped remote languages map
	 * @return a Writable Map of the form Id/Language
	 */
	@SuppressWarnings("unchecked")
	private Map<Long,Language> getWritableMappedRemoteLanguages(){
		return (Map<Long,Language>)params.computeIfAbsent(Config.MAPPED_REMOTE_LANGUAGES.name(),
		  s -> new ConcurrentHashMap<Long,Language>()
		);
	}

	/**
	 * This method is used to solve Language conflicts
	 * It allows the assignment of remote Lang id with an existing language
	 * Meaning that if for example Language: Finish has an id 3 on the sender.
	 * it also exists on the receiver under a different id let says 5
	 * We can says that 3 maps to 5.
	 * In the given scenario of a new Language the map has to be used to map the new id with the new Languange Instance.
	 * The same way if no conflict is found we can use the map to say an id should map to the non-conflicting the Local Lang.
	 * @param remoteId
	 * @param localLang
	 */
	public void mapRemoteLanguage(final Long remoteId, final Language localLang){
	    getWritableMappedRemoteLanguages().put(remoteId,localLang);
	}

	/**
	 * This method serves as a Read-Only accessor to the map used to handle language conflicts.
	 * @param remoteId
	 * @return the Language mapped to the given id.
	 */
	public Language getMappedRemoteLanguage(final Long remoteId){
		return getWritableMappedRemoteLanguages().get(remoteId);
	}

	/**
	 * Gets a map that matches bundle content identifiers with existing content found on the local
	 * instance (content matched by unique field)
	 * @return map with matches between bundle identifiers and local content
	 */
	public Map<String, String> getExistingContent() {
		return existingContent == null ? Collections.emptyMap() : new HashMap<>(existingContent);
	}

	/**
	 * Store a map that matches bundle content identifiers with existing content found on the local
	 * instance (content matched by unique field)
	 *
	 * @param existingContentMap map with matches between bundle identifiers and local content
	 */
	public void setExistingContent(Map<String, String> existingContentMap) {
		existingContent =
				existingContentMap == null ? new HashMap<>() : new HashMap<>(existingContentMap);
	}

	/**
	 * Clean up the map that matches bundle content identifiers with existing content.
	 */
	public void cleanupExistingContent() {
		if (existingContent != null) {
			existingContent.clear();
		}
	}

	public void setManifestBuilder(final ManifestBuilder manifestBuilder) {
		this.manifestBuilder = manifestBuilder;
	}

	public Optional<File> getManifestFile() {
		try {
			return Optional.of(manifestBuilder.getManifestFile());
		} catch (IllegalStateException e) {
			return Optional.empty();
		}
	}

	public Map<String, Object> getMap() {
		final Map<String, Object> clone = new HashMap<>();
		clone.putAll(this);
		return clone;
	}
}