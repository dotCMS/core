package com.dotcms.api.content;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.cache.VanityUrlCache;
import com.dotcms.content.model.VanityUrl;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.VanityUrlUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Implementation class for the {@link VanityUrlFactory}.
 * @author oswaldogallango
 *
 */
public class VanityUrlFactoryImpl implements VanityUrlFactory {
	final ContentletAPI contentletAPI = APILocator.getContentletAPI();
	final VanityUrlCache vanityURLCache = CacheLocator.getVanityURLCache();

	@Override
	public List<VanityUrl> getAllVanityUrls(User user) throws DotDataException, DotSecurityException {
		List<VanityUrl> results = new ArrayList<VanityUrl>();
		List<Contentlet> contentResults = contentletAPI.search("+baseType:"+BaseContentType.VANITY_URL.getType()+" +working:true", 0, 0, "", user, false);
		contentResults.stream().forEach((Contentlet con) ->{
			results.add(fromContentlet(con));
		});

		return results;
	}

	@Override
	public List<VanityUrl> getActiveVanityUrls(User user) throws DotDataException, DotSecurityException {
		List<VanityUrl> results = new ArrayList<VanityUrl>();
		List<Contentlet> contentResults = contentletAPI.search("+baseType:"+BaseContentType.VANITY_URL.getType()+" +live:true +deleted:false", 0, 0, "", user, false);
		contentResults.stream().forEach((Contentlet con) -> {
			VanityUrl vanityUrl = fromContentlet(con);
			addToVanityURLCache(vanityUrl);
			results.add(vanityUrl);
		});
		
		return results;
	}

	@Override
	public VanityUrl getVanityUrlByURI(String uri, Host host, long languageId, User user, boolean live) 
			throws DotDataException, DotSecurityException {
		VanityUrl result = vanityURLCache.get(VanityUrlUtil.sanitizeKey(host != null && InodeUtils.isSet(host.getInode())?host.getHostname()+"|"+uri:uri,languageId));
		if(result == null || !InodeUtils.isSet(result.getInode())){
			List<VanityUrl> results = new ArrayList<VanityUrl>();
			String hostCondition = (host != null?host.getHostname():"");
			List<Contentlet> contentResults = contentletAPI.search("+baseType:"+BaseContentType.VANITY_URL.getType()+" +vanityUrl:"+hostCondition+uri+(live?" +live:true":" +working:true"), 0, 0, "", user, false);
			contentResults.stream().forEach((Contentlet con) ->{
						VanityUrl vanityUrl = fromContentlet(con);
						addToVanityURLCache(vanityUrl);
						results.add(vanityUrl);
					});

			result = results.size() > 0?results.get(0):null;
		}

		return result;
	}

	/**
	 * Add the the search results to the vanityURLCache
	 * @param results
	 */
	private void addToVanityURLCache(VanityUrl vanityUrl){
		try {
			vanityURLCache.add(VanityUrlUtil.sanitizeKey(vanityUrl), vanityUrl);
		} catch (DotDataException | DotRuntimeException | DotSecurityException e) {
			Logger.error(this, "Error trying to add Vanity URL identifier:"+vanityUrl.getIdentifier()+" to VanityURLCache",e);
		}
	}

	/**
	 * Add the the search results to the vanityURLCache
	 * @param results
	 */
	private void removeFromVanityURLCache(VanityUrl vanityUrl){
		try {
			vanityURLCache.remove(VanityUrlUtil.sanitizeKey(vanityUrl));
		} catch (DotDataException | DotRuntimeException | DotSecurityException e) {
			Logger.error(this, "Error trying to add Vanity URL identifier:"+vanityUrl.getIdentifier()+" to VanityURLCache",e);
		}
	}

	@Override
	public VanityUrl fromContentlet(Contentlet con) {

		if (con != null){
			try{
				ContentType type = APILocator.getContentTypeAPI(APILocator.systemUser()).find(con.getContentTypeId());
				if(type.baseType() != BaseContentType.VANITY_URL) {
					throw new DotStateException("Contentlet : " + con.getInode() + " is not a Vanity Url");
				}
			}catch(DotDataException | DotSecurityException e){
				throw new DotStateException("Contentlet : " + con.getInode() + " is not a Vanity Url",e);
			}
		}else{
			throw new DotStateException("Contentlet is null");
		}

		VanityUrl vanityUrl;
		try {
			vanityUrl = (VanityUrl) CacheLocator.getVanityURLCache().get(VanityUrlUtil.sanitizeKey(con));
		} catch (DotDataException | DotRuntimeException | DotSecurityException e1) {
			throw new DotStateException(e1);
		}
		if(vanityUrl!=null){
			return vanityUrl;
		}
		vanityUrl = new VanityUrl();
		vanityUrl.setStructureInode(con.getContentTypeId());
		try {
			contentletAPI.copyProperties((Contentlet) vanityUrl, con.getMap());
		} catch (Exception e) {
			throw new DotStateException("Page Copy Failed", e);
		}
		vanityUrl.setHost(con.getHost());
		if(UtilMethods.isSet(con.getFolder())){
			try{
				Identifier ident = APILocator.getIdentifierAPI().find(con);
				User systemUser = APILocator.systemUser();
				Host host = APILocator.getHostAPI().find(con.getHost(), systemUser , false);
				Folder folder = APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), host, systemUser, false);
				vanityUrl.setFolder(folder.getInode());
			}catch(Exception e){
				vanityUrl=new VanityUrl();
				Logger.warn(this, "Unable to convert contentlet to Vanity URL " + con, e);
			}
		}

		try {
			CacheLocator.getVanityURLCache().add(VanityUrlUtil.sanitizeKey(vanityUrl),vanityUrl);
		} catch (Exception e) {

		}

		return vanityUrl;
	}

	@Override
	public void publish(VanityUrl vanityUrl, User user, boolean respectFrontendRoles)
			throws DotContentletStateException, DotDataException, DotSecurityException {

		contentletAPI.publish(vanityUrl, user, respectFrontendRoles);
	}

	@Override
	public void unpublish(VanityUrl vanityUrl, User user, boolean respectFrontendRoles)
			throws DotContentletStateException, DotDataException, DotSecurityException {
		contentletAPI.unpublish(vanityUrl, user, respectFrontendRoles);
	}

	@Override
	public void archive(VanityUrl vanityUrl, User user, boolean respectFrontendRoles)
			throws DotContentletStateException, DotDataException, DotSecurityException {
		contentletAPI.archive(vanityUrl, user, respectFrontendRoles);
	}

	@Override
	public void unarchive(VanityUrl vanityUrl, User user, boolean respectFrontendRoles)
			throws DotContentletStateException, DotDataException, DotSecurityException {
		contentletAPI.unarchive(vanityUrl, user, respectFrontendRoles);
	}

	@Override
	public void delete(VanityUrl vanityUrl, User user, boolean respectFrontendRoles)
			throws DotContentletStateException, DotDataException, DotSecurityException {
		contentletAPI.delete(vanityUrl, user, respectFrontendRoles);
	}

	@Override
	public VanityUrl save(VanityUrl vanityUrl, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException{
		if(vanityUrl != null){
			removeFromVanityURLCache(vanityUrl);
		}
		Contentlet c;
		try {
			c = APILocator.getContentletAPI().checkout(vanityUrl.getInode(), user, respectFrontendRoles);
		} catch (DotContentletStateException e) {

			c = new Contentlet();
			c.setStructureInode(vanityUrl.getContentTypeId());
		}
		APILocator.getContentletAPI().copyProperties(c, vanityUrl.getMap());
		c.setInode("");
		c = APILocator.getContentletAPI().checkin(c, user, respectFrontendRoles);

		VanityUrl savedVanityUrl =  fromContentlet(c);

		if(vanityUrl.isLive()){
			APILocator.getVersionableAPI().setLive(c);
			addToVanityURLCache(savedVanityUrl);
		}

		return savedVanityUrl;
	}
}
