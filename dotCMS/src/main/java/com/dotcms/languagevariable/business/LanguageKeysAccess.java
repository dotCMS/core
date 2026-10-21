package com.dotcms.languagevariable.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;

/**
 * Access rule for saving language keys from the legacy language keys page. Both legacy entry
 * points that save keys (the DWR {@code LanguageAjax.saveKeys} call and the Struts
 * {@code edit_language_keys} action with {@code cmd=save}) use this class, so the rule lives in
 * one place: the user must have access to the Languages or the Locales portlet, the two tools
 * that manage language keys.
 */
public final class LanguageKeysAccess {

    private LanguageKeysAccess() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Checks that the user may save language keys: they must have access to the Languages or the
     * Locales portlet. A rejected request is written once to the security log with the user id
     * only, however many portlets were checked, and the exception carries a fixed message.
     *
     * @param user the user saving the keys; {@code null} is rejected
     * @throws DotSecurityException if there is no user, or the user has access to neither portlet
     * @throws DotDataException     if the user's portlets cannot be loaded
     */
    public static void requireLanguagesOrLocalesPortlet(final User user)
            throws DotSecurityException, DotDataException {

        if (null != user) {
            final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
            if (layoutAPI.doesUserHaveAccessToPortlet(PortletID.LANGUAGES.toString(), user)
                    || layoutAPI.doesUserHaveAccessToPortlet(PortletID.LOCALES.toString(), user)) {
                return;
            }
        }

        SecurityLogger.logInfo(LanguageKeysAccess.class, String.format(
                "User [%s] does not have access to the [%s] or the [%s] Portlet, required to save language keys",
                null == user ? "[not logged in]" : user.getUserId(), PortletID.LANGUAGES,
                PortletID.LOCALES));
        throw new DotSecurityException(
                "Saving language keys requires access to the Languages or the Locales portlet");
    }
}
