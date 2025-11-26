package com.dotcms.csspreproc;

import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.ema.proxy.MockHttpCaptureResponse;
import com.dotcms.ema.proxy.MockPrintWriter;
import com.dotcms.mock.request.DotCMSMockRequestWithSession;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.ConfigTestHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.util.StringPool;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Verifies that the Dart SASS Compiler behaves correctly.
 *
 * @author Jose Castro
 * @since Aug 10th, 2022
 */
public class CSSPreProcessServletTest extends IntegrationTestBase {

    protected static String defaultSiteId = null;
    protected static Host defaultSite = null;
    protected static Folder testThemeHome = null;
    protected static FileAsset inputSimpleScssFile = null;
    protected static FileAsset inputScssFileWithImports = null;
    protected static FileAsset inputInvalidScssFile = null;

    private static final String VALID_SCSS_FILE_RESPONSE = "body {\n" + "  font: 100% Helvetica, sans-serif;\n" + "  color: #555;\n" + "}\n";
    private static final String SCSS_FILE_WITH_IMPORT_RESPONSE = "h3 {\n" + "  color: blue;\n" + "}\n" + "\n" + "body {\n" + "  font: 100% Helvetica, sans-serif;\n" + "  color: #555;\n" + "}\n" + "\n" + "h1 {\n" + "  color: red;\n" + "}\n";
    private static final String INVALID_SCSS_FILE_RESPONSE = "/* Error: expected \":\".\n" + " *   ,\n" + " * 3 | $prim ary-color: #555;\n" + " *   |       ^\n" + " *   '";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        defaultSite = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        defaultSiteId = defaultSite.getIdentifier();

        // Test data generation
        testThemeHome = new FolderDataGen().site(defaultSite).name("test-theme").nextPersisted();
        final Folder scssFolder = new FolderDataGen().site(defaultSite).name("css").parent(testThemeHome).nextPersisted();
        final Folder importedScssFolder =
                new FolderDataGen().site(defaultSite).name("imported-css").parent(testThemeHome).nextPersisted();
        // Simple SASS file
        inputSimpleScssFile = createTestScssFile("dart-sass/css/sample-sass.scss", scssFolder);
        // SASS file with an @import directive
        inputScssFileWithImports = createTestScssFile("dart-sass/css/sample-sass-with-imports.scss", scssFolder);
        // SASS file that will be imported during compilation. We just need it to exist
        createTestScssFile("dart-sass/css/sample-imported-sass.scss", importedScssFolder);
        // SASS file with invalid code to make the compilation process fail
        inputInvalidScssFile = createTestScssFile("dart-sass/css/sample-invalid-sass.scss", scssFolder);
    }

    @AfterClass
    public static void cleanupAfter() throws DotDataException, DotSecurityException {
        try {
            // Simply delete the folder containing all the test data
            APILocator.getFolderAPI().delete(testThemeHome, APILocator.systemUser(), false);
        } catch (final Exception e) {
            Logger.error(CSSPreProcessServletTest.class, "Error during data cleanup", e);
            throw e;
        }
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link CSSPreProcessServlet#doGet(HttpServletRequest, HttpServletResponse)}</li>
     *     <li><b>Given Scenario:</b> Compiling a simple valid SCSS file.</li>
     *     <li><b>Expected Result:</b> Valid CSS code is returned. Because of the Servlet being mocked, the status code
     *     must be -1.</li>
     * </ul>
     */
    @Test
    public void testCompileValidScssFile() {
        executeTest(inputSimpleScssFile, VALID_SCSS_FILE_RESPONSE, false);
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link CSSPreProcessServlet#doGet(HttpServletRequest, HttpServletResponse)}</li>
     *     <li><b>Given Scenario:</b> Compiling a simple valid SCSS file with an @import directive. This adds more
     *     complexity to the process as it forces dotCMS to scan files in a different folder.</li>
     *     <li><b>Expected Result:</b> Valid CSS code is returned. Because of the Servlet being mocked, the status code
     *     must be -1.</li>
     * </ul>
     */
    @Test
    public void testCompileScssFileWithImport() {
        executeTest(inputScssFileWithImports, SCSS_FILE_WITH_IMPORT_RESPONSE, false);
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link CSSPreProcessServlet#doGet(HttpServletRequest, HttpServletResponse)}</li>
     *     <li><b>Given Scenario:</b> Compiling an invalid SCSS file. This failure must NOT cause harmful exceptions in
     *     the system as the page rendering process must finish as usual.</li>
     *     <li><b>Expected Result:</b> By default, an error message is added as part of the CSS response indicating the
     *     situation to the user. Because of the Servlet being mocked, the status code must be -1.</li>
     * </ul>
     */
    @Test
    public void testCompileInvalidScssFile() {
        executeTest(inputInvalidScssFile, INVALID_SCSS_FILE_RESPONSE, true);
    }

    /**
     * Utility method used to create the SCSS files in dotCMS.
     *
     * @param testScssFilePath The location of the test SCSS file inside the integration-test project.
     * @param parentFolder     The {@link Folder} containing the SCSS file.
     *
     * @return The test SCSS file in the form of a {@link FileAsset} object.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The User creating the file doesn't have the required permissions to do so.
     */
    private static FileAsset createTestScssFile(final String testScssFilePath, final Folder parentFolder) throws DotDataException, DotSecurityException {
        final String resource = ConfigTestHelper.getPathToTestResource(testScssFilePath);
        final File file = new File(resource);
        final Contentlet simpleScssFile = new FileAssetDataGen(parentFolder, file).nextPersisted();
        return APILocator.getFileAssetAPI().fromContentlet(simpleScssFile);
    }

    /**
     * Generic method that performs the SCSS file compilation process. After mocking several related objects, the
     * {@link CSSPreProcessServlet#doGet(HttpServletRequest, HttpServletResponse)} method is called, and both the status
     * code and response are retrieved, which will determine whether the specific test was successful or not.
     *
     * @param inputScssFile  The SCSS file that will be compiled.
     * @param expectedOutput The expected CSS output, or part of the error message if the SCSS is expected to fail.
     */
    private void executeTest(final FileAsset inputScssFile, final String expectedOutput, final boolean isError) {
        String cssCode = StringPool.BLANK;
        int status = 0;
        try {
            // Initialization
            final HttpServletRequest mockRequest = hydrateMockedRequest(inputScssFile);
            final MockHttpCaptureResponse mockResponse = new MockHttpCaptureResponse(new MockHttpResponse()){
                @Override
                public PrintWriter getWriter() {
                    return new MockPrintWriter(getOutputStream());
                }
            };
            final CSSPreProcessServlet servlet = new CSSPreProcessServlet();

            // Test data generation
            servlet.doGet(mockRequest, mockResponse);
            cssCode = new String(mockResponse.getBytes(), StandardCharsets.UTF_8);
            status = mockResponse.getStatus();
        } catch (final ServletException | IOException | DotDataException | DotSecurityException e) {
            Assert.fail(String.format("An error occurred when compiling the test SCSS file '%s'. Aborting test " +
                                              "execution...", inputScssFile));
        }
        if (isError) {
            // The SCSS file is expected to fail, so we need to check that the error message is part of the response
            Assert.assertTrue(String.format("The SCSS file '%s' was expected to fail but it didn't. The output was: %s",
                    inputScssFile.getName(), cssCode), cssCode.contains("Error:")
            );
        } else {
            Assert.assertTrue(String.format("This is NOT the expected SCSS compiler output. cssCode [%s] expected [%s].",
                    cssCode, expectedOutput), CssComparator.areSemanticallyEqual(cssCode, expectedOutput));
        }
        // Assertions

        Assert.assertNotEquals("The SCSS file could not be found.", HttpStatus.SC_NOT_FOUND, status);
        Assert.assertNotEquals("The SCSS file could not be read by the specified dotCMS user.",
                HttpStatus.SC_FORBIDDEN, status);
        Assert.assertNotEquals("The SASS Compiler output is null.", HttpStatus.SC_INTERNAL_SERVER_ERROR, status);
    }

    /**
     * Mocks the required {@link HttpServletRequest} object so that the {@link CSSPreProcessServlet} can run without
     * problems.
     *
     * @param inputScssFile The test SCSS file that will be compiled.
     *
     * @return The mocked {@link HttpServletRequest} object.
     *
     * @throws DotDataException     An error occurred when retrieving data form dotCMS.
     * @throws DotSecurityException The specified User doesn't have the required permissions to perform this operation.
     * @throws IOException          Servlet output and/or writer objects failed.
     */
    private HttpServletRequest hydrateMockedRequest(final FileAsset inputScssFile) throws DotDataException, DotSecurityException {
        final HttpSession mockSession = Mockito.mock(HttpSession.class);
        when(mockSession.getAttribute(WebKeys.CMS_SELECTED_HOST_ID)).thenReturn(defaultSiteId);
        final DotCMSMockRequestWithSession mockRequest = new DotCMSMockRequestWithSession(mockSession, false);
        mockRequest.setRemoteHost(defaultSiteId);
        // Make sure that SCSS files are ALWAYS compiled
        mockRequest.setAttribute("recompile", "true");
        mockRequest.setAttribute(com.liferay.portal.util.WebKeys.USER, APILocator.getUserAPI().loadUserById("dotcms.org.1"));
        mockRequest.setRequestURI(inputScssFile.getURI());
        return mockRequest;
    }

}