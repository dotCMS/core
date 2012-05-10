package com.dotmarketing.viewtools;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.cms.rating.api.RatingAPI;
import com.dotmarketing.util.CookieUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class RatingWebAPI implements ViewTool {

	private HttpServletRequest req;

	public void init(Object obj) {
		this.req = ((ViewContext) obj).getRequest();
	}

	/**
	 * Returns the user rating based on the user long lived cookie
	 * @param identifier
	 * @return
	 */
	public float getRating(String identifier) {
		String _dotCMSID = "";
		if(!UtilMethods.isSet(UtilMethods.getCookieValue(req.getCookies(),
				com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE))) {
			Cookie idCookie = CookieUtil.createCookie();
		}
		_dotCMSID = UtilMethods.getCookieValue(req.getCookies(),
				com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);

		User currentUser = null;
		String userId = "";
		try {
			currentUser = com.liferay.portal.util.PortalUtil.getUser(req);

			if(currentUser==null) {
				HttpSession session = req.getSession(false);
				if (session != null) {
					currentUser = (User) session.getAttribute(WebKeys.CMS_USER);
				}
			}
			userId = currentUser.getUserId();
		} catch (Exception e) {
			Logger.debug(this, "Error trying to obtain the current liferay user from the request.", e);
		}

		return RatingAPI.getRating(_dotCMSID, identifier, userId).getRating();
	}

	@Deprecated
	public float getRating(long identifier) {
		return getRating(String.valueOf(identifier));
	}

	public float getAverageRating(String identifier) {
		return RatingAPI.getAverageRating(identifier);
	}

	@Deprecated
	public float getAverageRating(long identifier) {
		return getAverageRating(String.valueOf(identifier));
	}

	/**
	 * This method return if a content was alreaded rated by a user based on the user long lived cookie
	 * @param inode
	 * @return boolean
	 * @author Oswaldo Gallango
	 */
	public boolean wasAlreadyRated(String identifier){
		String _dotCMSID = "";
		if(!UtilMethods.isSet(UtilMethods.getCookieValue(req.getCookies(),
				com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE))) {
			Cookie idCookie = CookieUtil.createCookie();
		}
		_dotCMSID = UtilMethods.getCookieValue(req.getCookies(),
				com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);
		return RatingAPI.wasAlreadyRated(identifier, _dotCMSID);
	}


	/**
	 * Get the number of vote that rate this content
	 * @param inode
	 * @return String
	 */
	@Deprecated
	public String getRatingVotesNumber(long identifier) {
		return getRatingVotesNumber(String.valueOf(identifier));
	}

	/**
	 * Get the number of vote that rate this content
	 * @param inode
	 * @return String
	 */
	public String getRatingVotesNumber(String identifier) {
		return RatingAPI.getRatingVotesNumber(identifier);
	}

	/**
	 * Returns the Maximun Rating value could be set
	 * @return int
	 * @author Oswaldo Gallango
	 */
	public int getMaxRatingValue(){
		return RatingAPI.getMaxRatingValue();
	}

//http://jira.dotmarketing.net/browse/DOTCMS-3956
	public String convertToJSCompatibleInode(String inode) {

		String jsCompatibleInode = null;

		if (inode != null && inode.contains("-")) {
			jsCompatibleInode = inode.replace("-", "_");

		}
		if (jsCompatibleInode != null) {
			return jsCompatibleInode;
		} else
			return inode;
	}
}