package com.dotcms.api.system.event;

/**
 * Visibility for a {@link SystemEvent}, it may be by user, role, permission or global
 *
 * <u>
 *     <li><b>USER:</b> Choose this visibility if you want your event triggered to a specific user.</li>
 *     <li><b>USER_SESSION:</b> Choose this visibility if you want your event triggered to a specific user and specific session.</li>
 *     <li><b>ROLE:</b> Choose this visibility if you want your event triggered to a specific role.</li>
 *     <li><b>ROLES:</b> Choose this visibility if you want your event triggered to a specific roles (Depending on the operator pass to the {@link VisibilityRoles}, it is will apply an OR/AND logic over the roles.).</li>
 *     <li><b>PERMISSION:</b> Choose this visibility if you want your event triggered to an users with a specific permission .</li>
 *     <li><b>EXCLUDE_OWNER:</b> This visibility is a special case when you want to exclude the event from the owner (the user that triggered the event)<br>
 *
 *         For instance, this example includes a Visibility by permission (for read) but executes the message the user who triggered the action.
 *      <code>new Payload(contentlet, Visibility.EXCLUDE_OWNER,
 *          new ExcludeOwnerVerifierBean(contentlet.getModUser(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION))
 *      </code>
 *     .</li>
 *     <li><b>GLOBAL:</b> If you want to sent an event to everyone logged in, use this visibility .</li>
 * </u>
 *
 * @author jsanca
 */
public enum Visibility {

    USER,
    USER_SESSION,
    USERS,
    ROLE,
    ROLES,
    PERMISSION,
    EXCLUDE_OWNER,
    GLOBAL;
}