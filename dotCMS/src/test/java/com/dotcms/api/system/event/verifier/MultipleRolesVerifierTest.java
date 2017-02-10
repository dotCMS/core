package com.dotcms.api.system.event.verifier;

import com.dotcms.UnitTestBase;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.VisibilityRoles;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import static com.dotcms.util.CollectionsUtils.set;
import static org.mockito.Mockito.when;

/**
 * MultipleRolesVerifier
 * Test
 * @author jsanca
 */

public class MultipleRolesVerifierTest extends UnitTestBase {

    @Test
    public void verifiedAndFailTest() throws DotDataException {

        final RoleAPI roleAPI = Mockito.mock(RoleAPI.class);
        final MultipleRolesVerifier rolesVerifier = new MultipleRolesVerifier(roleAPI);

        final Role simpleUserRole = new Role();  simpleUserRole.setId("simple");
        final Role superUserRole  = new Role();  superUserRole.setId ("super");
        final Role adminUserRole  = new Role();  adminUserRole.setId ("admin");

        final Set<Role> roles = set(simpleUserRole, superUserRole, adminUserRole);
        final Payload payload = new Payload(Visibility.ROLES, new VisibilityRoles(VisibilityRoles.Operator.AND, roles));

        final User user = new User();  user.setUserId("dotcms.1");

        when(roleAPI.doesUserHaveRole(user, simpleUserRole.getId())).thenReturn(true);
        when(roleAPI.doesUserHaveRole(user, superUserRole.getId())).thenReturn(true);
        when(roleAPI.doesUserHaveRole(user, adminUserRole.getId())).thenReturn(false);

        final boolean verified = rolesVerifier.verified(payload, user);

        Assert.assertFalse(verified);
    } // verifiedAndFailTest.


    @Test
    public void verifiedAndSuccessTest() throws DotDataException {

        final RoleAPI roleAPI = Mockito.mock(RoleAPI.class);
        final MultipleRolesVerifier rolesVerifier = new MultipleRolesVerifier(roleAPI);

        final Role simpleUserRole = new Role();  simpleUserRole.setId("simple");
        final Role superUserRole  = new Role();  superUserRole.setId ("super");
        final Role adminUserRole  = new Role();  adminUserRole.setId ("admin");

        final Set<Role> roles = set(simpleUserRole, superUserRole, adminUserRole);
        final Payload payload = new Payload(Visibility.ROLES, new VisibilityRoles(VisibilityRoles.Operator.AND, roles));

        final User user = new User();  user.setUserId("dotcms.1");

        when(roleAPI.doesUserHaveRole(user, simpleUserRole.getId())).thenReturn(true);
        when(roleAPI.doesUserHaveRole(user, superUserRole.getId())).thenReturn(true);
        when(roleAPI.doesUserHaveRole(user, adminUserRole.getId())).thenReturn(true);

        final boolean verified = rolesVerifier.verified(payload, user);

        Assert.assertTrue(verified);
    } // verifiedAndSuccessTest.


    @Test
    public void verifiedOrFailTest() throws DotDataException {

        final RoleAPI roleAPI = Mockito.mock(RoleAPI.class);
        final MultipleRolesVerifier rolesVerifier = new MultipleRolesVerifier(roleAPI);

        final Role simpleUserRole = new Role();  simpleUserRole.setId("simple");
        final Role superUserRole  = new Role();  superUserRole.setId ("super");
        final Role adminUserRole  = new Role();  adminUserRole.setId ("admin");

        final Set<Role> roles = set(simpleUserRole, superUserRole, adminUserRole);
        final Payload payload = new Payload(Visibility.ROLES, new VisibilityRoles(VisibilityRoles.Operator.OR, roles));

        final User user = new User();  user.setUserId("dotcms.1");

        when(roleAPI.doesUserHaveRole(user, simpleUserRole.getId())).thenReturn(false);
        when(roleAPI.doesUserHaveRole(user, superUserRole.getId())).thenReturn(false);
        when(roleAPI.doesUserHaveRole(user, adminUserRole.getId())).thenReturn(false);

        final boolean verified = rolesVerifier.verified(payload, user);

        Assert.assertFalse(verified);
    } // verifiedAndFailTest.


    @Test
    public void verifiedOrSuccessTest() throws DotDataException {

        final RoleAPI roleAPI = Mockito.mock(RoleAPI.class);
        final MultipleRolesVerifier rolesVerifier = new MultipleRolesVerifier(roleAPI);

        final Role simpleUserRole = new Role();  simpleUserRole.setId("simple");
        final Role superUserRole  = new Role();  superUserRole.setId ("super");
        final Role adminUserRole  = new Role();  adminUserRole.setId ("admin");

        final Set<Role> roles = set(simpleUserRole, superUserRole, adminUserRole);
        final Payload payload = new Payload(Visibility.ROLES, new VisibilityRoles(VisibilityRoles.Operator.OR, roles));

        final User user = new User();  user.setUserId("dotcms.1");

        when(roleAPI.doesUserHaveRole(user, simpleUserRole.getId())).thenReturn(true);
        when(roleAPI.doesUserHaveRole(user, superUserRole.getId())).thenReturn(false);
        when(roleAPI.doesUserHaveRole(user, adminUserRole.getId())).thenReturn(false);

        final boolean verified = rolesVerifier.verified(payload, user);

        Assert.assertTrue(verified);
    } // verifiedOrSuccessTest.


    @Test
    public void verifiedOrSuccess2Test() throws DotDataException {

        final RoleAPI roleAPI = Mockito.mock(RoleAPI.class);
        final MultipleRolesVerifier rolesVerifier = new MultipleRolesVerifier(roleAPI);

        final Role simpleUserRole = new Role();  simpleUserRole.setId("simple");
        final Role superUserRole  = new Role();  superUserRole.setId ("super");
        final Role adminUserRole  = new Role();  adminUserRole.setId ("admin");

        final Set<Role> roles = set(simpleUserRole, superUserRole, adminUserRole);
        final Payload payload = new Payload(Visibility.ROLES, new VisibilityRoles(VisibilityRoles.Operator.OR, roles));

        final User user = new User();  user.setUserId("dotcms.1");

        when(roleAPI.doesUserHaveRole(user, simpleUserRole.getId())).thenReturn(false);
        when(roleAPI.doesUserHaveRole(user, superUserRole.getId())).thenReturn(true);
        when(roleAPI.doesUserHaveRole(user, adminUserRole.getId())).thenReturn(false);

        final boolean verified = rolesVerifier.verified(payload, user);

        Assert.assertTrue(verified);
    } // verifiedOrSuccessTest.

    @Test
    public void verifiedOrSuccess3Test() throws DotDataException {

        final RoleAPI roleAPI = Mockito.mock(RoleAPI.class);
        final MultipleRolesVerifier rolesVerifier = new MultipleRolesVerifier(roleAPI);

        final Role simpleUserRole = new Role();  simpleUserRole.setId("simple");
        final Role superUserRole  = new Role();  superUserRole.setId ("super");
        final Role adminUserRole  = new Role();  adminUserRole.setId ("admin");

        final Set<Role> roles = set(simpleUserRole, superUserRole, adminUserRole);
        final Payload payload = new Payload(Visibility.ROLES, new VisibilityRoles(VisibilityRoles.Operator.OR, roles));

        final User user = new User();  user.setUserId("dotcms.1");

        when(roleAPI.doesUserHaveRole(user, simpleUserRole.getId())).thenReturn(false);
        when(roleAPI.doesUserHaveRole(user, superUserRole.getId())).thenReturn(false);
        when(roleAPI.doesUserHaveRole(user, adminUserRole.getId())).thenReturn(true);

        final boolean verified = rolesVerifier.verified(payload, user);

        Assert.assertTrue(verified);
    } // verifiedOrSuccessTest.

    @Test
    public void verifiedOrSuccess4Test() throws DotDataException {

        final RoleAPI roleAPI = Mockito.mock(RoleAPI.class);
        final MultipleRolesVerifier rolesVerifier = new MultipleRolesVerifier(roleAPI);

        final Role simpleUserRole = new Role();  simpleUserRole.setId("simple");
        final Role superUserRole  = new Role();  superUserRole.setId ("super");
        final Role adminUserRole  = new Role();  adminUserRole.setId ("admin");

        final Set<Role> roles = set(simpleUserRole, superUserRole, adminUserRole);
        final Payload payload = new Payload(Visibility.ROLES, new VisibilityRoles(VisibilityRoles.Operator.OR, roles));

        final User user = new User();  user.setUserId("dotcms.1");

        when(roleAPI.doesUserHaveRole(user, simpleUserRole.getId())).thenReturn(false);
        when(roleAPI.doesUserHaveRole(user, superUserRole.getId())).thenReturn(true);
        when(roleAPI.doesUserHaveRole(user, adminUserRole.getId())).thenReturn(true);

        final boolean verified = rolesVerifier.verified(payload, user);

        Assert.assertTrue(verified);
    } // verifiedOrSuccessTest.
} // E:O:F:MultipleRolesVerifierTest.
