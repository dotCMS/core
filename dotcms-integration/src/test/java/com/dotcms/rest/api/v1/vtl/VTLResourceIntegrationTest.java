package com.dotcms.rest.api.v1.vtl;

import static com.dotcms.datagen.TestDataUtils.getEmployeeLikeContentType;
import static com.dotcms.rest.api.v1.vtl.RequestBodyVelocityReader.EMBEDDED_VELOCITY_KEY_NAME;
import static com.dotcms.rest.api.v1.vtl.VTLResource.VTL_PATH;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rendering.velocity.viewtools.secrets.DotVelocitySecretAppConfigThreadLocal;
import com.dotcms.rendering.velocity.viewtools.secrets.DotVelocitySecretAppKeys;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.WebResource;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.util.ConfigTestHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class VTLResourceIntegrationTest {

    enum ResourceMethod {
        GET, DYNAMIC_GET
    }

    private static final String VALID_GET_VTL_DOTJSON_OUTPUT = "com/dotcms/rest/api/v1/vtl/valid_get_dotjson_response.vtl";
    private static final String VALID_GET_VTL_RAW_OUTPUT = "com/dotcms/rest/api/v1/vtl/valid_get_raw_response.vtl";
    private static final String INVALID_GET_VTL = "com/dotcms/rest/api/v1/vtl/invalid_get.vtl";
    private static final String ONE_EMPLOYEE_JSON_RESPONSE = "com/dotcms/rest/api/v1/vtl/one_employee_json_response.json";
    private static final String ONE_EMPLOYEE_XML_RESPONSE = "com/dotcms/rest/api/v1/vtl/one_employee_xml_response.xml";

    private final User systemUser = APILocator.systemUser();
    private static final String ANONYMOUS_USER_ID = "anonymous";
    private static final String SECRET_VALUE = "vtl-secret-value";
    private static final String SECRET_REFERENCE = "$dotsecrets.get(\"mySecret\")";

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] getTestCases() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        final ContentType employeeContentType = getEmployeeLikeContentType();
        final Contentlet employee = getEmployeeContent(employeeContentType.id());
        ContentletDataGen.publish(employee);
        final String KNOWN_EMPLOYEE_ID = employee.getInode();
        //Assign permissions
        APILocator.getPermissionAPI().save(
                new Permission(employee.getPermissionId(),
                        APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                        PermissionAPI.PERMISSION_READ),
                employee, APILocator.systemUser(), false);

        String expectedJSONResponse = FileUtil
                .read(new File(
                        ConfigTestHelper.getUrlToTestResource(ONE_EMPLOYEE_JSON_RESPONSE).toURI()));
        expectedJSONResponse = expectedJSONResponse
                .replace("{IDENTIFIER_TO_REPLACE}", employee.getIdentifier());

        final MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
        queryParameters.put("key1", List.of("value1"));
        queryParameters.put("key2", Arrays.asList("value2", "value3"));

        final String folderName = System.currentTimeMillis() + "employees";

        final Map<String, String> dynamicGetBodyMap = new HashMap<>();
        final File getVTL = new File(ConfigTestHelper.getUrlToTestResource(VALID_GET_VTL_DOTJSON_OUTPUT).toURI());
        final String getVTLasString = new String ( java.nio.file.Files.readAllBytes(getVTL.toPath()));
        dynamicGetBodyMap.put(EMBEDDED_VELOCITY_KEY_NAME, getVTLasString);

        return new VTLResourceTestCase[] {
                new VTLResourceTestCase.Builder().setVtlFile(new File(ConfigTestHelper.getUrlToTestResource(VALID_GET_VTL_DOTJSON_OUTPUT).toURI()))
                        .setFolderName(folderName)
                        .setQueryParameters(queryParameters)
                        .setPathParameter(KNOWN_EMPLOYEE_ID)
                        .setExpectedJSON(expectedJSONResponse)
                        .setExpectedOutput(null)
                        .setResourceMethod(ResourceMethod.GET)
                        .build(),
                new VTLResourceTestCase.Builder().setVtlFile(new File(ConfigTestHelper.getUrlToTestResource(VALID_GET_VTL_RAW_OUTPUT).toURI()))
                        .setFolderName(folderName)
                        .setQueryParameters(queryParameters)
                        .setPathParameter(KNOWN_EMPLOYEE_ID)
                        .setExpectedJSON(FileUtil.read(new File(ConfigTestHelper.getUrlToTestResource(ONE_EMPLOYEE_XML_RESPONSE).toURI())))
                        .setResourceMethod(ResourceMethod.GET)
                        .setExpectedOutput(null)
                        .build(),
                new VTLResourceTestCase.Builder().setVtlFile(new File(ConfigTestHelper.getUrlToTestResource(INVALID_GET_VTL).toURI()))
                        .setFolderName(folderName)
                        .setQueryParameters(queryParameters)
                        .setPathParameter(KNOWN_EMPLOYEE_ID)
                        .setExpectedJSON(null)
                        .setExpectedOutput(null)
                        .setExpectedException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                        .setResourceMethod(ResourceMethod.GET)
                        .build(),
                new VTLResourceTestCase.Builder().setVtlFile(new File(ConfigTestHelper.getUrlToTestResource(VALID_GET_VTL_DOTJSON_OUTPUT).toURI()))
                        .setFolderName(folderName)
                        .setQueryParameters(queryParameters)
                        .setPathParameter(KNOWN_EMPLOYEE_ID)
                        .setExpectedJSON(expectedJSONResponse)
                        .setExpectedOutput(null)
                        .setUser(ANONYMOUS_USER_ID)
                        .setResourceMethod(ResourceMethod.GET)
                        .build(),
                new VTLResourceTestCase.Builder().setVtlFile(new File(ConfigTestHelper.getUrlToTestResource(VALID_GET_VTL_DOTJSON_OUTPUT).toURI()))
                        .setFolderName(folderName)
                        .setQueryParameters(queryParameters)
                        .setPathParameter(KNOWN_EMPLOYEE_ID)
                        .setExpectedJSON(expectedJSONResponse)
                        .setExpectedOutput(null)
                        .setBodyMapString(new ObjectMapper().writeValueAsString(dynamicGetBodyMap))
                        .setResourceMethod(ResourceMethod.DYNAMIC_GET)
                        .build(),
                new VTLResourceTestCase.Builder().setVtlFile(new File(ConfigTestHelper.getUrlToTestResource(VALID_GET_VTL_DOTJSON_OUTPUT).toURI()))
                        .setFolderName(folderName)
                        .setQueryParameters(queryParameters)
                        .setPathParameter(KNOWN_EMPLOYEE_ID)
                        .setExpectedJSON(null)
                        .setExpectedOutput(null)
                        .setExpectedException(Response.Status.FORBIDDEN.getStatusCode())
                        .setBodyMapString(new ObjectMapper().writeValueAsString(dynamicGetBodyMap))
                        .setUser(ANONYMOUS_USER_ID)
                        .setResourceMethod(ResourceMethod.DYNAMIC_GET)
                        .build(),
        };
    }

    @Test
    @UseDataProvider("getTestCases")
    public void testGet(final VTLResourceTestCase testCase) throws
            DotDataException, DotSecurityException, IOException {

        final Host site = new SiteDataGen().nextPersisted();

        final Folder vtlFolder = createVTLFolder(testCase.getFolderName(), site);
        // the anonymous cases expect READ globally (other tests may leave it changed) and the
        // scripting opt-in for the file endpoint; authenticated cases are unaffected
        final String originalGlobal = Config.getStringProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, null);
        final String originalScripting = Config.getStringProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, null);
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "READ");
        Config.setProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, true);

        try {
            final Response response = getResponseFromVTLResource(testCase, vtlFolder,
                    site.getHostname());

            final String expectedOutput = expectedOutput(testCase);
            final String actualOutput = actualOutput(testCase, response);
            assertEquals(expectedOutput, actualOutput);
        } finally {
            Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, originalGlobal);
            Config.setProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, originalScripting);
            APILocator.getFolderAPI().delete(vtlFolder, APILocator.systemUser(), false);
        }
    }

    /**
     * Given scenario: an anonymous GET to a file-based /api/vtl endpoint whose get.vtl reads a
     * Velocity Secret, and the file was last edited by a user with the Scripting Developer role.
     * Expected result: the secret value is rendered.
     */
    @Test
    public void testGet_anonymousReadsSecret_whenFileLastEditedByScriptingDeveloper() throws Exception {
        final User scriptingDeveloper = new UserDataGen().roles(
                APILocator.getRoleAPI().loadCMSAdminRole(),
                APILocator.getRoleAPI().loadRoleByKey(Role.SCRIPTING_DEVELOPER)).nextPersisted();

        assertEquals(SECRET_VALUE, getSecretAnonymously(scriptingDeveloper));
    }

    /**
     * Given scenario: the same anonymous request, but the file's last editor lacks the Scripting
     * Developer role.
     * Expected result: the secret is not rendered.
     */
    @Test
    public void testGet_anonymousCannotReadSecret_whenFileEditorLacksScriptingRole() throws Exception {
        final User editor = new UserDataGen().roles(APILocator.getRoleAPI().loadCMSAdminRole()).nextPersisted();

        // the denied call returns null, which Velocity renders as the unresolved reference
        assertEquals(SECRET_REFERENCE, getSecretAnonymously(editor));
    }

    /**
     * Seeds a dotVelocitySecretApp secret on a new site, publishes a get.vtl that prints it as the
     * given editor, and calls the endpoint as the anonymous user.
     *
     * @param editor user who checks in and publishes the get.vtl file
     * @return the raw output of the endpoint
     */
    private String getSecretAnonymously(final User editor) throws Exception {
        final String allowAnonymous = Config.getStringProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, null);
        Config.setProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, true);
        DotVelocitySecretAppConfigThreadLocal.INSTANCE.clearConfig();

        final Host site = new SiteDataGen().nextPersisted();
        final String folderName = System.currentTimeMillis() + "secrets";
        final Folder vtlFolder = createVTLFolder(folderName, site);
        try {
            APILocator.getAppsAPI().saveSecrets(new AppSecrets.Builder()
                    .withKey(DotVelocitySecretAppKeys.APP_KEY)
                    .withSecret(DotVelocitySecretAppKeys.TITLE.key, "VTL secrets test")
                    .withHiddenSecret("mySecret", SECRET_VALUE)
                    .build(), site, systemUser);

            final File vtl = new File(Files.createTempDirectory("dotcms-test").toFile(), "get.vtl");
            Files.writeString(vtl.toPath(), SECRET_REFERENCE);

            final VTLResourceTestCase testCase = new VTLResourceTestCase.Builder()
                    .setVtlFile(vtl)
                    .setFolderName(folderName)
                    .setQueryParameters(new MultivaluedHashMap<>())
                    .setPathParameter("secret")
                    .setUser(ANONYMOUS_USER_ID)
                    .setResourceMethod(ResourceMethod.GET)
                    .build();
            final Response response = getResponseFromVTLResource(testCase, vtlFolder,
                    site.getHostname(), editor);
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            return response.getEntity().toString().trim();
        } finally {
            Config.setProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, allowAnonymous);
            DotVelocitySecretAppConfigThreadLocal.INSTANCE.clearConfig();
            APILocator.getAppsAPI().deleteSecrets(DotVelocitySecretAppKeys.APP_KEY, site, systemUser);
            APILocator.getFolderAPI().delete(vtlFolder, systemUser, false);
            APILocator.getHostAPI().archive(site, systemUser, false);
            APILocator.getHostAPI().delete(site, systemUser, false);
        }
    }

    private Response getResponseFromVTLResource(final VTLResourceTestCase testCase,
            final Folder vtlFolder, final String hostName)
            throws IOException, DotSecurityException, DotDataException {
        return getResponseFromVTLResource(testCase, vtlFolder, hostName, systemUser);
    }

    /**
     * Publishes the test case's VTL file as {@code fileEditor} and calls the resource method the
     * test case names.
     *
     * @param testCase   the request to make and the VTL file to serve
     * @param vtlFolder  folder that holds the endpoint's get.vtl
     * @param hostName   server name the mocked request reports
     * @param fileEditor user who checks in and publishes get.vtl, and so is its last editor
     * @return the resource's response
     * @throws IOException          if the VTL file cannot be copied
     * @throws DotSecurityException if the editor cannot save or publish the file
     * @throws DotDataException     if the file or the requesting user cannot be loaded
     */
    private Response getResponseFromVTLResource(final VTLResourceTestCase testCase,
            final Folder vtlFolder, final String hostName, final User fileEditor)
            throws IOException, DotSecurityException, DotDataException {
        createVTLFile(testCase.getVtlFile(), vtlFolder, fileEditor);

        final HttpServletRequest request = getMockedRequest();
        when(request.getServerName()).thenReturn(hostName);

        final HttpServletResponse response = getMockedResponse();

        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters()).thenReturn(testCase.getQueryParameters());

        final WebResource webResource = getSpiedWebResource(testCase, request, response);

        final HTTPMethodParams params = new HTTPMethodParamsBuilder()
                .setRequest(request)
                .setServletResponse(response)
                .setUriInfo(uriInfo)
                .setPathParam(testCase.getPathParameter())
                .setBodyMap(testCase.getBodyMap())
                .setBodyMapString(testCase.getBodyMapString())
                .setFolderName(testCase.getFolderName())
                .setWebResource(webResource)
                .build();

        final MethodToTest methodToTest = MethodToTestFactory.getMethodToTest(testCase.getResourceMethod());
        return methodToTest.execute(params);
    }

    private String expectedOutput(final VTLResourceTestCase testCase) {
        String expectedOutput;

        if(testCase.getExpectedException()>0) {
            expectedOutput = Integer.toString(testCase.getExpectedException());
        } else if(UtilMethods.isSet(testCase.getExpectedJSON())) {
            expectedOutput = testCase.getExpectedJSON();
        } else {
            expectedOutput = testCase.getExpectedOutput();
        }

        return expectedOutput;
    }

    private String actualOutput(final VTLResourceTestCase testCase, final Response response) throws JsonProcessingException {
        final String output;
        if(testCase.getExpectedException()>0) {
            output = Integer.toString(response.getStatus());
        } else if(UtilMethods.isSet(testCase.getExpectedJSON())) {
            final ObjectMapper objectMapper = new ObjectMapper();
            output = objectMapper.writeValueAsString(response.getEntity());
        } else {
            output = Map.class.isInstance(response.getEntity())?
                    (String) Map.class.cast(response.getEntity()).get("message"):
                    response.getEntity().toString();
        }

        return  output;
    }

    private WebResource getSpiedWebResource(final VTLResourceTestCase testCase, final HttpServletRequest request, final HttpServletResponse response) throws DotDataException, DotSecurityException {
        final User requestingUser = APILocator.getUserAPI().loadUserById(testCase.getUserId(),
                APILocator.systemUser(), false);

        final WebResource webResource = spy(WebResource.class);
        doReturn(requestingUser).when(webResource).getCurrentUser(eq(request), eq(response),
                anyMap(), any(AnonymousAccess.class), any(WebResource.AuthCheckOptions[].class));
        return webResource;
    }

    @NotNull
    private HttpServletRequest getMockedRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(mock(HttpSession.class));
        when(request.getSession()).thenReturn(mock(HttpSession.class));
        // keep request attributes, which the file readers use to tell SecretTool which file runs
        final Map<String, Object> attributes = new HashMap<>();
        doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        when(request.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.<String>getArgument(0)));
        return request;
    }

    private static final EmptyHttpResponse EMPTY_HTTP_RESPONSE = new EmptyHttpResponse();
    @NotNull
    private HttpServletResponse getMockedResponse() {
        return EMPTY_HTTP_RESPONSE;
    }

    /**
     * Checks in and publishes the given file as get.vtl under the folder, so that {@code editor}
     * is its last editor, and lets the anonymous role read it.
     *
     * @param vtlFile    file whose contents become get.vtl
     * @param vtlFolder  folder under /application/apivtl that holds the endpoint
     * @param editor     user who checks in and publishes the file
     * @throws IOException          if the temporary copy of the file cannot be written
     * @throws DotSecurityException if the editor cannot save or publish the file
     * @throws DotDataException     if the file or its permissions cannot be saved
     */
    private void createVTLFile(final File vtlFile, final Folder vtlFolder, final User editor)
            throws IOException, DotSecurityException, DotDataException {
        final File getVTLFile = new File(Files.createTempDirectory("dotcms-test").toFile(), "get.vtl");
        FileUtil.copyFile(vtlFile, getVTLFile);

        final Contentlet newFileAsset = new FileAssetDataGen(vtlFolder, getVTLFile).next();
        newFileAsset.setIndexPolicy(IndexPolicy.FORCE);
        newFileAsset.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        final Contentlet getVTLFileAsset = APILocator.getContentletAPI().checkin(newFileAsset, editor, false);
        getVTLFileAsset.setIndexPolicy(IndexPolicy.FORCE);
        getVTLFileAsset.setIndexPolicyDependencies(IndexPolicy.FORCE);
        getVTLFileAsset.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        getVTLFileAsset.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        APILocator.getContentletAPI().publish(getVTLFileAsset, editor, false);

        //Assign permissions
        APILocator.getPermissionAPI().save(
                new Permission(getVTLFileAsset.getPermissionId(),
                        APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                        PermissionAPI.PERMISSION_READ),
                getVTLFileAsset, systemUser, false);

        Permission folderPermissions = new Permission();
        folderPermissions.setInode(vtlFolder.getPermissionId());
        folderPermissions.setRoleId(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
        folderPermissions.setPermission(PermissionAPI.PERMISSION_READ);
        APILocator.getPermissionAPI().save(folderPermissions, vtlFolder, systemUser, true);
    }

    private Folder createVTLFolder(final String folderName, final Host site)
            throws DotSecurityException, DotDataException {
        return APILocator.getFolderAPI()
                .createFolders(VTL_PATH + "/" + folderName,
                        site,
                        APILocator.systemUser(), false);
    }

    private static Contentlet getEmployeeContent(String contentTypeId) {

        if (null == contentTypeId) {
            contentTypeId = getEmployeeLikeContentType().id();
        }

        try {
            //Creating the content
            ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                    .setProperty("firstName", "Brent")
                    .setProperty("lastName", "Griffin")
                    .setProperty("jobTitle", "VP Wealth Liquidation")
                    .setProperty("phone", "(215) 555-5555")
                    .setProperty("mobile", "(215) 555-5555")
                    .setProperty("fax", "(215) 555-5916")
                    .setProperty("email", "brent.griffin@questfake.com");

            return contentletDataGen.nextPersisted();

        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    static class HTTPMethodParams {

        private final HttpServletRequest request;
        private final HttpServletResponse servletResponse;
        private final UriInfo uriInfo;
        private final String folderName;
        private final String pathParam;
        private final Map<String, Object> bodyMap;
        private final WebResource webResource;
        private final String bodyMapString;

        public HttpServletRequest getRequest() {
            return request;
        }

        HttpServletResponse getServletResponse() {
            return servletResponse;
        }

        UriInfo getUriInfo() {
            return uriInfo;
        }

        String getFolderName() {
            return folderName;
        }

        String getPathParam() {
            return pathParam;
        }

        Map<String, Object> getBodyMap() {
            return bodyMap;
        }

        WebResource getWebResource() {
            return webResource;
        }

        public String getBodyMapString() {
            return bodyMapString;
        }

        HTTPMethodParams(final HttpServletRequest request, final HttpServletResponse servletResponse, final UriInfo uriInfo,
                final String folderName, final String pathParam, final Map<String, Object> bodyMap,
                final WebResource webResource, final String bodyMapString) {
            this.request = request;
            this.servletResponse = servletResponse;
            this.uriInfo = uriInfo;
            this.folderName = folderName;
            this.pathParam = pathParam;
            this.bodyMap = bodyMap;
            this.webResource = webResource;
            this.bodyMapString = bodyMapString;
        }
    }

}