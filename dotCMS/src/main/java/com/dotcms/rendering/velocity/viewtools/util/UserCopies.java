package com.dotcms.rendering.velocity.viewtools.util;

import com.liferay.portal.model.User;
import java.util.HashMap;

/**
 * Builds the user copies that template tools hand out, so templates and scripts never hold the
 * cached or session user record.
 */
public final class UserCopies {

    private UserCopies() {
    }

    /**
     * Copies a user, keeping the profile fields, the additional info (as its own map) and the
     * modification date, and clearing the password, the password-reset token and the remember-me
     * token on the copy.
     *
     * @param user the cached or session account
     * @return a detached copy without credential fields, or {@code null} if {@code user} is
     *         {@code null}
     */
    public static User withoutCredentials(final User user) {
        if (user == null) {
            return null;
        }
        final User copy = (User) user.clone();
        copy.setPassword(null);
        copy.setIcqId(null);
        copy.setRememberMeToken(null);
        // clone() rebuilds the user without these two
        copy.setAdditionalInfo(user.getAdditionalInfo() == null ? null : new HashMap<>(user.getAdditionalInfo()));
        copy.setModificationDate(user.getModificationDate());
        return copy;
    }
}
