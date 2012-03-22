package com.dotmarketing.cms.rating.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import com.dotmarketing.beans.Rating;
import com.dotmarketing.cms.rating.api.RatingAPI;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

/**
 * This strut action was created to manage the content rating 
 * @author Oswaldo Gallango
 * @version 1.0
 */
public class RatingAction extends DispatchAction {

	/**
	 * This method save the rate
	 * @param mapping
	 * @param lf
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public ActionForward rate(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		String identifier = request.getParameter("id");
		String referer = request.getParameter("referer");
		int rating = Integer.parseInt(request.getParameter("rate"));
		String llCookie = UtilMethods.getCookieValue(request.getCookies(), WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);
		HttpSession session = request.getSession(false);
		User user = (User) session.getAttribute(WebKeys.CMS_USER);
		String userId = null;
		if(user != null){
			userId=user.getUserId();
		}
		
        Rating rt = RatingAPI.getRating(llCookie, identifier);
        
		rt.setUserId(userId);
		rt.setRating(rating);
		rt.setIdentifier(identifier);
		rt.setLongLiveCookiesId(llCookie);
		rt.setSessionId(session.getId());
		rt.setUserIP(request.getRemoteAddr());
		
		RatingAPI.saveRating (rt);

		return new ActionForward(referer,true);

	}

}
