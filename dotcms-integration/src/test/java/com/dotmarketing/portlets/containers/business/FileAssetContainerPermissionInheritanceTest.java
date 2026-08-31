package com.dotmarketing.portlets.containers.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.ContainerAsFileDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Reproduces the permission-inheritance defect behind support ticket #38795 (re-open of #38576).
 * <p>
 * A File Asset Container lives in {@code /application/containers/} and is identified, for
 * permission purposes, by the {@code container.vtl} file asset:
 * {@link FileAssetContainer} extends {@link Container} extends
 * {@link com.dotmarketing.beans.WebAsset}, so {@code getPermissionId()} returns that file asset's
 * <b>identifier</b>, and {@link FileAssetContainer#getPermissionType()} is overridden to return
 * {@code Contentlet}. The {@code container.vtl} {@link Contentlet} itself reports exactly the same
 * permission id and the same permission type.
 * <p>
 * Both objects therefore collapse onto the <b>same</b> {@code permission_reference} row
 * {@code (asset_id, permission_type)}, but they resolve <b>different</b> parents:
 * <ul>
 *   <li>{@link Contentlet#getParentPermissionable()} returns the container <b>folder</b> -- correct,
 *       and the only parent that honours the folder's individual permissions.</li>
 *   <li>{@link Container#getParentPermissionable()} returns the <b>Site</b> (or System Host when the
 *       parent Site cannot be resolved) -- it never looks at the folder.</li>
 * </ul>
 * Whichever code path rebuilds the reference last wins and is persisted, so a limited user who was
 * granted View on the container folder silently loses the Container from the Template Builder
 * picker, and the loss survives permission-cache flushes.
 */
public class FileAssetContainerPermissionInheritanceTest {

    private static final String PERMISSION_TYPE_CONTENTLET =
            Contentlet.class.getCanonicalName();

    /** Stand-in for a missing permission_reference row, so failures read clearly. */
    private static final String NO_REFERENCE = "<no reference row>";

    private static User systemUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        // Make the permission_reference upsert synchronous so the test is deterministic.
        Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", false);
    }

    /**
     * Method to test: {@link com.dotmarketing.business.PermissionBitFactoryImpl} permission
     * reference resolution for a File Asset Container.
     * <p>
     * Given Scenario: a File Asset Container whose folder carries individual permissions granting
     * View to a limited role, on a Site that grants that role nothing. The permission reference for
     * {@code container.vtl} is rebuilt through the {@link Container} code path
     * ({@link ContainerAPI#getWorkingContainerById}).
     * <p>
     * Expected Result: the rebuilt {@code permission_reference} row must point at the container
     * <b>folder</b>, exactly as it does when the same asset is loaded as a {@link Contentlet}.
     */
    @Test
    public void permissionReferenceForFileAssetContainerMustResolveToContainerFolder()
            throws DotDataException, DotSecurityException {

        final TestScenario scenario = newScenario();

        // ---- Control: rebuild the reference through the Contentlet (file asset) path ----
        resetPermissionState(scenario.containerVtlId);
        final String afterResetOne = referenceIdFor(scenario.containerVtlId).orElse(NO_REFERENCE);
        loadPermissionsAsContentlet(scenario);
        final String afterContentletLoad =
                referenceIdFor(scenario.containerVtlId).orElse(NO_REFERENCE);

        // ---- Same asset, rebuilt through the FileAssetContainer path ----
        resetPermissionState(scenario.containerVtlId);
        final String afterResetTwo = referenceIdFor(scenario.containerVtlId).orElse(NO_REFERENCE);
        loadPermissionsAsContainer(scenario);
        final String afterContainerLoad =
                referenceIdFor(scenario.containerVtlId).orElse(NO_REFERENCE);

        // Everything observed, reported on either outcome so a failure explains itself.
        final String observed = String.format(
                "%n  container folder inode : %s"
              + "%n  site id                : %s"
              + "%n  after 1st reset        : %s"
              + "%n  after Contentlet load  : %s"
              + "%n  after 2nd reset        : %s"
              + "%n  after Container  load  : %s%n",
                scenario.containerFolder.getInode(), scenario.site.getIdentifier(),
                afterResetOne, afterContentletLoad, afterResetTwo, afterContainerLoad);

        assertEquals("Both resets must actually clear the reference row, otherwise neither load "
                        + "below performs a parent walk-up and this test proves nothing." + observed,
                NO_REFERENCE + "|" + NO_REFERENCE, afterResetOne + "|" + afterResetTwo);

        assertEquals("Loaded as a Contentlet, container.vtl must inherit from its container "
                        + "folder." + observed,
                scenario.containerFolder.getInode(), afterContentletLoad);

        assertEquals("Loaded as a FileAssetContainer, container.vtl must STILL inherit from its "
                        + "container folder -- resolving to the Site or System Host skips the "
                        + "folder's permissions entirely." + observed,
                scenario.containerFolder.getInode(), afterContainerLoad);
    }

    /**
     * Method to test: {@link ContainerAPI#findContainers(User, ContainerAPI.SearchParams)}
     * <p>
     * Given Scenario: the limited user can see the Container in the picker; then some other request
     * rebuilds the permission reference through the {@link Container} code path.
     * <p>
     * Expected Result: the limited user must still see the Container. The folder permissions did
     * not change, so the visibility must not change either.
     */
    @Test
    public void limitedUserMustKeepSeeingContainerAfterReferenceIsRebuiltAsContainer()
            throws DotDataException, DotSecurityException {

        final TestScenario scenario = newScenario();

        // The limited user sees the Container while the reference points at the folder.
        resetPermissionState(scenario.containerVtlId);
        loadPermissionsAsContentlet(scenario);

        assertTrue("Precondition: the limited user must see the Container in the picker",
                isContainerVisibleTo(scenario, scenario.limitedUser));

        // Anything that reloads permissions through the Container path re-poisons the reference,
        // e.g. ContainerAPI.getWorkingContainerById(), which the Page/Template APIs call.
        resetPermissionState(scenario.containerVtlId);
        loadPermissionsAsContainer(scenario);

        assertTrue("The limited user must still see the Container -- nothing about the folder's "
                        + "permissions changed",
                isContainerVisibleTo(scenario, scenario.limitedUser));
    }

    /**
     * Method to test: {@link com.dotmarketing.business.PermissionBitFactoryImpl} permission
     * reference resolution.
     * <p>
     * Given Scenario: the reference is rebuilt through the {@link Container} path, so it points at
     * the Site instead of the container folder. A Site-level inheritable View grant exists for a
     * <i>different</i> role (the "reviewer" role in the customer's environment).
     * <p>
     * Expected Result: the role that was granted View on the folder must keep View on the
     * Container, and must not be overtaken by the Site-level grant given to another role.
     */
    @Test
    public void folderGrantedRoleMustNotLoseViewToSiteLevelGrantOfAnotherRole()
            throws DotDataException, DotSecurityException {

        final TestScenario scenario = newScenario();

        // A second role, granted View at Site level with inheritance -- this mirrors the customer's
        // "Reviewer" roles, which are present in the Site's inheritable set while the "Editor"
        // roles are not.
        final Role reviewerRole = new RoleDataGen().nextPersisted();
        // View on the Site itself, so the picker can resolve the Site for this user at all.
        // Without it findFolderAssetContainers() catches the DotSecurityException and returns an
        // empty list, which would fail the assertion below for the wrong reason.
        APILocator.getPermissionAPI().save(
                new Permission(scenario.site.getPermissionId(), reviewerRole.getId(),
                        PermissionAPI.PERMISSION_READ, true),
                scenario.site, systemUser, false);
        // Inheritable View on the Site's child content. This is the grant the customer's Reviewer
        // roles hold, and the reason they keep the Container while the Editor roles lose it.
        APILocator.getPermissionAPI().save(
                new Permission(PERMISSION_TYPE_CONTENTLET, scenario.site.getPermissionId(),
                        reviewerRole.getId(), PermissionAPI.PERMISSION_READ, true),
                scenario.site, systemUser, false);
        final User reviewerUser = new UserDataGen()
                .roles(reviewerRole, APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();

        resetPermissionState(scenario.containerVtlId);
        loadPermissionsAsContainer(scenario);

        assertTrue("Sanity check: the Site-level grant makes the Container visible to the reviewer",
                isContainerVisibleTo(scenario, reviewerUser));

        assertTrue("The editor role was granted View on the container folder, so it must see the "
                        + "Container too", isContainerVisibleTo(scenario, scenario.limitedUser));
    }

    // ------------------------------------------------------------------ helpers

    private static final class TestScenario {
        Host site;
        Folder containerFolder;
        FileAssetContainer container;
        String containerVtlId;
        Role editorRole;
        User limitedUser;
    }

    /**
     * Builds the customer's configuration: a Site, a File Asset Container under
     * {@code /application/containers/}, and a limited role that is granted View on the container
     * folder only -- never at Site level.
     */
    private TestScenario newScenario() throws DotDataException, DotSecurityException {

        final TestScenario scenario = new TestScenario();
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

        scenario.site = new SiteDataGen().nextPersisted();

        final String folderName = "agency-default-" + System.currentTimeMillis();
        scenario.container = new ContainerAsFileDataGen()
                .host(scenario.site)
                .folderName(folderName)
                .nextPersisted();
        scenario.containerVtlId = scenario.container.getIdentifier();

        scenario.containerFolder = APILocator.getFolderAPI().findFolderByPath(
                Constants.CONTAINER_FOLDER_PATH + "/" + folderName + "/",
                scenario.site, systemUser, false);
        assertNotNull("The container folder must exist", scenario.containerFolder);

        scenario.editorRole = new RoleDataGen().nextPersisted();
        // The Back-end User role is required. PermissionBitAPIImpl refuses READ on a non-live
        // Contentlet for any user that is not a back-end user, and container.vtl only ever has a
        // working version here -- without this the Container is filtered out of the picker for a
        // reason that has nothing to do with the defect under test.
        scenario.limitedUser = new UserDataGen()
                .roles(scenario.editorRole, APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();

        // Break inheritance on the container folder and grant the editor role View on the folder
        // and, inheritably, on the folder's child content -- this is the supported way of granting
        // a limited user access to a File Asset Container.
        // The editor role can browse the Site itself -- without this the Container picker cannot
        // even resolve the Site for this user and returns nothing, which would mask the defect.
        permissionAPI.save(
                new Permission(scenario.site.getPermissionId(), scenario.editorRole.getId(),
                        PermissionAPI.PERMISSION_READ, true),
                scenario.site, systemUser, false);

        permissionAPI.permissionIndividually(scenario.site, scenario.containerFolder, systemUser);
        permissionAPI.save(
                new Permission(scenario.containerFolder.getPermissionId(),
                        scenario.editorRole.getId(), PermissionAPI.PERMISSION_READ, true),
                scenario.containerFolder, systemUser, false);
        permissionAPI.save(
                new Permission(PERMISSION_TYPE_CONTENTLET,
                        scenario.containerFolder.getPermissionId(),
                        scenario.editorRole.getId(), PermissionAPI.PERMISSION_READ, true),
                scenario.containerFolder, systemUser, false);

        return scenario;
    }

    /**
     * Clears every cached and persisted permission reference for the asset, so the next permission
     * load has to walk the parent hierarchy again -- the state left behind by a push publish, a
     * folder save or a permission cache flush.
     */
    private void resetPermissionState(final String assetId) throws DotDataException {
        new DotConnect().setSQL("delete from permission_reference where asset_id = ?")
                .addParam(assetId).loadResult();
        CacheLocator.getPermissionCache().clearCache();
    }

    /**
     * Rebuilds the permission reference through the {@link Contentlet} code path -- what the
     * Container picker does when it permission-filters the {@code container.vtl} file assets in
     * {@code ContainerFactoryImpl.findContainersAssetsByHost()}.
     */
    private void loadPermissionsAsContentlet(final TestScenario scenario)
            throws DotDataException, DotSecurityException {

        final Contentlet containerVtl = APILocator.getContentletAPI()
                .findContentletByIdentifier(scenario.containerVtlId, false,
                        APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        systemUser, false);
        assertNotNull("container.vtl must be loadable as a Contentlet", containerVtl);
        assertEquals("Both objects must share the same permission id",
                scenario.containerVtlId, containerVtl.getPermissionId());
        assertEquals("Both objects must share the same permission type",
                PERMISSION_TYPE_CONTENTLET, containerVtl.getPermissionType());
        // getPermissions() goes straight into PermissionBitFactoryImpl.loadPermissions(), with no
        // user short-circuit, so the walk-up and the permission_reference upsert always happen.
        APILocator.getPermissionAPI().getPermissions(containerVtl);
    }

    /**
     * Rebuilds the permission reference through the {@link Container} code path -- what
     * {@link ContainerAPI#find(String, User, boolean)} does when it checks READ on the
     * {@link FileAssetContainer} itself.
     */
    private void loadPermissionsAsContainer(final TestScenario scenario)
            throws DotDataException, DotSecurityException {

        final Container container = APILocator.getContainerAPI()
                .getWorkingContainerById(scenario.containerVtlId, systemUser, false);
        assertNotNull("The File Asset Container must be loadable", container);
        assertTrue("The Container must be a FileAssetContainer",
                container instanceof FileAssetContainer);
        assertEquals("The FileAssetContainer must share container.vtl's permission id",
                scenario.containerVtlId, container.getPermissionId());
        assertEquals("The FileAssetContainer must share container.vtl's permission type",
                PERMISSION_TYPE_CONTENTLET, container.getPermissionType());
        APILocator.getPermissionAPI().getPermissions(container);
    }

    /** Reads the persisted {@code permission_reference} row for the asset. */
    private Optional<String> referenceIdFor(final String assetId) throws DotDataException {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("select reference_id from permission_reference "
                        + "where asset_id = ? and permission_type = ?")
                .addParam(assetId)
                .addParam(PERMISSION_TYPE_CONTENTLET)
                .loadObjectResults();
        return rows.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable((String) rows.get(0).get("reference_id"));
    }

    /** Mirrors what the Template Builder container picker returns for a user. */
    private boolean isContainerVisibleTo(final TestScenario scenario, final User user)
            throws DotDataException, DotSecurityException {

        final ContainerAPI.SearchParams searchParams = ContainerAPI.SearchParams.newBuilder()
                .siteId(scenario.site.getIdentifier())
                .includeArchived(false)
                .includeSystemContainer(false)
                .build();
        return APILocator.getContainerAPI().findContainers(user, searchParams).stream()
                .anyMatch(found -> scenario.containerVtlId.equals(found.getIdentifier()));
    }
}
