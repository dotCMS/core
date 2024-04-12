package com.dotmarketing.portlets.browser.ajax;

import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.rest.api.v1.browsertree.BrowserTreeHelper;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ConfigTestHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentAsDraftActionlet;
import com.dotmarketing.portlets.workflows.actionlet.VelocityScriptActionlet;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ThreadUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.FileUtil;
import com.liferay.util.servlet.SessionMessages;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This test class validates the correct behavior of the methods exposed in the {@link BrowserAjax} class.
 *
 * @author Jose Castro
 * @since Apr 18th, 2023
 */
public class BrowserAjaxTest extends BaseWorkflowIntegrationTest {

    private static Host testSite = null;
    private static Folder parentFolderOne = null;
    private static Folder parentFolderTwo = null;
    private static Folder childFolderOne = null;
    private static Folder childFolderTwo = null;

    private static HttpSession session = null;

    /**
     * Set up the DWR context and data for this test. The Site's folder tree looks like this:
     *
     * |_test-site
     *      |_ parent-one
     *          |_child-one
     *              |_child-two
     *      |_parent-two
     *
     * @throws Exception An error occurred during the setup.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
        final long time = System.currentTimeMillis();
        testSite = new SiteDataGen().name("browserajaxtest." + time).nextPersisted();

        parentFolderOne = new FolderDataGen().site(testSite).name("parent-one").nextPersisted();
        parentFolderTwo = new FolderDataGen().site(testSite).name("parent-two").nextPersisted();

        childFolderOne = new FolderDataGen().site(testSite).parent(parentFolderOne).name("child-one").nextPersisted();
        childFolderTwo = new FolderDataGen().site(testSite).parent(childFolderOne).name("child-two").nextPersisted();

        setUpDwrContext(APILocator.getUserAPI().getSystemUser());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getTree(String)}</li>
     *     <li><b>Given Scenario:</b> Display the folder tree for a specific Site in the Site Browser.</li>
     *     <li><b>Expected Result:</b> If a Site is set to be displayed in the Site Browser, the returned data tree must
     *     reflect only the direct child Folders under such a Site in the UI.</li>
     * </ul>
     * This is how the folder tree must look like:
     * <pre>
     *      --> test-site [open]
     *          |_ parent-one
     *          |_ parent-two
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testSelectingSiteRoot() throws Exception {
        // Set test values in session
        when(session.getAttribute(BrowserTreeHelper.ACTIVE_FOLDER_ID)).thenReturn(null);
        when(session.getAttribute(BrowserTreeHelper.OPEN_FOLDER_IDS)).thenReturn(new HashSet<>());

        final BrowserAjax browserAjax = new BrowserAjax();
        final List<Map> folderTree = browserAjax.getTree(testSite.getIdentifier());

        // Check the main Site folder tree structure
        // Site MUST be "open"
        assertNotNull("Folder tree must never be null", folderTree);
        assertEquals("There must be only one Site folder tree", 1, folderTree.size());
        final Map<String, Object> siteFolderTree = folderTree.get(0);
        assertEquals("The returned Site ID [" + siteFolderTree.get("identifier") + "] doesn't match the ID of the " +
                             "test Site [" + testSite.getIdentifier() + "]", testSite.getIdentifier(),
                siteFolderTree.get("identifier"));
        assertTrue("The Site node must be open", (boolean) siteFolderTree.get("open"));

        // Only parent-one MUST be open
        // parent-two MUST NOT be neither "open" nor "selected"
        final List<Map<String, Object>> parentFolders = (List<Map<String, Object>>) siteFolderTree.get("childrenFolders");
        assertEquals("There must be two parent Folders for the Test Site", 2, parentFolders.size());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getTree(String)}</li>
     *     <li><b>Given Scenario:</b> The grandchild of a folder has been selected via the HTTP Session so that the Site
     *     Browser displays it in the UI.</li>
     *     <li><b>Expected Result:</b> If a sub-folder is set to be displayed in the Site Browser, the returned data
     *     tree must reflect both its selected status and open up all the respective parent folders so that it can be
     *     seen correctly in the UI.</li>
     * </ul>
     * This is how the folder tree must look like:
     * <pre>
     *      --> test-site [open]
     *          |_ parent-one [open]
     *              |_child-one [open]
     *                  |_child-two [open, selected]
     *          |_ parent-two
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void testExpectedFolderTreeInfo() throws Exception {
        // Set test values in session
        when(session.getAttribute(BrowserTreeHelper.ACTIVE_FOLDER_ID)).thenReturn(childFolderTwo.getIdentifier());
        final Set<String> openFolderIds = CollectionsUtils.set(parentFolderOne.getIdentifier(),
                childFolderOne.getIdentifier(), childFolderTwo.getIdentifier());
        when(session.getAttribute(BrowserTreeHelper.OPEN_FOLDER_IDS)).thenReturn(openFolderIds);

        final BrowserAjax browserAjax = new BrowserAjax();
        final List<Map> folderTree = browserAjax.getTree(testSite.getIdentifier());

        // Only parent-one MUST be open
        // parent-two MUST NOT be neither "open" nor "selected"
        final Map<String, Object> siteFolderTree = folderTree.get(0);
        final List<Map<String, Object>> parentFolders = (List<Map<String, Object>>) siteFolderTree.get("childrenFolders");
        assertEquals("There must be two parent Folders for the Test Site", 2, parentFolders.size());
        assertTrue("Folder '" + parentFolderOne.getPath() + "' must be open", (boolean) parentFolders.get(0).get("open"));
        assertFalse("Folder '" + parentFolderOne.getPath() + "' must NOT be selected", (boolean) parentFolders.get(0).get("selected"));

        assertFalse("Folder '" + parentFolderTwo.getPath() + "' must NOT be open", (boolean) parentFolders.get(1).get("open"));
        assertFalse("Folder '" + parentFolderTwo.getPath() + "' must NOT be selected", (boolean) parentFolders.get(1).get("selected"));

        // Check the parent-one folder tree structure
        final List<Map<String, Object>> parentFolderOneData = (List<Map<String, Object>>) parentFolders.get(0).get("childrenFolders");
        assertEquals("There must be only one child folder under '" + parentFolderOne.getPath() + "'", 1,
                parentFolderOneData.size());

        // Check the child-one folder tree structure
        // child-one MUST be "open" but NOT "selected"
        final Map<String, Object> childFolderOneData = parentFolderOneData.get(0);
        assertEquals("The returned Child One ID [" + childFolderOneData.get("identifier") + "] doesn't match the ID of the " +
                             "test Child One [" + childFolderOne.getIdentifier() + "]", childFolderOne.getIdentifier(),
                childFolderOneData.get("identifier"));
        assertTrue("Folder '" + childFolderOne.getPath() + "' must be open", (boolean) childFolderOneData.get("open"));
        assertFalse("Folder '" + childFolderOne.getPath() + "' must NOT be selected", (boolean) childFolderOneData.get("selected"));

        // Check the child-one folder tree structure
        final List<Map<String, Object>> parentFolderTwoData = (List<Map<String, Object>>) childFolderOneData.get("childrenFolders");
        assertEquals("There must be only one child folder under '" + childFolderOne.getPath() + "'", 1,
                parentFolderOneData.size());

        // Check the child-two folder tree structure
        // child-two MUST be "open" AND "selected"
        final Map<String, Object> childFolderTwoData = parentFolderTwoData.get(0);
        assertEquals("The returned Child Two ID [" + childFolderTwoData.get("identifier") + "] doesn't match the ID of the " +
                             "test Child Two [" + childFolderTwo.getIdentifier() + "]", childFolderTwo.getIdentifier(),
                childFolderTwoData.get("identifier"));
        assertTrue("Folder '" + childFolderTwo.getPath() + "' must be open", (boolean) childFolderTwoData.get("open"));
        assertTrue("Folder '" + childFolderTwo.getPath() + "' must be selected", (boolean) childFolderTwoData.get("selected"));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getTree(String)}</li>
     *     <li><b>Given Scenario:</b> The grandchild of a folder has been selected via the HTTP Session so that the Site
     *     Browser displays it in the UI.</li>
     *     <li><b>Expected Result:</b> If a sub-folder is set to be displayed in the Site Browser, the returned data
     *     tree must reflect both its selected status and open up all the respective parent folders so that it can be
     *     seen correctly in the UI.</li>
     * </ul>
     * This is how the folder tree must look like:
     * <pre>
     *      --> test-site [open]
     *          |_ parent-one
     *          |_ parent-two
     * </pre>
     * @throws Exception
     */
    @Test
    public void testFolderTreeInfoWithInvalidFolderId() throws Exception {
        // Set test values in session
        when(session.getAttribute(BrowserTreeHelper.ACTIVE_FOLDER_ID)).thenReturn("123");
        when(session.getAttribute(BrowserTreeHelper.OPEN_FOLDER_IDS)).thenReturn(new HashSet<>());

        final BrowserAjax browserAjax = new BrowserAjax();
        final List<Map> folderTree = browserAjax.getTree(testSite.getIdentifier());

        // Only parent-one MUST be open
        // parent-two MUST NOT be neither "open" nor "selected"
        final Map<String, Object> siteFolderTree = folderTree.get(0);
        final List<Map<String, Object>> parentFolders = (List<Map<String, Object>>) siteFolderTree.get("childrenFolders");
        assertEquals("There must be two parent Folders for the Test Site", 2, parentFolders.size());
        assertFalse("Folder '" + parentFolderOne.getPath() + "' must NOT be open", (boolean) parentFolders.get(0).get("open"));
        assertFalse("Folder '" + parentFolderOne.getPath() + "' must NOT be selected", (boolean) parentFolders.get(0).get("selected"));

        assertFalse("Folder '" + parentFolderTwo.getPath() + "' must NOT be open", (boolean) parentFolders.get(1).get("open"));
        assertFalse("Folder '" + parentFolderTwo.getPath() + "' must NOT be selected", (boolean) parentFolders.get(1).get("selected"));
    }

    /**
     * Sets up the DWR context using Mockito so that the {@link BrowserAjax} class can be tested correctly.
     */
    private static void setUpDwrContext(User user) throws DotDataException, DotSecurityException {
        session = mock(HttpSession.class);
        when(session.getAttribute(SessionMessages.KEY)).thenReturn(new LinkedHashMap());

        final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getSession()).thenReturn(session);

        when(httpServletRequest.getAttribute(WebKeys.USER)).thenReturn(user);

        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(httpServletRequest);

        final WebContextFactory.WebContextBuilder webContextBuilderMock =
                mock(WebContextFactory.WebContextBuilder.class);
        when(webContextBuilderMock.get()).thenReturn(webContext);

        final com.dotcms.repackage.org.directwebremoting.Container containerMock =
                mock(com.dotcms.repackage.org.directwebremoting.Container.class);
        when(containerMock.getBean(WebContextFactory.WebContextBuilder.class)).thenReturn(webContextBuilderMock);

        WebContextFactory.attach(containerMock);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#openFolderContent(String, String, boolean, long)}</li>
     *     <li><b>Given Scenario:</b> Opening a Folder to view their items</li>
     *     <li><b>Expected Result:</b> The items should be sorted by modDate descending by default</li>
     * </ul>
     * @throws Exception
     */
    @Test
    public void test_openFolderContent_defaultBehavior() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();

        final Folder mainFolder = new FolderDataGen().site(host).nextPersisted();

        final Link link = new LinkDataGen().parent(mainFolder).nextPersisted(false);
        ThreadUtils.sleep(1000);
        final Template templateDataGen = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(mainFolder, templateDataGen).nextPersisted();
        ThreadUtils.sleep(1000);
        final Folder subFolder = new FolderDataGen().parent(mainFolder).nextPersisted();


        final BrowserAjax browserAjax = new BrowserAjax();
        final List<Map<String, Object>> folderContent = browserAjax.openFolderContent(mainFolder.getIdentifier(),
                "",false,APILocator.getLanguageAPI().getDefaultLanguage().getId());
        //The order should be subfolder, page, link.
        assertTrue(folderContent.get(0).get("identifier").equals(subFolder.getIdentifier()));
        assertTrue(folderContent.get(1).get("identifier").equals(page.getIdentifier()));
        assertTrue(folderContent.get(2).get("identifier").equals(link.getIdentifier()));
    }

    //crete the test for the method public List<Map<String, Object>> getFolderSubfolders(final String parentFolderId)
    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getFolderSubfolders(String)}</li>
     *     <li><b>Given Scenario: The folder is sorted by Title not Name,
     *     subfolders in content search are not sorted alphabetically if the folder Name is changed</b> </li>
     *     <li><b>Expected Result:The subfolder list should be sorted alphabetically </b> </li>
     * </ul>
     * @throws Exception
     */
    @Test
    public void test_getFolderSubfolders() throws Exception {
        setUpDwrContext(APILocator.getUserAPI().getSystemUser());
        final User sysUser = APILocator.getUserAPI().getSystemUser();
        final Host host = new SiteDataGen().nextPersisted();
        final Folder mainFolder = new FolderDataGen().name("mainFolder"+System.currentTimeMillis()).site(host).nextPersisted();
        final Folder subFolderC = new FolderDataGen().name("c"+System.currentTimeMillis()).parent(mainFolder).nextPersisted();
        final Folder subFolderB = new FolderDataGen().name("b"+System.currentTimeMillis()).parent(mainFolder).nextPersisted();
        final Folder subFolderA = new FolderDataGen().name("a"+System.currentTimeMillis()).parent(mainFolder).nextPersisted();
        final BrowserAjax browserAjax = new BrowserAjax();
        //method to test
        List<Map<String, Object>> folderContent = browserAjax.getFolderSubfolders(mainFolder.getIdentifier());
        //assert all the subfolders are alphabetical sorted by name
        assertEquals(folderContent.get(0).get("name"),subFolderA.getName());
        //rename the subfolderA to z...
        APILocator.getFolderAPI().renameFolder(subFolderA, "z"+System.currentTimeMillis(), sysUser , false);
        //method to test
        folderContent = browserAjax.getFolderSubfolders(mainFolder.getIdentifier());
        //now to subFolderA should be the last one
        assertNotEquals(folderContent.get(0).get("name"), subFolderA.getName());
        assertEquals(folderContent.get(folderContent.size()-1).get("name"),subFolderA.getName());
    }


    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#getHosts()}</li>
     *     <li><b>Given Scenario: Limited users should be able to create content types on System Host if given the proper permissions</b> </li>
     *     <li><b>Expected Result:The System host should appear in the list of host </b> </li>
     * </ul>
     * @throws Exception
     */
    @Test
    public void test_getHosts_ShoudlRetrieveSystemHosts() throws DotDataException, DotSecurityException, SystemException, PortalException {
        final BrowserAjax browserAjax = new BrowserAjax();
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

        //create the user
        final User user =TestUserUtils.getChrisPublisherUser();
        //assign permissions to system host
        final Permission permissions = new Permission(Host.class.getCanonicalName(),
                APILocator.systemHost().getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(user.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT, true);
        permissionAPI.save(permissions, APILocator.systemHost(), APILocator.getUserAPI().getSystemUser(), false);
        //set up the dwr context
        setUpDwrContext(user);
        //get the logged-in user
        WebContext ctx = WebContextFactory.get();
        UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
        User loggedInUser = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
        Host nsite = new SiteDataGen().nextPersisted();
        //method to test
        List<Map<String, Object>> hosts = browserAjax.getHosts();
        assertNotNull(hosts);
        assertTrue(hosts.size() > 0);
        //should contain the system host as a limited user
        assertTrue(hosts.stream().anyMatch(host -> host.get("identifier").equals(APILocator.systemHost().getIdentifier())) && loggedInUser.getFirstName().equals(user.getFirstName()) );
        setUpDwrContext(APILocator.getUserAPI().getSystemUser());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAjax#saveFileAction(String,String,String,String,String,String,String,String,String,String,String,String,String)}</li>
     *     <li><b>Given Scenario: The user is able to execute a workflow action</b> </li>
     *     <li><b>Expected Result:The action should be executed successfully </b> </li>
     * </ul>
     * @throws Exception An error occurred trying to execute the workflow action.
     */
    @Test
    public void testExecuteWorkflowAction() throws Exception {

        WorkflowScheme workflowScheme = null;
        Category rootCategory = null;
        ContentType contentType = null;
        Contentlet testContent = null;
        try {
            setUpDwrContext(APILocator.getUserAPI().getSystemUser());

            // creates the test categories
            final Category childCategory1 = createTestCategory(
                    "BrowserAjaxWorkflowTestChildCat1", 1).next();
            final Category childCategory2 = createTestCategory(
                    "BrowserAjaxWorkflowTestChildCat2", 2).next();
            rootCategory = createTestCategory(
                    "BrowserAjaxWorkflowTestRootCat", 0)
                    .children(childCategory1, childCategory2).nextPersisted();

            // creates the workflow scheme and actions
            final String schemeName = "BrowserAjaxTestScheme" + UUIDGenerator.generateUuid();
            final CreateSchemeStepActionResult schemeStepFirstActionResult =
                    createSchemeStepActionActionlet(
                        schemeName, "step1", "action1",
                        SaveContentAsDraftActionlet.class);
            workflowScheme = schemeStepFirstActionResult.getScheme();
            final WorkflowStep workflowStep = schemeStepFirstActionResult.getStep();
            final WorkflowAction saveDraftAction = schemeStepFirstActionResult.getAction();

            final CreateSchemeStepActionResult schemeStepVelocityActionResult =
                    createActionActionlet(workflowScheme.getId(),
                        workflowStep.getId(), "action2",
                        VelocityScriptActionlet.class);

            saveActionletScriptCode(schemeStepVelocityActionResult, rootCategory);

            final String veloctiyActionId = schemeStepVelocityActionResult.getAction().getId();
            addActionletToAction(veloctiyActionId, SaveContentActionlet.class, 1);

            // creates content type with categories
            contentType = createTestTypeWithCategories(
                    "BrowserAjaxWorkflowTestType" + System.currentTimeMillis(),
                    workflowScheme.getId(),
                    rootCategory.getInode());

            // create test content
            testContent = createTestContentletWithCategories(contentType,
                    new ArrayList<>(List.of(childCategory1, childCategory2)),
                    saveDraftAction.getId());

            // execute the workflow action
            final String contentId = testContent.getInode();
            final BrowserAjax browserAjax = new BrowserAjax();
            final Map<String, Object> resultMap = browserAjax.saveFileAction(
                    "", "", veloctiyActionId, "", contentId,
                    "", "", "", "", "",
                    "", "", "");

            assertNotNull(resultMap);
            assertEquals("success", resultMap.get("status"));

            // Check that result code in contentlet contains category names
            final Contentlet resultContent = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(
                    testContent.getIdentifier());
            assertNotNull(resultContent);
            final Object resultObj = resultContent.get ("result");
            assertNotNull(resultObj);
            final Map<?, ?> resultCodeMap = (Map<?, ?>) resultObj;
            final Object outputCodeObj = resultCodeMap.get("output");
            assertNotNull(outputCodeObj);
            final String resultCode = outputCodeObj.toString();

            assertTrue(resultCode.contains(childCategory1.getCategoryName()));
            assertTrue(resultCode.contains(childCategory2.getCategoryName()));

        } finally {
            if (null != testContent) {
                ContentletDataGen.remove(testContent);
            }
            if (null != contentType) {
                ContentTypeDataGen.remove(contentType);
            }
            if (null != rootCategory) {
                APILocator.getCategoryAPI().delete(
                        rootCategory, APILocator.systemUser(), false);
            }
            if (null != workflowScheme) {
                cleanScheme(workflowScheme);
            }
        }
    }

    /**
     * Creates a workflow scheme, with a step, an action and a velocity actionlet.
     *
     * @param schemeStepActionResult The result of creating the scheme step.
     * @param rootCategory The root category.
     * @throws Exception An error occurred trying to create the scheme step.
     */
    private void saveActionletScriptCode(
            final CreateSchemeStepActionResult schemeStepActionResult,
            final Category rootCategory)
            throws Exception {

        final String code = FileUtil.read(ConfigTestHelper.getPathToTestResource(
                "com/dotmarketing/portlets/browser/ajax/list-categories.vtl"))
                .replace("{{rootCategoryKey}}", rootCategory.getKey());

        final WorkflowActionClass workflowActionClass = schemeStepActionResult.getActionClass();
        final List<WorkflowActionClassParameter> params = new ArrayList<>();
        final User user = APILocator.systemUser();

        final WorkflowActionClassParameter parameter = new WorkflowActionClassParameter();
        parameter.setActionClassId(workflowActionClass.getId());
        parameter.setKey("script");
        parameter.setValue(code);
        params.add(parameter);

        final WorkflowActionClassParameter parameterResult = new WorkflowActionClassParameter();
        parameterResult.setActionClassId(workflowActionClass.getId());
        parameterResult.setKey("resultKey");
        parameterResult.setValue("result");
        params.add(parameterResult);

        APILocator.getWorkflowAPI().saveWorkflowActionClassParameters(params, user);

    }

    /**
     * Creates a test category.
     * @param categoryNamePrefix The prefix for the category name.
     * @param sortOrder The sort order for the category.
     * @return The category data generator.
     */
    private CategoryDataGen createTestCategory(
            final String categoryNamePrefix, final int sortOrder) {
        final String categoryName = categoryNamePrefix + System.currentTimeMillis();
        final String categoryKey = categoryName.toLowerCase().replace("\\s", "");

        return new CategoryDataGen().setCategoryName(categoryName)
                .setKey(categoryKey).setCategoryVelocityVarName(categoryKey)
                .setSortOrder(sortOrder);

    }

    /**
     * Creates a content type with categories and assigns it to a workflow scheme.
     * @param contentTypeName The name of the content type.
     * @param workflowSchemeId The ID of the workflow scheme.
     * @param categoryId The ID of the root category.
     * @return The created content type.
     * @throws Exception An error occurred trying to create the content type.
     */
    private ContentType createTestTypeWithCategories(
            final String contentTypeName, final String workflowSchemeId, final String categoryId)
            throws Exception {

        // Create content type
        ContentType contentType = new ContentTypeDataGen()
                .name(contentTypeName)
                .baseContentType(BaseContentType.CONTENT)
                .workflowId(workflowSchemeId)
                .nextPersisted();

        // Add fields to the contentType
        final Field titleField =
                FieldBuilder.builder(TextField.class).name("Title")
                        .contentTypeId(contentType.id())
                        .variable("title")
                        .indexed(true)
                        .required(true)
                        .dataType(DataTypes.TEXT).build();

        final Field categoryField =
                FieldBuilder.builder(CategoryField.class).name("TestCategory")
                        .contentTypeId(contentType.id())
                        .indexed(true)
                        .required(true)
                        .values(categoryId).build();

        final Field resultField =
                FieldBuilder.builder(KeyValueField.class).name("Result")
                        .contentTypeId(contentType.id())
                        .variable("result")
                        .required(false)
                        .indexed(false)
                        .build();

        contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).save(
                contentType, List.of(titleField, categoryField, resultField));

        return contentType;

    }

    /**
     * Creates a test contentlet with categories.
     * @param contentType The content type.
     * @param categoryList The list of categories.
     * @param actionId The ID of the action to be executed.
     * @return The created contentlet.
     * @throws Exception An error occurred trying to create the contentlet.
     */
    private Contentlet createTestContentletWithCategories(
            final ContentType contentType, final List<Category> categoryList,
            final String actionId) throws Exception {

        final Contentlet testContent = new ContentletDataGen(contentType.id())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty("title", "BrowserAjaxWorkflowTestContent" + System.currentTimeMillis())
                .next();
        return APILocator.getWorkflowAPI().fireContentWorkflow(testContent,
            new ContentletDependencies.Builder()
                    .modUser(APILocator.getUserAPI().getSystemUser())
                    .respectAnonymousPermissions(false)
                    .workflowActionId(actionId)
                    .categories(categoryList)
                    .indexPolicy(IndexPolicy.FORCE)
                    .indexPolicyDependencies(IndexPolicy.FORCE)
                    .build());

    }

}
