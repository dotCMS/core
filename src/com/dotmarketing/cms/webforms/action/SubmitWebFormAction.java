package com.dotmarketing.cms.webforms.action;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.captcha.Captcha;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.actions.DispatchAction;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicAddressFactory;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.factories.ClickstreamFactory;
import com.dotmarketing.factories.EmailFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.user.factories.UserCommentsFactory;
import com.dotmarketing.portlets.user.model.UserComment;
import com.dotmarketing.portlets.webforms.model.WebForm;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.servlet.UploadServletRequest;

/**
 *
 * @author David
 * @version $Revision: 1.5 $ $Date: 2007/07/18 16:48:42 $
 */
public final class SubmitWebFormAction extends DispatchAction {

	HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
	
	@SuppressWarnings("unchecked")
	public ActionForward unspecified(ActionMapping rMapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
	throws Exception {
		ActionErrors errors = new ActionErrors();
		//Email parameters
		HttpSession session = request.getSession();
		Host currentHost = hostWebAPI.getCurrentHost(request);
		User currentUser = (User) session.getAttribute(WebKeys.CMS_USER);
		
		String method = request.getMethod();
		String errorURL = request.getParameter("errorURL");		
		errorURL = (!UtilMethods.isSet(errorURL) ? request.getHeader("referer") : errorURL);
		if(errorURL.indexOf("?") > -1)
		{
			errorURL = errorURL.substring(0,errorURL.lastIndexOf("?"));
		}
		String x = request.getRequestURI();
		if(request.getParameterMap().size() <2){
			
			return null;
			
		}

		//Checking for captcha
		boolean useCaptcha = Config.getBooleanProperty("FORCE_CAPTCHA",true);
		if(!useCaptcha){
			useCaptcha = new Boolean(request.getParameter("useCaptcha")).booleanValue();
		}
		
		String captcha = request.getParameter("captcha");
		if (useCaptcha) {
		    Captcha captchaObj = (Captcha) session.getAttribute(Captcha.NAME);
            String captchaSession=captchaObj!=null ? captchaObj.getAnswer() : null;
            
			if(captcha ==null && Config.getBooleanProperty("FORCE_CAPTCHA",true)){
				response.getWriter().write("Captcha is required to submit this form ( FORCE_CAPTCHA=true ).<br>To change this, edit the dotmarketing-config.properties and set FORCE_CAPTCHA=false");
				return null;
			}
			
			
			if(!UtilMethods.isSet(captcha) || !UtilMethods.isSet(captchaSession) || !captcha.equals(captchaSession)) {
				errors.add(Globals.ERROR_KEY, new ActionMessage("message.contentlet.required", "Validation Image"));
				request.setAttribute(Globals.ERROR_KEY, errors);
				session.setAttribute(Globals.ERROR_KEY, errors);
				String queryString = request.getQueryString();
				String invalidCaptchaURL = request.getParameter("invalidCaptchaReturnUrl");
				if(!UtilMethods.isSet(invalidCaptchaURL)) {
					invalidCaptchaURL = errorURL;
				}
				ActionForward af = new ActionForward();
					af.setRedirect(true);
					if (UtilMethods.isSet(queryString)) {
						
						af.setPath(invalidCaptchaURL + "?" + queryString + "&error=Validation-Image");
					} else {
						af.setPath(invalidCaptchaURL + "?error=Validation-Image");
					}
			

				
				return af;
			}
			
		}



		Map<String, Object> parameters = null;
		if (request instanceof UploadServletRequest)
		{
			UploadServletRequest uploadReq = (UploadServletRequest) request;
			parameters = new HashMap<String, Object> (uploadReq.getParameterMap());
			for (Entry<String, Object> entry : parameters.entrySet())
			{
				if(entry.getKey().toLowerCase().indexOf("file") > -1 && !entry.getKey().equals("attachFiles"))
				{
					parameters.put(entry.getKey(), uploadReq.getFile(entry.getKey()));
				}
			}
		}
		else
		{
			parameters = new HashMap<String, Object> (request.getParameterMap());
		}

		Set<String> toValidate = new java.util.HashSet<String>(parameters.keySet());

		//Enhancing the ignored parameters not to be send in the email
		String ignoredParameters = (String) EmailFactory.getMapValue("ignore", parameters);
		if(ignoredParameters == null)
		{
			ignoredParameters = "";
		}
		ignoredParameters += ":useCaptcha:captcha:invalidCaptchaReturnUrl:return:returnUrl:errorURL:ignore:to:from:cc:bcc:dispatch:order:prettyOrder:autoReplyTo:autoReplyFrom:autoReplyText:autoReplySubject:";
		parameters.put("ignore", ignoredParameters);

		// getting categories from inodes
		// getting parent category name and child categories name
		// and replacing the "categories" parameter
		String categories = "";
		String[] categoriesArray = request.getParameterValues("categories");
		if (categoriesArray != null) {
			HashMap hashCategories = new HashMap<String, String>();
			for (int i = 0; i < categoriesArray.length; i++) {
				Category node = (Category) InodeFactory.getInode(categoriesArray[i], Category.class);
				Category parent = (Category) InodeFactory.getParentOfClass(node, Category.class);
				String parentCategoryName = parent.getCategoryName();

				if (hashCategories.containsKey(parentCategoryName)) {
					String childCategoryName = (String) hashCategories.get(parentCategoryName);
					if (UtilMethods.isSet(childCategoryName)) {
						childCategoryName += ", ";
					}
					childCategoryName += node.getCategoryName();
					hashCategories.put(parentCategoryName, childCategoryName);
				}
				else {
					hashCategories.put(parentCategoryName, node.getCategoryName());
				}
			}

			Set<String> keySet = hashCategories.keySet();
			for (String stringKey: keySet) {

				if (UtilMethods.isSet(categories)) {
					categories += "; "; 
				}
				categories += stringKey + " : " + (String) hashCategories.get(stringKey);
				parameters.put(stringKey, (String) hashCategories.get(stringKey));
			}
			parameters.remove("categories");
		}

		WebForm webForm = new WebForm();
		try
		{
			/*validation parameter should ignore the returnUrl and erroURL field in the spam check*/
			String[] removeParams = ignoredParameters.split(":");
			for(String param : removeParams){
				toValidate.remove(param);
			}
			
			
			
			parameters.put("request", request);
			parameters.put("response", response);
			
			//Sending the email			
			webForm = EmailFactory.sendParameterizedEmail(parameters, toValidate, currentHost, currentUser);
			
			
			
			webForm.setCategories(categories);

			if(UtilMethods.isSet(request.getParameter("createAccount")) && request.getParameter("createAccount").equals("true"))
			{
				//if we create account set to true we create a user account and add user comments.
				createAccount(webForm, request);
				try{
    				String userInode = webForm.getUserInode();
    				String customFields = webForm.getCustomFields();
    				customFields += " User Inode = " + String.valueOf(userInode) + " | ";
    				webForm.setCustomFields(customFields);
				}
				catch(Exception e){
				    
				}

			}

			
            if(UtilMethods.isSet(webForm.getFormType())){
                HibernateUtil.saveOrUpdate(webForm);
            }
			
			
			if (request.getParameter("return") != null)
			{
				ActionForward af = new ActionForward(SecurityUtils.stripReferer(request, request.getParameter("return")));
				af.setRedirect(true);
				return af;
			}
			else if (request.getParameter("returnUrl") != null)
			{
				ActionForward af = new ActionForward(SecurityUtils.stripReferer(request, request.getParameter("returnUrl")));
				af.setRedirect(true);
				return af;
			}
			else
			{
				return rMapping.findForward("thankYouPage");
			}

        }
        catch (DotRuntimeException e)
        {
            errors.add(Globals.ERROR_KEY, new ActionMessage("error.processing.your.email"));
            request.getSession().setAttribute(Globals.ERROR_KEY, errors);

            String queryString = request.getQueryString();

            if (queryString == null) {
                java.util.Enumeration<String> parameterNames = request.getParameterNames();
                queryString = "";
                String parameterName;
                for (; parameterNames.hasMoreElements();) {
                    parameterName = parameterNames.nextElement();

                    if (0 < queryString.length()) {
                        queryString = queryString + "&" + parameterName + "=" + UtilMethods.encodeURL(request.getParameter(parameterName));
                    } else {
                        queryString = parameterName + "=" + UtilMethods.encodeURL(request.getParameter(parameterName));
                    }
                }
            }

            ActionForward af;
            if (UtilMethods.isSet(queryString)) {
                af = new ActionForward(SecurityUtils.stripReferer(request, errorURL + "?" + queryString));
            } else {
                af = new ActionForward(SecurityUtils.stripReferer(request, errorURL));
            }

            af.setRedirect(true);

            return af;
        }
		

	}
	private void createAccount(WebForm form, HttpServletRequest request) throws Exception {

		User user = APILocator.getUserAPI().loadByUserByEmail(form.getEmail(), APILocator.getUserAPI().getSystemUser(), false);
		User defaultUser = APILocator.getUserAPI().getDefaultUser();
		Date today = new Date();

		if (user.isNew() || (!user.isNew() && user.getLastLoginDate() == null)) {

			// ### CREATE USER ###
			Company company = PublicCompanyFactory.getDefaultCompany();
			user.setEmailAddress(form.getEmail().trim().toLowerCase());
			user.setFirstName(form.getFirstName() == null ? "" : form.getFirstName());
			user.setMiddleName(form.getMiddleName() == null ? "" : form.getMiddleName());
			user.setLastName(form.getLastName() == null ? "" : form.getLastName());
			user.setNickName("");
			user.setCompanyId(company.getCompanyId());
			user.setPasswordEncrypted(true);
			user.setGreeting("Welcome, " + user.getFullName() + "!");

			// Set defaults values
			if (user.isNew()) {
				//if it's a new user we set random password
				String pass = PublicEncryptionFactory.getRandomPassword();
				user.setPassword(PublicEncryptionFactory.digestString(pass));
				user.setLanguageId(defaultUser.getLanguageId());
				user.setTimeZoneId(defaultUser.getTimeZoneId());
				user.setSkinId(defaultUser.getSkinId());
				user.setDottedSkins(defaultUser.isDottedSkins());
				user.setRoundedSkins(defaultUser.isRoundedSkins());
				user.setResolution(defaultUser.getResolution());
				user.setRefreshRate(defaultUser.getRefreshRate());
				user.setLayoutIds("");
				user.setActive(true);
				user.setCreateDate(today);
			}
			APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
			// ### END CREATE USER ###

			// ### CREATE USER_PROXY ###
			UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user.getUserId(),APILocator.getUserAPI().getSystemUser(), false);
			userProxy.setPrefix("");
			userProxy.setTitle(form.getTitle());
			userProxy.setOrganization(form.getOrganization());
			userProxy.setUserId(user.getUserId());
			com.dotmarketing.business.APILocator.getUserProxyAPI().saveUserProxy(userProxy,APILocator.getUserAPI().getSystemUser(), false);
			// ### END CRETE USER_PROXY ###

			// saving user inode on web form
			form.setUserInode(userProxy.getInode());
			if(UtilMethods.isSet(form.getFormType())){
			HibernateUtil.saveOrUpdate(form);
			}

			///// WE CAN DO THIS! BUT WE NEED TO ADD CATEGORIES TO WEBFORM AND ALSO CHANGE THE PROCESSES THAT
			//// CREATE THE EXCEL DOWNLOAD FROM WEB FORMS. I DIDN'T ADD IT SO I COMMENTED THIS CODE FOR NOW
			//get the old categories, wipe them out
			/*
			List<Category> categories = InodeFactory.getParentsOfClass(userProxy, Category.class);
			for (int i = 0; i < categories.size(); i++) {
				categories.get(i).deleteChild(userProxy);
			}
			 */
			// Save the new categories
			/*String[] arr = form.getCategories();
			if (arr != null) {
				for (int i = 0; i < arr.length; i++) {
					Category node = (Category) InodeFactory.getInode(arr[i], Category.class);
					node.addChild(userProxy);
				}
			}*/

			// ### CREATE ADDRESS ###
			try {
				List<Address> addresses = PublicAddressFactory.getAddressesByUserId(user.getUserId());
				Address address = (addresses.size() > 0 ? addresses.get(0) : PublicAddressFactory.getInstance());
				address.setStreet1(form.getAddress1() == null ? "" : form.getAddress1());
				address.setStreet2(form.getAddress2() == null ? "" : form.getAddress2());
				address.setCity(form.getCity() == null ? "" : form.getCity());
				address.setState(form.getState() == null ? "" : form.getState());
				address.setZip(form.getZip() == null ? "" : form.getZip());
				String phone = form.getPhone();
				address.setPhone(phone == null ? "" : phone);
				address.setUserId(user.getUserId());
				address.setCompanyId(company.getCompanyId());
				PublicAddressFactory.save(address);
			} catch (Exception ex) {
				Logger.error(this,ex.getMessage(),ex);
			}

			Role defaultRole = com.dotmarketing.business.APILocator.getRoleAPI().loadRoleByKey(Config.getStringProperty("CMS_VIEWER_ROLE"));
			String roleId = defaultRole.getId();
			if (InodeUtils.isSet(roleId)) {
				com.dotmarketing.business.APILocator.getRoleAPI().addRoleToUser(roleId, user);
			}
		}
		// ### END CREATE ADDRESS ###

		// ### BUILD THE USER COMMENT ###
		addUserComments(user.getUserId(),form,request);
		// ### END BUILD THE USER COMMENT ###

		/* associate user with their clickstream request */
		if(Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", false)){
			ClickstreamFactory.setClickStreamUser(user.getUserId(), request);
		}

	}

	private void addUserComments(String userid, WebForm webForm, HttpServletRequest request) throws Exception {

		Date now = new Date();
		String webFormType = webForm.getFormType();
		String webFormId = webForm.getWebFormId();

		UserComment userComments = new UserComment();
		userComments.setUserId(userid);
		userComments.setCommentUserId(userid);
		userComments.setDate(now);
		if (request.getParameter("comments")!=null) {
			userComments.setComment(request.getParameter("comments"));
		}
		else if(UtilMethods.isSet(webForm.getFormType())) {
		    userComments.setSubject("User submitted: " + webFormType);
			userComments.setComment("Web Form: " + webFormType + " - ID: " + webFormId);
		}
		else{
		    userComments.setSubject("User submitted Form: Open Entry ");
		    StringBuffer buffy = new StringBuffer();
		    Enumeration x = request.getParameterNames();
		    while(x.hasMoreElements()){
		        String key = (String) x.nextElement();
		        buffy.append(key);
		        buffy.append(":\t");
		        buffy.append(request.getParameter(key));
		        buffy.append("\n");
		        if(buffy.length() > 65000){
		            break;
		        }
		    }
		    userComments.setComment(buffy.toString());
		    
		}
		
		userComments.setTypeComment(UserComment.TYPE_INCOMING);
		userComments.setMethod(UserComment.METHOD_WEB);
		userComments.setCommunicationId(null);
		UserCommentsFactory.saveUserComment(userComments);
	}

}
