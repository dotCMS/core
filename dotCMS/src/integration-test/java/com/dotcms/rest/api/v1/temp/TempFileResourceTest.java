package com.dotcms.rest.api.v1.temp;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple5;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class TempFileResourceTest {

    static HttpServletResponse response;
    static TempFileResource resource;
    static User user;
    static Host defaultHost;
    static User systemUser;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        resource = new TempFileResource();
        user = new UserDataGen().nextPersisted();
        response = new MockHttpResponse();

        systemUser = APILocator.systemUser();
        defaultHost = APILocator.getHostAPI().findDefaultHost(systemUser, true);
    }

    private static File testFile() throws Exception {
        File testFile = new File(UUIDGenerator.shorty() + "test.png");
        RandomAccessFile fileWrite = new RandomAccessFile(testFile, "rw");
        final long fileLength = 1024 * 50;
        fileWrite.setLength(fileLength);
        fileWrite.seek(fileLength - 1);
        fileWrite.writeByte(20);
        fileWrite.close();
        assert (fileLength == testFile.length());
        return testFile;
    }

    private void resetTempResourceConfig() {
        Config.setProperty(TempFileAPI.TEMP_RESOURCE_MAX_FILE_SIZE_ANONYMOUS, -1);
        Config.setProperty(TempFileAPI.TEMP_RESOURCE_MAX_FILE_SIZE, -1);
        Config.setProperty(TempFileAPI.TEMP_RESOURCE_ENABLED, true);
        Config.setProperty(TempFileAPI.TEMP_RESOURCE_MAX_AGE_SECONDS, 1800);
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "WRITE");
    }


    private static HttpServletRequest mockRequest(String host) {
        return new MockSessionRequest(
                new MockHeaderRequest(new MockHttpRequest(host, "/api/v1/tempResource").request(),
                        "Origin", host)
                        .addHeader("Host", host).request());
    }

    private static HttpServletRequest mockRequest() {
        return new MockSessionRequest(
                new MockHeaderRequest(
                        new MockHttpRequest("localhost", "/api/v1/tempResource").request(),
                        "Origin", "localhost").request());
    }

    private InputStream inputStream() {
        return this.getClass().getResourceAsStream("/images/SqcP9KgFqruagXJfe7CES.png");
    }

    private DotTempFile saveTempFile_usingTempResource(final String fileName,
            final HttpServletRequest request) {

        final BodyPart filePart1 = new StreamDataBodyPart(fileName, inputStream());

        final MultiPart multipartEntity = new FormDataMultiPart()
                .bodyPart(filePart1);

        final Response jsonResponse = resource.uploadTempResourceMulti(request, response, -1,
                (FormDataMultiPart) multipartEntity);

        final Map<String, List<DotTempFile>> dotTempFiles = (Map) jsonResponse.getEntity();
        return dotTempFiles.get("tempFiles").get(0);
    }

    @Test
    public void test_temp_resource_upload() {
        resetTempResourceConfig();
        final String fileName = "test.file";
        final DotTempFile dotTempFile = saveTempFile_usingTempResource(fileName, mockRequest());
        // its not an image because we set the filename to "test.file"
        assertFalse((Boolean) dotTempFile.image);

        assertTrue(dotTempFile.id.startsWith(TempFileAPI.TEMP_RESOURCE_PREFIX));

        assertTrue(dotTempFile.file.getName().equals(fileName));
        assertTrue(dotTempFile.length() > 0);
    }

    @Test
    public void test_temp_resource_multifile_upload() {
        resetTempResourceConfig();
        final String fileName1 = "here-is-my-file.png";
        final BodyPart filePart1 = new StreamDataBodyPart(fileName1, inputStream());

        final String fileName2 = "here-is-my-file2.png";
        final BodyPart filePart2 = new StreamDataBodyPart(fileName2, inputStream());

        //uploading 2 files
        final MultiPart multipartEntity = new FormDataMultiPart()
                .bodyPart(filePart1)
                .bodyPart(filePart2);

        final Response jsonResponse = resource.uploadTempResourceMulti(mockRequest(), response, -1,
                (FormDataMultiPart) multipartEntity);
        assertNotNull(jsonResponse);

        final Map<String, List<DotTempFile>> dotTempFile = (Map) jsonResponse.getEntity();
        assertFalse(dotTempFile.isEmpty());

        assertEquals(2, dotTempFile.get("tempFiles").size());
        DotTempFile file = (DotTempFile) dotTempFile.get("tempFiles").get(0);
        assertEquals(fileName1, file.fileName);
        assertNotNull(file.image);
        assertTrue(file.length() > 1000);

        file = (DotTempFile) dotTempFile.get("tempFiles").get(1);
        assertEquals(fileName2, file.fileName);
        assertNotNull(file.image);
        assertTrue(file.length() > 1000);

    }

    /**
     * This test will creates a temporal file. then will request this temporal with a new request,
     * which means not same finger print b/c the host will be diff, so should return an empty file.
     * Then an user is set to the request and it works b/c is the same user.
     */
    @Test
    public void test_tempResourceAPI_who_can_use_via_userID() {
        resetTempResourceConfig();

        HttpServletRequest request = mockRequest();
        request.setAttribute(WebKeys.USER, user);
        request.setAttribute(WebKeys.USER_ID, user.getUserId());

        final String fileName = "test.png";
        final DotTempFile dotTempFile = saveTempFile_usingTempResource(fileName, request);

        // CANNOT get the file again because we have a new session ID in the new mock request
        Optional<DotTempFile> file = new TempFileAPI()
                .getTempFile(mockRequest("anotherHost"), dotTempFile.id);
        assertFalse(file.isPresent());

        request.setAttribute(WebKeys.USER, user);
        request.setAttribute(WebKeys.USER_ID, user.getUserId());
        // CAN get the file again because we are the user who uploaded it
        file = new TempFileAPI().getTempFile(request, dotTempFile.id);
        assertTrue(file.isPresent() && !file.get().file.isDirectory());
    }

    @Test
    public void test_tempResourceapi_max_age() {
        resetTempResourceConfig();
        HttpServletRequest request = mockRequest();
        final String fileName = "test.png";

        final DotTempFile dotTempFile = saveTempFile_usingTempResource(fileName, request);

        Optional<DotTempFile> file = new TempFileAPI().getTempFile(request, dotTempFile.id);

        assertTrue(file.isPresent());

        int tempResourceMaxAgeSeconds = Config
                .getIntProperty("TEMP_RESOURCE_MAX_AGE_SECONDS", 1800);

        // this works becuase we set the file age to newer than max age
        file.get().file.setLastModified(System.currentTimeMillis() - 60 * 10 * 1000);
        file = new TempFileAPI().getTempFile(request, dotTempFile.id);
        assertTrue(file.isPresent());

        // Setting the file to older than max age makes the file inaccessable
        file.get().file.setLastModified(
                System.currentTimeMillis() - (tempResourceMaxAgeSeconds * 1000) - 1);
        file = new TempFileAPI().getTempFile(request, dotTempFile.id);
        assertFalse(file.isPresent());
    }


    @Test
    public void test_tempResourceapi_test_anonymous_access() {
        resetTempResourceConfig();

        final String fileName = "test.png";
        HttpServletRequest request = mockRequest();
        Config.setProperty(TempFileAPI.TEMP_RESOURCE_ALLOW_ANONYMOUS, false);
        final BodyPart filePart1 = new StreamDataBodyPart(fileName, inputStream());

        final MultiPart multipartEntity = new FormDataMultiPart()
                .bodyPart(filePart1);
        Response jsonResponse = resource.uploadTempResourceMulti(request, response, -1,
                (FormDataMultiPart) multipartEntity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), jsonResponse.getStatus());

        Config.setProperty(TempFileAPI.TEMP_RESOURCE_ALLOW_ANONYMOUS, true);
        jsonResponse = resource.uploadTempResourceMulti(request, response, -1,
                (FormDataMultiPart) multipartEntity);
        final Map<String, List<DotTempFile>> dotTempFiles = (Map) jsonResponse.getEntity();
        final DotTempFile dotTempFile = dotTempFiles.get("tempFiles").get(0);
        assertNotNull(dotTempFile.id);
    }

    /**
     * this tests that TEMP_RESOURCE_MAX_FILE_SIZE_ANONYMOUS takes preciedence
     * TEMP_RESOURCE_MAX_FILE_SIZE and what is passed in on the request
     *
     * @throws Exception
     */
    @Test
    public void test_tempResourceapi_test_user_max_filesize() throws Exception {
        resetTempResourceConfig();
        final File testFile = testFile();
        /**
         * setting TEMP_RESOURCE_MAX_FILE_SIZE_ANONYMOUS less than file size should
         * reject the file for anon user, even though TEMP_RESOURCE_MAX_FILE_SIZE is
         * set to more
         */

        Config.setProperty(TempFileAPI.TEMP_RESOURCE_MAX_FILE_SIZE_ANONYMOUS,
                testFile.length() - 10);
        Config.setProperty(TempFileAPI.TEMP_RESOURCE_MAX_FILE_SIZE, testFile.length() + 10);

        HttpServletRequest request = new MockParameterRequest(mockRequest(),
                ImmutableMap.of(resource.MAX_FILE_LENGTH_PARAM, "0")).request();
        request.setAttribute(WebKeys.USER, user);
        request.setAttribute(WebKeys.USER_ID, user.getUserId());

        final MultiPart multipartEntity = new FormDataMultiPart().bodyPart(
                new StreamDataBodyPart(testFile.getName(), new FileInputStream(testFile)));

        Response jsonResponse = resource.uploadTempResourceMulti(request, response, -1,
                (FormDataMultiPart) multipartEntity);
        assertTrue("User can upload temp file larger than TEMP_RESOURCE_MAX_FILE_SIZE_ANONYMOUS",
                jsonResponse.getStatus() == 200);


    }

    /**
     * this tests that TEMP_RESOURCE_MAX_FILE_SIZE takes preciedence TEMP_RESOURCE_MAX_FILE_SIZE_ANONYMOUS
     * when a user is passed in
     *
     * @throws Exception
     */
    @Test
    public void test_tempResourceapi_test_anonymous_max_filesize() throws Exception {
        resetTempResourceConfig();
        final File testFile = testFile();
        /**
         * setting TEMP_RESOURCE_MAX_FILE_SIZE greater than file size should
         * allow the file for a real user
         */

        Config.setProperty(TempFileAPI.TEMP_RESOURCE_MAX_FILE_SIZE_ANONYMOUS,
                testFile.length() - 10);
        Config.setProperty(TempFileAPI.TEMP_RESOURCE_MAX_FILE_SIZE, testFile.length() + 10);

        HttpServletRequest request = new MockParameterRequest(mockRequest(),
                ImmutableMap.of(resource.MAX_FILE_LENGTH_PARAM, "-1")).request();

        final MultiPart multipartEntity = new FormDataMultiPart().bodyPart(
                new StreamDataBodyPart(testFile.getName(), new FileInputStream(testFile)));

        Response jsonResponse = resource.uploadTempResourceMulti(request, response, -1,
                (FormDataMultiPart) multipartEntity);
        assertTrue(
                "Anon User cannot upload temp file larger than TEMP_RESOURCE_MAX_FILE_SIZE_ANONYMOUS" + jsonResponse.getStatus(),
                jsonResponse.getStatus() == 400);

    }

    /**
     * this tests that the temp endpoint respects max filesize that is requested (if it is smaller
     * than what is configured)
     *
     * @throws Exception
     */
    @Test
    public void test_tempResourceapi_test_max_filesize() throws Exception {
        resetTempResourceConfig();
        final File testFile = testFile();
        HttpServletRequest request = mockRequest();
        final BodyPart filePart1 = new StreamDataBodyPart(testFile.getName(),
                new FileInputStream(testFile));

        final MultiPart multipartEntity = new FormDataMultiPart()
                .bodyPart(filePart1);

        /**
         * dotCMS configured for unlimited max file size,
         * so that is our max
         */
        Config.setProperty(TempFileAPI.TEMP_RESOURCE_MAX_FILE_SIZE, testFile.length() - 10);
        Response jsonResponse = resource.uploadTempResourceMulti(request, response, -1,
                (FormDataMultiPart) multipartEntity);
        assertTrue("anon user cannot upload >TEMP_RESOURCE_MAX_FILE_SIZE " + jsonResponse.getStatus(),
                jsonResponse.getStatus() == 400);
    }

    @Test
    public void temp_resource_makes_it_into_checked_in_content() throws Exception {
        resetTempResourceConfig();
        HttpServletRequest request = mockRequest();
        // set user to system user
        request.setAttribute(WebKeys.USER, systemUser);

        // create a file asset type

        ContentType contentType = ContentTypeBuilder
                .builder(BaseContentType.FILEASSET.immutableClass()).description("description")
                .folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                .name("ContentTypeTesting" + System.currentTimeMillis()).owner("owner")
                .variable("velocityVarNameTesting" + System.currentTimeMillis()).build();
        contentType = APILocator.getContentTypeAPI(systemUser).save(contentType);

        // Add another binary field
        final List<Field> fields = new ArrayList<>(contentType.fields());

        final Field fieldToSave =
                FieldBuilder.builder(BinaryField.class)
                        .name("testBinary")
                        .variable("testBinary")
                        .contentTypeId(contentType.id())
                        .build();

        fields.add(fieldToSave);

        contentType = APILocator.getContentTypeAPI(systemUser).save(contentType, fields);

        final String fileName1 = "testFileName1" + UUIDGenerator.shorty() + ".png";
        final String fileName2 = "testFileName2" + UUIDGenerator.shorty() + ".gif";
        final DotTempFile dotTempFile1 = saveTempFile_usingTempResource(fileName1, request);

        final RemoteUrlForm form = new RemoteUrlForm(
                "https://raw.githubusercontent.com/dotCMS/core/master/dotCMS/src/main/webapp/html/images/skin/logo.gif",
                fileName2, null);

        final Response jsonResponse = resource
                .copyTempFromUrl(request, new MockHttpResponse(), form);
        final Map<String, List<DotTempFile>> dotTempFiles = (Map) jsonResponse.getEntity();
        final DotTempFile dotTempFile2 = dotTempFiles.get("tempFiles").get(0);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("stInode", contentType.id());
        m.put("hostFolder", defaultHost.getIdentifier());
        m.put("languageId", defaultHost.getLanguageId());
        m.put("title", dotTempFile1.fileName);
        m.put("fileAsset", dotTempFile1.id);
        m.put("showOnMenu", "false");
        m.put("sortOrder", 1);
        m.put("description", "description");
        m.put("testBinary", dotTempFile2.id);
        Contentlet contentlet = new Contentlet(m);
        contentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, true);

        contentlet = APILocator.getContentletAPI().find(contentlet.getInode(), systemUser, true);

        assertNotNull(contentlet.getBinary("fileAsset"));
        assertEquals(fileName1, contentlet.getBinary("fileAsset").getName());
        assertTrue(contentlet.getBinary("fileAsset").length() > 0);
        assertNotNull(contentlet.getBinary("testBinary").exists());
        assertEquals(fileName2, contentlet.getBinary("testBinary").getName());
        assertTrue(contentlet.getBinary("testBinary").length() > 0);

    }


    /**
     * This test is for the TEMP_RESOURCE_ENABLED property If is set to false the temp resource is
     * disabled and a 403 should be thrown.
     *
     * @throws Exception
     */
    @Test
    public void test_TempResourceAPI_TempResourceEnabledProperty() {
        resetTempResourceConfig();
        final boolean tempResourceEnabledOriginalValue = Config
                .getBooleanProperty(TempFileAPI.TEMP_RESOURCE_ENABLED, true);
        try {
            HttpServletRequest request = mockRequest();
            final String fileName = "test.png";

            Config.setProperty(TempFileAPI.TEMP_RESOURCE_ENABLED, false);
            final BodyPart filePart1 = new StreamDataBodyPart(fileName, inputStream());

            final MultiPart multipartEntity = new FormDataMultiPart()
                    .bodyPart(filePart1);

            final Response jsonResponse = resource.uploadTempResourceMulti(request, response, 0,
                    (FormDataMultiPart) multipartEntity);

            assertEquals(Status.NOT_FOUND.getStatusCode(), jsonResponse.getStatus());
        } finally {
            Config.setProperty(TempFileAPI.TEMP_RESOURCE_ENABLED, tempResourceEnabledOriginalValue);
        }
    }

    @Test
    public void test_TempResource_uploadFileByURL_success() {
        HttpServletRequest request = mockRequest();
        final String fileName = "test.png";
        final String url = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b8/Bocas2.jpg/250px-Bocas2.jpg";

        final RemoteUrlForm remoteUrlForm = new RemoteUrlForm(url, fileName, null);

        final Response jsonResponse = resource.copyTempFromUrl(request, response, remoteUrlForm);

        assertEquals(Status.OK.getStatusCode(), jsonResponse.getStatus());
    }

    @Test
    public void test_TempResource_uploadFileByURL_UrlNotSent_BadRequest() {
        HttpServletRequest request = mockRequest();
        final String fileName = "test.png";
        final String url = "";

        final RemoteUrlForm remoteUrlForm = new RemoteUrlForm(url, fileName, null);

        final Response jsonResponse = resource.copyTempFromUrl(request, response, remoteUrlForm);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), jsonResponse.getStatus());
    }

    @Test
    @UseDataProvider("testCasesChangeFingerPrint")
    public void testGetTempFile_fileIsNotReturned_fingerprintIsDifferent(
            final testCaseChangeFingerPrint testCase) {

        resetTempResourceConfig();
        HttpServletRequest request = mockRequest();
        request.setAttribute(WebKeys.USER, user);

        final String fileName = "test.png";
        final DotTempFile dotTempFile = saveTempFile_usingTempResource(fileName, request);

        // CAN get the file again because it is the same request
        Optional<DotTempFile> file = new TempFileAPI().getTempFile(request, dotTempFile.id);
        assertTrue(file.isPresent() && !file.get().file.isDirectory());

        // CANNOT get the file again because the request header changed
        HttpServletRequest newRequest = new MockSessionRequest(
                new MockHeaderRequest(
                        new MockHttpRequest("localhost", "/api/v1/tempResource").request(),
                        testCase.headerName, "newValue")
                        .request());
        file = new TempFileAPI().getTempFile(newRequest, dotTempFile.id);
        assertFalse(file.isPresent());
    }

    private static class testCaseChangeFingerPrint {

        String headerName;

        testCaseChangeFingerPrint(final String headerName) {
            this.headerName = headerName;
        }
    }

    @DataProvider
    public static Object[] testCasesChangeFingerPrint() {
        return new Object[]{
                new testCaseChangeFingerPrint("User-Agent"),
                new testCaseChangeFingerPrint("Host"),
                new testCaseChangeFingerPrint("Accept-Language"),
                new testCaseChangeFingerPrint("Accept-Encoding"),
                new testCaseChangeFingerPrint("X-Forwarded-For"),
                new testCaseChangeFingerPrint("referer")
        };
    }

    @Test
    public void test_get_max_allowed_filesize_for_users() {
        resetTempResourceConfig();
        for (final Tuple5<String, String, Long, Long, Long> testCase : testAllowedMaxFileSizes()) {

            HttpServletRequest request = new MockParameterRequest(mockRequest(),
                    ImmutableMap.of(TempFileResource.MAX_FILE_LENGTH_PARAM, testCase._2));
            request.setAttribute(WebKeys.USER_ID, testCase._1);
            Config.setProperty(TempFileAPI.TEMP_RESOURCE_MAX_FILE_SIZE, testCase._3);
            Config.setProperty(TempFileAPI.TEMP_RESOURCE_MAX_FILE_SIZE_ANONYMOUS, testCase._4);

            assertTrue("testing config:" + testCase,
                    new TempFileAPI().maxFileSize(request) == testCase._5);
        }
    }


    /**
     * (UserId, requestedMaxSize, MAX_FILE_SIZE, MAX_FILE_SIZE_ANONYMOUS, expected result)
     *
     * @return
     */
    public static Tuple5<String, String, Long, Long, Long>[] testAllowedMaxFileSizes() {
        return new Tuple5[]{
                new Tuple5<String, String, Long, Long, Long>(user.getUserId(), "-1", 10L, 0L, 10L),
                new Tuple5<String, String, Long, Long, Long>(user.getUserId(), "-1", -1L, 0L, -1L),
                new Tuple5<String, String, Long, Long, Long>(user.getUserId(), "-1", 10L, 0L, 10L),
                new Tuple5<String, String, Long, Long, Long>(user.getUserId(), "0", 10L, 0L, 0L),
                new Tuple5<String, String, Long, Long, Long>(user.getUserId(), "5", 10L, 0L, 5L),
                new Tuple5<String, String, Long, Long, Long>(UserAPI.CMS_ANON_USER_ID, "-1", -1L,
                        0L, 0L),
                new Tuple5<String, String, Long, Long, Long>(UserAPI.CMS_ANON_USER_ID, "-1", 10L,
                        -1L, 10L),
                new Tuple5<String, String, Long, Long, Long>(UserAPI.CMS_ANON_USER_ID, "-1", -1L,
                        -1L, -1L),
                new Tuple5<String, String, Long, Long, Long>(UserAPI.CMS_ANON_USER_ID, "-1", 10L,
                        20L, 10L),
                new Tuple5<String, String, Long, Long, Long>(UserAPI.CMS_ANON_USER_ID, "-1", 0L,
                        20L, 0L),
                new Tuple5<String, String, Long, Long, Long>(UserAPI.CMS_ANON_USER_ID, "5", 10L,
                        20L, 5L),
        };
    }

    @Test
    public void test_TempResource_uploadFileByURL_URLDoesNotExists_returnBadRequest() {
        HttpServletRequest request = mockRequest();
        final String fileName = "test.png";
        final String url = "https://upload.wikimedia.org/this/not/exists/Bocas2.jpg";

        final RemoteUrlForm remoteUrlForm = new RemoteUrlForm(url, fileName, null);

        final Response jsonResponse = resource.copyTempFromUrl(request, response, remoteUrlForm);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), jsonResponse.getStatus());
    }

}