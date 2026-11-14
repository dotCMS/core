package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for user-id keys in {@link UserCacheImpl}.
 *
 * <p>The id is trimmed and lowercased before the primary-group prefix is applied, so a session
 * id that differs only by case or surrounding spaces is the same entry the stored id evicts.
 * The email-group entry keeps the email string unchanged.</p>
 */
public class UserCacheImplTest {

    private static final String EMAIL_GROUP = "UserEmailDotCMSCache";

    private final Map<String, Object> store = new ConcurrentHashMap<>();
    private final List<String[]> puts = new ArrayList<>();

    private MockedStatic<CacheLocator> cacheLocator;
    private UserCacheImpl userCache;

    /**
     * Points {@link CacheLocator#getCacheAdministrator()} at an in-memory map for put, get, and
     * remove. A missing entry comes back as null.
     */
    @Before
    public void setUp() throws DotCacheException {
        store.clear();
        puts.clear();

        final DotCacheAdministrator cache = mock(DotCacheAdministrator.class);
        when(cache.get(anyString(), anyString())).thenAnswer(invocation ->
                store.get(entry(invocation.getArgument(0), invocation.getArgument(1))));
        doAnswer(invocation -> {
            final String key = invocation.getArgument(0);
            final String group = invocation.getArgument(2);
            puts.add(new String[] {key, group});
            store.put(entry(key, group), invocation.getArgument(1));
            return null;
        }).when(cache).put(anyString(), any(), anyString());
        doAnswer(invocation -> {
            store.remove(entry(invocation.getArgument(0), invocation.getArgument(1)));
            return null;
        }).when(cache).remove(anyString(), anyString());

        cacheLocator = mockStatic(CacheLocator.class);
        cacheLocator.when(CacheLocator::getCacheAdministrator).thenReturn(cache);
        userCache = new UserCacheImpl();
    }

    @After
    public void tearDown() {
        cacheLocator.close();
    }

    /**
     * An id stored as {@code Jane.Doe} is the same entry as {@code jane.doe} with surrounding
     * spaces.
     */
    @Test
    public void get_returnsUserAddedUnderDifferentCaseAndSurroundingSpaces() {
        final User user = user("Jane.Doe", "Jane.Doe@Example.com");

        userCache.add("Jane.Doe", user);

        assertSame(user, userCache.get(" jane.doe "));
    }

    /**
     * Removing the uppercase id drops the entry stored under the lowercase id, including when the
     * cached user's own id still has mixed case.
     */
    @Test
    public void remove_dropsUserLookedUpUnderDifferentCase() {
        final User user = user("Jane.Doe", "Jane.Doe@Example.com");
        userCache.add("Jane.Doe", user);

        userCache.remove("JANE.DOE");

        assertNull(userCache.get("jane.doe"));
    }

    /**
     * The email-group put keeps {@link User#getEmailAddress()} exactly: not lowercased here, and
     * not prefixed with the primary group.
     */
    @Test
    public void add_storesEmailInEmailGroupUnderTheEmailUnchanged() {
        final User user = user("Jane.Doe", "Jane.Doe@Example.com");

        userCache.add("Jane.Doe", user);

        final String emailKey = puts.stream()
                .filter(put -> EMAIL_GROUP.equals(put[1]))
                .map(put -> put[0])
                .findFirst()
                .orElse(null);
        assertEquals(user.getEmailAddress(), emailKey);
        assertEquals("Jane.Doe@Example.com", emailKey);
    }

    private static User user(final String userId, final String email) {
        final User user = new User(userId);
        user.setEmailAddress(email);
        return user;
    }

    private static String entry(final String key, final String group) {
        return group + '\u0000' + key;
    }
}
