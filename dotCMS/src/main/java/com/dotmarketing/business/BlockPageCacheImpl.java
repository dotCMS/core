package com.dotmarketing.business;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.dotcms.concurrent.Debouncer;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Logger;

/**
 * Provides the caching implementation for HTML pages. This approach uses a main
 * key to retrieve a cached page, and a subkey to retrieve the different
 * versions of it. With this structure, during the removal of a page, all the 
 * different versions of it will also be deleted easily. So, basically:
 * <ul>
 * 	<li>
 * 		The main key is composed of:
 * 		<ul>
 * 		<li>The page Inode.</li>
 * 		<li>The page modification date in milliseconds.</li>
 * 		</ul>
 *  </li>
 *  <li>
 * 		The subkey is composed of:
 * 		<ul>
 * 		<li>The current user ID.</li>
 * 		<li>The currently selected language ID.</li>
 * 		<li>The URL map.</li>
 * 		<li>The query String in the URL.</li>
 * 		</ul>
 *  </li>
 * </ul>
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 10-17-2014
 *
 */
public class BlockPageCacheImpl extends BlockPageCache {

	private boolean canCache = false;
	private DotCacheAdministrator cache = null;
	private static String primaryCacheGroup = "BlockDirectiveHTMLPageCache";

	/**
	 * Default constructor. Initializes the internal caching structures.
	 */
	public BlockPageCacheImpl() {
		this.cache = CacheLocator.getCacheAdministrator();
		this.canCache = LicenseUtil.getLevel() >= LicenseLevel.COMMUNITY.level;
	}

	@Override
	public String getPrimaryGroup() {
		return primaryCacheGroup;
	}

	@Override
	public String[] getGroups() {
	    return new String[]{ primaryCacheGroup };

	}

	@Override
	public void clearCache() {
		cache.flushGroup(primaryCacheGroup);
	}

	Debouncer debounceAdd = new Debouncer();
	
	
    @Override
    public void add(IHTMLPage page, final String pageContent, PageCacheParameters pageChacheParams) {
        if (!canCache || page == null || pageChacheParams == null ) {
            return;
        }
        
        final String key = pageChacheParams.getKey();
        Map<String,Serializable> cacheMap = Map.of(BlockDirectiveCache.PAGE_CONTENT_KEY, pageContent);
        final BlockDirectiveCacheObject cto = new BlockDirectiveCacheObject(cacheMap, (int) page.getCacheTTL());


        debounceAdd.debounce(key, () -> 

            this.cache.put(key, cto, primaryCacheGroup)


        , 1, TimeUnit.SECONDS);


    }

    @Override
    public String get(final IHTMLPage page, final PageCacheParameters pageChacheParams) {
        if (!canCache || page == null || pageChacheParams == null ) {
            return null;
        }

        final String key = pageChacheParams.getKey();

        // Lookup the cached versions of the page based on inode and moddate

        BlockDirectiveCacheObject bdco = (BlockDirectiveCacheObject) this.cache.getNoThrow(key, primaryCacheGroup);
        if (bdco == null) {
            return null;
        }
        
        // if we are not expired, return
        if (bdco.getCreated() + ((int) page.getCacheTTL() * 1000) > System.currentTimeMillis()) {
            return (String) bdco.getMap().getOrDefault(BlockDirectiveCache.PAGE_CONTENT_KEY,"");
        }

        remove(page);
        return null;
    }

	@Override
	public void remove(IHTMLPage page) {
		try {
			StringBuilder key = new StringBuilder();
			key.append(page.getInode());
			key.append("_" + page.getModDate().getTime());
			this.cache.remove(key.toString(), primaryCacheGroup);
		} catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		}
	}

}
