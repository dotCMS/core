package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableCategoryField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FieldVariableDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;


public class SecondaryCategoryPermissionTest {

    static PermissionAPI permissionAPI;
    static Host site;
    static User sysuser;
    static Template template;
    static int permissionCacheSize = 0;
    static Contentlet snowContentlet, waterContentlet, ecoContentlet, ecoWaterContentlet, notCategorizedContentlet;
    static int allThePermissions = PermissionAPI.PERMISSION_READ
            | PermissionAPI.PERMISSION_WRITE
            | PermissionAPI.PERMISSION_PUBLISH
            | PermissionAPI.PERMISSION_EDIT_PERMISSIONS
            | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;


    static Role backendRole, frontEndRole, anonymousRole, snowRole, waterRole, ecoRole;
    static User snowUser, waterUser, ecoWaterUser;
    static String unique, snowStr, waterStr, ecoStr;
    static ContentType contentType;
    static Category topSnowWaterCategory, skiCategory, snowboardCategory, waterCategory, surfCategoryNested, scubaCategoryNested, topEcoCategory, ecoCategory;
    static Field category1Field, category2Field;


    @BeforeClass
    @WrapInTransaction
    public static void setup_test() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        _setupSiteRolesAndUsers();

        _setupCategoryTree();
        _setupContentType();
        _setUpContentlets();
        _insure_category_permissions_before_test();
        _insure_content_type_permissions();
    }

    private static void _setupSiteRolesAndUsers() throws DotDataException, DotSecurityException {

        backendRole = APILocator.getRoleAPI().loadBackEndUserRole();
        frontEndRole = APILocator.getRoleAPI().loadFrontEndUserRole();
        anonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();

        unique = String.valueOf(System.currentTimeMillis());
        snowStr = "snow" + unique;
        waterStr = "water" + unique;
        ecoStr = "eco" + unique;

        String hostName = "testHost" + unique;

        permissionAPI = APILocator.getPermissionAPI();
        sysuser = APILocator.getUserAPI().getSystemUser();
        site = new Host();
        site.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        site.setHostname(hostName);
        site = APILocator.getHostAPI().save(site, sysuser, false);
        saveInhertablePermissions(site, backendRole, allThePermissions);

        // create our roles
        snowRole = getOrCreateRole("role" + snowStr);
        waterRole = getOrCreateRole("role" + waterStr);
        ecoRole = getOrCreateRole("role" + ecoStr);

        // create our users
        snowUser = new UserDataGen()
                .id("user" + snowStr)
                .emailAddress("user" + snowStr + "@dotcms.com")
                .roles(new Role[]{backendRole, snowRole})
                .nextPersisted();

        waterUser = new UserDataGen()
                .id("user" + waterStr)
                .emailAddress("user" + waterStr + "@dotcms.com")
                .roles(new Role[]{backendRole, waterRole})
                .nextPersisted();

        ecoWaterUser = new UserDataGen()
                .id("user" + ecoStr)
                .emailAddress("user" + ecoStr + "@dotcms.com")
                .roles(new Role[]{backendRole, ecoRole, waterRole})
                .nextPersisted();

        // test role memberships
        assertTrue(APILocator.getRoleAPI().doesUserHaveRole(snowUser, backendRole));
        assertTrue(APILocator.getRoleAPI().doesUserHaveRole(snowUser, snowRole));
        assertFalse(APILocator.getRoleAPI().doesUserHaveRole(snowUser, ecoRole));
        assertFalse(APILocator.getRoleAPI().doesUserHaveRole(snowUser, waterRole));

        assertTrue(APILocator.getRoleAPI().doesUserHaveRole(waterUser, backendRole));
        assertTrue(APILocator.getRoleAPI().doesUserHaveRole(waterUser, waterRole));
        assertFalse(APILocator.getRoleAPI().doesUserHaveRole(waterUser, snowRole));
        assertFalse(APILocator.getRoleAPI().doesUserHaveRole(waterUser, ecoRole));

        assertTrue(APILocator.getRoleAPI().doesUserHaveRole(ecoWaterUser, backendRole));
        assertTrue(APILocator.getRoleAPI().doesUserHaveRole(ecoWaterUser, ecoRole));
        assertTrue(APILocator.getRoleAPI().doesUserHaveRole(ecoWaterUser, waterRole));
        assertFalse(APILocator.getRoleAPI().doesUserHaveRole(ecoWaterUser, snowRole));

    }

    private static void _setupContentType() throws DotDataException, DotSecurityException {

        // Create content type with category field that has secondaryPermissionCheck=true

        contentType = new ContentTypeDataGen()
                .name("contentTypeCatTest" + unique)
                .host(APILocator.systemHost())
                .velocityVarName("contentTypeCatTest" + unique)
                //.host(site)
                .fields(
                        List.of(new FieldDataGen().type(ImmutableTextField.class).name("Title").velocityVarName("title")
                                .next())
                )
                .nextPersisted();
        category1Field = new FieldDataGen()
                .type(ImmutableCategoryField.class)
                .sortOrder(10)
                .contentTypeId(contentType.id())
                .name("category1Field")
                .velocityVarName("category1Field")
                .values(topSnowWaterCategory.getInode())
                .nextPersisted();

        category2Field = new FieldDataGen()
                .type(ImmutableCategoryField.class)
                .sortOrder(20)
                .contentTypeId(contentType.id())
                .name("category2Field")
                .velocityVarName("category2Field")
                .values(topEcoCategory.getInode())
                .nextPersisted();

        new FieldVariableDataGen().key("secondaryPermissionCheck").value("true").field(category1Field).nextPersisted();
        new FieldVariableDataGen().key("secondaryPermissionCheck").value("true").field(category2Field).nextPersisted();

        List<Permission> newSetOfPermissions = new ArrayList<>();
        newSetOfPermissions.add(
                new Permission(contentType.getPermissionId(), backendRole.getId(), allThePermissions, true));
        newSetOfPermissions.add(
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(), backendRole.getId(),
                        allThePermissions, true));

        newSetOfPermissions.add(
                new Permission(contentType.getPermissionId(), snowRole.getId(), allThePermissions, true));
        newSetOfPermissions.add(
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(), snowRole.getId(),
                        allThePermissions, true));

        newSetOfPermissions.add(
                new Permission(contentType.getPermissionId(), waterRole.getId(), allThePermissions, true));
        newSetOfPermissions.add(
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(), waterRole.getId(),
                        allThePermissions, true));

        newSetOfPermissions.add(
                new Permission(contentType.getPermissionId(), ecoRole.getId(), allThePermissions, true));
        newSetOfPermissions.add(
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(), ecoRole.getId(),
                        allThePermissions, true));

        // needed so the inheritable permissions are set correctly
        newSetOfPermissions.add(
                new Permission(contentType.getPermissionId(), frontEndRole.getId(), allThePermissions, true));
        newSetOfPermissions.add(
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(), frontEndRole.getId(),
                        allThePermissions, true));
        permissionAPI.assignPermissions(newSetOfPermissions, contentType, APILocator.systemUser(), false);

    }

    private static void _setUpContentlets() throws DotDataException, DotSecurityException {

        snowContentlet = new ContentletDataGen(contentType)
                .host(APILocator.systemHost())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty("title", "snowContentlet Content ")
                .setProperty(category1Field.variable(), snowboardCategory.getInode())
                .addCategory(snowboardCategory)
                .nextPersisted();

        ecoContentlet = new ContentletDataGen(contentType)
                .host(APILocator.systemHost())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty("title", "ecoContentlet Content ")
                .setProperty(category2Field.variable(), ecoCategory)
                .addCategory(ecoCategory)
                .nextPersisted();

        ecoWaterContentlet = new ContentletDataGen(contentType)
                .host(APILocator.systemHost())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty("title", "ecoWaterContentlet Content ")
                .setProperty(category1Field.variable(), waterCategory)
                .setProperty(category2Field.variable(), ecoCategory)
                .addCategory(ecoCategory)
                .addCategory(waterCategory)
                .nextPersisted();

        // Create contentlet without category selected
        notCategorizedContentlet = new ContentletDataGen(contentType)
                .host(APILocator.systemHost())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty("title", "notCategorizedContentlet Content")
                .nextPersisted();

        assertNotNull(snowContentlet);
        assertNotNull(snowContentlet.getIdentifier());
        assertNotNull(snowContentlet.getContentType().fieldMap().get(category1Field.variable()));
        assertNotNull(snowContentlet.getContentType().fieldMap().get(category2Field.variable()));

        List<Category> cats1 = (List<Category>) APILocator.getContentletAPI()
                .getFieldValue(snowContentlet, category1Field, APILocator.systemUser(), false);

        assertTrue(cats1.size() == 1);

    }


    private static void _setupCategoryTree() throws DotDataException, DotSecurityException {

        // create category tree
        topSnowWaterCategory = new CategoryDataGen()
                .setCategoryName("topSnowWaterCategory" + unique)
                .setKey("topSnowWaterCategory" + unique)
                .setCategoryVelocityVarName("topSnowWaterCategory" + unique)
                .nextPersisted();

        skiCategory = new CategoryDataGen()
                .setCategoryName("skiCategory" + unique)
                .setKey("skiCategory" + unique)
                .setCategoryVelocityVarName("skiCategory" + unique)
                .parent(topSnowWaterCategory)
                .nextPersisted();

        snowboardCategory = new CategoryDataGen()
                .setCategoryName("snowboardCategory" + unique)
                .setKey("snowboardCategory" + unique)
                .setCategoryVelocityVarName("boardCategory" + unique)
                .parent(topSnowWaterCategory)
                .nextPersisted();

        // Create water categories
        waterCategory = new CategoryDataGen()
                .setCategoryName("waterCategory" + unique)
                .setKey("waterCategory" + unique)
                .setCategoryVelocityVarName("waterCategory" + unique)
                .parent(topSnowWaterCategory)
                .nextPersisted();

        surfCategoryNested = new CategoryDataGen()
                .setCategoryName("surfCategory" + unique)
                .setKey("surfCategory" + unique)
                .setCategoryVelocityVarName("surfCategory" + unique)
                .parent(waterCategory)
                .nextPersisted();

        scubaCategoryNested = new CategoryDataGen()
                .setCategoryName("scubaCategory" + unique)
                .setKey("scubaCategory" + unique)
                .setCategoryVelocityVarName("scubaCategory" + unique)
                .parent(waterCategory)
                .nextPersisted();

        topEcoCategory = new CategoryDataGen()
                .setCategoryName("topEcoCategory" + unique)
                .setKey("topEcoCategory" + unique)
                .setCategoryVelocityVarName("topEcoCategory" + unique)
                .nextPersisted();

        ecoCategory = new CategoryDataGen()
                .setCategoryName("ecoCategory" + unique)
                .setKey("ecoCategory" + unique)
                .setCategoryVelocityVarName("ecoCategory" + unique)
                .parent(topEcoCategory)
                .nextPersisted();

        // set permissions snowRole
        saveIndividualPermissions(topSnowWaterCategory, snowRole, allThePermissions);
        saveInhertablePermissions(topSnowWaterCategory, snowRole, allThePermissions);

        saveIndividualPermissions(skiCategory, snowRole, allThePermissions);
        saveIndividualPermissions(snowboardCategory, snowRole, allThePermissions);

        // set permissions waterRole
        saveIndividualPermissions(topSnowWaterCategory, waterRole, allThePermissions);
        saveInhertablePermissions(topSnowWaterCategory, waterRole, allThePermissions);

        saveIndividualPermissions(waterCategory, waterRole, allThePermissions);
        saveInhertablePermissions(waterCategory, waterRole, allThePermissions);

        // set permissions only eco role for eco category
        saveIndividualPermissions(topEcoCategory, ecoRole, allThePermissions);
        saveInhertablePermissions(topEcoCategory, ecoRole, allThePermissions);


    }


    /**
     * Generate a new role with the given name
     */
    private static Role getOrCreateRole(String roleName) throws DotDataException {
        Role nrole = APILocator.getRoleAPI().loadRoleByKey(roleName);
        if (!UtilMethods.isSet(nrole) || !UtilMethods.isSet(nrole.getId())) {
            nrole = new Role();
            nrole.setName(roleName);
            nrole.setRoleKey(roleName);
            nrole.setEditUsers(true);
            nrole.setEditPermissions(true);
            nrole.setEditLayouts(true);
            nrole.setDescription(roleName);
            nrole = APILocator.getRoleAPI().save(nrole);
        }
        return nrole;
    }


    public static void _insure_category_permissions_before_test() throws DotDataException {

        assertTrue(permissionAPI.doesRoleHavePermission(topSnowWaterCategory, PermissionAPI.PERMISSION_READ, snowRole));

        assertTrue(permissionAPI.doesUserHavePermission(topSnowWaterCategory, PermissionAPI.PERMISSION_READ, snowUser,
                false));
        assertTrue(permissionAPI.doesUserHavePermission(topSnowWaterCategory, PermissionAPI.PERMISSION_READ, waterUser,
                false));
        assertTrue(permissionAPI.doesUserHavePermission(topSnowWaterCategory, PermissionAPI.PERMISSION_READ,
                ecoWaterUser, false));

        assertTrue(permissionAPI.doesUserHavePermission(snowboardCategory, PermissionAPI.PERMISSION_READ, snowUser,
                false));
        assertFalse(permissionAPI.doesUserHavePermission(snowboardCategory, PermissionAPI.PERMISSION_READ, waterUser,
                false));

        assertTrue(
                permissionAPI.doesUserHavePermission(ecoCategory, PermissionAPI.PERMISSION_READ, ecoWaterUser, false));
        assertTrue(permissionAPI.doesUserHavePermission(topEcoCategory, PermissionAPI.PERMISSION_READ, ecoWaterUser,
                false));

        assertFalse(
                permissionAPI.doesUserHavePermission(topEcoCategory, PermissionAPI.PERMISSION_READ, snowUser, false));

    }

    public static void _insure_content_type_permissions() throws DotDataException, DotSecurityException {

        contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find(contentType.variable());
        assertTrue(contentType.fieldMap().containsKey("category1Field"));
        assertTrue(contentType.fieldMap().get("category1Field").fieldVariablesMap()
                .containsKey("secondaryPermissionCheck"));
        assertTrue(contentType.fieldMap().containsKey("category2Field"));
        assertTrue(contentType.fieldMap().get("category2Field").fieldVariablesMap()
                .containsKey("secondaryPermissionCheck"));

        assertNotNull(APILocator.getContentTypeAPI(snowUser).find(contentType.variable()));
        assertNotNull(APILocator.getContentTypeAPI(waterUser).find(contentType.variable()));
        assertNotNull(APILocator.getContentTypeAPI(ecoWaterUser).find(contentType.variable()));

    }

    @Test
    public void test_category_permissions_on_content_not_categorized() throws DotDataException, DotSecurityException {

        assertNotNull(notCategorizedContentlet.getIdentifier());

        // snow user permissions
        assertTrue(
                permissionAPI.doesUserHavePermission(notCategorizedContentlet, PermissionAPI.PERMISSION_READ, snowUser,
                        false));

        // water user has permission
        assertTrue(
                permissionAPI.doesUserHavePermission(notCategorizedContentlet, PermissionAPI.PERMISSION_READ, waterUser,
                        false));

        // eco user has permission
        assertTrue(permissionAPI.doesUserHavePermission(notCategorizedContentlet, PermissionAPI.PERMISSION_READ,
                ecoWaterUser, false));


    }


    @Test
    public void test_categories_pull_correctly() throws DotDataException, DotSecurityException {

        List<Category> cats1 = (List<Category>) APILocator.getContentletAPI()
                .getFieldValue(snowContentlet, category1Field, APILocator.systemUser(), false);

        List<Category> cats2 = (List<Category>) APILocator.getContentletAPI()
                .getFieldValue(snowContentlet, category2Field, APILocator.systemUser(), false);
        assertTrue(cats1.size() == 1);
        assertTrue(cats1.get(0).getKey().equals(snowboardCategory.getKey()));
        assertTrue(cats2.size() == 0);

        cats1 = (List<Category>) APILocator.getContentletAPI()
                .getFieldValue(snowContentlet, category1Field, ecoWaterUser, false);
        assertEquals(0, cats1.size());

    }

    /**
     * Method to test: {@link PermissionBitAPIImpl#checkCategoryPermissionsSecond(Contentlet, int, User, boolean)} Given
     * Scenario: Test various scenarios for category permission checking on contentlets ExpectedResult: Method should
     * return appropriate boolean values based on user's category permissions
     */
    @Test
    public void test_category_permissions_1_field() throws DotDataException, DotSecurityException {

        // snow user YES permission snowContentlet
        assertTrue(
                permissionAPI.doesUserHavePermission(snowContentlet, PermissionAPI.PERMISSION_READ, snowUser, false));

        // water user NO permission snowContentlet
        assertFalse(
                permissionAPI.doesUserHavePermission(snowContentlet, PermissionAPI.PERMISSION_READ, waterUser, false));

        // eco user NO permission snowContentlet
        assertFalse(permissionAPI.doesUserHavePermission(snowContentlet, PermissionAPI.PERMISSION_READ, ecoWaterUser,
                false));

        // snow user NO permission ecoContentlet
        assertFalse(
                permissionAPI.doesUserHavePermission(ecoContentlet, PermissionAPI.PERMISSION_READ, snowUser, false));

        // water user NO permission ecoContentlet
        assertFalse(
                permissionAPI.doesUserHavePermission(ecoContentlet, PermissionAPI.PERMISSION_READ, waterUser, false));

        // eco user YES permission ecoContentlet
        assertTrue(permissionAPI.doesUserHavePermission(ecoContentlet, PermissionAPI.PERMISSION_READ, ecoWaterUser,
                false));

        // snow user NO permission ecoWaterContentlet
        assertFalse(permissionAPI.doesUserHavePermission(ecoWaterContentlet, PermissionAPI.PERMISSION_READ, snowUser,
                false));

        // water user NO permission ecoWaterContentlet
        assertFalse(permissionAPI.doesUserHavePermission(ecoWaterContentlet, PermissionAPI.PERMISSION_READ, waterUser,
                false));

        // eco user YES permission ecoWaterContentlet
        assertTrue(permissionAPI.doesUserHavePermission(ecoWaterContentlet, PermissionAPI.PERMISSION_READ, ecoWaterUser,
                false));


    }

    private static void saveInhertablePermissions(Permissionable permissionable, Role role, int permissionBit)
            throws DotDataException, DotSecurityException {

        permissionAPI.save(
                new Permission(permissionable.getClass().getCanonicalName(), permissionable.getPermissionId(),
                        role.getId(), permissionBit),
                permissionable, APILocator.systemUser(), false);

    }

    private static void saveIndividualPermissions(Permissionable permissionable, Role role, int permissionBit)
            throws DotDataException, DotSecurityException {

        permissionAPI.save(
                new Permission(permissionable.getPermissionId(),
                        role.getId(), permissionBit),
                permissionable, APILocator.systemUser(), false);

    }


    @Test
    public void test_opensearch_query_respects_permissions() throws Exception {

        // we added 4 content
        List<Contentlet> results = APILocator.getContentletAPI()
                .search("+contentType:" + contentType.variable(), 100, 0, "modDate", APILocator.systemUser(), false);
        assertEquals(4, results.size());

        ESMappingAPIImpl mapping = new ESMappingAPIImpl();

        Map<String, Object> mappingMap = mapping.toMap(notCategorizedContentlet);

        // snowUser can see 2 of them (not categorized and Snow categorized)
        results = APILocator.getContentletAPI()
                .search("+contentType:" + contentType.variable(), 100, 0, "modDate", snowUser, false);
        assertEquals(2, results.size());

        // waterUser can see 2 of them (not categorized and Water categorized)
        results = APILocator.getContentletAPI()
                .search("+contentType:" + contentType.variable(), 100, 0, "modDate", waterUser, false);
        assertEquals(2, results.size());

        // ecoWaterUser can see 1 of them (not categorized, Eco categorized and EcoWater categorized)
        results = APILocator.getContentletAPI()
                .search("+contentType:" + contentType.variable(), 100, 0, "modDate", ecoWaterUser, false);
        assertEquals(3, results.size());


    }


}
