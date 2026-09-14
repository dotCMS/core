package com.dotcms.enterprise.publishing.remote.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.enterprise.publishing.remote.bundler.FolderBundler;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.FolderWrapper;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.business.FolderFactoryImpl;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that {@link FolderHandler} — the Push Publishing receiver-side handler for
 * {@code *.folder.xml} bundle files — persists every folder property sent by the authoring server.
 *
 * @see <a href="https://github.com/dotCMS/core/issues/37459">Issue 37459</a>
 */
public class FolderHandlerTest extends IntegrationTestBase {

    private static User systemUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
        systemUser = APILocator.getUserAPI().getSystemUser();
    }

    /**
     * Writes the given wrapper where {@link FolderHandler#handle(File)} expects it —
     * {@code <bundle>/ROOT/<identifier>.folder.xml} — mirroring what {@link FolderBundler}
     * produces on the sender, and returns the bundle root to hand to the handler.
     */
    private static File writeBundle(final FolderWrapper wrapper) throws IOException {
        final File bundleRoot = Files.createTempDirectory("folder-handler-test").toFile();
        final File bundleFile = new File(bundleRoot, "ROOT" + File.separator
                + wrapper.getFolder().getIdentifier() + FolderBundler.FOLDER_EXTENSION);
        BundlerUtil.objectToXML(wrapper, bundleFile);
        return bundleRoot;
    }

    /**
     * Reads the folder row straight from the database, bypassing every cache, the same way the
     * issue's reproduction steps verify the receiver.
     */
    private static Map<String, Object> findFolderRow(final String inode) throws DotDataException {
        final List<Map<String, Object>> results = new DotConnect()
                .setSQL("select * from folder where inode = ?")
                .addParam(inode)
                .loadObjectResults();
        assertFalse("Folder row must exist for inode " + inode, results.isEmpty());
        return results.get(0);
    }

    private static PushPublisherConfig newConfig() {
        final PushPublisherConfig config = new PushPublisherConfig();
        config.setId("folder-handler-test-" + System.currentTimeMillis());
        return config;
    }

    /**
     * Builds a detached copy of an existing folder, simulating the folder object deserialized from
     * a sender's bundle: same inode/identifier/path, different mutable properties.
     */
    private static Folder detachedSenderCopy(final Folder existing, final long millis) {
        final Folder sender = new Folder();
        sender.setInode(existing.getInode());
        sender.setIdentifier(existing.getIdentifier());
        sender.setHostId(existing.getHostId());
        sender.setName(existing.getName());
        sender.setTitle("pushed-title-" + millis);
        sender.setShowOnMenu(!existing.isShowOnMenu());
        sender.setSortOrder(existing.getSortOrder() + 7);
        sender.setFilesMasks("*.png,*.jpg");
        sender.setDefaultFileType(existing.getDefaultFileType());
        sender.setDefaultBaseType(BaseContentType.DOTASSET.name());
        sender.setOwner("pp-sender-owner-" + millis);
        sender.setModDate(new Date(millis));
        // second precision so the value survives the java.util.Date -> java.sql.Timestamp round-trip
        sender.setIDate(new Date(((millis - 86_400_000L) / 1_000L) * 1_000L));
        return sender;
    }

    /**
     * Method to test: {@link FolderHandler#handle(File)}, update branch (folder already exists on
     * the receiver).
     * Given scenario: a folder exists locally; a bundle arrives carrying the same folder with a
     * flipped {@code showOnMenu}, a set {@code defaultBaseType} and distinct values for every
     * other mutable property.
     * Expected result: every column upserted by
     * {@code FolderFactoryImpl#upsertFolder(Folder)} reflects the sender's value. Iterating
     * {@link FolderFactoryImpl#getUpsertExtraColumns()} makes this test fail when a future folder
     * column is added without being copied in {@code FolderHandler}'s update branch, which is
     * exactly how issue #37459 happened twice (PR #32341 dropped {@code showOnMenu}, PR #36649
     * never added {@code defaultBaseType}).
     */
    @Test
    public void testHandle_existingFolder_copiesEveryUpsertColumn() throws Exception {
        final Host site = new SiteDataGen().nextPersisted();
        final Folder existing = new FolderDataGen().site(site).showOnMenu(false).nextPersisted();

        final long millis = System.currentTimeMillis();
        final Folder sender = detachedSenderCopy(existing, millis);

        final Identifier folderId = APILocator.getIdentifierAPI().find(existing.getIdentifier());
        final Identifier hostId = APILocator.getIdentifierAPI().find(site.getIdentifier());
        final FolderWrapper wrapper = new FolderWrapper(sender, folderId, site, hostId,
                Operation.PUBLISH);

        new FolderHandler(newConfig()).handle(writeBundle(wrapper));

        final Map<String, Object> row = findFolderRow(existing.getInode());
        for (final String column : FolderFactoryImpl.getUpsertExtraColumns()) {
            switch (column) {
                case "name":
                    // a matching path is the precondition of the update branch, so it never changes
                    assertEquals(existing.getName(), row.get("name"));
                    break;
                case "identifier":
                    assertEquals(existing.getIdentifier(), row.get("identifier"));
                    break;
                case "title":
                    assertEquals(sender.getTitle(), row.get("title"));
                    break;
                case "show_on_menu":
                    assertEquals("show_on_menu must match the sender's value after the push (#37459)",
                            sender.isShowOnMenu(),
                            DbConnectionFactory.isDBTrue(String.valueOf(row.get("show_on_menu"))));
                    break;
                case "sort_order":
                    assertEquals(sender.getSortOrder(), ((Number) row.get("sort_order")).intValue());
                    break;
                case "files_masks":
                    assertEquals(sender.getFilesMasks(), row.get("files_masks"));
                    break;
                case "default_file_type":
                    assertEquals(sender.getDefaultFileType(), row.get("default_file_type"));
                    break;
                case "owner":
                    assertEquals(sender.getOwner(), row.get("owner"));
                    break;
                case "idate":
                    assertEquals(sender.getIDate().getTime(), ((Date) row.get("idate")).getTime());
                    break;
                case "mod_date":
                    // FolderAPIImpl#save stamps mod_date with the receiver's clock; not assertable
                    break;
                case "default_base_type":
                    assertEquals("default_base_type must match the sender's value after the push (#37459)",
                            sender.getDefaultBaseType(), row.get("default_base_type"));
                    break;
                default:
                    fail("Column '" + column + "' was added to FolderFactoryImpl.UPSERT_EXTRA_COLUMNS but is"
                            + " not covered by this test. Copy it in FolderHandler's update branch (the"
                            + " explicit setter block — see issue #37459) and assert it here, or push"
                            + " publishing will silently drop it when the folder already exists on the"
                            + " receiver.");
            }
        }
    }

    /**
     * Method to test: {@link FolderHandler#handle(File)}, update branch.
     * Given scenario: the folder exists on the receiver with {@code showOnMenu=true} and a bundle
     * arrives turning the flag off.
     * Expected result: the flag is disabled on the receiver — the fix must work in both
     * directions, not just when enabling.
     */
    @Test
    public void testHandle_existingFolder_disablesShowOnMenu() throws Exception {
        final Host site = new SiteDataGen().nextPersisted();
        final Folder existing = new FolderDataGen().site(site).showOnMenu(true).nextPersisted();

        final Folder sender = detachedSenderCopy(existing, System.currentTimeMillis());
        assertFalse(sender.isShowOnMenu());

        final Identifier folderId = APILocator.getIdentifierAPI().find(existing.getIdentifier());
        final Identifier hostId = APILocator.getIdentifierAPI().find(site.getIdentifier());
        new FolderHandler(newConfig())
                .handle(writeBundle(new FolderWrapper(sender, folderId, site, hostId, Operation.PUBLISH)));

        final Map<String, Object> row = findFolderRow(existing.getInode());
        assertFalse("Disabling show_on_menu on the sender must disable it on the receiver (#37459)",
                DbConnectionFactory.isDBTrue(String.valueOf(row.get("show_on_menu"))));
    }

    /**
     * Method to test: {@link FolderHandler#handle(File)}, create branch (folder does not exist on
     * the receiver yet).
     * Given scenario: a bundle arrives for a folder unknown to the receiver, with
     * {@code showOnMenu=true} and a {@code defaultBaseType} set.
     * Expected result: the first push persists both values — guarding against a regression to the
     * create branch while fixing the update branch.
     */
    @Test
    public void testHandle_newFolder_carriesShowOnMenuAndDefaultBaseType() throws Exception {
        final Host site = new SiteDataGen().nextPersisted();
        final long millis = System.currentTimeMillis();
        final String name = "pp-new-folder-" + millis;

        final Folder sender = new Folder();
        sender.setInode(UUIDGenerator.generateUuid());
        sender.setIdentifier(UUIDGenerator.generateUuid());
        sender.setHostId(site.getIdentifier());
        sender.setName(name);
        sender.setTitle(name);
        sender.setShowOnMenu(true);
        sender.setSortOrder(3);
        sender.setFilesMasks("*.pdf");
        sender.setDefaultFileType(new FolderDataGen().next().getDefaultFileType());
        sender.setDefaultBaseType(BaseContentType.FILEASSET.name());
        sender.setOwner(systemUser.getUserId());
        sender.setModDate(new Date(millis));
        sender.setIDate(new Date(millis));

        // the sender-side Identifier of a folder that has never existed on this receiver
        final Identifier folderId = new Identifier();
        folderId.setId(sender.getIdentifier());
        folderId.setAssetName(name);
        folderId.setParentPath("/");
        folderId.setAssetType("folder");
        folderId.setHostId(site.getIdentifier());

        final Identifier hostId = APILocator.getIdentifierAPI().find(site.getIdentifier());
        new FolderHandler(newConfig())
                .handle(writeBundle(new FolderWrapper(sender, folderId, site, hostId, Operation.PUBLISH)));

        final Map<String, Object> row = findFolderRow(sender.getInode());
        assertTrue("A first push must persist show_on_menu on the receiver",
                DbConnectionFactory.isDBTrue(String.valueOf(row.get("show_on_menu"))));
        assertEquals("A first push must persist default_base_type on the receiver",
                sender.getDefaultBaseType(), row.get("default_base_type"));
    }
}
