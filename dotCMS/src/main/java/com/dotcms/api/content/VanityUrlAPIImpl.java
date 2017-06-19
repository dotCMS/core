package com.dotcms.api.content;

import com.dotcms.cache.VanityUrlCache;
import com.dotcms.content.model.DefaultVanityUrl;
import com.dotcms.content.model.VanityUrl;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.util.VanityUrlUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation class for the {@link VanityUrlAPI}.
 * 
 * @author oswaldogallango
 *
 */
public class VanityUrlAPIImpl implements VanityUrlAPI{

	final ContentletAPI contentletAPI = APILocator.getContentletAPI();
	final VanityUrlCache vanityURLCache = CacheLocator.getVanityURLCache();

	@Override
	public void initializeActiveVanityURLsCache(final User user) {
		this.getActiveVanityUrls(user);
	}

	@Override
	public List<VanityUrl> getAllVanityUrls(final User user) {
        ImmutableList.Builder<VanityUrl> results = ImmutableList.builder();
		try {
			List<Contentlet> contentResults = contentletAPI.search("+baseType:" + BaseContentType.VANITY_URL.getType() + " +working:true", 0, 0, StringPool.BLANK, user, false);
			contentResults.stream().forEach((Contentlet con) -> {
				results.add(getVanityUrlFromContentlet(con));
			});
		} catch(DotDataException | DotSecurityException e){
			Logger.error(this,"Error searching vanity URLs",e);
		}
		return results.build();
	}

	@Override
	public VanityUrl getWorkingVanityUrl(final String uri, final Host host, final long languageId, final User user) {
		return getVanityUrlByURI(uri,host,languageId,user,false);
	}

	@Override
	public VanityUrl getLiveVanityUrl(final String uri, final Host host, final long languageId, final User user) {
		return getVanityUrlByURI(uri,host,languageId,user,true);
	}

	protected VanityUrl getVanityUrlByURI(final String uri, final Host host, final long languageId, final User user, final boolean live) {
		VanityUrl result = vanityURLCache.get(VanityUrlUtil.sanitizeKey(host != null && InodeUtils.isSet(host.getInode())?host.getHostname()+"|"+VanityUrlUtil.fixURI(uri):VanityUrlUtil.fixURI(uri),languageId));
		if(result == null || !InodeUtils.isSet(result.getInode())){
			List<VanityUrl> results = new ArrayList<>();
			String hostCondition = (host != null?host.getHostname():"");
			try {
                List<Contentlet> contentResults = contentletAPI.search("+baseType:" + BaseContentType.VANITY_URL.getType() + " +languageId:" + languageId + " +vanityUrl:" + hostCondition + VanityUrlUtil.fixURI(uri) + (live ? " +live:true" : " +working:true"), 0, 0, StringPool.BLANK, user, false);
                contentResults.stream().forEach((Contentlet con) -> {
                    VanityUrl vanityUrl = getVanityUrlFromContentlet(con);
                    try {
                        if (con.isLive()) {
                            addToVanityURLCache(vanityUrl);
                        } else {
                            invalidateVanityUrl(vanityUrl);
                        }
                    } catch (DotDataException | DotSecurityException e) {
                        Logger.error(this, "Error processing Vanity Url - contentlet Id:" + con.getIdentifier(), e);
                    }
                    results.add(vanityUrl);
                });


			    if(results.size() == 0 && Config.getBooleanProperty("DEFAULT_VANITY_URL_TO_DEFAULT_LANGUAGE", false)){
                    contentResults = contentletAPI.search("+baseType:"+BaseContentType.VANITY_URL.getType()+" +languageId:"+APILocator.getLanguageAPI().getDefaultLanguage().getId()+" +vanityUrl:"+hostCondition+VanityUrlUtil.fixURI(uri)+(live?" +live:true":" +working:true"), 0, 0, StringPool.BLANK, user, false);
				    contentResults.stream().forEach((Contentlet con) ->{
					    VanityUrl vanityUrl = getVanityUrlFromContentlet(con);
					    try {
					        if(con.isLive()) {
						        addToVanityURLCache(vanityUrl);
					        } else{
                                invalidateVanityUrl(vanityUrl);
					        }
					    }catch(DotDataException | DotSecurityException e){
						    Logger.error(this,"Error processing Vanity Url - contentlet Id:"+con.getIdentifier(),e);
					    }
					    results.add(vanityUrl);
				    });
			    }
            } catch(DotDataException | DotSecurityException e){
                Logger.error(this,"Error searching vanity URLs",e);
            }
			result = results.size() > 0?results.get(0):null;
		}
		return result;
	}

	@Override
	public List<VanityUrl> getActiveVanityUrls(final User user) {
        ImmutableList.Builder<VanityUrl> results = new ImmutableList.Builder();
		try {
			List<Contentlet> contentResults = contentletAPI.search("+baseType:" + BaseContentType.VANITY_URL.getType() + " +live:true +deleted:false", 0, 0, StringPool.BLANK, user, false);
			contentResults.stream().forEach((Contentlet con) -> {
				VanityUrl vanityUrl = getVanityUrlFromContentlet(con);
				addToVanityURLCache(vanityUrl);
				results.add(vanityUrl);
			});
		} catch(DotDataException | DotSecurityException e){
		    Logger.error(this,"Error searching vanity URLs",e);
	    }
		return results.build();
	}

	@Override
	public VanityUrl getVanityUrlFromContentlet(final Contentlet con) {
		if (con != null){
			try{
				if(!con.isVanityUrl()) {
					throw new DotStateException("Contentlet : " + con.getInode() + " is not a Vanity Url");
				}
			}catch(DotDataException | DotSecurityException e){
				throw new DotStateException("Contentlet : " + con.getInode() + " is not a Vanity Url",e);
			}
		}else{
			throw new DotStateException("Contentlet is null");
		}

		DefaultVanityUrl vanityUrl;
		try {
			vanityUrl = (DefaultVanityUrl) CacheLocator.getVanityURLCache().get(VanityUrlUtil.sanitizeKey(con));
		} catch (DotDataException | DotRuntimeException | DotSecurityException e1) {
			throw new DotStateException(e1);
		}
		if(vanityUrl!=null){
			return vanityUrl;
		}
		vanityUrl = new DefaultVanityUrl();
		vanityUrl.setStructureInode(con.getContentTypeId());
		try {
			contentletAPI.copyProperties((Contentlet) vanityUrl, con.getMap());
		} catch (Exception e) {
			throw new DotStateException("Vanity Url Copy Failed", e);
		}
		vanityUrl.setHost(con.getHost());
		if(UtilMethods.isSet(con.getFolder())){
			try{
				Folder folder = APILocator.getFolderAPI().find(con.getFolder(), APILocator.systemUser(), false);
				vanityUrl.setFolder(folder.getInode());
			}catch(Exception e){
				vanityUrl=new DefaultVanityUrl();
				Logger.warn(this, "Unable to convert contentlet to Vanity Url " + con, e);
			}
		}

		return vanityUrl;
	}

	@Override
	public void addToVanityURLCache(VanityUrl vanityUrl){
		try {
		    if(vanityUrl.isLive()) {
                vanityURLCache.add(VanityUrlUtil.sanitizeKey((Contentlet)vanityUrl), vanityUrl);
            }else {
		        invalidateVanityUrl(vanityUrl);
            }
		} catch (DotDataException | DotRuntimeException | DotSecurityException e) {
			Logger.error(this, "Error trying to add Vanity URL identifier:"+vanityUrl.getIdentifier()+" to VanityURLCache",e);
		}
	}

	@Override
	public void invalidateVanityUrl(VanityUrl vanityUrl){
		invalidateVanityUrl((Contentlet)vanityUrl);
	}

	@Override
	public void invalidateVanityUrl(Contentlet vanityUrl){
		try {
			vanityURLCache.remove(VanityUrlUtil.sanitizeKey(vanityUrl));
		} catch (DotDataException | DotRuntimeException | DotSecurityException e) {
			Logger.error(this, "Error trying to add Vanity URL identifier:"+vanityUrl.getIdentifier()+" to VanityURLCache",e);
		}
	}
}
