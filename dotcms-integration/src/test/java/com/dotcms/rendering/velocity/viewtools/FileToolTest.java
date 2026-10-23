package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.util.WebKeys;
import com.dotcms.datagen.RoleDataGen;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.context.ViewContext;
import java.io.File;
import javax.servlet.http.HttpServletRequest;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileToolTest {

    private static final List<Host> sites = new ArrayList<>();
    private static final List<Role> roles = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @AfterClass
    public static void cleanUp() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        roles.forEach(role -> RoleDataGen.remove(role, true));
        sites.forEach(site -> {
            Try.run(() -> APILocator.getHostAPI().archive(site, APILocator.systemUser(), false));
            Try.run(() -> APILocator.getHostAPI().delete(site, APILocator.systemUser(), false));
        });
    }

    private static Host site() {
        final Host site = new SiteDataGen().nextPersisted();
        sites.add(site);
        return site;
    }

    @Test
    public void testGetFile_givenNotLiveFile_ShouldReturnNull()
            throws DotDataException, DotSecurityException {
        final File binary = new File(Thread.currentThread()
                .getContextClassLoader().getResource("images/test.jpg").getFile());
        final Host site = new SiteDataGen().nextPersisted();

        final Contentlet fileAsset = new FileAssetDataGen(site, binary).nextPersisted();

        final FileTool fileTool = new FileTool();
        final Contentlet result = fileTool.getFile(fileAsset.getIdentifier(),
                true, fileAsset.getLanguageId());
        assertNull(result);
    }

    /**
     * Method to test: {@link FileTool#getFile(String, boolean, long)
     * When: try to get a {@link com.dotmarketing.portlets.fileassets.business.FileAsset}
     * and the Current Variant is not the Default one,  and the FileAsset has a version inside the currentVariant
     * Should return the specific version of the FileAsset
     * <p>
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void getFileNotDefaultVariant()
            throws DotDataException, DotSecurityException {
        final File binary = new File(Thread.currentThread()
                .getContextClassLoader().getResource("images/test.jpg").getFile());
        final Host site = new SiteDataGen().nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(VariantAPI.VARIANT_KEY)).thenReturn(variant.name());
        when(request.getAttribute(WebKeys.USER)).thenReturn(APILocator.systemUser());

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final Contentlet fileAsset = new FileAssetDataGen(site, binary)
                .variant(variant)
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .nextPersisted();

        final FileTool fileTool = new FileTool();
        final Contentlet result = fileTool.getFile(fileAsset.getIdentifier(),
                false, fileAsset.getLanguageId());
        assertNotNull(result);

        assertEquals(variant.name(), result.getVariantId());
    }

    /**
     * Method to test: {@link FileTool#getFile(String, boolean, long)
     * When: try to get a {@link com.dotmarketing.portlets.fileassets.business.FileAsset} and:
     * - The Current Variant is not the Default one.
     * - the FileAsset has not a version inside the currentVariant but has a version inside the DEFAULT Variant.
     * Should: return the Default version of the FileAsset
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void getFileByFallback()
            throws DotDataException, DotSecurityException {
        final File binary = new File(Thread.currentThread()
                .getContextClassLoader().getResource("images/test.jpg").getFile());
        final Host site = new SiteDataGen().nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(VariantAPI.VARIANT_KEY)).thenReturn(variant.name());
        when(request.getAttribute(WebKeys.USER)).thenReturn(APILocator.systemUser());

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final Contentlet fileAsset = new FileAssetDataGen(site, binary)
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .nextPersisted();

        final FileTool fileTool = new FileTool();
        final Contentlet result = fileTool.getFile(fileAsset.getIdentifier(),
                false, fileAsset.getLanguageId());
        assertNotNull(result);

        assertEquals(VariantAPI.DEFAULT_VARIANT.name(), result.getVariantId());
    }

    /**
     * Method to test: {@link FileTool#getFile(String, boolean, long)
     * When: try to get a {@link com.dotmarketing.portlets.fileassets.business.FileAsset} and:
     * - The Current Variant is not the Default one.
     * - the FileAsset has a version inside the currentVariant and the DEFAULT Variant.
     * Should: return the specific Variant version of the FileAsset
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void getFileWithTwoVersions()
            throws DotDataException, DotSecurityException {
        final File binary = new File(Thread.currentThread()
                .getContextClassLoader().getResource("images/test.jpg").getFile());
        final Host site = new SiteDataGen().nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(VariantAPI.VARIANT_KEY)).thenReturn(variant.name());
        when(request.getAttribute(WebKeys.USER)).thenReturn(APILocator.systemUser());

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final Contentlet fileAsset = new FileAssetDataGen(site, binary)
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .nextPersisted();

        ContentletDataGen.createNewVersion(fileAsset, variant, null);

        final FileTool fileTool = new FileTool();
        final Contentlet result = fileTool.getFile(fileAsset.getIdentifier(),
                false, fileAsset.getLanguageId());
        assertNotNull(result);

        assertEquals(variant.name(), result.getVariantId());
    }

    /**
     * Method to test: {@link FileTool#getFile(String, boolean, long)
     * When: try to get a {@link com.dotmarketing.portlets.fileassets.business.FileAsset} and:
     * - The Current Variant is not the Default one.
     * - the FileAsset has a version inside the any other No Default Variant
     * Should: return the specific Variant version of the FileAsset
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void getFileWithVersionInAnotherVariant()
            throws DotDataException, DotSecurityException {
        final File binary = new File(Thread.currentThread()
                .getContextClassLoader().getResource("images/test.jpg").getFile());
        final Host site = new SiteDataGen().nextPersisted();

        final Variant variant_1 = new VariantDataGen().nextPersisted();
        final Variant variant_2 = new VariantDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(VariantAPI.VARIANT_KEY)).thenReturn(variant_1.name());
        when(request.getAttribute(WebKeys.USER)).thenReturn(APILocator.systemUser());

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final Contentlet fileAsset = new FileAssetDataGen(site, binary)
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .variant(variant_2)
                .nextPersisted();

        final FileTool fileTool = new FileTool();

        try {
            final Contentlet result = fileTool.getFile(fileAsset.getIdentifier(),
                    false, fileAsset.getLanguageId());

            throw  new AssertionError("Should throw an exception");
        } catch (DotDataException e) {

        }
    }

    private static FileTool fileToolFor(final User user) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        final ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getRequest()).thenReturn(request);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        final FileTool fileTool = new FileTool();
        fileTool.init(viewContext);
        return fileTool;
    }

    /**
     * Method to test: {@link FileTool#getFile(String, boolean, long)} and
     * {@link FileTool#getFileAsContentlet(String, boolean, long)}
     * When: the File Asset can only be read by a role the request user does not have
     * Should: return null for the anonymous user, and the File Asset for a user that can read it
     */
    @Test
    public void getFile_usesRequestUserPermissions() throws DotDataException, DotSecurityException {
        final File binary = new File(Thread.currentThread()
                .getContextClassLoader().getResource("images/test.jpg").getFile());
        final Host site = site();
        final Contentlet fileAsset = new FileAssetDataGen(site, binary)
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .nextPersisted();

        final Role otherRole = new RoleDataGen().nextPersisted();
        roles.add(otherRole);
        APILocator.getPermissionAPI().save(new Permission(fileAsset.getPermissionId(),
                        otherRole.getId(), PermissionAPI.PERMISSION_READ),
                fileAsset, APILocator.systemUser(), false);

        final User anonymous = APILocator.getUserAPI().getAnonymousUser();
        assertNull(fileToolFor(anonymous).getFile(fileAsset.getIdentifier(), false,
                fileAsset.getLanguageId()));
        assertNull(fileToolFor(anonymous).getFileAsContentlet(fileAsset.getIdentifier(), false,
                fileAsset.getLanguageId()));
        assertNull(fileToolFor(null).getFile(fileAsset.getIdentifier(), false,
                fileAsset.getLanguageId()));

        final Contentlet result = fileToolFor(APILocator.systemUser())
                .getFile(fileAsset.getIdentifier(), false, fileAsset.getLanguageId());
        assertNotNull(result);
        assertEquals(fileAsset.getIdentifier(), result.getIdentifier());
    }

    /**
     * Method to test: {@link FileTool#getFile(String, boolean, long)}
     * When: the anonymous user is allowed to read the live File Asset
     * Should: return it to the anonymous user
     */
    @Test
    public void getFile_anonymousCanReadPublicFile() throws DotDataException, DotSecurityException {
        final File binary = new File(Thread.currentThread()
                .getContextClassLoader().getResource("images/test.jpg").getFile());
        final Host site = site();
        final Contentlet fileAsset = new FileAssetDataGen(site, binary)
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .nextPersisted();

        APILocator.getPermissionAPI().save(new Permission(fileAsset.getPermissionId(),
                        APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                        PermissionAPI.PERMISSION_READ),
                fileAsset, APILocator.systemUser(), false);
        // anonymous users can only read live content
        ContentletDataGen.publish(fileAsset);

        final Contentlet result = fileToolFor(APILocator.getUserAPI().getAnonymousUser())
                .getFile(fileAsset.getIdentifier(), true, fileAsset.getLanguageId());
        assertNotNull(result);
        assertEquals(fileAsset.getIdentifier(), result.getIdentifier());
    }
}
