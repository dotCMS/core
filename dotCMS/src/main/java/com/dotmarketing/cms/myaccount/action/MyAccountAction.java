package com.dotmarketing.cms.myaccount.action;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotcms.repackage.org.apache.struts.action.ActionErrors;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.repackage.org.apache.struts.action.ActionMessage;
import com.dotcms.repackage.org.apache.struts.action.ActionMessages;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.SecureAction;
import com.dotmarketing.cms.factories.PublicAddressFactory;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.myaccount.struts.MyAccountForm;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.User;

@Deprecated
public class MyAccountAction extends SecureAction {

	public ActionForward unspecified(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (request.getSession().getAttribute(WebKeys.CMS_USER) == null) {
			ActionForward af = new ActionForward(
					"/dotCMS/login?referrer=/dotCMS/myAccount");
			af.setRedirect(true);
			return af;
		}

		// HttpSession session = request.getSession();
		MyAccountForm form = (MyAccountForm) lf;

		// Getting the user from the session
		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
		String userId = user.getUserId();

		loadUserInfoInRequest(form, userId, request);

		return mapping.findForward("myAccountPage");
	}

	public ActionForward saveUserInfo(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (request.getSession().getAttribute(WebKeys.CMS_USER) == null) {
			return new ActionForward("/dotCMS/login");
		}

		MyAccountForm form = (MyAccountForm) lf;
		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
		String userId = user.getUserId();

		if (!Validator.validate(request, lf, mapping))
			return mapping.findForward("myAccountPage");

		// Saving Personal Information

		HibernateUtil.startTransaction();

		// User user = PublicUserFactory.getUserByEmail(userEmail);
		
		UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);

		user.setFirstName(form.getFirstName());
		user.setLastName(form.getLastName());

		userProxy.setOrganization(form.getOrganization());
		userProxy.setWebsite(form.getWebsite());
		userProxy.setMailSubscription(form.isMailSubscription());
		userProxy.setPrefix(form.getPrefix());
		userProxy.setSuffix(form.getSuffix());
		userProxy.setTitle(form.getTitle());

		// User Name and password
		boolean reauthenticate = false;
		if (!form.getNewPassword().equals("")
				|| !user.getEmailAddress().equals(form.getEmailAddress())) {

            boolean passwordMatch = false;

            if (PasswordFactoryProxy.isUnsecurePasswordHash(user.getPassword())) {
                passwordMatch = user.getPassword().equals(form.getPassword())
                        || user.getPassword().equals(PublicEncryptionFactory.digestString(form.getPassword()));
            } else {
                // Has new hash algorithm
                if (PasswordFactoryProxy.authPassword(form.getPassword(), user.getPassword()).equals(
                        PasswordFactoryProxy.AuthenticationStatus.NOT_AUTHENTICATED)) {
                    passwordMatch = false;
                } else {
                    passwordMatch = true;
                }
            }

			if (passwordMatch == false) {
				ActionErrors errors = new ActionErrors();
				errors.add("password", new ActionMessage(
						"current.usermanager.password.incorrect"));
				saveMessages(request, errors);
				return mapping.findForward("myAccountPage");
			}

            // Use new password hash method
            user.setPassword(PasswordFactoryProxy.generateHash(form.getNewPassword()));

			user.setEmailAddress(form.getEmailAddress().trim().toLowerCase());
			reauthenticate = true;
		}

		APILocator.getUserAPI().save(user,APILocator.getUserAPI().getSystemUser(),false);
		HibernateUtil.saveOrUpdate(userProxy);

		List<TagInode> tags = APILocator.getTagAPI().getTagInodesByInode(userProxy.getInode());
		for (TagInode tag: tags) {
			Tag tempTag = APILocator.getTagAPI().getTagByTagId(tag.getTagId());
			APILocator.getTagAPI().deleteTagInode(tempTag, userProxy.getInode(), null);
		}
		if(tags.size() > 0){
			APILocator.getTagAPI().addUserTag(form.getTags(), userProxy.getUserId(), userProxy.getInode());
		}
		
		CategoryAPI categoryAPI = APILocator.getCategoryAPI();
		List<Category> myUserCategories = categoryAPI.getChildren(userProxy, APILocator.getUserAPI().getSystemUser(), false);
		for (Object object: myUserCategories) {
			if ((object instanceof Category) && categoryAPI.canUseCategory((Category) object, APILocator.getUserAPI().getSystemUser(), false)) {
				categoryAPI.removeChild(userProxy, (Category) object, APILocator.getUserAPI().getSystemUser(), false);
			}
		}

		if (UtilMethods.isSet(form.getCategory())) {
			Category category;
			for (String categoryId: form.getCategory()) {
				category = categoryAPI.find(categoryId, APILocator.getUserAPI().getSystemUser(), false);
				if(InodeUtils.isSet(category.getInode())) {
					categoryAPI.addChild(userProxy, category, APILocator.getUserAPI().getSystemUser(), false);
				}
			}
		}

		HibernateUtil.closeAndCommitTransaction();

		loadUserInfoInRequest(form, user.getUserId(), request);

		ActionErrors ae = new ActionErrors();
		ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(
				"message.createaccount.success"));
		saveMessages(request, ae);
		ActionForward forward = null;
		if (reauthenticate) {
			forward = mapping.findForward("reauthenticate");
		} else {
			forward = mapping.findForward("myAccountPage");
		}
		return forward;
	}

	private void loadUserInfoInRequest(MyAccountForm form, String userId,
			HttpServletRequest request) throws Exception {

		User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);

		// Retriving info from db
		UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);
		Address address = null;
		if (UtilMethods.isSet(form.getAddressID())) {
			address = PublicAddressFactory.getAddressById(form.getAddressID());
			if(address != null && address.getUserId() != null && !address.getUserId().equals(userId)){
				address = null;
			}
		}
		int addrId = 0;
		try{
			addrId = Integer.parseInt(form.getAddressID());
		}
		catch(Exception e){}

		if (addrId > 0) {
			address = PublicAddressFactory.getAddressById(form.getAddressID());
			if(address != null && address.getUserId() != null && !address.getUserId().equals(userId)){
				address = null;
			}
		}
		if (address == null) {
			address = PublicAddressFactory.getInstance();
			address.setUserId(userId);
			address.setCompanyId(PublicCompanyFactory.getDefaultCompanyId());
		}
		if (!InodeUtils.isSet(userProxy.getInode())) {
			userProxy.setUserId(user.getUserId());
			HibernateUtil.saveOrUpdate(userProxy);
		}

		// Copy the attributes
		BeanUtils.copyProperties(form, user);
		BeanUtils.copyProperties(form, address);
		BeanUtils.copyProperties(form, userProxy);

		// Extra user info
		form.setEmailAddress(user.getEmailAddress());

		List<TagInode> tags = APILocator.getTagAPI().getTagInodesByInode(userProxy.getInode());
		StringBuilder tagsString = new StringBuilder(128);
		tagsString.ensureCapacity(32);
		for (TagInode tag: tags) {
			Tag retrievedTag = APILocator.getTagAPI().getTagByTagId(tag.getTagId());
			if (0 < tagsString.length())
				tagsString.append(", " + retrievedTag.getTagName());
			else
				tagsString.append(retrievedTag.getTagName());
		}
		form.setTags(tagsString.toString());

		CategoryAPI categoryAPI = APILocator.getCategoryAPI();
		List<String> categories = new ArrayList<String>();
		List<Category> myUserCategories = categoryAPI.getChildren(userProxy, APILocator.getUserAPI().getSystemUser(), false);
		for (Object object: myUserCategories) {
			if (object instanceof Category) {
				categories.add(((Category) object).getCategoryId());
			}
		}

		form.setCategory(categories.toArray(new String[0]));
	}

	public static boolean subscribeDotCMSMailingList(User user) {
		String to = "dotcms-subscribe@yahoogroups.com";
		String from = user.getEmailAddress();
		String subject = "Subscribe to dotCMS mailing list";
		return sendEmailForMailingList(to, from, subject);
	}

	public static boolean unSubsribeDotCMSMailingList(User user) {
		String to = "dotcms-unsubscribe@yahoogroups.com";
		String from = user.getEmailAddress();
		String subject = "UnSubscribe to dotCMS mailing list";
		return sendEmailForMailingList(to, from, subject);
	}

	protected static boolean sendEmailForMailingList(String to, String from,
			String subject) {
		Mailer m = new Mailer();
		m.setToEmail(to);
		m.setFromEmail(from);
		m.setSubject(subject);
		return m.sendMessage();
	}

	public ActionForward back(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (request.getSession().getAttribute(WebKeys.CMS_USER) == null) {
			return new ActionForward("/dotCMS/login");
		}

		// HttpSession session = request.getSession();
		MyAccountForm form = (MyAccountForm) lf;

		// Getting the user from the session
		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
		String userId = user.getUserId();

		loadUserInfoInRequest(form, userId, request);

		return mapping.findForward("myAccountPage");
	}

	public ActionForward editUserCategories(ActionMapping mapping,
			ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		if (request.getSession().getAttribute(WebKeys.CMS_USER) == null) {
			return new ActionForward("/dotCMS/login");
		}

		// Getting the user from the session
		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
		String userId = user.getUserId();

		MyAccountForm form = (MyAccountForm) lf;
		loadUserInfoInRequest(form, userId, request);
		return mapping.findForward("editUserCategoriesPage");
	}

	public ActionForward editUserAddress(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (request.getSession().getAttribute(WebKeys.CMS_USER) == null) {
			return new ActionForward("/dotCMS/login");
		}

		// Getting the user from the session
		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
		String userId = user.getUserId();

		MyAccountForm form = (MyAccountForm) lf;

		loadUserInfoInRequest(form, userId, request);
		return mapping.findForward("editUserAddressPage");
	}

	public ActionForward editUserInfo(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (request.getSession().getAttribute(WebKeys.CMS_USER) == null) {
			return new ActionForward("/dotCMS/login");
		}

		// Getting the user from the session
		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
		String userId = user.getUserId();

		MyAccountForm form = (MyAccountForm) lf;
		loadUserInfoInRequest(form, userId, request);
		return mapping.findForward("editUserInfoPage");
	}

	public ActionForward editUserOrganization(ActionMapping mapping,
			ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		if (request.getSession().getAttribute(WebKeys.CMS_USER) == null) {
			return new ActionForward("/dotCMS/login");
		}

		// Getting the user from the session
		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
		String userId = user.getUserId();

		MyAccountForm form = (MyAccountForm) lf;
		loadUserInfoInRequest(form, userId, request);
		return mapping.findForward("editUserOrganizationPage");
	}

	public ActionForward saveUserAddress(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (request.getSession().getAttribute(WebKeys.CMS_USER) == null) {
			return new ActionForward("/dotCMS/login");
		}

		MyAccountForm form = (MyAccountForm) lf;
		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
		String userId = user.getUserId();

		if (!Validator.validate(request, lf, mapping))
			return mapping.findForward("editUserAddressPage");

		// Saving Address Information
		Address address = null;
		int addrId = 0;
		try{
			addrId = Integer.parseInt(form.getAddressID());
		}
		catch(Exception e){}

		if (addrId > 0) {
			address = PublicAddressFactory.getAddressById(form.getAddressID());
			if(address != null && address.getUserId() != null && !address.getUserId().equals(userId)){
				address = null;
			}
		}
		if (address == null) {
			address = PublicAddressFactory.getInstance();
			address.setUserId(userId);
			address.setCompanyId(PublicCompanyFactory.getDefaultCompanyId());
		}

		address.setDescription(form.getDescription());
		address.setStreet1(form.getStreet1());
		address.setStreet2(form.getStreet2());
		address.setCity(form.getCity());
		address.setCountry(form.getCountry());
		address.setState(form.getState());
		address.setZip(form.getZip());
		address.setPhone(form.getPhone());
		address.setFax(form.getFax());

		PublicAddressFactory.save(address);

		loadUserInfoInRequest(form, userId, request);

		return mapping.findForward("myAccountPage");
	}



	@SuppressWarnings("unchecked")
	public ActionForward saveUserCategories(ActionMapping mapping,
			ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		if (request.getSession().getAttribute(WebKeys.CMS_USER) == null) {
			return new ActionForward("/dotCMS/login");
		}

		MyAccountForm form = (MyAccountForm) lf;
		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
		String userId = user.getUserId();
		String companyId = Config.getStringProperty("COMPANY_ID");
		UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);

		if (!Validator.validate(request, lf, mapping))
			return mapping.findForward("editUserCategoriesPage");

		// Saving User Categories
		// Delete the old categories
		if (!InodeUtils.isSet(userProxy.getInode())) {
			List<Category> categories = InodeFactory.getParentsOfClass(
					userProxy, Category.class);
			for (int i = 0; i < categories.size(); i++) {
				categories.get(i).deleteChild(userProxy);
			}
		}

		// Save the new categories
		String[] arr = form.getCategory();
		if (arr != null) {
			for (int i = 0; i < arr.length; i++) {
				Category node = (Category) InodeFactory.getInode(arr[i],
						Category.class);
				node.addChild(userProxy);
			}
		}

		HibernateUtil.flush();

		loadUserInfoInRequest(form, userId, request);

		return mapping.findForward("myAccountPage");
	}

	public ActionForward saveUserOrganization(ActionMapping mapping,
			ActionForm lf, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		if (request.getSession().getAttribute(WebKeys.CMS_USER) == null) {
			return new ActionForward("/dotCMS/login");
		}

		MyAccountForm form = (MyAccountForm) lf;
		User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
		String userId = user.getUserId();
		String companyId = Config.getStringProperty("COMPANY_ID");
		UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);
		// ExtUser extUser = (ExtUser) InodeFactory.getChildOfClass(userProxy,
		// ExtUser.class);

		if (!Validator.validate(request, lf, mapping))
			return mapping.findForward("editUserOrganizationPage");



		// extUser.setNeedUpdateInfo(false);
		// HibernateUtil.saveOrUpdate(extUser);
		HibernateUtil.flush();

		loadUserInfoInRequest(form, userId, request);
		if (request.getSession().getAttribute(
				WebKeys.REDIRECT_AFTER_UPDATE_ACCOUNT_INFO) != null) {
			String redir = (String) request.getSession().getAttribute(
					WebKeys.REDIRECT_AFTER_UPDATE_ACCOUNT_INFO);
			request.getSession().removeAttribute(
					WebKeys.REDIRECT_AFTER_UPDATE_ACCOUNT_INFO);
			ActionForward af = new ActionForward(SecurityUtils.stripReferer(request, redir));
			af.setRedirect(true);
			return af;
		}
		return mapping.findForward("myAccountPage");
	}

}
