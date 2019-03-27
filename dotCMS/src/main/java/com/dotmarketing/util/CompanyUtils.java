package com.dotmarketing.util;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.liferay.portal.model.Company;

public class CompanyUtils {

	@CloseDBIfOpened
	public static Company getDefaultCompany()
	{
		Company company = PublicCompanyFactory.getDefaultCompany();
		return company;
	}
}