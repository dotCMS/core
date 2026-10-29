package com.dotmarketing.portlets.links.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;

/**
 * Struts action behind the publish button of the Links screen: makes the working version of each
 * posted menu link live. Every link is checked against the user's PUBLISH permission before it is
 * touched, so the screen publishes only what the user is allowed to publish.
 *
 * @author  Maria Ahues
 * @version $Revision: 1.2 $
 */
public class PublishLinksAction extends DotPortletAction {

	private static final String PUBLISH_INODE_PARAM = "publishInode";

	/**
	 * Publishes the links named by the request and returns to the referer when one is given.
	 *
	 * @param mapping the Struts mapping that led here
	 * @param form the Struts form, unused
	 * @param config the portlet configuration
	 * @param req the portlet request carrying the {@code publishInode} values
	 * @param res the portlet response
	 * @throws Exception if the referer cannot be decoded
	 */
	@Override
	public void processAction(
			 ActionMapping mapping, ActionForm form, PortletConfig config,
			 ActionRequest req, ActionResponse res)
		 throws Exception {

		Logger.debug(this, "Running PublishLinksAction!!!!");

		String referer = req.getParameter("referer");
		if ((referer!=null) && (referer.length()!=0)) {
			referer = URLDecoder.decode(referer,"UTF-8");
		}
		
		try {
			//get the user
			User user = com.liferay.portal.util.PortalUtil.getUser(req);

			_publishLinks(req, user);
			
			if ((referer!=null) && (referer.length()!=0)) {
				_sendToReferral(req, res, referer);
			}
			
			setForward(req, "portlet.ext.links.publish_links");

		}
		catch (Exception e) {
			_handleException(e, req);
		}
	}

	/**
	 * Publishes every link named by a {@code publishInode} parameter that the user may publish.
	 * A link the user holds no PUBLISH permission on is left as it is and reported as an error on
	 * the screen, and so is an archived one; the links the user may publish still go live.
	 *
	 * @param req the portlet request carrying the inodes
	 * @param user the {@link User} publishing, whose PUBLISH permission decides each link
	 * @throws Exception if a link cannot be loaded or published
	 */
	private void _publishLinks(final ActionRequest req, final User user) throws Exception {

		final String[] publishInodes = req.getParameterValues(PUBLISH_INODE_PARAM);
		if (publishInodes == null) {
			return;
		}

		final HttpServletRequest httpRequest = ((ActionRequestImpl) req).getHttpServletRequest();
		final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
		boolean published = false;
		boolean refused = false;

		for (final String publishInode : publishInodes) {
			final Link link = (Link) InodeFactory.getInode(publishInode, Link.class);
			if (!InodeUtils.isSet(link.getInode())) {
				continue;
			}

			if (!permissionAPI.doesUserHavePermission(link, PERMISSION_PUBLISH, user)) {
				Logger.warn(this, "User " + user.getUserId()
						+ " does not have permission to publish link " + link.getInode());
				refused = true;
				continue;
			}

			if (link.isArchived()) {
				continue;
			}

			try {
				PublishFactory.publishAsset(link, httpRequest);
				published = true;
			} catch (final WebAssetException wax) {
				Logger.error(this, wax.getMessage(), wax);
				SessionMessages.add(req, "error", "message.webasset.published.failed");
			}
		}

		if (published) {
			SessionMessages.add(req, "message", "message.link_list.published");
		}
		if (refused) {
			SessionMessages.add(req, "error", "Publish-failed-check-you-have-the-required-permission");
		} else if (!published) {
			SessionMessages.add(req, "error", "message.webasset.cannot.published.archived");
		}
	}

}
