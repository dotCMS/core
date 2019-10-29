/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portlet.admin.ejb;

import java.util.List;
import java.util.Map;

import com.dotmarketing.util.UUIDUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.PrincipalException;
import com.liferay.portal.ejb.CompanyManagerUtil;
import com.liferay.portal.ejb.PrincipalBean;
import com.liferay.portal.model.Company;
import com.liferay.portal.util.ContentUtil;
import com.liferay.portlet.admin.model.AdminConfig;
import com.liferay.portlet.admin.model.EmailConfig;
import com.liferay.portlet.admin.model.JournalConfig;
import com.liferay.portlet.admin.model.ShoppingConfig;
import com.liferay.portlet.admin.model.UserConfig;
import com.liferay.util.CollectionFactory;
import com.liferay.util.GetterUtil;
import com.liferay.util.SimpleCachePool;

/**
 * <a href="AdminConfigManagerImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.4 $
 *
 */
public class AdminConfigManagerImpl
	extends PrincipalBean implements AdminConfigManager {

	// Business methods

	public List getAdminConfig(String companyId, String type)
		throws SystemException {

		Map configsPool = _getConfigsPool();

		List configs = (List)configsPool.get(companyId + "." + type);

		if (configs == null) {
			configs = AdminConfigUtil.findByC_T(companyId, type, 0, 1);

			configsPool.put(companyId + "." + type, configs);
		}

		return configs;
	}

	public JournalConfig getJournalConfig(String companyId, String portletId)
		throws PortalException, SystemException {

		String type = JournalConfig.JOURNAL_CONFIG + "." + portletId;
		String name = JournalConfig.JOURNAL_CONFIG;

		List configs = getAdminConfig(companyId, type);

		AdminConfig config = null;

		JournalConfig journalConfig = null;

		if (configs.size() == 0) {
			String configId = UUIDUtil.uuid();

			Company company = CompanyManagerUtil.getCompany(companyId);

			journalConfig = new JournalConfig(
				JournalConfig.DEFAULT_ALLOW_SIMPLE_ARTICLES,
				_getJournalApprovalRequestedEmail(),
				_getJournalApprovalGrantedEmail(),
				_getJournalApprovalDeniedEmail());

			config = AdminConfigUtil.create(configId);

			config.setCompanyId(companyId);
			config.setType(type);
			config.setName(name);
			config.setConfigObj(journalConfig);

			AdminConfigUtil.update(config);

			configs.add(config);
		}
		else {
			config = (AdminConfig)configs.iterator().next();

			journalConfig = (JournalConfig)config.getConfigObj();
		}

		return journalConfig;
	}

	public ShoppingConfig getShoppingConfig(String companyId)
		throws PortalException, SystemException {

		String type = ShoppingConfig.SHOPPING_CONFIG;
		String name = ShoppingConfig.SHOPPING_CONFIG;

		List configs = getAdminConfig(companyId, type);

		AdminConfig config = null;

		ShoppingConfig shoppingConfig = null;

		if (configs.size() == 0) {
			String configId = UUIDUtil.uuid();

			Company company = CompanyManagerUtil.getCompany(companyId);

			shoppingConfig = new ShoppingConfig(
				company.getEmailAddress(), ShoppingConfig.CC_TYPES,
				ShoppingConfig.DEFAULT_CURRENCY_ID,
				GetterUtil.get(
					company.getState(), ShoppingConfig.DEFAULT_TAX_STATE),
				ShoppingConfig.DEFAULT_TAX_RATE,
				ShoppingConfig.DEFAULT_SHIPPING_FORMULA,
				ShoppingConfig.DEFAULT_SHIPPING,
				ShoppingConfig.DEFAULT_ALTERNATIVE_SHIPPING,
				ShoppingConfig.DEFAULT_MIN_ORDER,
				ShoppingConfig.DEFAULT_SHOW_SPECIAL_ITEMS,
				_getShoppingConfigOrderEmail(),
				_getShoppingConfigShippingEmail());

			config = AdminConfigUtil.create(configId);

			config.setCompanyId(companyId);
			config.setType(type);
			config.setName(name);
			config.setConfigObj(shoppingConfig);

			AdminConfigUtil.update(config);

			configs.add(config);
		}
		else {
			config = (AdminConfig)configs.iterator().next();

			shoppingConfig = (ShoppingConfig)config.getConfigObj();
		}

		// Make sure new fields are not null

		if (shoppingConfig.getCcTypes() == null) {
			shoppingConfig.setCcTypes(new String[0]);

			config.setConfigObj(shoppingConfig);

			AdminConfigUtil.update(config);
		}

		if (shoppingConfig.getOrderEmail() == null) {
			shoppingConfig.setOrderEmail(_getShoppingConfigOrderEmail());

			config.setConfigObj(shoppingConfig);

			AdminConfigUtil.update(config);
		}

		if (shoppingConfig.getShippingEmail() == null) {
			shoppingConfig.setShippingEmail(_getShoppingConfigShippingEmail());

			config.setConfigObj(shoppingConfig);

			AdminConfigUtil.update(config);
		}

		return shoppingConfig;
	}

	public UserConfig getUserConfig(String companyId)
		throws PortalException, SystemException {

		String type = UserConfig.USER_CONFIG;
		String name = UserConfig.USER_CONFIG;

		List configs = getAdminConfig(companyId, type);

		AdminConfig config = null;

		UserConfig userConfig = null;

		if (configs.size() == 0) {
			String configId = UUIDUtil.uuid();

			Company company = CompanyManagerUtil.getCompany(companyId);

			userConfig = new UserConfig(
				UserConfig.DEFAULT_GROUPS, UserConfig.DEFAULT_ROLES,
				UserConfig.DEFAULT_RESERVED_USER_IDS,
				UserConfig.DEFAULT_RESERVED_USER_EMAIL_ADDRESSES,
				UserConfig.DEFAULT_MAIL_HOST_NAMES,
				_getUserConfigRegistrationEmail());

			config = AdminConfigUtil.create(configId);

			config.setCompanyId(companyId);
			config.setType(type);
			config.setName(name);
			config.setConfigObj(userConfig);

			AdminConfigUtil.update(config);

			configs.add(config);
		}
		else {
			config = (AdminConfig)configs.iterator().next();

			userConfig = (UserConfig)config.getConfigObj();
		}

		// Make sure new fields are not null

		if (userConfig.getRegistrationEmail() == null) {
			userConfig.setRegistrationEmail(_getUserConfigRegistrationEmail());

			config.setConfigObj(userConfig);

			AdminConfigUtil.update(config);
		}

		return userConfig;
	}

	public void updateJournalConfig(
			JournalConfig journalConfig, String portletId)
		throws PortalException, SystemException {

		String companyId = getUser().getCompanyId();

		if (!hasAdministrator(companyId)) {
			throw new PrincipalException();
		}

		String type = JournalConfig.JOURNAL_CONFIG + "." + portletId;

		getJournalConfig(companyId, portletId);

		List configs = getAdminConfig(companyId, type);

		AdminConfig config = (AdminConfig)configs.iterator().next();

		config.setConfigObj(journalConfig);

		AdminConfigUtil.update(config);
	}

	public void updateShoppingConfig(ShoppingConfig shoppingConfig)
		throws PortalException, SystemException {

		String companyId = getUser().getCompanyId();

		if (!hasAdministrator(companyId)) {
			throw new PrincipalException();
		}

		String type = ShoppingConfig.SHOPPING_CONFIG;

		getShoppingConfig(companyId);

		List configs = getAdminConfig(companyId, type);

		AdminConfig config = (AdminConfig)configs.iterator().next();

		config.setConfigObj(shoppingConfig);

		AdminConfigUtil.update(config);
	}

	public void updateUserConfig(UserConfig userConfig)
		throws PortalException, SystemException {

		String companyId = getUser().getCompanyId();

		if (!hasAdministrator(companyId)) {
			throw new PrincipalException();
		}

		String type = UserConfig.USER_CONFIG;

		getUserConfig(companyId);

		List configs = getAdminConfig(companyId, type);

		AdminConfig config = (AdminConfig)configs.iterator().next();

		config.setConfigObj(userConfig);

		AdminConfigUtil.update(config);
	}

	// Private methods

	private Map _getConfigsPool() {
		String scpId = AdminConfigManagerImpl.class.getName();

		Map configsPool = (Map)SimpleCachePool.get(scpId);

		if (configsPool == null) {
			configsPool = CollectionFactory.getSyncHashMap();

			SimpleCachePool.put(scpId, configsPool);
		}

		return configsPool;
	}

	private EmailConfig _getJournalApprovalDeniedEmail() {
		String approvalDeniedEmailSubject =
			"[$PORTLET_NAME$] Article Approval Denied: " +
				"[$ARTICLE_ID$]/[$ARTICLE_VERSION$]";

		String approvalDeniedEmailBody = ContentUtil.get(
			"messages/en_US/journal_article_approval_denied.tmpl");

		EmailConfig approvalDeniedEmail = new EmailConfig(
			approvalDeniedEmailSubject, approvalDeniedEmailBody);

		return approvalDeniedEmail;
	}

	private EmailConfig _getJournalApprovalGrantedEmail() {
		String approvalGrantedEmailSubject =
			"[$PORTLET_NAME$] Article Approval Granted: " +
				"[$ARTICLE_ID$]/[$ARTICLE_VERSION$]";

		String approvalGrantedEmailBody = ContentUtil.get(
			"messages/en_US/journal_article_approval_granted.tmpl");

		EmailConfig approvalGrantedEmail = new EmailConfig(
			approvalGrantedEmailSubject, approvalGrantedEmailBody);

		return approvalGrantedEmail;
	}

	private EmailConfig _getJournalApprovalRequestedEmail() {
		String approvalRequestedEmailSubject =
			"[$PORTLET_NAME$] Article Approval Requested: " +
				"[$ARTICLE_ID$]/[$ARTICLE_VERSION$]";

		String approvalRequestedEmailBody = ContentUtil.get(
			"messages/en_US/journal_article_approval_requested.tmpl");

		EmailConfig approvalRequestedEmail = new EmailConfig(
			approvalRequestedEmailSubject, approvalRequestedEmailBody);

		return approvalRequestedEmail;
	}

	private EmailConfig _getShoppingConfigOrderEmail() {
		String orderEmailSubject = "[$COMPANY_NAME$] Order #[$ORDER_NUMBER$]";

		String orderEmailBody = ContentUtil.get(
			"messages/en_US/shopping_config_order_email_body.tmpl");

		EmailConfig orderEmail = new EmailConfig(
			orderEmailSubject, orderEmailBody);

		return orderEmail;
	}

	private EmailConfig _getShoppingConfigShippingEmail() {
		String shippingEmailSubject =
			"[$COMPANY_NAME$] Order #[$ORDER_NUMBER$] Shipped";

		String shippingEmailBody = ContentUtil.get(
			"messages/en_US/shopping_config_shipping_email_body.tmpl");

		EmailConfig shippingEmail = new EmailConfig(
			shippingEmailSubject, shippingEmailBody);

		return shippingEmail;
	}

	private EmailConfig _getUserConfigRegistrationEmail() {
		String registrationEmailSubject = "[$COMPANY_NAME$] Portal Account";

		String registrationEmailBody = "does not exist";

		EmailConfig registrationEmail = new EmailConfig(
			registrationEmailSubject, registrationEmailBody);

		return registrationEmail;
	}

}