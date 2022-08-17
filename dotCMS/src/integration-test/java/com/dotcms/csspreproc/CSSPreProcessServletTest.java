package com.dotcms.csspreproc;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.mock.response.DotCMSMockResponse;
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
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.util.StringPool;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.when;

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
     *     must be zero.</li>
     * </ul>
     */
    @Test
    public void testCompileValidScssFile() {
        executeTest(inputSimpleScssFile, false);
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link CSSPreProcessServlet#doGet(HttpServletRequest, HttpServletResponse)}</li>
     *     <li><b>Given Scenario:</b> Compiling a simple valid SCSS file with an @import directive. This adds more
     *     complexity to the process as it forces dotCMS to scan files in a different folder.</li>
     *     <li><b>Expected Result:</b> Valid CSS code is returned. Because of the Servlet being mocked, the status code
     *     must be zero.</li>
     * </ul>
     */
    @Test
    public void testCompileScssFileWithImports() {
        executeTest(inputScssFileWithImports, false);
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link CSSPreProcessServlet#doGet(HttpServletRequest, HttpServletResponse)}</li>
     *     <li><b>Given Scenario:</b> Compiling an invalid SCSS file. This failure must NOT cause harmful exceptions in
     *     the system as the page rendering process must finish as usual.</li>
     *     <li><b>Expected Result:</b> By default, an error message is added as part of the CSS response indicating the
     *     situation to the user.</li>
     * </ul>
     */
    @Test
    public void testCompileInvalidScssFile() {
        executeTest(inputInvalidScssFile, true);
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
     * @param inputScssFile The SCSS file that will be compiled.
     * @param expectError   If the SCSS compilation process is expected to fail based on the input data, set this to
     *                      {@code true}.
     */
    private void executeTest(final FileAsset inputScssFile, final boolean expectError) {
        // Initialization
        final HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockResponse = Mockito.mock(DotCMSMockResponse.class);
        final HttpSession mockSession = Mockito.mock(HttpSession.class);
        final MockServletOutputStream mockServletOutputStream = new MockServletOutputStream();

        String cssCode = StringPool.BLANK;
        int status = -1;
        try {
            hydrateMockedObjects(mockRequest, mockResponse, mockSession, mockServletOutputStream, inputScssFile);
            final CSSPreProcessServlet servlet = new CSSPreProcessServlet();
            servlet.doGet(mockRequest, mockResponse);
            cssCode = mockServletOutputStream.toString();
            status = mockResponse.getStatus();
        } catch (final ServletException | IOException | DotDataException | DotSecurityException e) {
            Assert.fail(String.format("An error occurred when compiling the test SCSS file '%s'. Aborting test " +
                                              "execution...", inputScssFile));
        }
        // Assertions
        if (expectError) {
            Assert.assertTrue("The SASS compilation must have failed with an error message.", cssCode.startsWith(
                    "/* Error:"));
        } else {
            Assert.assertTrue("There must be compiled CSS code in the response.", UtilMethods.isSet(cssCode));
        }
        Assert.assertNotEquals("The SCSS file could not be found.", HttpStatus.SC_NOT_FOUND, status);
        Assert.assertNotEquals("The SCSS file could not be read by the specified dotCMS user.",
                HttpStatus.SC_FORBIDDEN, status);
        Assert.assertNotEquals("The SASS Compiler output is null.", HttpStatus.SC_INTERNAL_SERVER_ERROR, status);
    }

    /**
     * Mocks all the required objects so that the {@link CSSPreProcessServlet} can run without problems.
     *
     * @param mockRequest             Mocked request.
     * @param mockResponse            Mocked response.
     * @param mockSession             Mocked session.
     * @param mockServletOutputStream Mocked Servlet Output Stream used to retrieve the actual response.
     * @param inputScssFile           Test SCSS file that will be compiled.
     *
     * @throws DotDataException     An error occurred when retrieving data form dotCMS.
     * @throws DotSecurityException The specified User doesn't have the required permissions to perform this operation.
     * @throws IOException          Servlet output and/or writer objects failed.
     */
    private void hydrateMockedObjects(final HttpServletRequest mockRequest, final HttpServletResponse mockResponse,
                                      final HttpSession mockSession,
                                      final MockServletOutputStream mockServletOutputStream,
                                      final FileAsset inputScssFile) throws DotDataException, DotSecurityException, IOException {
        when(mockRequest.getParameter("host_id")).thenReturn(defaultSiteId);
        when(mockRequest.getParameter("recompile")).thenReturn("true");
        when(mockRequest.getRequestURI()).thenReturn(inputScssFile.getURI());
        when(mockRequest.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(APILocator.getUserAPI().loadUserById("dotcms.org.1"));
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockResponse.getOutputStream()).thenReturn(mockServletOutputStream);
        when(mockResponse.getWriter()).thenReturn(new PrintWriter(mockServletOutputStream));
        when(mockSession.getAttribute(WebKeys.CMS_SELECTED_HOST_ID)).thenReturn(defaultSiteId);
    }

    /**
     * Mock ServletOutputStream for testing purposes only.
     */
    private class MockServletOutputStream extends ServletOutputStream {

        String out = null;

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {

        }

        @Override
        public void write(int b) {

        }

        @Override
        public void write(byte b[]) {
            out = new String(b, StandardCharsets.UTF_8);
        }

        @Override
        public String toString() {
            return out;
        }

    }

}