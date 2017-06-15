package com.dotcms.api.content;

import java.util.List;

import com.dotcms.content.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

/**
 * Implementation class for the {@link VanityUrlAPI}.
 * 
 * @author oswaldogallango
 *
 */
public class VanityUrlAPIImpl implements VanityUrlAPI{

	final VanityUrlFactory vanityURLFactory = FactoryLocator.getVanityUrlFactory();

	@Override
	public void initializeActiveVanityURLsCache(User user) throws DotDataException, DotSecurityException {
		this.getActiveVanityUrls(user);
	}

	@Override
	public List<VanityUrl> getAllVanityUrls(User user) throws DotDataException, DotSecurityException {
		return vanityURLFactory.getAllVanityUrls(user);
	}

	@Override
	public VanityUrl getVanityUrlByURI(String uri, Host host, long languageId, User user, boolean live) 
			throws DotDataException, DotSecurityException {
		return vanityURLFactory.getVanityUrlByURI(uri, host, languageId, user, live);
	}

	@Override
	public List<VanityUrl> getActiveVanityUrls(User user) throws DotDataException, DotSecurityException {
		return vanityURLFactory.getActiveVanityUrls(user);
	}

	@Override
	public VanityUrl fromContentlet(Contentlet con) {
		return vanityURLFactory.fromContentlet(con);
	}

	@Override
	public void publish(VanityUrl vanityUrl, User user, boolean respectFrontendRoles)
			throws DotContentletStateException, DotDataException, DotSecurityException {
		vanityURLFactory.publish(vanityUrl, user, respectFrontendRoles);
	}

	@Override
	public void unpublish(VanityUrl vanityUrl, User user, boolean respectFrontendRoles)
			throws DotContentletStateException, DotDataException, DotSecurityException {
		vanityURLFactory.unpublish(vanityUrl, user, respectFrontendRoles);
	}

	@Override
	public void archive(VanityUrl vanityUrl, User user, boolean respectFrontendRoles)
			throws DotContentletStateException, DotDataException, DotSecurityException {
		vanityURLFactory.archive(vanityUrl, user, respectFrontendRoles);
	}

	@Override
	public void unarchive(VanityUrl vanityUrl, User user, boolean respectFrontendRoles)
			throws DotContentletStateException, DotDataException, DotSecurityException {
		vanityURLFactory.unarchive(vanityUrl, user, respectFrontendRoles);
	}

	@Override
	public void delete(VanityUrl vanityUrl, User user, boolean respectFrontendRoles)
			throws DotContentletStateException, DotDataException, DotSecurityException {
		vanityURLFactory.delete(vanityUrl, user, respectFrontendRoles);
	}
	
	@Override
	public VanityUrl save(VanityUrl vanityUrl, User user, boolean respectFrontendRoles) 
			throws DotDataException, DotSecurityException{
		return vanityURLFactory.save(vanityUrl, user, respectFrontendRoles);
	}
	
}
