package com.dotmarketing.portlets.usermanager.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicAddressFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.EmailFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.usermanager.struts.UserManagerForm;
import com.dotmarketing.portlets.usermanager.struts.UserManagerListSearchForm;
import com.dotmarketing.tag.factories.TagFactory;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

/*
 * @author Oswaldo Gallango
 */
public class EditUserManagerAction extends DotPortletAction{

	private CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	
    public CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public void setCategoryAPI(CategoryAPI categoryAPI) {
		this.categoryAPI = categoryAPI;
	}

	public static boolean debug = false;

	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req,
			ActionResponse res) throws Exception {

		HibernateUtil.startTransaction();
		String cmd = req.getParameter(com.liferay.portal.util.Constants.CMD);
		String referer = req.getParameter("referer");
		if ((referer!=null) && (referer.length()!=0))
		{
			referer = URLDecoder.decode(referer,"UTF-8");			
		}
		HttpServletRequest httpReq = ((ActionRequestImpl)req).getHttpServletRequest();

		//get user
		try {
			_editUserManager("",form, req, res, cmd);
		} catch (Exception e) {
		}

		req.setAttribute(WebKeys.USERMANAGER_EDIT_FORM, form);

		// Save / Update usermanager
		if (com.liferay.portal.util.Constants.SAVE.equals(cmd)) {
			Logger.debug(this, "Saving UserInfo");

			///Validate Form
			if (!Validator.validate(req, form, mapping)) {
				Logger.debug(this, "Form Validation Failed");
				req.setAttribute(WebKeys.USERMANAGER_EDIT_FORM, form);
				BeanUtils.copyProperties(form, req.getAttribute(WebKeys.USERMANAGER_EDIT_FORM));
				setForward(req, "portlet.ext.usermanager.edit_usermanager");
				return;
			} else {
				try {
					String userId = _save(form, req, res);
					UserManagerForm userForm = (UserManagerForm)form;
					if(!UtilMethods.isSet(userForm.getUserID())){
						httpReq.getSession().setAttribute("userID", userId);
					}
					_editUserManager(userId,form, req, res, "");

					_sendToReferral(req,res,referer);
					return;
				} catch (Exception e) {
					_handleException(e, req);
				}
			}
		} else

			// Delete usermanager
			if (com.liferay.portal.util.Constants.DELETE.equals(cmd)) {
				try {
					_delete(form, req, res);
				} catch (Exception e) {
					_handleException(e, req);
				}
				_sendToReferral(req,res,referer);
				return;

			} else

//				Save / Update registeruser
				if ("save_register_user".equals(cmd)) {
					UserManagerForm userForm = (UserManagerForm) form;
					ActionErrors ae = new ActionErrors ();

					Logger.debug(this, "Saving UserInfo");
					
					if(!_isNewUser(userForm.getUserID())){
						ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.userExists",""));
						req.setAttribute(Globals.ERROR_KEY,ae);
						setForward(req, "portlet.ext.usermanager.register_user");
						HibernateUtil.commitTransaction();
						return;
					}

					///Validate Form
					if (!Validator.validate(req, form, mapping)) {
						Logger.debug(this, "Form Validation Failed");
						req.setAttribute(WebKeys.USERMANAGER_EDIT_FORM, form);
						
						setForward(req, "portlet.ext.usermanager.register_user");
						return;
					} else {
						try {
							userForm = (UserManagerForm) form;
							ae = new ActionErrors ();
							if(!UtilMethods.isSet(userForm.getUserID()))
							{
								if(!validateUniqueEmail(userForm.getEmailAddress()))
								{
									ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.forgotPasswordClickHere","javascript:forgotPassword();"));
								}
							}

							String userId = null;

							if ((ae != null) && (ae.size() > 0)) {
								req.setAttribute(Globals.ERROR_KEY,ae);
							} else {

								userId = _save(form, req, res);

								User user = retrieveMember(userId, userForm);
								Address address = retrieveAddress(user.getUserId());
								UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);

								UserManagerListSearchForm searchForm = new UserManagerListSearchForm();
								BeanUtils.copyProperties(searchForm, user);
								BeanUtils.copyProperties(searchForm, address);
								BeanUtils.copyProperties(searchForm, userProxy);

								if(UtilMethods.isSet(httpReq.getSession().getAttribute(WebKeys.WEBEVENTS_REG_USER))){
									httpReq.getSession().setAttribute(WebKeys.WEBEVENTS_REG_USERID,userId);
								}

								SessionMessages.add(req, "message", "message.usermanager.saved");
								HttpSession session = httpReq.getSession();
								session.setAttribute(WebKeys.USERMANAGERLISTFORM,searchForm);

							}

							if (UtilMethods.isSet(referer) && UtilMethods.isSet(userId)) {
								referer += "&userId=" + userId;
								if (!UtilMethods.isSet(cmd)) {
									referer += "&cmd=search";
								}
								Logger.debug(this, "After registering user going to:" + referer);
								_sendToReferral(req,res,referer);
								return;
							}


							if (!UtilMethods.isSet(userId)) {
								setForward(req, "portlet.ext.usermanager.register_user");
							}
							else {
								
								java.util.Map<String, String[]> params = new java.util.HashMap<String, String[]>();
								params.put("struts_action", new String[] { "/ext/usermanager/view_usermanagerlist" });
								referer = com.dotmarketing.util.PortletURLUtil.getRenderURL(httpReq,
										WindowState.MAXIMIZED.toString(), params);

								if(UtilMethods.isSet(httpReq.getSession().getAttribute(WebKeys.WEBEVENTS_REG_USER))){
									referer = URLDecoder.decode((String) httpReq.getSession().getAttribute(WebKeys.WEBEVENTS_REG_USER),"UTF-8");
								}
								_sendToReferral(req,res,referer);
							}
							HibernateUtil.commitTransaction();
							return;

						} catch (Exception e) {
							_handleException(e, req);

							setForward(req, "portlet.ext.usermanager.register_user");
							return;
						}
					}
				} else if (cmd != null && cmd.equals("updateUserProxy"))
				{
					try {
						_updateUserProxy(form, req, res);
						_sendToReferral(req,res,referer);
					} catch (Exception e) {
						_handleException(e, req);

						setForward(req, "portlet.ext.usermanager.register_user");
						return;
					}
				} else if(UtilMethods.isSet(cmd) && cmd.equals("forgotPassword"))
				{
					_forgotPassword(form, req, res);
					setForward(req, "portlet.ext.usermanager.register_user");
					return;
				}

		if ("load_register_user".equals(cmd)) {
//			Register User Case
			setForward(req, "portlet.ext.usermanager.register_user");
		} else {
//			User Manager Case
			req.setAttribute(WebKeys.USERMANAGER_EDIT_FORM, form);
			BeanUtils.copyProperties(form, req.getAttribute(WebKeys.USERMANAGER_EDIT_FORM));
			setForward(req, "portlet.ext.usermanager.edit_usermanager");
		}

		HibernateUtil.commitTransaction();
	}

	/*Private Methods*/

	//	get user profile
	private void _editUserManager(String userId,ActionForm form, ActionRequest req, ActionResponse res, String cmd)
	throws Exception {

		UserManagerForm userForm = (UserManagerForm) form;

		String companyId = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getCompanyId();
		if(!UtilMethods.isSet(userId))
			userId = req.getParameter("userID");
		User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);

		if (user.getUserId() != null && !com.liferay.portal.util.Constants.SAVE.equals(cmd)) {

			// Retriving info from db
			UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);

			Address address = retrieveAddress(user.getUserId());

			// Copy the attributes
			BeanUtils.copyProperties(form, user);

			if(!UtilMethods.isSet(userProxy.getPrefix())){
				userProxy.setPrefix("other");
			}

			BeanUtils.copyProperties(form, userProxy);

			if(!UtilMethods.isSet(address.getDescription())){
				address.setDescription("other");
			}

			BeanUtils.copyProperties(form, address);

			// Extra user info
			userForm.setUserID(user.getUserId());


			List<Category> oldcategories = InodeFactory.getChildrenClass(userProxy,Category.class);

//			Add User Categories
			List<Category> catList = InodeFactory.getChildrenClass(userProxy,Category.class);
			if(catList.size() > 0){
				String[] categories = new String[catList.size()];
				for(int i = 0 ; i < catList.size() ;++i){
					Category cat = catList.get(i);
					categories[i] = String.valueOf(cat.getInode());
					userProxy.addChild(cat);
				}
			}
			BeanUtils.copyProperties(form, address);

			BeanUtils.copyProperties(form, address);

		}

	}

	private Address retrieveAddress(String userID) throws Exception {
		String companyId = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getCompanyId();

		Address address = null;
		if (UtilMethods.isSet(userID)) {
			List addresses = PublicAddressFactory.getAddressesByUserId(userID);
			if (addresses.size() == 0) {
				address = PublicAddressFactory.getInstance();
				address.setCompanyId(companyId);
				address.setUserId(userID);
			} else {
				address = (Address) addresses.get(0);
			}
		} else {
			address = PublicAddressFactory.getInstance();
			address.setCompanyId(companyId);
			address.setCreateDate(new java.util.Date());
			address.setNew(true);
			address.setUserId(userID);
		}
		return address;
	}

	//Deleting User manager
	private void _delete(ActionForm form, ActionRequest req, ActionResponse res)
	throws Exception {

		UserManagerForm userForm = (UserManagerForm) form;
		String userId = userForm.getUserID();

		//Saving Personal Information
		String companyId = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getCompanyId();
		User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
		UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);

		//delete user tags
		List<TagInode> userTagsList = TagFactory.getTagInodeByInode(String.valueOf(userProxy.getInode()));
		for(TagInode tag : userTagsList){
		    Tag retrievedTag = TagFactory.getTagByTagId(tag.getTagId());
			TagFactory.deleteTagInode(tag);
			TagFactory.deleteTag(retrievedTag.getTagId());
		}
		
		//deletes user proxy
		InodeFactory.deleteInode(userProxy); 
		//deletes liferay user
		APILocator.getUserAPI().delete(APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false),com.dotmarketing.business.APILocator.getUserAPI().getSystemUser(), false);
	}

	//Saving User manager
	private String _save(ActionForm form, ActionRequest req, ActionResponse res)
	throws Exception {

		User adminUser = _getUser(req);
		
		UserManagerForm userForm = (UserManagerForm) form;
		String userId = userForm.getUserID();

		//Saving Personal Information
		String companyId = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getCompanyId();

		User user = null;

		try {
			user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e) {
		}

		if (user == null)
			user = retrieveMember(userId, userForm);

		UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);

		user.setFirstName(userForm.getFirstName());

		if (userForm.getMiddleName() != null)
			user.setMiddleName(userForm.getMiddleName());

		user.setLastName(userForm.getLastName());

		if (userForm.getDateOfBirthDate() != null)
			user.setBirthday(userForm.getDateOfBirthDate());

		if (userForm.getNickName() != null)
			user.setNickName(userForm.getNickName());

		if (userForm.getSex() != null)
			user.setMale(userForm.getSex().equalsIgnoreCase("M") ? true : false);

		if(UtilMethods.isSet(userForm.getChallengeQuestionId()) && UtilMethods.isInt(userForm.getChallengeQuestionId())){
			userProxy.setChallengeQuestionId(userForm.getChallengeQuestionId());
		}
		if(UtilMethods.isSet(userForm.getChallengeQuestionAnswer())){
			userProxy.setChallengeQuestionAnswer(userForm.getChallengeQuestionAnswer());
		}
		if (!userForm.getPrefix().equals("other"))
			userProxy.setPrefix(userForm.getPrefix());
		else
			userProxy.setPrefix(userForm.getOtherPrefix());

		userProxy.setSuffix(userForm.getSuffix());
		userProxy.setTitle(userForm.getTitle());
		userProxy.setCompany(companyId);

		if (userForm.getSchool() != null)
			userProxy.setSchool(userForm.getSchool());

		if (0 < userForm.getGraduation_year())
			userProxy.setGraduation_year(userForm.getGraduation_year());

		// User Name and password
		if (!UtilMethods.isSet(user.getEmailAddress()) || !user.getEmailAddress().equals(userForm.getEmailAddress()))
			user.setEmailAddress(userForm.getEmailAddress().trim().toLowerCase());

		if ((userForm.getNewPassword() != null) && (!userForm.getNewPassword().equals(""))) {
			user.setPassword(PublicEncryptionFactory.digestString(userForm.getNewPassword()));
			user.setPasswordEncrypted(true);
		}

		if (user.isNew() || userForm.getPassChanged().equals("true")) {
			user.setPassword(PublicEncryptionFactory.digestString(userForm.getPassword()));
			user.setPasswordEncrypted(true);
		}

		APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
        
		_setRolePermission(userProxy, req);
		
		if(!InodeUtils.isSet(userProxy.getInode()) ){
			userProxy.setUserId(user.getUserId());
			userProxy.setChallengeQuestionId("0");
			com.dotmarketing.business.APILocator.getUserProxyAPI().saveUserProxy(userProxy,APILocator.getUserAPI().getSystemUser(), false);
		} else {
			com.dotmarketing.business.APILocator.getUserProxyAPI().saveUserProxy(userProxy,APILocator.getUserAPI().getSystemUser(), false);

		}

		//Saving User Address Information
		Address address = retrieveAddress(user.getUserId());

		address.setUserName(user.getFullName());
		address.setClassName(user.getClass().getName());
		address.setClassPK(user.getUserId());
		address.setDescription(userForm.getDescription());
		address.setStreet1(userForm.getStreet1());
		address.setStreet2(userForm.getStreet2());
		address.setCity(userForm.getCity());
		address.setState(userForm.getState());
		address.setZip(userForm.getZip());
		address.setPhone(userForm.getPhone());
		address.setFax(userForm.getFax());

		if (userForm.getCountry() != null)
			address.setCountry(userForm.getCountry());

		address.setCell(userForm.getCell());
		PublicAddressFactory.save(address);

		//		Add User Categories
		if(userForm.getCategory() != null){
			String[] categories = userForm.getCategory();
			for(int i = 0 ; i < categories.length ;++i){
				Category cat = categoryAPI.find(categories[i], adminUser, false);
				categoryAPI.addChild(userProxy, cat, adminUser, false);
			}
		}

		HibernateUtil.flush();

		SessionMessages.add(req, "message", "message.usermanager.saved");

		return user.getUserId();
	}

	private void _updateUserProxy(ActionForm form, ActionRequest req, ActionResponse res){

		UserManagerForm userForm = new UserManagerForm(); 
		try {
			BeanUtils.copyProperties(userForm,form);

			UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userForm.getUserID(),APILocator.getUserAPI().getSystemUser(), false);

			if (!userForm.getPrefix().equals("other"))
				userProxy.setPrefix(userForm.getPrefix());
			else
				userProxy.setPrefix(userForm.getOtherPrefix());

			userProxy.setSuffix(userForm.getSuffix());
			userProxy.setTitle(userForm.getTitle());
			userProxy.setSchool(userForm.getSchool());
			userProxy.setGraduation_year(userForm.getGraduation_year());

			HibernateUtil.saveOrUpdate(userProxy);

		} catch (IllegalAccessException e) {
			Logger.error(this,e.getMessage(),e);
		} catch (InvocationTargetException e) {
			Logger.error(this,e.getMessage(),e);
		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

	private boolean validateUniqueEmail(String emailAddress)
	{
		String companyId = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getCompanyId();
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

	private User retrieveMember(String userId, UserManagerForm form) throws Exception
	{
		String companyId = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getCompanyId();
		User member = new User();

		if (UtilMethods.isSet(userId))
		{
			try{
				member = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
				return member;
			}catch(com.dotmarketing.business.NoSuchUserException nsu){
				member = APILocator.getUserAPI().createUser(null, null);
				member.setUserId(userId.toLowerCase());
			}
		}
		else
		{
			member = APILocator.getUserAPI().createUser(null, null);
			
		}
		member.setActive(true);
		member.setCreateDate(new Date());
		return member;
	}

	private void _forgotPassword(ActionForm form, ActionRequest request, ActionResponse res) throws Exception
	{
		UserManagerForm userForm = (UserManagerForm) form;
		//if we have some errors
		ActionErrors aes = new ActionErrors();

		User user = APILocator.getUserAPI().loadByUserByEmail(userForm.getEmailAddress(), APILocator.getUserAPI().getSystemUser(), false);
		if(user.isNew())
		{
			aes = new ActionErrors();
			aes.add(Globals.ERROR_KEY, new ActionMessage("error.forgotPasswordUserNotFound"));
			request.setAttribute(Globals.ERROR_KEY,aes);
			return;
		}
		String pass = PublicEncryptionFactory.getRandomPassword();
		user.setPassword(PublicEncryptionFactory.digestString(pass));
		APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
		Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
		EmailFactory.sendForgotPassword(user, pass, host.getIdentifier());

		aes = new ActionErrors();
		aes.add(Globals.ERROR_KEY, new ActionMessage("error.forgotPasswordMailSend"));
		request.setAttribute(Globals.ERROR_KEY,aes);
		return;
	}

	private void _setRolePermission(UserProxy userProxy, ActionRequest req)
	throws Exception {
		
		User user = _getUser(req);
		
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		// read permission
		String[] readPermissions = req.getParameterValues("readRole");
		// write permission
		String[] writePermissions = req.getParameterValues("writeRole");

		String userProxyInode = userProxy.getInode();

		if (InodeUtils.isSet(userProxyInode)) {

			//adding roles to user
			Permission permission = null;
			if (readPermissions != null) {
				for (int n = 0; n < readPermissions.length; n++) {
					permission = new Permission(userProxyInode, readPermissions[n],	PERMISSION_READ);
					perAPI.save(permission, userProxy, user, false);
				}
			}

			if (writePermissions != null) {
				for (int n = 0; n < writePermissions.length; n++) {
					permission = new Permission(userProxyInode, writePermissions[n], PERMISSION_WRITE);
					perAPI.save(permission, userProxy, user, false);
				}
			}
		}
	}
	
	/**
	 * Returns true if the user is a new user since no user with the given user id cannot be found
	 * @param userId: The user id for whom the user search is made 
	 */
	private boolean _isNewUser(String userId){
		User user = null;
		try
		{
			user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
		}
		catch(NoSuchUserException ex)
		{
			//If the user is not found, then is a new user
		}
		catch(Exception e){
			Logger.error(this, "Exception in user search");
		}
		
		return (user == null );
	}
}