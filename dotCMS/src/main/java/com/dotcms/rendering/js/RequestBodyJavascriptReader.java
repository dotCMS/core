package com.dotcms.rendering.js;

import com.dotcms.rendering.util.ScriptingReaderParams;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;

import java.io.Reader;
import java.io.StringReader;

/**
 * This strategy reads the javascript code from a {@link java.util.Map} in the entry mapped to a convention-based
 * key {@link RequestBodyJavascriptReader#EMBEDDED_JAVASCRIPT_KEY_NAME}
 */

public class RequestBodyJavascriptReader implements JavascriptReader {

    static final String EMBEDDED_JAVASCRIPT_KEY_NAME = "javascript";

    private static final String SCRIPTING_USER_ROLE_KEY = "Scripting Developer";

    @Override
    public Reader getJavaScriptReader(final ScriptingReaderParams params) throws DotSecurityException, DotDataException {

        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final boolean canRenderJs = APILocator.getRoleAPI()
                .doesUserHaveRole(params.getUser(), roleAPI.loadRoleByKey(SCRIPTING_USER_ROLE_KEY));
        if(!canRenderJs) {
            Logger.warn(this, "User does not have the required role. User: " + params.getUser());
            throw new DotSecurityException("User does not have the required role");
        }
        final String javascriptString = (String)params.getBodyMap().get(EMBEDDED_JAVASCRIPT_KEY_NAME);
        return new StringReader(javascriptString);
    }
}
