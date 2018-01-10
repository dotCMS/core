package com.dotmarketing.portlets.workflows.model;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.Validator;
import java.util.StringTokenizer;

/**
 * This type of workflow actionlet parameter allows content managers to enter a comma-separated list
 * of values used to specify different system users. This class provides 3 mechanisms to specify
 * users in you actionlet:
 * <ol>
 * <li>The user ID.</li>
 * <li>The user's email address.</li>
 * <li>The role key (this includes all the users associated to the specified role).</li>
 * </ol>
 *
 * @author Jose Castro
 * @version 4.3.0
 * @since Jan 9, 2018
 */
public class MultiUserReferenceParameter extends WorkflowActionletParameter {

    /**
     * Creates a new instance of this class.
     *
     * @param key          The unique key for this parameter.
     * @param displayName  The human-readable name of this parameter.
     * @param defaultValue The optional default value of this parameter.
     * @param isRequired   If {@code true}, the value of this parameter is required. Otherwise, set
     *                     to {@code false}.
     */
    public MultiUserReferenceParameter(String key, String displayName,
            String defaultValue, boolean isRequired) {
        super(key, displayName, defaultValue, isRequired);
    }

    @Override
    public String hasError(final String stringToValidate) {
        final StringBuffer errorMsg = new StringBuffer();
        if (UtilMethods.isSet(stringToValidate)) {
            final StringTokenizer tokenizer = new StringTokenizer(stringToValidate, ",");
            while (tokenizer.hasMoreTokens()) {
                final String token = tokenizer.nextToken().trim();
                if (Validator.isEmailAddress(token)) {
                    try {
                        APILocator.getUserAPI()
                                .loadByUserByEmail(token, APILocator.getUserAPI().getSystemUser(),
                                        false);
                    } catch (Exception e) {
                        Logger.error(this.getClass(), "Unable to find user with email: " + token);
                        errorMsg.append("Unable to find user with email: " + token + "</br>");
                    }
                } else {
                    String error = null;
                    if (isUserId(token)) {
                        continue;
                    } else {
                        error = "Unable to find user with ID: " + token + "</br>";
                        if (isRoleKey(token)) {
                            continue;
                        } else {
                            error = (UtilMethods.isSet(error)) ? error
                                    : "Unable to find users assigned to role key: " + token;
                        }
                        errorMsg.append(error);
                    }
                }
            }
        }
        return (UtilMethods.isSet(errorMsg.toString())) ? errorMsg.toString() : null;
    }

    /**
     * Checks whether the specified value belongs to an existing user ID or not.
     *
     * @param userId The potential user ID.
     *
     * @return Returns {@code true} if the specified value is a valid user ID. Otherwise, returns
     * {@code false}.
     */
    private boolean isUserId(final String userId) {
        try {
            APILocator.getUserAPI()
                    .loadUserById(userId, APILocator.getUserAPI().getSystemUser(), false);
            return Boolean.TRUE;
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }

    /**
     * Checks whether the specified value belongs to an existing role key or not.
     *
     * @param roleKey The potential role key.
     *
     * @return Returns {@code true} if the specified value is a valid role key. Otherwise, returns
     * {@code false}.
     */
    private boolean isRoleKey(final String roleKey) {
        try {
            final Role role = APILocator.getRoleAPI().loadRoleByKey(roleKey);
            APILocator.getRoleAPI()
                    .findUsersForRole(role);
            return Boolean.TRUE;
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }

}
