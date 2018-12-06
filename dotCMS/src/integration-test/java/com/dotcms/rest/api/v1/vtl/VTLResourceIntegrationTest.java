package com.dotcms.rest.api.v1.vtl;

import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.javax.ws.rs.core.MultivaluedHashMap;
import com.dotcms.repackage.javax.ws.rs.core.MultivaluedMap;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.UriInfo;
import com.dotcms.rest.WebResource;
import com.dotcms.util.ConfigTestHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import com.google.common.io.Files;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.dotcms.rest.api.v1.vtl.VTLResource.VTL_PATH;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(DataProviderRunner.class)
public class VTLResourceIntegrationTest {

    private final static String VALID_GET_VTL_DOTJSON_OUTPUT = "com/dotcms/rest/api/v1/vtl/valid_get_dotjson_response.vtl";
    private final static String INVALID_GET_VTL = "com/dotcms/rest/api/v1/vtl/invalid_get.vtl";
    private final static String ONE_EMPLOYEE_JSON_RESPONSE = "com/dotcms/rest/api/v1/vtl/one_employee_json_response.json";

    private static final String KNOWN_EMPLOYEE_ID = "37f93fcb-6124-46af-83b4-9ece6c1c5380";

    private final User systemUser = APILocator.systemUser();
    private static final String ANONYMOUS_USER_ID = "anonymous";

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] getTestCases() throws URISyntaxException, IOException {
        final MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
        queryParameters.put("key1", Collections.singletonList("value1"));
        queryParameters.put("key2", Arrays.asList("value2", "value3"));

        final String folderName = System.currentTimeMillis() + "employees";

        return new VTLResourceTestCase[] {
                new VTLResourceTestCase.Builder().setVtlFile(new File(ConfigTestHelper.getUrlToTestResource(VALID_GET_VTL_DOTJSON_OUTPUT).toURI()))
                        .setFolderName(folderName)
                        .setQueryParameters(queryParameters)
                        .setPathParameter(KNOWN_EMPLOYEE_ID)
                        .setExpectedJSON(FileUtil.read(new File(ConfigTestHelper.getUrlToTestResource(ONE_EMPLOYEE_JSON_RESPONSE).toURI())))
                        .setExpectedOutput(null)
                        .build(),
//                new VTLResourceTestCase.Builder().setVtlFile(VALID_GET_VTL_RAW_OUTPUT)
//                        .setFolderName(folderName)
//                        .setQueryParameters(queryParameters)
//                        .setPathParameter("aaee9776-8fb7-4501-8048-844912a20405")
//                        .setExpectedJSON(null)
//                        .setExpectedOutput(VALID_GET_VTL_RAW_OUTPUT)
//                        .build(),
                new VTLResourceTestCase.Builder().setVtlFile(new File(ConfigTestHelper.getUrlToTestResource(INVALID_GET_VTL).toURI()))
                        .setFolderName(folderName)
                        .setQueryParameters(queryParameters)
                        .setPathParameter(KNOWN_EMPLOYEE_ID)
                        .setExpectedJSON(null)
                        .setExpectedOutput(null)
                        .setExpectedException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                        .build(),
                new VTLResourceTestCase.Builder().setVtlFile(new File(ConfigTestHelper.getUrlToTestResource(VALID_GET_VTL_DOTJSON_OUTPUT).toURI()))
                        .setFolderName(folderName)
                        .setQueryParameters(queryParameters)
                        .setPathParameter(KNOWN_EMPLOYEE_ID)
                        .setExpectedJSON(FileUtil.read(new File(ConfigTestHelper.getUrlToTestResource(ONE_EMPLOYEE_JSON_RESPONSE).toURI())))
                        .setExpectedOutput(null)
                        .setUser(ANONYMOUS_USER_ID)
                        .build()
        };
    }

    @Test
    @UseDataProvider("getTestCases")
    public void testGet(final VTLResourceTestCase testCase) throws
            DotDataException, DotSecurityException, IOException {

        final Host demoSite = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);

        final Folder vtlFolder = APILocator.getFolderAPI()
                .createFolders(VTL_PATH + "/" + testCase.getFolderName(),
                        demoSite,
                        APILocator.systemUser(), false);

        try {
            final File getVTLFile = new File(Files.createTempDir(), "get.vtl");
            FileUtil.copyFile(testCase.getVtlFile(), getVTLFile);

            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(vtlFolder, getVTLFile);
            final Contentlet getVTLFileAsset = fileAssetDataGen.nextPersisted();
            getVTLFileAsset.setIndexPolicy(IndexPolicy.FORCE);
            getVTLFileAsset.setIndexPolicyDependencies(IndexPolicy.FORCE);
            getVTLFileAsset.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            APILocator.getContentletAPI().publish(getVTLFileAsset, systemUser, false);

            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getSession(false)).thenReturn(mock(HttpSession.class));
            when(request.getSession()).thenReturn(mock(HttpSession.class));

            final HttpServletResponse servletResponse = mock(HttpServletResponse.class);

            final UriInfo uriInfo = mock(UriInfo.class);
            when(uriInfo.getQueryParameters()).thenReturn(testCase.getQueryParameters());

            final User requestingUser = APILocator.getUserAPI().loadUserById(testCase.getUserId(),
                    APILocator.systemUser(), false);

            final WebResource webResource = spy(WebResource.class);
            doReturn(requestingUser).when(webResource).getCurrentUser(request,
                    WebResource.buildParamsMap(testCase.getPathParameter()), false);

            final VTLResource resource = new VTLResource(APILocator.getHostAPI(), APILocator.getIdentifierAPI(),
                    APILocator.getContentletAPI(), webResource);
            final Response response = resource.get(request, servletResponse, uriInfo, testCase.getFolderName(),
                    testCase.getPathParameter(), null);

            String expectedOutput;
            String getOutput;

            if(testCase.getExpectedException()>0) {
                expectedOutput = Integer.toString(testCase.getExpectedException());
                getOutput = Integer.toString(response.getStatus());
            } else if(UtilMethods.isSet(testCase.getExpectedJSON())) {
                expectedOutput = testCase.getExpectedJSON();
                final ObjectMapper objectMapper = new ObjectMapper();
                getOutput = objectMapper.writeValueAsString(response.getEntity());
            } else {
                expectedOutput = testCase.getExpectedOutput();
                getOutput      = Map.class.isInstance(response.getEntity())?
                    (String) Map.class.cast(response.getEntity()).get("message"):
                    response.getEntity().toString();
            }

            assertEquals(expectedOutput, getOutput);
        } finally {
            APILocator.getFolderAPI().delete(vtlFolder, APILocator.systemUser(), false);
        }


    }

//    private static final String VALID_GET_VTL_DOTJSON_OUTPUT = "## GETS NEWS BY ID, LOADS A FEW FIELD\n" +
//            "#set($news = $dotcontent.find($pathParam))\n" +
//            "\n" +
//            "##  QUERY PARAMS\n" +
//            "\n" +
//            "$dotJSON.put(\"queryParam\", $queryParams)\n" +
//            "\n" +
//            "## NEW ARRAY\n" +
//            "#set ($fields = [])   \n" +
//            "$fields.add(\"title\")\n" +
//            "$fields.add(\"urlTitle\")\n" +
//            "$fields.add(\"story\")\n" +
//            "\n" +
//            "#foreach($field in $fields)\n" +
//            "    #set($item = {})\n" +
//            "    $item.put($field,${news.get(\"$field\")})\n" +
//            "    $dotJSON.put(\"$field\",$item)\n" +
//            "#end\n" +
//            "\n" +
//            "$dotJSON.put(\"cache\", 10)\n";

    private static final String VALID_GET_VTL_RAW_OUTPUT = "helloworld";
}
