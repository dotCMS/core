package com.dotcms.security.apps;

import com.dotcms.auth.providers.jwt.factories.SigningKeyFactory;
import com.dotmarketing.business.APILocator;
import java.security.Key;

public class AppsKeyDefaultProvider implements SigningKeyFactory {

    private AppsKeyDefaultProvider() {
    }

    @Override
    public Key getKey() {
        return APILocator.getCompanyAPI().getDefaultCompany().getKeyObj();
    }

    enum INSTANCE {
        SINGLETON;
        final SigningKeyFactory provider = new AppsKeyDefaultProvider();

        public static SigningKeyFactory get() {
            return SINGLETON.provider;
        }

   }

}
