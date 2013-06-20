package com.dotmarketing.business;

import com.dotcms.TestBase;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Jonathan Gamba
 *         Date: 6/20/13
 */
public class RoleAPITest extends TestBase {

    private static DotCacheAdministrator cache;
    private static User systemUser;

    //Current group keys
    private String primaryGroup = "dotCMSRoleCache";
    private String keyGroup = "dotCMSRoleKeyCache";
    private String userGroup = "dotCMSUserRoleCache";
    private String layoutGroup = "dotCMSLayoutCache";
    private String rootRolesGroup = "dotCMSRootRolesCache";

    @BeforeClass
    public static void prepare () throws DotSecurityException, DotDataException {

        cache = CacheLocator.getCacheAdministrator();

        //Setting the test user
        systemUser = APILocator.getUserAPI().getSystemUser();
    }

    /**
     * Testing {@link RoleAPI#loadRolesForUser(String, boolean)}, {@link RoleAPI#loadRolesForUser(String)},
     * {@link RoleAPI#doesUserHaveRole(com.liferay.portal.model.User, Role)} and the cache for Roles in general
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotCacheException
     * @see RoleAPI
     * @see RoleFactory
     * @see RoleCache
     */
    @Test
    public void loadRolesForUser () throws DotDataException, DotSecurityException, DotCacheException {

        RoleAPI roleAPI = APILocator.getRoleAPI();
        UserAPI userAPI = APILocator.getUserAPI();

        //Search for the current root roles
        List<Role> originalRootRoles = roleAPI.findRootRoles();
        assertTrue( originalRootRoles != null && !originalRootRoles.isEmpty() );

        //Get a test role
        Role multipleLevelsRole = null;
        for ( Role role : originalRootRoles ) {
            if ( role.getName().equals( "Publisher / Legal" ) ) {
                multipleLevelsRole = role;
            }
        }
        assertNotNull( multipleLevelsRole );

        //Search for a user associated with our test role
        List<User> foundUsers = roleAPI.findUsersForRole( multipleLevelsRole );
        assertTrue( foundUsers != null && !foundUsers.isEmpty() );
        User testUser = foundUsers.get( 0 );

        //Verify if we get the implicit roles
        List<Role> foundRoles = roleAPI.loadRolesForUser( testUser.getUserId(), true );
        assertTrue( foundRoles != null && !foundRoles.isEmpty() );
        assertTrue( foundRoles.size() == 4 );//We know we have 3 levels here: "Publisher/Legal" -> "Reviewer" -> "Contributor" + User role

        //Cache validations
        List<Role> originalCachedRootRoles = CacheLocator.getRoleCache().getRootRoles();
        assertTrue( originalCachedRootRoles != null && !originalCachedRootRoles.isEmpty() );
        assertEquals( originalRootRoles.size(), originalCachedRootRoles.size() );

        //-----------------------------------------------
        //ADDING NEW RECORDS
        //-----------------------------------------------

        //Creating a new test user
        User newUser = userAPI.createUser( "test@test.com", "test@test.com" );
        newUser.setFirstName( "Test" );
        newUser.setLastName( "User" );
        userAPI.save( newUser, systemUser, false );

        //Creating test roles
        Role rootRole = new Role();
        rootRole.setName( "Test Root Role" );
        rootRole.setRoleKey( "testRootRole" );
        rootRole.setEditUsers( true );
        rootRole.setEditPermissions( true );
        rootRole.setEditLayouts( true );
        rootRole.setDescription( "Test Root Role" );
        roleAPI.save( rootRole );//This will clear the cache for the root roles -> cache.clearRootRoleCache
        //Verify for this one
        verifyNewRole( rootRole );
        //The root roles changed
        List<Role> newCachedRootRoles = CacheLocator.getRoleCache().getRootRoles();
        assertNotNull( newCachedRootRoles );

        List<Role> newRootRoles = roleAPI.findRootRoles();//This should populate again the cache for the roots roles
        assertTrue( newRootRoles != null && !newRootRoles.isEmpty() );
        assertEquals( newRootRoles.size(), originalRootRoles.size() + 1 );

        newCachedRootRoles = CacheLocator.getRoleCache().getRootRoles();
        assertTrue( newCachedRootRoles != null && !newCachedRootRoles.isEmpty() );
        assertEquals( newCachedRootRoles.size(), originalCachedRootRoles.size() + 1 );

        //Child role
        Role childRole = new Role();
        childRole.setName( "Test Child Role 1" );
        childRole.setRoleKey( "testChildRole1" );
        childRole.setEditUsers( true );
        childRole.setEditPermissions( true );
        childRole.setEditLayouts( true );
        childRole.setDescription( "Test Child Role 1" );
        childRole.setParent( rootRole.getParent() );
        roleAPI.save( childRole );
        //Verify for this one
        verifyNewRole( childRole );

        //Child role 2
        Role childRole2 = new Role();
        childRole2.setName( "Test Child Role 2" );
        childRole2.setRoleKey( "testChildRole2" );
        childRole2.setEditUsers( true );
        childRole2.setEditPermissions( true );
        childRole2.setEditLayouts( true );
        childRole2.setDescription( "Test Child Role 2" );
        childRole2.setParent( childRole.getParent() );
        roleAPI.save( childRole2 );
        //Verify for this one
        verifyNewRole( childRole2 );

        //Add to the test user the root role
        roleAPI.addRoleToUser( rootRole, newUser );

        List<String> rolesIds = CacheLocator.getRoleCache().getRoleIdsForUser( newUser.getUserId() );
        assertTrue( rolesIds != null && !rolesIds.isEmpty() );
        assertEquals( rolesIds.size(), 3 );
        //Verify if we get the implicit roles
        foundRoles = roleAPI.loadRolesForUser( newUser.getUserId(), true );//We know we have 3 levels here: "Publisher/Legal" -> "Reviewer" -> "Contributor" + User role
        assertTrue( foundRoles != null && !foundRoles.isEmpty() );
        assertEquals( foundRoles.size(), 4 );

        foundRoles = roleAPI.loadRolesForUser( newUser.getUserId(), false );
        assertTrue( foundRoles != null && !foundRoles.isEmpty() );
        assertEquals( foundRoles.size(), 2 );

        //Test the doesUserHaveRole(...) method
        boolean does = roleAPI.doesUserHaveRole( newUser, rootRole.getId() );
        assertTrue( does );
        does = roleAPI.doesUserHaveRole( newUser, childRole.getId() );
        assertTrue( does );
        does = roleAPI.doesUserHaveRole( newUser, childRole2.getId() );
        assertTrue( does );
        does = roleAPI.doesUserHaveRole( newUser, multipleLevelsRole.getId() );
        assertFalse( does );
    }

    /**
     * Verify if a given Roles was saved properly
     *
     * @param testRole
     * @throws DotCacheException
     * @throws DotDataException
     */
    private void verifyNewRole ( Role testRole ) throws DotCacheException, DotDataException {

        RoleAPI roleAPI = APILocator.getRoleAPI();

        String key = primaryGroup + testRole.getId();
        Object cachedRole = cache.get( key, primaryGroup );
        assertNotNull( cachedRole );
        assertEquals( cachedRole, testRole );

        Role foundRole = roleAPI.loadRoleById( testRole.getId() );
        assertNotNull( foundRole );
        assertEquals( cachedRole, foundRole );
    }

}