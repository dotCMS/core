package com.dotmarketing.portlets.workflows.util;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowHistoryState;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.Validator;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.*;

/**
 * This utility class provides common-use methods that can be accessed by the different workflow
 * actionlets provided either OOTB by dotCMS, or added via Static or Dynamic Plugins.
 *
 * @author Jose Castro
 * @version 4.3.0
 * @since Jan 9, 2018
 */
public class WorkflowActionletUtil {

    private WorkflowActionletUtil() {

    }

    /**
     * Retrieves the list of {@link User} objects from a list of IDs represented as a String. This
     * list can be composed of user IDs, email addresses, or role keys. For role keys, all the users
     * assigned to those roles will be included in the result user list. The tokens will be split by
     * the specified delimiter value.
     *
     * @param ids       The user IDs, email addresses, or role keys that will be used to retrieve
     *                  the
     *                  {@link User} objects.
     * @param delimiter The delimiter character used to separate each token in the String.
     *
     * @return The list of {@link User} objects based on the specified IDs.
     */
    public static Tuple2<Set<User>, Set<Role>> getUsersFromIds(final String ids, final String delimiter) {

        final Set<User> userSet = new HashSet<>();
        final Set<Role> roleSet = new HashSet<>();
        final StringTokenizer tokenizer = new StringTokenizer(ids, delimiter);
        while (tokenizer.hasMoreTokens()) {

            boolean idNotFound  = Boolean.FALSE;
            Exception exception = null;
            final String token  = tokenizer.nextToken().trim();
            if (Validator.isEmailAddress(token)) {
                try {
                    final User user = APILocator.getUserAPI()
                            .loadByUserByEmail(token, APILocator.getUserAPI().getSystemUser(),
                                    false);
                    userSet.add(user);
                } catch (Exception e) {
                    // Email not found
                    idNotFound = Boolean.TRUE;
                    exception = e;
                }
            } else {

                try {
                    final User user = APILocator.getUserAPI()
                            .loadUserById(token, APILocator.getUserAPI().getSystemUser(), false);
                    userSet.add(user);
                    continue;
                } catch (Exception e) {
                    // User ID not found
                    idNotFound = Boolean.TRUE;
                    exception = e;
                }

                try {
                    final Role role = APILocator.getRoleAPI().loadRoleByKey(token);
                    final List<User> approvingUsersInRole = APILocator.getRoleAPI()
                            .findUsersForRole(role);
                    userSet.addAll(approvingUsersInRole);
                    roleSet.add(role);
                    idNotFound = Boolean.FALSE;
                } catch (DotSecurityException e) {
                    Logger.warn(WorkflowActionletUtil.class,
                            "An error occurred when retrieving users from role key: " + token,
                            e);
                } catch (DotDataException e) {
                    // The specified role could not be found
                    idNotFound = Boolean.TRUE;
                    exception = e;
                }
            }

            if (idNotFound) {
                Logger.warn(WorkflowActionletUtil.class,
                        "The following email/userID/role key could not be found: " + token,
                        exception);
            }
        }

        return Tuple.of(userSet, roleSet);
    }


    /**
     * Returns the value of the specified actionlet parameter. If the value is {@code null}, the
     * specified default value will be returned.
     *
     * @param parameter    The actionlet parameter specified through the UI.
     * @param defaultValue The default value of the parameter if null.
     *
     * @return The value of the specified parameter.
     */
    public static <T> T getParameterValue(final WorkflowActionClassParameter parameter,
            final T defaultValue) {
        final Object paramValue = getParameterValue(parameter);
        return (null != paramValue) ? (T) paramValue : defaultValue;
    }

    /**
     * Returns the value of the specified actionlet parameter.
     *
     * @param parameter The actionlet parameter specified through the UI.
     *
     * @return The value of the specified parameter.
     */
    public static <T> T getParameterValue(final WorkflowActionClassParameter parameter) {
        T paramValue = null;
        try {
            if (null != parameter && UtilMethods.isSet(parameter.getValue())) {
                paramValue = (T) parameter.getValue();
            }
        } catch (Exception e) {
            Logger.debug(WorkflowActionletUtil.class,
                    "Value of parameter '" + parameter.getId()
                            + "' could not be returned.", e);
        }
        return paramValue;
    }

    /**
     * Based on a list of specific users, verifies how many of them have approved a given contentlet
     * and returns them as a Set. This method takes the history of the workflow and traverses every
     * entry in order to determine which user in the specified list has approved the new content.
     *
     * @param historyList       The history of a workflow.
     * @param requiredApprovers The list of users whose approval will be verified.
     * @param contentId         The ID of the new content which needs approval.
     *
     * @return The Set of {@link User} objects that have approved the new content.
     */
    public static Set<User> getApproversFromHistory(final List<WorkflowHistory> historyList,
            final Collection<User> requiredApprovers, final String contentId) {
        return getApproversFromHistory(historyList, requiredApprovers, contentId, 0);
    }

    /**
     * Based on a list of specific users, verifies how many of them have approved a given contentlet
     * and returns them as a Set. This method takes the history of the workflow and traverses every
     * entry in order to determine which user in the specified list has approved the new content.
     * <p>
     * The {@code limit} parameter allows you to return a minimum number of approvers. For example,
     * if the limit is set to {@code 3}, only the first three users that approved the new content
     * will be included in the result list. This limit is meant to avoid traversing all the history
     * records and just return a subset of them.
     *
     * @param historyList       The history of a workflow.
     * @param requiredApprovers The list of users whose approval will be verified.
     * @param contentId         The ID of the new content which needs approval.
     * @param limit             The minimum number of approvers to return.
     *
     * @return The Set of {@link User} objects that have approved the new content.
     */
    public static Set<User> getApproversFromHistory(final List<WorkflowHistory> historyList,
            final Collection<User> requiredApprovers, final String contentId, final int limit) {
        final Set<User> hasApproved = new HashSet<>();
        for (final User user : requiredApprovers) {
            for (final WorkflowHistory historyItem : historyList) {

                final Map<String, Object> changeMap = historyItem.getChangeMap();
                if (historyItem.getActionId().equals(contentId) &&
                        user.getUserId().equals(historyItem.getMadeBy()) && // if it is the action id and it is not reset.
                        !WorkflowHistoryState.RESET.name().equals(changeMap.get("state"))
                ) {
                    hasApproved.add(user);
                    if (limit > 0 && hasApproved.size() >= limit) {
                        return hasApproved;
                    }
                }
            }
        }
        return hasApproved;
    }

}
