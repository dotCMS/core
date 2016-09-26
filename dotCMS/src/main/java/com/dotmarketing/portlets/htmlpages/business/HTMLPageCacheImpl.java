package com.dotmarketing.portlets.htmlpages.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.Logger;

public class HTMLPageCacheImpl extends HTMLPageCache {
	
	private DotCacheAdministrator cache;
	
	private static String primaryGroup = "HTMLPageCache";
    // region's name for the cache
    private static String[] groupNames = {primaryGroup};

	public HTMLPageCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	public IHTMLPage add(IHTMLPage htmlPage) throws DotStateException, DotDataException, DotSecurityException {
		String key = primaryGroup + htmlPage.getIdentifier();
		
		if(!htmlPage.isLive()){
			throw new DotStateException("HTMLPageCache is only designed (for now) to store live versions");
		}
		
        // Add the key to the cache
        cache.put(key, htmlPage, primaryGroup);
        
        
        key = primaryGroup + htmlPage.getInode();
        cache.put(key, htmlPage, primaryGroup);
        
		return htmlPage;
		
	}
	
	@Override
	public IHTMLPage get(String key) {

		key = primaryGroup + key;
		IHTMLPage htmlPage = null;
    	try{
    		htmlPage = (IHTMLPage)cache.get(key,primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        return htmlPage;	
	}

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
    public void clearCache() {
        // clear the cache
        cache.flushGroup(primaryGroup);
    }
    @Override
    public void remove(String pageIdentifier){
    	
    	IHTMLPage page = new HTMLPage();
    	page.setIdentifier(pageIdentifier);
    	remove(page);
    }
    
    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
    @Override
    public void remove(IHTMLPage page){
		IHTMLPage holder = get(page.getIdentifier());
		
		if(holder!=null){
	    	try{
	    		cache.remove(primaryGroup + holder.getIdentifier(),primaryGroup);
	    	}catch (Exception e) {
				Logger.debug(this, "Cache not able to be removed", e);
			} 
	    	try{
	    		cache.remove(primaryGroup + holder.getInode(),primaryGroup);
	    	}catch (Exception e) {
				Logger.debug(this, "Cache not able to be removed", e);
			} 
		}
    }
    public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
}
