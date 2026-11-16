package com.dotmarketing.business;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.portal.model.User;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the pure decisions in {@code UserAPIImpl} that need no database.
 */
class UserAPIImplTest {

    private static User user(final boolean active) {
        final User user = new User();
        user.setUserId("user_a");
        user.setActive(active);
        return user;
    }

    @Test
    void deactivates_isTrueOnlyWhenAnActiveUserBecomesInactive() {
        assertTrue(UserAPIImpl.deactivates(user(true), user(false)),
                "an active user saved as inactive is a deactivation");
        assertFalse(UserAPIImpl.deactivates(user(true), user(true)),
                "a user that stays active is not a deactivation");
        assertFalse(UserAPIImpl.deactivates(user(false), user(true)),
                "activating a user again is not a deactivation");
        assertFalse(UserAPIImpl.deactivates(user(false), user(false)),
                "a user that stays inactive is not a deactivation");
        assertFalse(UserAPIImpl.deactivates(null, user(false)),
                "a user with no stored record yet is not a deactivation");
        assertFalse(UserAPIImpl.deactivates(user(true), null),
                "nothing to save is not a deactivation");
    }
}
