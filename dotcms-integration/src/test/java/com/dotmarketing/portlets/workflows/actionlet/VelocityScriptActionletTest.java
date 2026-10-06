package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.rendering.velocity.viewtools.secrets.DotVelocitySecretAppConfigThreadLocal;
import com.dotcms.rendering.velocity.viewtools.secrets.DotVelocitySecretAppKeys;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VelocityScriptActionletTest extends BaseWorkflowIntegrationTest {

    private static CreateSchemeStepActionResult schemeStepActionResult = null;
    private static WorkflowAPI workflowAPI = null;
    private static ContentletAPI contentletAPI = null;
    private static ContentTypeAPI contentTypeAPI = null;
    private static ContentType customContentType = null;
    private static final int LIMIT = 20;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        contentletAPI = APILocator.getContentletAPI();

        // creates the scheme and actions
        schemeStepActionResult = createSchemeStepActionActionlet
                ("VelocityScriptActionlet" + UUIDGenerator.generateUuid(), "step1", "action1",
                        VelocityScriptActionlet.class);

        // creates the type to trigger the scheme
        customContentType = createTestType(contentTypeAPI);
        saveActionletScriptCode(schemeStepActionResult);

        // associated the scheme to the type
        workflowAPI.saveSchemesForStruct(new StructureTransformer(customContentType).asStructure(),
                List.of(schemeStepActionResult.getScheme()));

        setDebugMode(false);
    }

    private static void saveActionletScriptCode(final CreateSchemeStepActionResult schemeStepActionResult) throws DotDataException {

        final String code =  "#set($title = $content.getTitle()) \n" +
                    "#set($pow = $math.pow(2, 3)) \n" +
                    "#set($title = \"$title $pow\") \n" +
                    "$content.getMap().put(\"title\",\"$title\") \n" +
                    "$dotJSON.put(\"title\",$title) \n";
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
        workflowAPI.saveWorkflowActionClassParameters(params, user);
    }

    private static ContentType createTestType(final ContentTypeAPI contentTypeAPI)
            throws DotDataException, DotSecurityException {

        final ContentType type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .description("Dot..")
                        .name("DotVelocityActionletTest" + System.currentTimeMillis())
                        .owner(APILocator.systemUser().toString())
                        .variable("DotVelocityActionletTest" + System.currentTimeMillis()).build());

        final List<Field> fields = new ArrayList<>(type.fields());

        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

        return contentTypeAPI.save(type, fields);
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup()
            throws DotDataException, DotSecurityException, AlreadyExistException {

        if (null != customContentType) {

            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
            contentTypeAPI.delete(customContentType);
        }

        if (null != schemeStepActionResult) {

            cleanScheme(schemeStepActionResult.getScheme());
        }

        cleanupDebug(VelocityScriptActionletTest.class);
    } // cleanup

    /**
     * Given Scenario: background/scheduled-job context. SiteA and System Host sites each with same secret name but different value
     * Expected result: {@code $dotsecrets.get("mySecret")} returns the siteA value, not the System Host value.
     */
    @Test
    public void Test_Velocity_Script_Get_ResolvesSiteSpecificSecret_InBackgroundJob() throws Exception {

        final User admin = APILocator.systemUser();
        final AppsAPI appsAPI = APILocator.getAppsAPI();
        final Host siteA = new SiteDataGen().nextPersisted();
        WorkflowScheme secretsScheme = null;
        ContentType secretsContentType = null;
        Contentlet checkedIn = null;

        // Save the original thread-local request and clear it to simulate a background job
        final HttpServletRequest originalRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        // Clear the per-thread secrets cache so stale entries do not interfere
        DotVelocitySecretAppConfigThreadLocal.INSTANCE.clearConfig();

        try {
            // Configure dotVelocitySecretApp for siteA
            final AppSecrets siteASecrets = new AppSecrets.Builder()
                    .withKey(DotVelocitySecretAppKeys.APP_KEY)
                    .withSecret(DotVelocitySecretAppKeys.TITLE.key, "SiteA Config")
                    .withHiddenSecret("mySecret", "secretValueSiteA")
                    .build();
            appsAPI.saveSecrets(siteASecrets, siteA, admin);

            // Configure dotVelocitySecretApp for System Host with a different value
            final AppSecrets systemSecrets = new AppSecrets.Builder()
                    .withKey(DotVelocitySecretAppKeys.APP_KEY)
                    .withSecret(DotVelocitySecretAppKeys.TITLE.key, "System Config")
                    .withHiddenSecret("mySecret", "secretValueSystemHost")
                    .build();
            appsAPI.saveSecrets(systemSecrets, APILocator.systemHost(), admin);

            // Create a content type and workflow scheme for this test
            secretsContentType = contentTypeAPI.save(
                    ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                            .folder(FolderAPI.SYSTEM_FOLDER)
                            .host(siteA.getIdentifier())
                            .name("SecretsTest" + System.currentTimeMillis())
                            .variable("SecretsTest" + System.currentTimeMillis())
                            .build());

            final CreateSchemeStepActionResult secretsSchemeResult = createSchemeStepActionActionlet(
                    "SecretsScheme" + UUIDGenerator.generateUuid(), "step1", "action1",
                    VelocityScriptActionlet.class);
            secretsScheme = secretsSchemeResult.getScheme();

            // Script reads the site-specific secret and stores it in dotJSON for assertion
            final String script = "$dotJSON.put(\"secretValue\", $dotsecrets.get(\"mySecret\"))";
            final WorkflowActionClass actionClass = secretsSchemeResult.getActionClass();
            final List<WorkflowActionClassParameter> params = new ArrayList<>();
            final WorkflowActionClassParameter scriptParam = new WorkflowActionClassParameter();
            scriptParam.setActionClassId(actionClass.getId());
            scriptParam.setKey("script");
            scriptParam.setValue(script);
            params.add(scriptParam);
            final WorkflowActionClassParameter resultKeyParam = new WorkflowActionClassParameter();
            resultKeyParam.setActionClassId(actionClass.getId());
            resultKeyParam.setKey("resultKey");
            resultKeyParam.setValue("result");
            params.add(resultKeyParam);
            workflowAPI.saveWorkflowActionClassParameters(params, admin);

            workflowAPI.saveSchemesForStruct(
                    new StructureTransformer(secretsContentType).asStructure(),
                    List.of(secretsScheme));

            // Create a contentlet assigned to siteA
            final Contentlet contentlet = new Contentlet();
            contentlet.setContentType(secretsContentType);
            contentlet.setHost(siteA.getIdentifier());
            checkedIn = contentletAPI.checkin(contentlet, admin, false);

            // Fire the workflow action — no HTTP request in thread-local (background job)
            final Contentlet result = workflowAPI.fireContentWorkflow(checkedIn,
                    new ContentletDependencies.Builder()
                            .modUser(admin)
                            .workflowActionId(secretsSchemeResult.getAction().getId())
                            .build());

            Assert.assertNotNull(result);
            final Map<String, Object> resultMap = (Map<String, Object>) result.get("result");
            Assert.assertNotNull("Actionlet must store a result map", resultMap);
            final DotJSON dotJSON = (DotJSON) resultMap.get("dotJSON");
            Assert.assertNotNull("dotJSON must be present in the result", dotJSON);
            Assert.assertEquals(
                    "Expected site-specific secret from siteA, not System Host fallback",
                    "secretValueSiteA", dotJSON.get("secretValue"));

        } finally {
            DotVelocitySecretAppConfigThreadLocal.INSTANCE.clearConfig();

            // Destroy the contentlet first so it is no longer in the workflow step,
            // which would otherwise block cleanScheme() from deleting the step.
            try {
                if (null != checkedIn) {
                    contentletAPI.destroy(checkedIn, admin, false);
                }
            } catch (Exception ignored) { /* best-effort */ }

            try { appsAPI.deleteSecrets(DotVelocitySecretAppKeys.APP_KEY, siteA, admin); } catch (Exception ignored) {}
            try { appsAPI.deleteSecrets(DotVelocitySecretAppKeys.APP_KEY, APILocator.systemHost(), admin); } catch (Exception ignored) {}

            if (null != secretsScheme) { cleanScheme(secretsScheme); }
            if (null != secretsContentType) { contentTypeAPI.delete(secretsContentType); }

            try {
                APILocator.getHostAPI().archive(siteA, admin, false);
                APILocator.getHostAPI().delete(siteA, admin, false);
            } catch (Exception ignored) { /* best-effort */ }

            // Restore thread-local last so all cleanup above runs under the background-job context
            HttpServletRequestThreadLocal.INSTANCE.setRequest(originalRequest);
        }
    }

    @Test
    public void Test_Velocity_Script_Actionlet_Expect_Success() throws Exception {

        final Contentlet contentlet = new Contentlet();

        contentlet.setContentType(customContentType);
        contentlet.setProperty("title", "Test");
        contentlet.setProperty("txt", "Test Txt");

        final Contentlet checkinContentlet = contentletAPI.checkin(contentlet, APILocator.systemUser(), false);
        final Contentlet resultContentlet = workflowAPI.fireContentWorkflow(checkinContentlet, new ContentletDependencies.Builder()
                .modUser(APILocator.systemUser())
                .workflowActionId(schemeStepActionResult.getAction().getId()).build());

        final String expectedTitle = "Test 8";
        Assert.assertNotNull(resultContentlet);
        Assert.assertNotNull(resultContentlet.getTitle());
        Assert.assertNotNull(resultContentlet.getMap().get("result"));
        Assert.assertEquals(expectedTitle, resultContentlet.getTitle());
        final Map<String, Object> result  = (Map)resultContentlet.get("result");
        Assert.assertFalse(result.isEmpty());
        Assert.assertTrue(result.containsKey("dotJSON"));
        Assert.assertEquals(expectedTitle, DotJSON.class.cast(result.get("dotJSON")).get("title"));
    }

    /**
     * Regression test for #35347. The mock request the actionlet builds for the script
     * engine had no {@code WebKeys.USER} attribute, so any viewtool resolving the user via
     * {@code PortalUtil.getUser(req)} — including {@code WorkflowTool.init()} — fell back
     * to the anonymous user and {@code $workflowtool.fire(...)} 403'd whenever the target
     * action wasn't readable by anonymous. The fix sets {@code WebKeys.USER} on the mock
     * request before the script runs; this test reads that attribute back from inside the
     * script and asserts it resolves to the firing user.
     */
    @Test
    public void Test_Velocity_Script_Actionlet_PropagatesTriggeringUser_OntoMockRequest() throws Exception {

        final User admin = APILocator.systemUser();

        WorkflowScheme userBindScheme = null;
        ContentType userBindContentType = null;
        Contentlet checkedIn = null;

        try {
            userBindContentType = contentTypeAPI.save(
                    ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                            .folder(FolderAPI.SYSTEM_FOLDER)
                            .host(Host.SYSTEM_HOST)
                            .name("UserBindTest" + System.currentTimeMillis())
                            .variable("UserBindTest" + System.currentTimeMillis())
                            .build());

            final CreateSchemeStepActionResult userBindResult = createSchemeStepActionActionlet(
                    "UserBindScheme" + UUIDGenerator.generateUuid(), "step1", "action1",
                    VelocityScriptActionlet.class);
            userBindScheme = userBindResult.getScheme();

            // Read WebKeys.USER ("USER") off the mock request the actionlet builds.
            // Pre-fix: this attribute was never set, so getAttribute returned null and
            // PortalUtil.getUser(req) — used by WorkflowTool.init() — fell through to the
            // anonymous user.
            final String script =
                    "#set($attrUser = $request.getAttribute(\"USER\"))\n" +
                    "#if($attrUser)\n" +
                    "$dotJSON.put(\"userId\", $attrUser.getUserId())\n" +
                    "$dotJSON.put(\"isAnonymous\", $attrUser.isAnonymousUser())\n" +
                    "#else\n" +
                    "$dotJSON.put(\"userId\", \"MISSING\")\n" +
                    "$dotJSON.put(\"isAnonymous\", true)\n" +
                    "#end\n";

            final WorkflowActionClass actionClass = userBindResult.getActionClass();
            final List<WorkflowActionClassParameter> params = new ArrayList<>();
            final WorkflowActionClassParameter scriptParam = new WorkflowActionClassParameter();
            scriptParam.setActionClassId(actionClass.getId());
            scriptParam.setKey("script");
            scriptParam.setValue(script);
            params.add(scriptParam);
            final WorkflowActionClassParameter resultKeyParam = new WorkflowActionClassParameter();
            resultKeyParam.setActionClassId(actionClass.getId());
            resultKeyParam.setKey("resultKey");
            resultKeyParam.setValue("result");
            params.add(resultKeyParam);
            workflowAPI.saveWorkflowActionClassParameters(params, admin);

            workflowAPI.saveSchemesForStruct(
                    new StructureTransformer(userBindContentType).asStructure(),
                    List.of(userBindScheme));

            final Contentlet contentlet = new Contentlet();
            contentlet.setContentType(userBindContentType);
            checkedIn = contentletAPI.checkin(contentlet, admin, false);

            final Contentlet result = workflowAPI.fireContentWorkflow(checkedIn,
                    new ContentletDependencies.Builder()
                            .modUser(admin)
                            .workflowActionId(userBindResult.getAction().getId())
                            .build());

            Assert.assertNotNull(result);
            final Map<String, Object> resultMap = (Map<String, Object>) result.get("result");
            Assert.assertNotNull("Actionlet must store a result map", resultMap);
            final DotJSON dotJSON = (DotJSON) resultMap.get("dotJSON");
            Assert.assertNotNull("dotJSON must be present in the result", dotJSON);
            Assert.assertEquals(
                    "WebKeys.USER on the mock request must be the triggering admin, not missing or anonymous",
                    admin.getUserId(), dotJSON.get("userId"));
            Assert.assertEquals(
                    "Triggering user must not resolve to anonymous",
                    false, dotJSON.get("isAnonymous"));

        } finally {
            try {
                if (null != checkedIn) {
                    contentletAPI.destroy(checkedIn, admin, false);
                }
            } catch (Exception ignored) { /* best-effort */ }

            if (null != userBindScheme) {
                cleanScheme(userBindScheme);
            }
            if (null != userBindContentType) {
                contentTypeAPI.delete(userBindContentType);
            }
        }
    }

}
