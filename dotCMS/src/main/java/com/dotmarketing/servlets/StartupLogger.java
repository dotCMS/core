package com.dotmarketing.servlets;

import java.util.TimeZone;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.ChainableCacheAdministratorImpl;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
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

        String _dbType = DbConnectionFactory.getDBType();
        String _dailect = "";
        try {
            _dailect = HibernateUtil.getDialect();
        } catch (DotHibernateException e3) {
            Logger.error(InitServlet.class, e3.getMessage(), e3);
        }
        String expires = (license.isPerpetual()) ?  "never" : UtilMethods.dateToLongPrettyHTMLDate(license.getValidUntil());
        
        String _companyId = PublicCompanyFactory.getDefaultCompanyId();
        Logger.info(this, "");
        Logger.info(this, "   Initializing dotCMS");
        Logger.info(this, "   Using database: " + _dbType);
        Logger.info(this, "   Using dialect : " + _dailect);
        Logger.info(this, "   Company Name  : " + _companyId);
        Logger.info(this, "");
        Logger.info(this, "   License       : " + license.getLevelName(license.getLevel()));
        Logger.info(this, "   Licensed to   : " + license.getClientName());
        Logger.info(this, "   Server id     : " + license.getDisplayServerId());
        Logger.info(this, "   License id    : " + license.getDisplaySerial());
        Logger.info(this, "   Valid until   : " + expires);
        if(Config.getBooleanProperty("DIST_INDEXATION_ENABLED", false)){
            Logger.info(this, "   Clustering    : Enabled");
        }
        else{
            Logger.info(this, "   Clustering    : Disabled");
        }


        Logger.info(this, "");

        
        
    }
}
