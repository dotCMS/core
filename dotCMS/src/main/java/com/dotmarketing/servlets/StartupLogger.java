package com.dotmarketing.servlets;

import java.util.TimeZone;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.repackage.net.sf.hibernate.dialect.Dialect;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Company;

public class StartupLogger {

    public void log(){
        
        LicenseManager license = LicenseManager.getInstance();
        
        
        
        Company company = PublicCompanyFactory.getDefaultCompany();
        TimeZone companyTimeZone = company.getTimeZone();
        TimeZone.setDefault(companyTimeZone);
        Logger.info(this, "InitServlet: Setting Default Timezone: " + companyTimeZone.getDisplayName());

        String dbType = DbConnectionFactory.getDBType();
        Dialect dailect = HibernateUtil.getDialect();

        String expires = (license.isPerpetual()) ?  "never" : UtilMethods.dateToLongPrettyHTMLDate(license.getValidUntil());
        
        String companyId = PublicCompanyFactory.getDefaultCompanyId();
        Logger.info(this, "");
        Logger.info(this, "   Initializing dotCMS");
        Logger.info(this, "   Using database: " + dbType);
        Logger.info(this, "   Using dialect : " + dailect.getClass().getCanonicalName());
        Logger.info(this, "   Company Name  : " + companyId);
        Logger.info(this, "");
        Logger.info(this, "   License       : " + license.getLevelName(license.getLevel()));
        Logger.info(this, "   Licensed to   : " + license.getClientName());
        Logger.info(this, "   Server id     : " + license.getDisplayServerId());
        Logger.info(this, "   License id    : " + license.getDisplaySerial());
        Logger.info(this, "   Expires       : " + expires);
        Logger.info(this, "");

        
        
    }
}
