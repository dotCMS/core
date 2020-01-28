package com.dotcms.enterprise.rules;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.model.Rule;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration test of {@link RulesAPIImpl}
 */
public class RulesAPIImplIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to Test: {@link RulesAPIImpl#saveRule(Rule, User, boolean)}
     * When: A not admin user with right permission try to save a new rule
     * Should: Save the new rule
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldSaveNewRule() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule = new RuleDataGen().host(host).next();

        this.addPermission(role, host, true);
        APILocator.getRulesAPI().saveRule(rule, user, false);

        final List<Rule> rules = APILocator.getRulesAPI().getAllRulesByParent(host, APILocator.systemUser(), true);
        assertEquals(1, rules.size());
    }

    /**
     * Method to Test: {@link RulesAPIImpl#saveRule(Rule, User, boolean)}
     * When: A not admin user without permission try to save a new rule
     * Should: Not save the new rule
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void shouldNotSaveNewRule() throws DotSecurityException, DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Rule rule = new RuleDataGen().host(host).next();

        this.addPermission(role, host, false);

        try {
            APILocator.getRulesAPI().saveRule(rule, user, false);
            throw new AssertionError("DotSecurityException expected");
        } catch(DotSecurityException e) {
            //expected//
        }

        final List<Rule> rules = APILocator.getRulesAPI().getAllRulesByParent(host, APILocator.systemUser(), true);
        assertTrue(rules.isEmpty());
    }

    private void addPermission(final Role role, final Host host, final boolean notAddPublishPermission)
            throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();

        final List<Permission> permissions = new ArrayList<>();
        final Permission readPermission = new Permission();
        readPermission.setInode(host.getPermissionId());
        readPermission.setRoleId(role.getId());
        readPermission.setPermission(PermissionAPI.PERMISSION_READ);
        permissions.add(readPermission);

        if (notAddPublishPermission) {
            final Permission publishPermission = new Permission();
            publishPermission.setInode(host.getPermissionId());
            publishPermission.setRoleId(role.getId());
            publishPermission.setPermission(PermissionAPI.PERMISSION_PUBLISH);
            publishPermission.setType(Rule.class.getName());

            permissions.add(publishPermission);
        }

        APILocator.getPermissionAPI().save(permissions, host, systemUser, false);
    }
}
