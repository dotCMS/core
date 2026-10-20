package com.dotcms.rest.api.v1.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.company.CompanyAPI;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.storage.model.Metadata;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.liferay.portal.model.User;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the license parsing and the stock login logo lookup in
 * {@link CompanyConfigHelper}.
 *
 * @author hassandotcms
 */
class CompanyConfigHelperTest {

    private static final String STOCK_ID = "a083e942-5fb0-47ef-9619-6f8f40593474";
    private static final String STOCK_SHA =
            "6f59e8d34e7b67b14948f228110f14ef13bf2f19d949dc19ea3da08cd7ef74d0";
    private static final String LIVE_INODE = "52f6c0e2-7978-483b-a0a2-c589044db9e7";

    /**
     * The header values are read from the license file, trimmed, and the title carries the
     * license version.
     */
    @Test
    void parseLicenseInfo_readsHeaderValuesAndVersion() {
        final String text = "Licensor:             dotCMS LLC\n\n"
                + "Change Date:     Four years from August 01, 2025\n\n"
                + "Change License:  GNU General Public License (GPL) v3\n\n"
                + "Business Source License 1.1\n\nTerms...";

        final LicenseInfoView info = CompanyConfigHelper.parseLicenseInfo(text);

        assertEquals("dotCMS Business Source License 1.1", info.title());
        assertEquals("dotCMS LLC", info.licensor());
        assertEquals("Four years from August 01, 2025", info.changeDate());
        assertEquals("GNU General Public License (GPL) v3", info.changeLicense());
        assertEquals(text, info.text());
    }

    /**
     * When the file can't be read, the fallback sentence still gives a usable response.
     */
    @Test
    void parseLicenseInfo_fallbackText_givesDefaultTitleAndNullFields() {
        final String fallback = "Please see the LICENSE file in the root directory of the dotCMS "
                + "git hub repo: https://github.com/dotCMS/core/blob/main/LICENSE";

        final LicenseInfoView info = CompanyConfigHelper.parseLicenseInfo(fallback);

        assertEquals("dotCMS Business Source License", info.title());
        assertNull(info.licensor());
        assertNull(info.changeDate());
        assertNull(info.changeLicense());
        assertEquals(fallback, info.text());
    }

    // ==================== Stock login logo ====================

    private ContentletAPI contentletAPI;
    private VersionableAPI versionableAPI;
    private PermissionAPI permissionAPI;
    private Contentlet live;
    private ContentletVersionInfo versionInfo;
    private User anonymous;
    private CompanyConfigHelper helper;

    /**
     * Stubs a stock logo that passes every check; each test breaks one of them.
     */
    @BeforeEach
    void stubStockLogo() throws Exception {
        contentletAPI = mock(ContentletAPI.class);
        versionableAPI = mock(VersionableAPI.class);
        permissionAPI = mock(PermissionAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final User systemUser = mock(User.class);
        anonymous = mock(User.class);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(userAPI.getAnonymousUser()).thenReturn(anonymous);

        versionInfo = versionInfo(1L, LIVE_INODE);
        stubVersions(versionInfo);

        live = mock(Contentlet.class);
        when(contentletAPI.find(LIVE_INODE, systemUser, false)).thenReturn(live);
        when(permissionAPI.doesUserHavePermission(live, PermissionAPI.PERMISSION_READ,
                anonymous, true)).thenReturn(true);
        stubFile(STOCK_SHA, "company_logo.png");

        helper = new CompanyConfigHelper(mock(CompanyAPI.class), contentletAPI,
                versionableAPI, permissionAPI, userAPI);
    }

    private static ContentletVersionInfo versionInfo(final long lang, final String liveInode) {
        final ContentletVersionInfo info = new ContentletVersionInfo();
        info.setIdentifier(STOCK_ID);
        info.setLang(lang);
        info.setWorkingInode(LIVE_INODE);
        info.setLiveInode(liveInode);
        return info;
    }

    private void stubVersions(final ContentletVersionInfo... infos) throws Exception {
        when(versionableAPI.findContentletVersionInfos(STOCK_ID, VariantAPI.DEFAULT_VARIANT.name()))
                .thenReturn(List.of(infos));
    }

    private void stubFile(final String sha256, final String name) throws Exception {
        when(live.getBinaryMetadata(DotAssetContentType.ASSET_FIELD_VAR)).thenReturn(
                new Metadata(DotAssetContentType.ASSET_FIELD_VAR,
                        Map.<String, Serializable>of("sha256", sha256, "name", name)));
    }

    /**
     * A published, anonymously readable stock logo gives its {@code /dA} path, the same on
     * every call.
     */
    @Test
    void findStockLoginLogoPath_found_returnsDotAssetPath() throws Exception {
        final Optional<String> expected =
                Optional.of("/dA/" + STOCK_ID + "/asset/company_logo.png");

        assertEquals(expected, helper.findStockLoginLogoPath());
        assertEquals(expected, helper.findStockLoginLogoPath());
    }

    /**
     * The path uses the file name the published version actually has.
     */
    @Test
    void findStockLoginLogoPath_renamedFile_pathUsesActualName() throws Exception {
        stubFile(STOCK_SHA, "renamed.png");

        assertEquals(Optional.of("/dA/" + STOCK_ID + "/asset/renamed.png"),
                helper.findStockLoginLogoPath());
    }

    /**
     * No content with the stock identifier gives no path.
     */
    @Test
    void findStockLoginLogoPath_missing_returnsEmpty() throws Exception {
        stubVersions();

        assertEquals(Optional.empty(), helper.findStockLoginLogoPath());
    }

    /**
     * When only another language is published, that version is used, whatever order the
     * versions come back in.
     */
    @Test
    void findStockLoginLogoPath_publishedInOtherLanguageOnly_isFound() throws Exception {
        final ContentletVersionInfo published = versionInfo(2L, LIVE_INODE);
        stubVersions(versionInfo(1L, null), published);

        assertEquals(Optional.of("/dA/" + STOCK_ID + "/asset/company_logo.png"),
                helper.findStockLoginLogoPath());
    }

    /**
     * An archived stock logo gives no path.
     */
    @Test
    void findStockLoginLogoPath_archived_returnsEmpty() throws Exception {
        versionInfo.setDeleted(true);

        assertEquals(Optional.empty(), helper.findStockLoginLogoPath());
    }

    /**
     * A stock logo with no published version gives no path.
     */
    @Test
    void findStockLoginLogoPath_notPublished_returnsEmpty() throws Exception {
        versionInfo.setLiveInode(null);

        assertEquals(Optional.empty(), helper.findStockLoginLogoPath());
    }

    /**
     * A stock logo the anonymous user can't read gives no path, since the login page couldn't
     * load it.
     */
    @Test
    void findStockLoginLogoPath_anonymousCannotRead_returnsEmpty() throws Exception {
        when(permissionAPI.doesUserHavePermission(live, PermissionAPI.PERMISSION_READ,
                anonymous, true)).thenReturn(false);

        assertEquals(Optional.empty(), helper.findStockLoginLogoPath());
    }

    /**
     * A stock logo whose file is not the stock dotCMS image gives no path.
     */
    @Test
    void findStockLoginLogoPath_otherFile_returnsEmpty() throws Exception {
        stubFile("0000000000000000000000000000000000000000000000000000000000000000",
                "company_logo.png");

        assertEquals(Optional.empty(), helper.findStockLoginLogoPath());
    }

    /**
     * A stock logo without file details gives no path.
     */
    @Test
    void findStockLoginLogoPath_noMetadata_returnsEmpty() throws Exception {
        when(live.getBinaryMetadata(DotAssetContentType.ASSET_FIELD_VAR)).thenReturn(null);

        assertEquals(Optional.empty(), helper.findStockLoginLogoPath());
    }
}
