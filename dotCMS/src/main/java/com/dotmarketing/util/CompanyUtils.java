package com.dotmarketing.util;

import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.liferay.portal.model.Company;

public class CompanyUtils {

	public static Company getDefaultCompany()
	{
		Company company = PublicCompanyFactory.getDefaultCompany();
		return company;
	}
}