package com.dotcms.rendering.util;

import com.dotcms.rest.api.v1.HTTPMethod;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;

/**
 * Util method for the scripting api
 * @author jsanca
 */
public class ScriptingUtil {

    public static final String IDENTIFIER = "identifier";
    public static final String ESCAPE_ME_VALUE= " THIS_ESCAPES_LINE_BREAKS ";
    private static final String REQUIRED_ROLE_MESSAGE = "The file was not saved by a user with the required role";

    private static class SingletonHolder {
        private static final ScriptingUtil INSTANCE = new ScriptingUtil();
    }
    /**
     * Get the instance.
     * @return ScriptingUtil
     */
    public static ScriptingUtil getInstance() {

        return ScriptingUtil.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Validates the body map
     * @param bodyMap
     * @param httpMethod
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void validateBodyMap(final Map bodyMap, final HTTPMethod httpMethod) {

        // if it is an update method (not get) and the
        if (UtilMethods.isSet(bodyMap) && bodyMap.containsKey(IDENTIFIER) &&
                UtilMethods.isSet(bodyMap.get(IDENTIFIER))
                // an non-existing identifier could be just on get or post, put, patch or delete needs an existing id.
                && httpMethod != HTTPMethod.GET && httpMethod != HTTPMethod.POST) {

            final Optional<ShortyId> shortyId = APILocator.getShortyAPI().getShorty(bodyMap.get(IDENTIFIER).toString());
            final boolean isIdentifier = shortyId.isPresent() && shortyId.get().type == ShortType.IDENTIFIER;

            if (!isIdentifier) {

                throw new DoesNotExistException("The identifier: " + bodyMap.get(IDENTIFIER) + " does not exists");
            }
        }
    }

    /**
     * Escape the json values
     * @param jsonStr
     * @param escapeFrom
     * @return String
     */
    public String escapeJsonValues(final String jsonStr, final char escapeFrom) {

        final StringWriter writer  = new StringWriter();
        final char[] charArray     = jsonStr.toCharArray();
        boolean inQuotes           = false;

        for(int i=0; i <charArray.length; i++) {

            final char c = charArray[i];
            if(c=='"' && charArray[i-1] != '\\') {

                inQuotes = !inQuotes;
            }

            if(inQuotes && c==escapeFrom) {

                writer.append(ESCAPE_ME_VALUE);
            } else {
                writer.append(c);
            }
        }

        return writer.toString();
    }

    /**
     * Unescape the json values
     * @param escapedValue
     * @param escapeFrom
     * @return String
     */
    public String unescapeValue(final String escapedValue, final String escapeFrom) {

        return escapedValue.replace(ESCAPE_ME_VALUE, escapeFrom);
    }

    /**
     * Checks that a file run by the folder endpoints ({@code /api/vtl/{folder}} and
     * {@code /api/js/{folder}}) was last saved by a user who may write scripts: a holder of the
     * {@link Role#SCRIPTING_DEVELOPER} role or a CMS Administrator. The system user passes both
     * checks, so files saved by imports and startup keep running.
     *
     * @param scriptFile the file about to be run
     * @throws DotSecurityException when the last modifier is unknown or may not write scripts
     * @throws DotDataException     when the role cannot be loaded
     */
    public void checkLastModifierCanScript(final Contentlet scriptFile)
            throws DotSecurityException, DotDataException {

        final String modUserId = scriptFile.getModUser();
        if (!UtilMethods.isSet(modUserId)) {
            Logger.warn(this, String.format("File %s was not run: it has no last modifier",
                    scriptFile.getIdentifier()));
            throw new DotSecurityException(REQUIRED_ROLE_MESSAGE);
        }

        final Try<User> loadedUser = Try.of(() -> APILocator.getUserAPI()
                .loadUserById(modUserId, APILocator.systemUser(), false));
        if (loadedUser.isFailure()) {
            Logger.warnAndDebug(ScriptingUtil.class, String.format(
                    "File %s was not run: its last modifier '%s' could not be loaded: %s",
                    scriptFile.getIdentifier(), modUserId, loadedUser.getCause().getMessage()),
                    loadedUser.getCause());
            throw new DotSecurityException(REQUIRED_ROLE_MESSAGE);
        }
        final User modUser = loadedUser.get();
        if (null == modUser) {
            Logger.warn(this, String.format("File %s was not run: its last modifier '%s' was not found",
                    scriptFile.getIdentifier(), modUserId));
            throw new DotSecurityException(REQUIRED_ROLE_MESSAGE);
        }

        final Role scriptingDeveloper = APILocator.getRoleAPI().loadRoleByKey(Role.SCRIPTING_DEVELOPER);
        final boolean canScript = modUser.isAdmin()
                || APILocator.getRoleAPI().doesUserHaveRole(modUser, scriptingDeveloper);

        if (!canScript) {
            Logger.warn(this, String.format(
                    "File %s was not run: its last modifier '%s' does not hold the %s role",
                    scriptFile.getIdentifier(), modUserId, Role.SCRIPTING_DEVELOPER));
            throw new DotSecurityException(REQUIRED_ROLE_MESSAGE);
        }
    }
}
