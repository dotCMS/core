package com.dotcms.rest.api.v1.user;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;

/**
 * Just a helper to encapsulate AuthenticationResource functionality.
 * @author jsanca
 */
public class UserHelper implements Serializable {

    public static UserHelper INSTANCE =
            new UserHelper();


    private UserHelper() {

    }

    public void log (final String action, String message) {

        ActivityLogger.logInfo(UserResource.class, action, message);
        AdminLogger.log(UserResource.class, action, message);
    }

} // E:O:F:AuthenticationHelper.
