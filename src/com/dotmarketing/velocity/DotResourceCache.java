/**
 * 
 */
package com.dotmarketing.velocity;

import java.io.File;

import com.dotcms.repackage.org.apache.oro.text.regex.MalformedPatternException;
import com.dotcms.repackage.org.apache.oro.text.regex.MatchResult;
import com.dotcms.repackage.org.apache.oro.text.regex.Perl5Compiler;
import com.dotcms.repackage.org.apache.oro.text.regex.Perl5Matcher;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.ResourceCache;
import org.apache.velocity.runtime.resource.ResourceManager;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringUtil;

/**
 * @author Jason Tesser
 * @author Andres Olarte
 * @since 1.6.5
 * The DotResourceCache was created to allow velocities cache to be distributed across nodes
 * in a cluster.  It also allows the dotCMS to set velocity to always cache and pull from cache
 * Our services methods which generate the velocity files will handle the filling and removing of 
 * The cache.  If something is not in cache though the DotResourceLoader will be called.  
 */
public class DotResourceCache implements ResourceCache,Cachable {

	private static ThreadLocal<Perl5Matcher> localP5Matcher = new ThreadLocal<Perl5Matcher>(){
		protected Perl5Matcher initialValue() {
			return new Perl5Matcher();
		}
	};

	
	private static com.dotcms.repackage.org.apache.oro.text.regex.Pattern assetPattern;
	private static com.dotcms.repackage.org.apache.oro.text.regex.Pattern menuPattern;
	
//	private Pattern assetPattern=Pattern.compile("[/,\\\\][0-9][/,\\\\][0-9][/,\\\\][0-9]*.[a-zA-Z]*");
//	private Pattern menuPattern=Pattern.compile("static[/,\\\\]menus[/,\\\\].*");
//	private RegExp assetRX = new RegExp("[/,\\\\][0-9][/,\\\\][0-9][/,\\\\][0-9]*.[a-zA-Z]*");
//	private RegExp menuRX = new RegExp("static[/,\\\\]menus[/,\\\\].*");
	
	private DotCacheAdministrator cache;
	
	private String primaryGroup = "VelocityCache";
	public static final String primaryOnlyMemoryGroup = "VelocityMemoryOnlyCache";
	public static final String primaryUserVTLGroup = "VelocityUserVTLCache";
	private String menuGroup = "VelocityMenuCache";
	private String missGroup = "VelocityMissCache";

    // region's name for the cache
    private String[] groupNames = {primaryGroup,menuGroup,missGroup};
    
    public DotResourceCache() {
    	cache = CacheLocator.getCacheAdministrator();
    	Perl5Compiler c = new Perl5Compiler();
    	try{
	    	assetPattern = c.compile("[/\\\\][0-9a-zA-Z][/\\\\][0-9a-zA-Z][/\\\\][0-9a-zA-Z-]*\\.[a-zA-Z]*",Perl5Compiler.READ_ONLY_MASK);
	    	menuPattern = c.compile("dynamic[/\\\\]menus[/\\\\].*",Perl5Compiler.READ_ONLY_MASK);
    	}catch (MalformedPatternException mfe) {
    		Logger.fatal(this,"Unable to instaniate dotCMS Velocity Cache",mfe);
			Logger.error(this,mfe.getMessage(),mfe);
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.resource.ResourceCache#get(java.lang.Object)
	 */
	public Resource get(Object resourceKey) {

		String cleanedResourceKey = cleanKey(resourceKey.toString());
		String group = primaryGroup;

		if ( isMenu(cleanedResourceKey) ) {
			group = menuGroup;
		} else if ( isMemoryOnly(cleanedResourceKey) ) {
			group = primaryOnlyMemoryGroup;
		} else if ( isUserVtl(resourceKey.toString()) ) {
			group = primaryUserVTLGroup;
		}

		String key = group + cleanedResourceKey;
		ResourceWrapper rw = null;
		try {
			rw = (ResourceWrapper) cache.get(key, group);
		} catch ( DotCacheException e ) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        return rw != null ? rw.getResource() : null;	
	}
	
	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.resource.ResourceCache#initialize(org.apache.velocity.runtime.RuntimeServices)
	 */
	public void initialize(RuntimeServices rs) {
		cache = CacheLocator.getCacheAdministrator();
	}

	public void addMiss(Object resourceKey) {
		resourceKey=cleanKey(resourceKey.toString());
		String key = missGroup + resourceKey;
		cache.put(key, "MISS",missGroup);
	}
	
	public boolean isMiss(Object resourceKey){
		resourceKey=cleanKey(resourceKey.toString());
		String key = missGroup + resourceKey;
		try {
			return (cache.get(key, missGroup) == null) ? false:true;
		} catch (DotCacheException e) {
			Logger.debug(this,"Cache Entry not found : " + e.getMessage(),e);
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.resource.ResourceCache#put(java.lang.Object, org.apache.velocity.runtime.resource.Resource)
	 */
	public Resource put(Object resourceKey, Resource resource) {

		ResourceWrapper rw = new ResourceWrapper(resource);
		String cleanedResourceKey = cleanKey(resourceKey.toString());
		String group = primaryGroup;

		if ( isMenu(cleanedResourceKey) ) {
			group = menuGroup;
		} else if ( isMemoryOnly(cleanedResourceKey) ) {
			group = primaryOnlyMemoryGroup;
		} else if ( isUserVtl(resourceKey.toString()) ) {
			group = primaryUserVTLGroup;
		}

		String key = group + cleanedResourceKey;
		// Add the key to the cache
		cache.put(key, rw, group);

        return rw.getResource();

	}

	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.resource.ResourceCache#remove(java.lang.Object)
	 */
	public Resource remove(Object resourceKey) {

		String cleanedResourceKey = cleanKey(resourceKey.toString());
		String group = primaryGroup;

		if ( isMenu(cleanedResourceKey) ) {
			group = menuGroup;
		} else if ( isMemoryOnly(cleanedResourceKey) ) {
			group = primaryOnlyMemoryGroup;
		} else if ( isUserVtl(resourceKey.toString()) ) {
			group = primaryUserVTLGroup;
		}

		String key = group + cleanedResourceKey;
		ResourceWrapper rw = null;
    	try{
	       cache.remove(key,group);
			if ( cleanedResourceKey.contains("content") ) {
				cache.remove(missGroup + cleanedResourceKey, missGroup);
			}
		} catch ( Exception e ) {
			Logger.debug(this, e.getMessage(), e);
		} 
    	return rw != null ? rw.getResource() : null;	
	}
	
	public void clearCache() {
        cache.flushGroup(primaryGroup);
        cache.flushGroup(menuGroup);
        cache.flushGroup(missGroup);
    }

	public void clearMenuCache() {
        cache.flushGroup(menuGroup);
    }
	
	public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
    
    private String cleanKey(String key) {
    	if(key.startsWith(ResourceManager.RESOURCE_TEMPLATE + ""))
    		key=key.substring((ResourceManager.RESOURCE_TEMPLATE+"").length());
    	Perl5Matcher matcher = (Perl5Matcher) localP5Matcher.get();
    	if(matcher.contains(key, menuPattern)){
    		MatchResult match = matcher.getMatch();
    		key = match.group(0);
    	}else{
    		if(matcher.contains(key, assetPattern)){
    			MatchResult match = matcher.getMatch();
        		key = match.group(0);
    		}
    	}
    	if (key.startsWith(File.separatorChar +"")) {
    		key=key.substring(1);
    	}
    	if (key.startsWith("/")) {
    		key=key.substring(1);
    	}
    	key = StringUtil.replace(key, '\\', '/');
    	return key;
    }

	/**
	 * @return the menuGroup
	 */
	public String getMenuGroup() {
		return menuGroup;
	}

	/**
	 * @param menuGroup the menuGroup to set
	 */
	public void setMenuGroup(String menuGroup) {
		this.menuGroup = menuGroup;
	}

	/**
	 * @param primaryGroup the primaryGroup to set
	 */
	public void setPrimaryGroup(String primaryGroup) {
		this.primaryGroup = primaryGroup;
	}
	
	private boolean isMenu(String key){
		if(key.startsWith("dynamic/menus/")){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * Method that verifies if a key belongs to the <strong>VelocityMemoryOnlyCache</strong> group cache.
	 * <br>
	 * The <strong>VelocityMemoryOnlyCache</strong> is a group used for values that can not properly being <strong>Marshal/Unmarshal</strong>
	 * and for that reason cannot live on disk caches.
	 *
	 * @param key
	 * @return
	 */
	private boolean isMemoryOnly ( String key ) {
		return key.endsWith(".vm");
	}

	/**
	 * Method that verifies if a key belongs to the <strong>VelocityUserVTLCache</strong> group cache.
	 * <br>
	 * The <strong>VelocityUserVTLCache</strong> is a group used to store vtl files used mostly on the front end.
	 *
	 * @param key
	 * @return
	 */
	private boolean isUserVtl ( String key ) {

		if ( key.startsWith(ResourceManager.RESOURCE_TEMPLATE + "") ) {
			key = key.substring((ResourceManager.RESOURCE_TEMPLATE + "").length());
		}

		if ( (key.startsWith("/") || key.startsWith("\\")) && key.endsWith(".vtl") ) {

			if ( key.startsWith("/preview_") || key.startsWith("/static/preview_") ) {
				return false;
			}

			return true;
		}

		return false;
	}

	/**
	 * @return the missGroup
	 */
	public String getMissGroup() {
		return missGroup;
	}

}
