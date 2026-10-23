package com.dotmarketing.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.auth.providers.oauth.OAuthConstants;
import com.dotmarketing.filters.TimeMachineFilter;
import com.liferay.portal.util.WebKeys;
import org.junit.jupiter.api.Test;

/**
 * Tests the shared set of session and request attribute names that only dotCMS itself sets.
 */
class ReservedAttributeNamesTest {

    /**
     * Given each attribute name dotCMS uses for the signed-in user and the login flow
     * (the user id, the user, the Login As principal, the edit-mode redirect, the native-login flag)
     * and for the Time Machine preview (date, language, site),
     * When it is looked up in the shared set,
     * Then it is reported as reserved.
     */
    @Test
    void loginUserAndPreviewAttributeNamesAreReserved() {
        assertTrue(ReservedAttributeNames.isReserved(WebKeys.USER_ID));
        assertTrue(ReservedAttributeNames.isReserved(WebKeys.USER));
        assertTrue(ReservedAttributeNames.isReserved(WebKeys.PRINCIPAL_USER_ID));
        assertTrue(ReservedAttributeNames.isReserved(WebKeys.LOGIN_TO_EDIT_MODE));
        assertTrue(ReservedAttributeNames.isReserved(OAuthConstants.SESSION_NATIVE_LOGIN));
        assertTrue(ReservedAttributeNames.isReserved(TimeMachineFilter.TM_DATE_VAR));
        assertTrue(ReservedAttributeNames.isReserved(TimeMachineFilter.TM_LANG_VAR));
        assertTrue(ReservedAttributeNames.isReserved(TimeMachineFilter.TM_HOST_VAR));
    }

    /**
     * Given an ordinary attribute name, such as one a rule sets for page personalization,
     * When it is looked up in the shared set,
     * Then it is not reported as reserved.
     */
    @Test
    void ordinaryAttributeNameIsNotReserved() {
        assertFalse(ReservedAttributeNames.isReserved("visitorSegment"));
        assertFalse(ReservedAttributeNames.isReserved("userId"));
        assertFalse(ReservedAttributeNames.isReserved(WebKeys.USER_ID.toLowerCase()));
        assertFalse(ReservedAttributeNames.isReserved(" " + WebKeys.USER_ID + " "));
        assertFalse(ReservedAttributeNames.isReserved(WebKeys.USER_ID + "_X"));
        assertFalse(ReservedAttributeNames.isReserved(""));
        assertFalse(ReservedAttributeNames.isReserved(null));
    }
}
