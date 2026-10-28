package com.dotmarketing.business;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

/**
 * Covers {@link RoleAPIImpl#loadRoleByKeyOrId(String)}. Neither the key nor the id lookup caches a
 * miss, so the method must try the likelier form first and only then the other one.
 */
public class RoleAPIImplTest {

    private static final String ROLE_ID = "6b7a3c21-5d0e-4f8b-9a1c-2e3d4f5a6b7c";
    private static final String ROLE_KEY = Role.DOTCMS_BACK_END_USER;

    private RoleFactory roleFactory;
    private RoleAPIImpl roleAPI;
    private Role role;

    @Before
    public void setUp() {
        roleFactory = mock(RoleFactory.class);
        roleAPI = new RoleAPIImpl(roleFactory, mock(UserAPI.class));
        role = new Role();
        role.setId(ROLE_ID);
        role.setRoleKey(ROLE_KEY);
    }

    /**
     * Method to test: {@link RoleAPIImpl#loadRoleByKeyOrId(String)}
     * Given scenario: The value is shaped like a UUID and matches a role id.
     * Expected result: The id lookup runs first and the key lookup is never needed.
     */
    @Test
    public void testLoadRoleByKeyOrId_WhenValueIsAUuidMatchingAnId_ShouldTryTheIdFirst() throws Exception {
        when(roleFactory.getRoleById(ROLE_ID)).thenReturn(role);

        assertSame(role, roleAPI.loadRoleByKeyOrId(ROLE_ID));
        verify(roleFactory, never()).loadRoleByKey(anyString());
    }

    /**
     * Method to test: {@link RoleAPIImpl#loadRoleByKeyOrId(String)}
     * Given scenario: The value is shaped like a UUID but is actually a role key.
     * Expected result: After the id lookup misses, the key lookup finds the role.
     */
    @Test
    public void testLoadRoleByKeyOrId_WhenUuidShapedValueIsAKey_ShouldFallBackToTheKey() throws Exception {
        final Role uuidKeyedRole = new Role();
        uuidKeyedRole.setId("some-other-id");
        uuidKeyedRole.setRoleKey(ROLE_ID);
        when(roleFactory.getRoleById(ROLE_ID)).thenReturn(null);
        when(roleFactory.loadRoleByKey(ROLE_ID)).thenReturn(uuidKeyedRole);

        assertSame(uuidKeyedRole, roleAPI.loadRoleByKeyOrId(ROLE_ID));
        final InOrder order = inOrder(roleFactory);
        order.verify(roleFactory).getRoleById(ROLE_ID);
        order.verify(roleFactory).loadRoleByKey(ROLE_ID);
    }

    /**
     * Method to test: {@link RoleAPIImpl#loadRoleByKeyOrId(String)}
     * Given scenario: The value is a role key such as {@code DOTCMS_BACK_END_USER}.
     * Expected result: The key lookup runs first and the id lookup is never needed.
     */
    @Test
    public void testLoadRoleByKeyOrId_WhenValueIsAKey_ShouldTryTheKeyFirst() throws Exception {
        when(roleFactory.loadRoleByKey(ROLE_KEY)).thenReturn(role);

        assertSame(role, roleAPI.loadRoleByKeyOrId(ROLE_KEY));
        verify(roleFactory, never()).getRoleById(anyString());
    }

    /**
     * Method to test: {@link RoleAPIImpl#loadRoleByKeyOrId(String)}
     * Given scenario: The value is not shaped like a UUID and matches no key, but it is the id of
     * a role (ids are not always UUIDs).
     * Expected result: After the key lookup misses, the id lookup finds the role.
     */
    @Test
    public void testLoadRoleByKeyOrId_WhenNonUuidValueIsAnId_ShouldFallBackToTheId() throws Exception {
        final Role plainIdRole = new Role();
        plainIdRole.setId("cms_anon");
        when(roleFactory.loadRoleByKey("cms_anon")).thenReturn(null);
        when(roleFactory.getRoleById("cms_anon")).thenReturn(plainIdRole);

        assertSame(plainIdRole, roleAPI.loadRoleByKeyOrId("cms_anon"));
        final InOrder order = inOrder(roleFactory);
        order.verify(roleFactory).loadRoleByKey("cms_anon");
        order.verify(roleFactory).getRoleById("cms_anon");
    }

    /**
     * Method to test: {@link RoleAPIImpl#loadRoleByKeyOrId(String)}
     * Given scenario: Neither lookup finds a role, or the factory hands back a role with no id.
     * Expected result: {@code null}.
     */
    @Test
    public void testLoadRoleByKeyOrId_WhenNothingMatches_ShouldReturnNull() throws Exception {
        when(roleFactory.loadRoleByKey("nope")).thenReturn(null);
        when(roleFactory.getRoleById("nope")).thenReturn(new Role());

        assertNull(roleAPI.loadRoleByKeyOrId("nope"));
    }

    /**
     * Method to test: {@link RoleAPIImpl#loadRoleByKeyOrId(String)}
     * Given scenario: The value is {@code null} or blank.
     * Expected result: {@code null}, without touching the factory.
     */
    @Test
    public void testLoadRoleByKeyOrId_WhenValueIsBlank_ShouldReturnNullWithoutLookups() throws Exception {
        assertNull(roleAPI.loadRoleByKeyOrId(null));
        assertNull(roleAPI.loadRoleByKeyOrId("  "));
        verify(roleFactory, never()).loadRoleByKey(anyString());
        verify(roleFactory, never()).getRoleById(anyString());
    }

}
