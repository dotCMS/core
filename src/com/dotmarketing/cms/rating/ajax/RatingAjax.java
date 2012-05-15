package com.dotmarketing.cms.rating.ajax;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import uk.ltd.getahead.dwr.WebContextFactory;

import com.dotmarketing.beans.Rating;
import com.dotmarketing.cms.rating.api.RatingAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class RatingAjax  {

	public void rateContent(String identifier, int rating) throws ServletException, IOException {

        HttpSession session = WebContextFactory.get().getSession();
        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();

        String llCookie = UtilMethods.getCookieValue(req.getCookies(), WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);

        User currentUser = null;
		String userId = "";
		try {
			currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
			userId = currentUser.getUserId();
		} catch (Exception e) {
			Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
		}

		Rating rt = RatingAPI.getRating(llCookie, identifier, userId);

		rt.setUserId(userId);
		rt.setRating(rating);
		rt.setIdentifier(identifier);
		rt.setLongLiveCookiesId(llCookie);
		rt.setSessionId(session.getId());
		rt.setUserIP(req.getRemoteAddr());

		RatingAPI.saveRating (rt);

	}
}