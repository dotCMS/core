/**
 * 
 */
package com.dotmarketing.business;

import com.dotcms.repackage.org.apache.commons.lang.NotImplementedException;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The legacy cache administrator will invalidate cache entries within a cluster
 * on a put where the non legacy one will not.  
 * @author Jason Tesser
 * @version 1.6.5
 *
 * @deprecated Use {@link com.dotmarketing.business.DotGuavaCacheAdministratorImpl} instead
 */
public class DotJBCacheAdministratorLegacyImpl implements DotCacheAdministrator{

	private DotCacheAdministrator cache;
	private DistributedJournalAPI journalAPI;
	
	public DotJBCacheAdministratorLegacyImpl() {
		journalAPI = APILocator.getDistributedJournalAPI();
		cache = CacheLocator.getCacheAdministrator();
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.DotCacheAdministrator#flushAll()
	 */
	public void flushAll() {
		cache.flushAll();
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.DotCacheAdministrator#flushGroup(java.lang.String)
	 */
	public void flushGroup(String group) {
		cache.flushGroup(group);
	}

	@Override
	public void flushAlLocalOnly () {

	}

	public void flushGroupLocalOnly(String group) {
		cache.flushGroupLocalOnly(group);
	}
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.business.DotCacheAdministrator#get(java.lang.String)
	 */
	public Object get(String key, String group) throws DotCacheException {
		return cache.get(key, group);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.DotCacheAdministrator#put(java.lang.String, java.lang.Object, java.lang.String[])
	 */
	public void put(String key, Object content, String group) {
		cache.put(key, content, group);
		try{
			journalAPI.addCacheEntry(key, group);
		}catch(DotDataException e){
			Logger.error(this, "Unable to add journal entry for cluster", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.DotCacheAdministrator#remove(java.lang.String)
	 */
	public void remove(String key, String group) {
		cache.remove(key, group);
	}
	
	public void removeLocalOnly(String key, String group) {
		cache.removeLocalOnly(key, group);
	}

	public void flushAlLocalOnlyl() {
		cache.flushAlLocalOnly();
	}

	@Override
	public void initProviders () {

	}

	public Set<String> getKeys(String group) {
		return cache.getKeys(group);
	}

	@Override
	public Set<String> getGroups () {
		return cache.getGroups();
	}

	public void shutdown() {
		cache.shutdown();		
	}

    @Override
    public List<Map<String, Object>> getCacheStatsList() {
        return new ArrayList<Map<String, Object>>();
    }

    @Override
    public Class getImplementationClass() {
        return DotJBCacheAdministratorLegacyImpl.class;
    }

    @Override
    public DotCacheAdministrator getImplementationObject() {
        return this;
    }

	@Override
	public CacheTransport getTransport () {
		throw new NotImplementedException("Method no implemented in deprecated class.");
	}

	@Override
	public void setTransport ( CacheTransport transport ) {
		throw new NotImplementedException("Method no implemented in deprecated class.");
	}

}
