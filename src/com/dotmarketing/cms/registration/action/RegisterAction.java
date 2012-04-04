package com.dotmarketing.cms.registration.action;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicAddressFactory;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.cms.myaccount.action.AccountActivationAction;
import com.dotmarketing.cms.registration.struts.RegistrationForm;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.EmailFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;

public class RegisterAction extends DispatchAction
{
	
	private HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
	
	@SuppressWarnings("unchecked")
	public ActionForward unspecified(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
	throws Exception 
	{	
		RegistrationForm registrationForm = (RegistrationForm) lf;
		
		//Copy the variable from the bean to the form
		String userProxyInode = registrationForm.getUserProxyInode();
		if (request.getAttribute("userProxyInode") != null) {
			userProxyInode = (String) request.getAttribute("userProxyInode");
		}
		UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userProxyInode,APILocator.getUserAPI().getSystemUser(), false);
		String userID = userProxy.getUserId();
		
		//long extUserInode = registrationForm.getExtUserInode();                            
		//String userID = registrationForm.getUserID();
		
		//Load the variables to save
		User user = retrieveMember(userID,lf,request,response);
		
		//ExtUser extUser = ExtUserFactory.getExtUserByInode(extUserInode);
		
//		ExtUser extUser = (ExtUser) InodeFactory.getChildOfClass(userProxy,ExtUser.class);
		
		Address address = retrieveAddress(userID,lf,request,response);

		//Copy the attributes
		BeanUtils.copyProperties(registrationForm,user);
		
//		BeanUtils.copyProperties(registrationForm,extUser);
		BeanUtils.copyProperties(registrationForm, userProxy);
		
		BeanUtils.copyProperties(registrationForm,address);

		//Find user categories
		if (UtilMethods.isSet(userProxyInode)) {
			String[] selectCatsString = new String[0];
			List<Category> categories = InodeFactory.getParentsOfClass(userProxy,Category.class);
			selectCatsString = new String[categories.size()];
			for(int i = 0;i < categories.size();i++)
			{
				selectCatsString[i] = categories.get(i).getInode();
			}
			registrationForm.setCategory(selectCatsString);
			
			registrationForm.setPassword("XXXXXXXX");
			registrationForm.setVerifyPassword("XXXXXXXX");
			registrationForm.setPassChanged(false);
		} else {
			registrationForm.setPassChanged(true);
			registrationForm.setCategory(new String[0]);
		}
		
		//Variables from event registration
		if (request.getAttribute("from") != null)
			registrationForm.setFrom((String)request.getAttribute("from"));
		if (request.getAttribute("referrer") != null)
		{
			registrationForm.setReferrer((String)request.getAttribute("referrer"));
		}
		else
		{
			if (registrationForm.getReferrer() == null || registrationForm.getReferrer().equalsIgnoreCase("")) {
				registrationForm.setReferrer("/global/index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION"));
			}
		}
		
		if((request.getAttribute("fromFindMe") != null && ((Boolean) request.getAttribute("fromFindMe")).booleanValue()) ||
				UtilMethods.isSet(userProxyInode))
		{
			ActionErrors ae = new ActionErrors();
			if ((request.getAttribute("fromFindMe") != null && ((Boolean) request.getAttribute("fromFindMe")).booleanValue()))
			{
				ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.login.confirmation"));
			}
			saveMessages(request, ae);
			request.setAttribute("notNew",true);
			return mapping.findForward("confirmation");
		}
		return mapping.findForward("open");
	}
	
	/**
	 * Login the user a redirects to registration to prefill the registration fields 
	 * @param mapping
	 * @param lf
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	public ActionForward findMe(ActionMapping mapping, ActionForm lf, HttpServletRequest request,HttpServletResponse response) throws Exception 
	{
		RegistrationForm registrationForm = (RegistrationForm) lf;
		
		boolean login = false;
		try {
			login = LoginFactory.doLogin(registrationForm.getFindMeEmailAddress(), registrationForm.getFindMePassword(), false, request, response);
		} catch (Exception e) {
		}
		
//		if (LoginFactory.doLogin(registrationForm.getFindMeEmailAddress(), registrationForm.getFindMePassword(), false, request, response)) {
		if (login) {
			User user = APILocator.getUserAPI().loadByUserByEmail(registrationForm.getEmailAddress(), APILocator.getUserAPI().getSystemUser(), false);
			UserProxy proxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);
			registrationForm.setUserProxyInode(proxy.getInode());
			if (request.getParameter("from") != null)
			{
				request.setAttribute("from",request.getParameter("from"));    			
			}
			if (request.getParameter("referrer") != null)
			{
				request.setAttribute("referrer",request.getParameter("referrer"));
			}      
			return unspecified (mapping, lf, request, response);
		}
		
		ActionErrors errors = new ActionErrors();
		errors.add(Globals.MESSAGE_KEY, new ActionMessage("error.user.not.found"));
		saveMessages(request, errors);
		registrationForm.setFindMePassword("");
		
		return mapping.findForward("open");
	}
	
	@SuppressWarnings("unchecked")
	public ActionForward saveRegistration(ActionMapping mapping, ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		//Made the casting to the actualy form class
		RegistrationForm registrationForm = (RegistrationForm) lf;
		ActionErrors ae = registrationForm.validateRegistry(request);
		
		HibernateUtil.startTransaction();
		try {
			//Validate form
			//Validate if the user fill the password and confirm password
			
			if(!UtilMethods.isSet(registrationForm.getUserProxyInode()))
			{
				if(!validateUniqueEmail(registrationForm.getEmailAddress()))
				{
					ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.forgotPasswordClickHere","javascript:forgotPassword()"));
					//ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("studer.form.unique","Email"));
					//request.setAttribute("forgotPassword",true);
				}
			}
			
			if ((ae != null) && (ae.size() > 0)) 
			{
				saveMessages(request, ae);
				ActionForward af = mapping.findForward("fail");
				return af;
			}
			//End Validate form
			
			//Save the Organization in the DB
			//Copy the variable from the bean to the form
			//String userID = registrationForm.getUserID();
			//long extUserInode = registrationForm.getExtUserInode();     
			String userProxyInode = registrationForm.getUserProxyInode();
			//Load the variables to save
			//ExtUser extUser = ExtUserFactory.getExtUserByInode(extUserInode);
			String userID = userProxyInode;
			User user = retrieveMember(userID,lf,request,response);
			String lastPassword = user.getPassword();
			
//			ExtUser extUser = (ExtUser) InodeFactory.getChildOfClass(userProxy,ExtUser.class);
			
			Address address = retrieveAddress(userID,lf,request,response);
			
			//Copy the attributes
			BeanUtils.copyProperties(user,registrationForm);
			
//			BeanUtils.copyProperties(extUser,registrationForm);
			BeanUtils.copyProperties(address,registrationForm);
			
			//Save the new values
			user.setPasswordEncrypted(true);
			String newEncryptedPassword = PublicEncryptionFactory.digestString(user.getPassword());
			
			if (registrationForm.isPassChanged()) {
				user.setPassword(newEncryptedPassword);
			} else {
				user.setPassword(lastPassword);
			}
			
			boolean isNew = user.isNew();
			user.setNew(false);
			user.setActive(false);
			saveMember(user);
			
			//If is new set the default roles
			if (isNew)
			{	
				Role role = APILocator.getRoleAPI().loadRoleByKey(Config.getStringProperty("CMS_VIEWER_ROLE"));
				com.dotmarketing.business.APILocator.getRoleAPI().addRoleToUser(role.getId(),user);         
			}
			
			saveAddress(user,address);
			//userProxy
			UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user.getUserId(),APILocator.getUserAPI().getSystemUser(), false);
			BeanUtils.copyProperties(userProxy, registrationForm);
			userProxy.setUserId(user.getUserId());
			com.dotmarketing.business.APILocator.getUserProxyAPI().saveUserProxy(userProxy,APILocator.getUserAPI().getSystemUser(), false);
			//ExtUser
			
//			ExtUserFactory.saveExtUser(extUser);
			
			//Relation userProxy - extUser
			
//			userProxy.addChild(extUser);
			
			//update the inodes in the form, for the confirmation page
			registrationForm.setUserProxyInode(userProxy.getInode());
			
			//Delete the old categories
			if (UtilMethods.isSet(userProxy.getInode()))
			{			
				List<Category> categories = InodeFactory.getParentsOfClass(userProxy,Category.class);			
				for(int i = 0;i < categories.size();i++)
				{
					categories.get(i).deleteChild(userProxy);
				}			
			}
			
			//Save the new categories
			String[] arr = registrationForm.getCategory();
			if (arr != null) 
			{
				for (int i = 0; i < arr.length; i++) 
				{
					Category node = (Category) InodeFactory.getInode(arr[i],Category.class);
					node.addChild(userProxy);
				}
			}
			

			
			HibernateUtil.commitTransaction();
			HibernateUtil.closeSession();
			
			AccountActivationAction.sendActivationAccountEmail(user, request);
			
			//make the redirect
			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.registration.activation.email"));
			saveMessages(request, ae);
			return mapping.findForward("activation_email_message");
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			HibernateUtil.closeSession();
			
			ae.add(Globals.ERROR_KEY, new ActionMessage("error.registration.unknown"));
			saveMessages(request, ae);
			return mapping.findForward("fail");
		}
	}
	

	
	public void cancel(ActionMapping mapping, ActionForm lf, HttpServletRequest request,HttpServletResponse response) 
	throws Exception 
	{
		/*RegistrationForm registrationForm = (RegistrationForm) lf;
		 String referrer = registrationForm.getReferrer();
		 response.sendRedirect(referrer);*/     
		String referrer = "/global/index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
		response.sendRedirect(referrer);
	}
	
	public void finish(ActionMapping mapping, ActionForm lf, HttpServletRequest request,HttpServletResponse response) 
	throws Exception 
	{
		RegistrationForm registrationForm = (RegistrationForm) lf;
		String referrer = "";    	
		if (request.getSession().getAttribute(WebKeys.REDIRECT_AFTER_LOGIN) != null) 
		{
			String redir = (String) request.getSession().getAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
			request.getSession().removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
			Logger.debug(this.getClass(), "redirecting after account creation: " + redir);
			referrer = redir;    		
		}
		else
		{
			referrer = registrationForm.getReferrer();
		}
		
		response.sendRedirect(referrer);
	}
	
	private User retrieveMember(String userId,ActionForm form, HttpServletRequest req, HttpServletResponse res) throws Exception 
	{   
		RegistrationForm registrationForm = (RegistrationForm) form;
		String companyId = PublicCompanyFactory.getDefaultCompanyId();
		User member = new User();
		
		if (UtilMethods.isSet(userId)) 
		{
			member = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
		}
		else
		{
			member = APILocator.getUserAPI().createUser(null, null);
			member.setCompanyId(companyId);
			member.setActive(true);
			member.setCreateDate(new Date());
			member.setGreeting("Welcome, "+ registrationForm.getFirstName()+" "+ registrationForm.getLastName()+"!");
			member.setMiddleName("");
			member.setNickName("");
		}
		return member;
	}
	
	private boolean validateUniqueEmail(String emailAddress)
	{
		String companyId = PublicCompanyFactory.getDefaultCompanyId();      
		boolean returnValue = true;
		User member = null;                
		if (UtilMethods.isSet(emailAddress)) 
		{
			try
			{
				member = APILocator.getUserAPI().loadByUserByEmail(emailAddress, APILocator.getUserAPI().getSystemUser(), false);
			}
			catch(Exception ex)
			{
				Logger.debug(this,ex.toString());
			}
		}
		if(!(member == null))
		{
			returnValue = false;
		}
		return returnValue;
	}
	
	private void saveMember(User user) throws Exception 
	{        
		APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
	}
	
	private Address retrieveAddress(String userID,ActionForm form, HttpServletRequest req, HttpServletResponse res) throws Exception 
	{     
		String companyId = PublicCompanyFactory.getDefaultCompanyId();
		
		Address address = null;
		try  {
			if (UtilMethods.isSet(userID)) 
			{
				address = (Address) PublicAddressFactory.getAddressesByUserId(userID).get(0);
			}
			else
			{
				address = PublicAddressFactory.getInstance();
				address.setCompanyId(companyId);
			}
		}
		catch(Exception e) {
			address = PublicAddressFactory.getInstance();
			address.setCompanyId(companyId);
		}
		return address;
	}
	
	private void saveAddress(User user,Address address)
	{
		address.setUserId(user.getUserId());
		PublicAddressFactory.save(address);
	}
	
	public ActionForward forgotPassword(ActionMapping mapping, ActionForm lf, HttpServletRequest request,HttpServletResponse response) throws Exception 
	{
		RegistrationForm form = (RegistrationForm) lf;		
		//if we have some errors
		ActionErrors aes = form.validate(mapping, request);
		
		if(aes != null && aes.size() > 0){
			saveMessages(request, aes);
			return mapping.findForward("fail");
		}
		User user = APILocator.getUserAPI().loadByUserByEmail(form.getEmailAddress(), APILocator.getUserAPI().getSystemUser(), false);
		if(user.isNew()){
			aes= new ActionErrors();
			aes.add(Globals.ERROR_KEY, new ActionMessage("error.forgotPasswordUserNotFound"));
			saveMessages(request, aes);
			return mapping.findForward("fail");
		}
		String pass = PublicEncryptionFactory.getRandomPassword();
		user.setPassword(PublicEncryptionFactory.digestString(pass));
		APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
		Host host = hostWebAPI.getCurrentHost(request);
		EmailFactory.sendForgotPassword(user, pass, host.getIdentifier());
		
		aes = new ActionErrors();
		aes.add(Globals.ERROR_KEY, new ActionMessage("error.forgotPasswordMailSend"));
		saveMessages(request, aes);
				
		ActionForward af = mapping.findForward("fail");
		return af;
	}
	
}