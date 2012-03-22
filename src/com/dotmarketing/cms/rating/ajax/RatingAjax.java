package com.dotmarketing.cms.rating.ajax;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import uk.ltd.getahead.dwr.WebContextFactory;

import com.dotmarketing.beans.Rating;
import com.dotmarketing.cms.rating.api.RatingAPI;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class RatingAjax  {

	public void rateContent(String identifier, int rating) throws ServletException, IOException {

        HttpSession session = WebContextFactory.get().getSession();
        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        
        String llCookie = UtilMethods.getCookieValue(req.getCookies(), WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);
        
        Rating rt = RatingAPI.getRating(llCookie, identifier);
        
        User user = (User) session.getAttribute(WebKeys.CMS_USER);
		String userId = null;
		if(user != null){
			userId=user.getUserId();
		}		
		
		rt.setUserId(userId);
		rt.setRating(rating);
		rt.setIdentifier(identifier);
		rt.setLongLiveCookiesId(llCookie);
		rt.setSessionId(session.getId());
		rt.setUserIP(req.getRemoteAddr());
		
		RatingAPI.saveRating (rt);
		
	}
}