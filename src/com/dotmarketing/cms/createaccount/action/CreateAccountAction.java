package com.dotmarketing.cms.createaccount.action;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cms.createaccount.struts.CreateAccountForm;
import com.dotmarketing.cms.factories.PublicAddressFactory;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.login.action.LoginAction;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.cms.login.struts.LoginForm;
import com.dotmarketing.cms.myaccount.action.MyAccountAction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.mailinglists.factories.MailingListFactory;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.portlets.user.factories.UserCommentsFactory;
import com.dotmarketing.portlets.user.model.UserComment;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

public class CreateAccountAction extends DispatchAction {

	private CategoryAPI categoryAPI = APILocator.getCategoryAPI();

	public CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public void setCategoryAPI(CategoryAPI categoryAPI) {
		this.categoryAPI = categoryAPI;
	}

	public ActionForward unspecified(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		loadUser(lf,request);
		
		 if (request.getSession().getAttribute(WebKeys.REDIRECT_AFTER_LOGIN) != null) {
             String redir = (String) request.getSession().getAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
             request.removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
             request.getSession().setAttribute("createAccountForm",lf);
             Logger.debug(this.getClass(), "redirecting after account creation: " + redir);
             ActionForward af = new ActionForward(SecurityUtils.stripReferer(redir));
             af.setRedirect(true);
             return af;
         }
		 
		ActionForward af = mapping.findForward("createAccount");
		return af;
	}

	public ActionForward resetForm(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		lf = new CreateAccountForm();

		request.getSession().setAttribute("createAccountForm", lf);


		return unspecified(mapping, lf, request, response);
	}





	@SuppressWarnings("finally")
	public ActionForward quickCreate(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) {
		CreateAccountForm form = (CreateAccountForm) lf;
		ActionErrors ae = form.validate(mapping, request);
		try
		{
			if ((ae != null) && (ae.size() > 0)) {
				saveMessages(request.getSession(), ae);
				return mapping.findForward("createAccount");
			}
			//End Validate form    		
			createAccount(form, request, response);
			sendEmail(form,request);

			//Login the user
			LoginForm loginForm = new LoginForm();
			loginForm.setUserName(form.getEmailAddress().toLowerCase());
			loginForm.setPassword(form.getPassword1());

			LoginAction la = new LoginAction();
			ActionForward af = la.login(mapping,loginForm,request,response);
			
			loadUser(form, request);
			request.getSession().setAttribute("createAccountForm",form);
			//Verify the session.redirect
			if (request.getSession().getAttribute(WebKeys.REDIRECT_AFTER_LOGIN) == null) 
			{    		
				af = new ActionForward("/");
				af.setRedirect(true);    		
				request.getSession().setAttribute(WebKeys.REDIRECT_AFTER_LOGIN,af.getPath());                                
			}

			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.createaccount.success"));    		
			saveMessages(request, ae);
			return af;
		}
		catch(Exception ex)
		{
			Logger.error(this,ex.toString());
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.createaccount.failure"));
			saveMessages(request, ae);
			return mapping.findForward("createAccount");
		}        
	}

	@SuppressWarnings("unchecked")
	private void createAccount(CreateAccountForm form, HttpServletRequest request, HttpServletResponse response) throws NoSuchUserException, DotDataException, DotSecurityException {

		CreateAccountForm createAccountForm = (CreateAccountForm) form;
		User user = new User();
		UserAPI uAPI = APILocator.getUserAPI();
		try {
			user = uAPI.createUser(null, form.getEmailAddress());
			//user = APILocator.getUserAPI().loadByUserByEmail(form.getEmailAddress(), APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e1) {			
			Logger.warn(this, e1.toString());
		}   
		User defaultUser = APILocator.getUserAPI().getDefaultUser();
		Date today = new Date();

		//### CREATE USER ###
		Company company = PublicCompanyFactory.getDefaultCompany();
		user.setEmailAddress(createAccountForm.getEmailAddress().trim().toLowerCase());
		user.setFirstName(createAccountForm.getFirstName() == null ? "" : form.getFirstName());
		user.setLastName(createAccountForm.getLastName() == null ? "" : form.getLastName());
		user.setNickName("");
		user.setMiddleName("");        
		user.setCompanyId(company.getCompanyId());
		user.setLastLoginIP(request.getRemoteAddr());
		user.setLastLoginDate(today);
		user.setLoginIP(request.getRemoteAddr());
		user.setLoginDate(today);
		user.setPasswordEncrypted(true);
		user.setPassword(PublicEncryptionFactory.digestString(form.getPassword1()));
		user.setComments(form.getComments());
		user.setGreeting("Welcome, " + user.getFullName() + "!");            

		//Set defaults values
		if(user.isNew())
		{
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


		//### CREATE USER_PROXY ###        
		UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user.getUserId(),APILocator.getUserAPI().getSystemUser(), false);
		userProxy.setUserId(user.getUserId());
		userProxy.setPrefix(createAccountForm.getPrefix());
		userProxy.setSuffix(createAccountForm.getSuffix());
		userProxy.setTitle(createAccountForm.getTitle());
		userProxy.setSchool(createAccountForm.getSchool());
		userProxy.setGraduationYear(createAccountForm.getGraduationYear());
		userProxy.setOrganization(createAccountForm.getOrganization());
		userProxy.setCompany(createAccountForm.getOrganization());
		userProxy.setWebsite(createAccountForm.getWebsite());
		userProxy.setHowHeard(createAccountForm.getHowHeard());

		userProxy.setVar1(createAccountForm.getVar1());
		userProxy.setVar2(createAccountForm.getVar2());
		userProxy.setVar3(createAccountForm.getVar3());
		userProxy.setVar4(createAccountForm.getVar4());
		userProxy.setVar5(createAccountForm.getVar5());
		userProxy.setVar6(createAccountForm.getVar6());
		userProxy.setVar7(createAccountForm.getVar7());
		userProxy.setVar8(createAccountForm.getVar8());
		userProxy.setVar9(createAccountForm.getVar9());
		userProxy.setVar10(createAccountForm.getVar10());
		userProxy.setVar11(createAccountForm.getVar11());
		userProxy.setVar12(createAccountForm.getVar12());
		userProxy.setVar13(createAccountForm.getVar13());
		userProxy.setVar14(createAccountForm.getVar14());
		userProxy.setVar15(createAccountForm.getVar15());
		userProxy.setVar16(createAccountForm.getVar16());
		userProxy.setVar17(createAccountForm.getVar17());
		userProxy.setVar18(createAccountForm.getVar18());
		userProxy.setVar19(createAccountForm.getVar19());
		userProxy.setVar20(createAccountForm.getVar20());
		userProxy.setVar21(createAccountForm.getVar21());
		userProxy.setVar22(createAccountForm.getVar22());
		userProxy.setVar23(createAccountForm.getVar23());
		userProxy.setVar24(createAccountForm.getVar24());
		userProxy.setVar25(createAccountForm.getVar25());

		if (UtilMethods.isSet(createAccountForm.getDescription()) ||
				UtilMethods.isSet(createAccountForm.getStreet1()) ||
				UtilMethods.isSet(createAccountForm.getStreet2()) ||
				UtilMethods.isSet(createAccountForm.getCity()) ||
				UtilMethods.isSet(createAccountForm.getState()) ||
				UtilMethods.isSet(createAccountForm.getZip()) ||
				UtilMethods.isSet(createAccountForm.getCountry()) ||
				UtilMethods.isSet(createAccountForm.getPhone()) ||
				UtilMethods.isSet(createAccountForm.getFax()) ||
				UtilMethods.isSet(createAccountForm.getCell())) {
			try {
				List<Address> addresses = PublicAddressFactory.getAddressesByUserId(user.getUserId());
				Address address = (addresses.size() > 0 ? addresses.get(0) : PublicAddressFactory.getInstance());
				address.setDescription(createAccountForm.getDescription() == null ? "" : createAccountForm.getDescription());
				address.setStreet1(createAccountForm.getStreet1() == null ? "" : createAccountForm.getStreet1());
				address.setStreet2(createAccountForm.getStreet2() == null ? "" : createAccountForm.getStreet2());
				address.setCity(createAccountForm.getCity() == null ? "" : createAccountForm.getCity());
				address.setState(createAccountForm.getState() == null ? "" : createAccountForm.getState());
				address.setZip(createAccountForm.getZip() == null ? "" : createAccountForm.getZip());
				address.setCountry(createAccountForm.getCountry() == null ? "" : createAccountForm.getCountry());
				address.setPhone(createAccountForm.getPhone() == null ? "" : createAccountForm.getPhone());
				address.setFax(createAccountForm.getFax() == null ? "" : createAccountForm.getFax());
				address.setCell( createAccountForm.getCell() == null ? "" :  createAccountForm.getCell());
				address.setUserId(user.getUserId());
				address.setCompanyId(company.getCompanyId());
				PublicAddressFactory.save(address);
			} catch (Exception e) {
				Logger.warn(this, e.toString());
			}
		}

		userProxy.setMailSubscription(createAccountForm.isMailSubscription());
		com.dotmarketing.business.APILocator.getUserProxyAPI().saveUserProxy(userProxy,APILocator.getUserAPI().getSystemUser(), false);        
		//### END CRETE USER_PROXY ###


		String[] arr = form.getCategories();
		if (arr != null) {
			for (int i = 0; i < arr.length; i++) {
				try {
					Category node = (Category) InodeFactory.getInode(arr[i], Category.class);
					node.addChild(userProxy);
				} catch (Exception e) {
					Logger.warn(this, e.toString());
				}
			}
		}




		//### END CREATE USER COMMENT###
		if(UtilMethods.isSet(form.getComments())){
			UserComment userComments = new UserComment();
			userComments.setUserId(userProxy.getUserId());
			userComments.setCommentUserId(user.getUserId());
			userComments.setDate(new java.util.Date());
			userComments.setComment(form.getComments());
			userComments.setTypeComment("incoming");
			userComments.setSubject("User Comment");
			userComments.setMethod("Regular");
			userComments.setCommunicationId(null);
			UserCommentsFactory.saveUserComment(userProxy.getInode(),userComments);
		}        
		if (form.isMailSubscription()) {
			//Subscribe to the mailing list
			if(UtilMethods.isSet(Config.getStringProperty("CREATE_ACCOUNT_MAILING_LIST"))){
				List<MailingList> list = MailingListFactory.getAllMailingLists();
				for(MailingList ml : list){
					if(ml.getTitle().equals(Config.getStringProperty("CREATE_ACCOUNT_MAILING_LIST"))){ 
						MailingListFactory.addMailingSubscriber(ml, userProxy, true);
						break;
					}
				}
			}else{
				MyAccountAction.subscribeDotCMSMailingList(user);
			}
		}else {
			if(UtilMethods.isSet(Config.getStringProperty("CREATE_ACCOUNT_MAILING_LIST"))){
				List<MailingList> list = MailingListFactory.getAllMailingLists();
				for(MailingList ml : list){
					if(ml.getTitle().equals(Config.getStringProperty("CREATE_ACCOUNT_MAILING_LIST"))){ 
						MailingListFactory.deleteUserFromMailingList(ml, userProxy);
						break;
					}
				}
			}else{
				MyAccountAction.unSubsribeDotCMSMailingList(user);
			}
		}

		Role defaultRole = com.dotmarketing.business.APILocator.getRoleAPI().loadRoleByKey(Config.getStringProperty("CMS_VIEWER_ROLE"));
		String roleId = defaultRole.getId();
		if (InodeUtils.isSet(roleId)) {
			com.dotmarketing.business.APILocator.getRoleAPI().addRoleToUser(roleId, user);
		}

		try {
			LoginFactory.doLogin(form.getEmailAddress(), form.getPassword1(), true, request, response);
		} catch (Exception e) {
		}
	}

	@SuppressWarnings("unchecked")
	private void loadUser(ActionForm form, HttpServletRequest request) throws NoSuchUserException, DotDataException, DotSecurityException 
	{
		User user = null;
		CreateAccountForm createAccountForm = (CreateAccountForm) form;
		if(UtilMethods.isSet(createAccountForm.getEmailAddress()))
		{
			user = APILocator.getUserAPI().loadByUserByEmail(createAccountForm.getEmailAddress(), APILocator.getUserAPI().getSystemUser(), false); 
		}else{
			user = (User)request.getSession().getAttribute(WebKeys.CMS_USER);
		}	

		if(user != null)
		{
			//### LOAD USER ###        
			//createAccountForm.setUserName(user.getEmailAddress());
			createAccountForm.setEmailAddress(user.getEmailAddress());
			createAccountForm.setFirstName(user.getFirstName() == null ? "" : user.getFirstName());
			createAccountForm.setLastName(user.getLastName() == null ? "" : user.getLastName());
			createAccountForm.setComments(user.getComments());                            
			//### END LOAD USER ###

			//### LOAD USER_PROXY ###    			
			UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user.getUserId(),APILocator.getUserAPI().getSystemUser(), false);
			createAccountForm.setMailSubscription(userProxy.isMailSubscription());
			createAccountForm.setPrefix(userProxy.getPrefix());
			createAccountForm.setSuffix(userProxy.getSuffix());
			createAccountForm.setTitle(userProxy.getTitle());
			createAccountForm.setSchool(userProxy.getSchool());
			createAccountForm.setGraduationYear(userProxy.getGraduationYear());
			createAccountForm.setOrganization(userProxy.getOrganization());
			createAccountForm.setWebsite(userProxy.getWebsite());
			createAccountForm.setHowHeard(userProxy.getHowHeard());
			createAccountForm.setVar1(userProxy.getVar1());
			createAccountForm.setVar2(userProxy.getVar2());
			createAccountForm.setVar3(userProxy.getVar3());
			createAccountForm.setVar4(userProxy.getVar4());
			createAccountForm.setVar5(userProxy.getVar5());
			createAccountForm.setVar6(userProxy.getVar6());
			createAccountForm.setVar7(userProxy.getVar7());
			createAccountForm.setVar8(userProxy.getVar8());
			createAccountForm.setVar9(userProxy.getVar9());
			createAccountForm.setVar10(userProxy.getVar10());
			createAccountForm.setVar11(userProxy.getVar11());
			createAccountForm.setVar12(userProxy.getVar12());
			createAccountForm.setVar13(userProxy.getVar13());
			createAccountForm.setVar14(userProxy.getVar14());
			createAccountForm.setVar15(userProxy.getVar15());
			createAccountForm.setVar16(userProxy.getVar16());
			createAccountForm.setVar17(userProxy.getVar17());
			createAccountForm.setVar18(userProxy.getVar18());
			createAccountForm.setVar19(userProxy.getVar19());
			createAccountForm.setVar20(userProxy.getVar20());
			createAccountForm.setVar21(userProxy.getVar21());
			createAccountForm.setVar22(userProxy.getVar22());
			createAccountForm.setVar23(userProxy.getVar23());
			createAccountForm.setVar24(userProxy.getVar24());
			createAccountForm.setVar25(userProxy.getVar25());
			//### END LOAD USER_PROXY ###

			try{
				List<Address> addresses = PublicAddressFactory.getAddressesByUserId(user.getUserId());
				if(addresses.size() > 0){
					Address address = addresses.get(0);
					createAccountForm.setDescription(address.getDescription());
					createAccountForm.setStreet1(address.getStreet1());
					createAccountForm.setStreet2(address.getStreet2());
					createAccountForm.setCity(address.getCity());
					createAccountForm.setState(address.getState());
					createAccountForm.setZip(address.getZip());
					createAccountForm.setCountry(address.getCountry());
					createAccountForm.setPhone(address.getPhone());
					createAccountForm.setFax(address.getFax());
					createAccountForm.setCell( address.getCell() );

				}
			} catch (Exception e) {
				Logger.warn(this, e.toString());
			}
		}

	}

	public void sendEmail(CreateAccountForm form, HttpServletRequest request) throws NoSuchUserException, DotDataException, DotSecurityException, LanguageException
	{	            
		Mailer mailer = new Mailer();        

		//### CREATE MAIL ###
		StringBuffer body = new StringBuffer();

		body.append("<table border=\"1\">");
		body.append("<tr><td align=\"center\"><b>FIELD</b></td><td align=\"center\"><b>VALUE</b></td></tr>");
		//email
		String email = (UtilMethods.isSet(form.getEmailAddress()) ? form.getEmailAddress() : "&nbsp;");        
		body.append("<tr><td valign=\"top\"><b>Email Address:</b></td><td>" + email + "</td></tr>");
		//first name
		String firstName = (UtilMethods.isSet(form.getFirstName()) ? form.getFirstName() : "&nbsp;");
		body.append("<tr><td valign=\"top\"><b>First Name:</b></td><td>" + firstName + "</td></tr>");                        
		//last name
		String lastName = (UtilMethods.isSet(form.getLastName()) ? form.getLastName() : "&nbsp;");
		body.append("<tr><td valign=\"top\"><b>Last Name:</b></td><td>" + lastName + "</td></tr>");
		//title
		//organization
		String organization = (UtilMethods.isSet(form.getOrganization()) ? form.getOrganization() : "&nbsp;");
		body.append("<tr><td valign=\"top\"><b>Organization:</b></td><td>" + organization + "</td></tr>");
		//URL
		String webSite = (UtilMethods.isSet(form.getWebsite()) ? form.getWebsite() : "&nbsp;");
		body.append("<tr><td valign=\"top\"><b>webSite:</b></td><td>" + webSite + "</td></tr>");
		//comments
		String comments = (UtilMethods.isSet(form.getComments()) ? form.getComments() : "&nbsp;");
		body.append("<tr><td valign=\"top\"><b>Comment:</b></td><td>" + comments + "</td></tr>");
		//end table
		body.append("</table>");

		String emailBody = body.toString();
		//### END CREATE MAIL ###
		Company company = PublicCompanyFactory.getDefaultCompany();
		User user = APILocator.getUserAPI().loadByUserByEmail(form.getEmailAddress(), APILocator.getUserAPI().getSystemUser(), false);

		String toEmail = request.getParameter("emailAddress");
		String subject = request.getParameter("subject");
		String fromName = request.getParameter("fromName");
		String fromEmail = request.getParameter("fromEmail");
		subject = LanguageUtil.get(user, "verification-email-account-created");

		toEmail = (UtilMethods.isSet(toEmail) ? toEmail : Config.getStringProperty("CREATE_ACCOUNT_MAIL_ADDRESS"));
		subject = (UtilMethods.isSet(subject) ? subject : Config.getStringProperty("CREATE_ACCOUNT_MAIL_SUBJECT"));		
		fromName = (UtilMethods.isSet(fromName) ? fromName : Config.getStringProperty("CREATE_ACCOUNT_MAIL_NAME"));
		fromEmail = (UtilMethods.isSet(fromEmail) ? fromEmail : Config.getStringProperty("CREATE_ACCOUNT_MAIL_RETURN_ADDRESS"));        
		fromName = (UtilMethods.isSet(fromName) ? fromName : company.getName());
		fromEmail = (UtilMethods.isSet(fromEmail) ? fromEmail : company.getEmailAddress());

		mailer.setToEmail(toEmail);
		mailer.setSubject(subject);
		mailer.setFromName(fromName);
		mailer.setFromEmail(fromEmail);
		mailer.setHTMLBody(emailBody);
		mailer.sendMessage();
	}
}