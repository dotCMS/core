package com.dotcms.integritycheckers;

import static com.dotcms.content.business.json.ContentletJsonAPI.SAVE_CONTENTLET_AS_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.com.csvreader.CsvWriter;
import com.google.common.io.Files;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class IntegrityUtilTest  {

    @Before
    public void setup() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link IntegrityUtil#completeCheckIntegrity(String)}
     * When: There is a folder with the same path but different inode and identifier
     * Should: Insert a register in folders_ir
     * @throws Exception
     */
    @Test
    @UseDataProvider("getUseJsonTestCases")
    public void folderWithConflicts(final Boolean useContentletAsJson) throws Exception {
        final boolean defaultValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, useContentletAsJson);
        try {
            final Host host = new SiteDataGen().nextPersisted();
            final Folder folderParent = new FolderDataGen()
                    .site(host)
                    .name("parent_" + System.currentTimeMillis())
                    .nextPersisted();

            final Folder folder = new FolderDataGen()
                    .site(host)
                    .parent(folderParent)
                    .nextPersisted();

            final Identifier identifier = APILocator.getIdentifierAPI()
                    .find(folder.getIdentifier());
            final String endPointID = "endpointID_" + System.currentTimeMillis();

            final String path = createCSVFile(endPointID).get(IntegrityType.FOLDERS);

            final CsvWriter foldersCsvWriter = new CsvWriter(path, '|', StandardCharsets.UTF_8);
            final long currentTimeMillis = System.currentTimeMillis();
            final String newInode_1 = String.valueOf(currentTimeMillis);
            final String newIdentifier_1 = String.valueOf(currentTimeMillis + 1);

            foldersCsvWriter.write(newInode_1);
            foldersCsvWriter.write(newIdentifier_1);
            foldersCsvWriter.write(identifier.getParentPath());
            foldersCsvWriter.write(folder.getName());
            foldersCsvWriter.write(folder.getHostId());
            foldersCsvWriter.endRecord();

            final String newInode_2 = String.valueOf(currentTimeMillis + 2);
            final String newIdentifier_2 = String.valueOf(currentTimeMillis + 3);

            foldersCsvWriter.write(newInode_2);
            foldersCsvWriter.write(newIdentifier_2);
            foldersCsvWriter.write("/any-path");
            foldersCsvWriter.write("folder-name");
            foldersCsvWriter.write(folder.getHostId());
            foldersCsvWriter.endRecord();

            foldersCsvWriter.flush();
            foldersCsvWriter.close();

            IntegrityUtil.completeCheckIntegrity(endPointID);

            DotConnect dc = new DotConnect();
            dc.setSQL("select * from folders_ir where endpoint_id = ?");
            dc.addObject(endPointID);
            final List<Map<String, Object>> results = dc.loadObjectResults();

            assertEquals(1, results.size());

            assertEquals(folder.getInode(), results.get(0).get("local_inode"));
            assertEquals(folder.getIdentifier(), results.get(0).get("local_identifier"));
            assertEquals(newInode_1, results.get(0).get("remote_inode"));
            assertEquals(newIdentifier_1, results.get(0).get("remote_identifier"));
            assertEquals(endPointID, results.get(0).get("endpoint_id"));
            assertEquals(host.getHostname() + folder.getPath(),
                    results.get(0).get("folder") + File.separator);
        }finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }
    }

    /**
     * Method to test: {@link IntegrityUtil#completeCheckIntegrity(String)}
     * When: There is a File Asset with the same path but different inode ad identifier
     * Should: Insert a register in fileassets_ir
     * @throws Exception
     */
    @Test
    public void contentFileWithConflicts() throws Exception {
        final Language language = new LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folderParent = new FolderDataGen()
                .site(host)
                .name("parent_" + System.currentTimeMillis())
                .nextPersisted();

        File tempFile = File.createTempFile("testFileAsset2-es", ".jpg");
        URL url = FocalPointAPITest.class.getResource("/images/test.jpg");
        File testImage = new File(url.getFile());
        FileUtils.copyFile(testImage, tempFile);

        final Contentlet contentlet = new FileAssetDataGen(tempFile)
                .folder(folderParent)
                .languageId(language.getId())
                .nextPersisted();

        final Contentlet liveContentlet = ContentletDataGen.publish(contentlet);
        final Contentlet checkout = ContentletDataGen.checkout(contentlet);
        final Contentlet workingContentlet = ContentletDataGen.checkin(checkout);

        final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet.getIdentifier());
        final String endPointID = "endpointID_" + System.currentTimeMillis();

        final String path = createCSVFile(endPointID).get(IntegrityType.FILEASSETS);

        final CsvWriter csvWriter = new CsvWriter(path, '|', StandardCharsets.UTF_8);
        final long currentTimeMillis = System.currentTimeMillis();
        final String newLiveInode_1 = String.valueOf(currentTimeMillis);
        final String newWorkingInode_1 = String.valueOf(currentTimeMillis + 1);
        final String newIdentifier_1 = String.valueOf(currentTimeMillis + 2);

        csvWriter.write(newWorkingInode_1);
        csvWriter.write(newLiveInode_1);
        csvWriter.write(newIdentifier_1);
        csvWriter.write(identifier.getParentPath());
        csvWriter.write(contentlet.getName());
        csvWriter.write(contentlet.getHost());
        csvWriter.write(String.valueOf(contentlet.getLanguageId()));
        csvWriter.endRecord();

        final String newLiveInode_2 = String.valueOf(currentTimeMillis + 3);
        final String newWorkingInode_2 = String.valueOf(currentTimeMillis + 4);
        final String newIdentifier_2 = String.valueOf(currentTimeMillis + 5);

        csvWriter.write(newWorkingInode_2);
        csvWriter.write(newLiveInode_2);
        csvWriter.write(newIdentifier_2);
        csvWriter.write("/any-path");
        csvWriter.write("folder-name");
        csvWriter.write(contentlet.getHost());
        csvWriter.write(String.valueOf(contentlet.getLanguageId()));
        csvWriter.endRecord();

        csvWriter.flush();
        csvWriter.close();

        IntegrityUtil.completeCheckIntegrity(endPointID);

        DotConnect dc = new DotConnect();
        dc.setSQL("select * from fileassets_ir where endpoint_id = ?");
        dc.addObject(endPointID);
        final List<Map<String, Object>> results = dc.loadObjectResults();

        assertEquals(1, results.size());

        assertEquals(liveContentlet.getInode(), results.get(0).get("local_live_inode"));
        assertEquals(newLiveInode_1, results.get(0).get("remote_live_inode"));
        assertEquals(workingContentlet.getInode(), results.get(0).get("local_working_inode"));
        assertEquals(newWorkingInode_1, results.get(0).get("remote_working_inode"));
        assertEquals(liveContentlet.getIdentifier(), results.get(0).get("local_identifier"));
        assertEquals(newIdentifier_1, results.get(0).get("remote_identifier"));
        assertEquals(endPointID, results.get(0).get("endpoint_id"));
        assertEquals(language.getId(), results.get(0).get("language_id"));
    }


    /**
     * Method to test: {@link IntegrityUtil#completeCheckIntegrity(String)}
     * When: There is a Page with the same path but different inode ad identifier
     * Should: Insert a register in htmlpages_ir
     * @throws Exception
     */
    @Test
    public void pageFileWithConflicts() throws Exception {
        final Language language = new LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folderParent = new FolderDataGen()
                .site(host)
                .name("parent_" + System.currentTimeMillis())
                .nextPersisted();

        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final Contentlet contentlet = new HTMLPageDataGen(host, template)
                .host(host)
                .folder(folderParent)
                .languageId(language.getId())
                .nextPersisted();

        final Contentlet liveContentlet = ContentletDataGen.publish(contentlet);
        final Contentlet checkout = ContentletDataGen.checkout(contentlet);
        final Contentlet workingContentlet = ContentletDataGen.checkin(checkout);

        final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet.getIdentifier());
        final String endPointID = "endpointID_" + System.currentTimeMillis();

        final String path = createCSVFile(endPointID).get(IntegrityType.HTMLPAGES);

        final CsvWriter csvWriter = new CsvWriter(path, '|', StandardCharsets.UTF_8);
        final long currentTimeMillis = System.currentTimeMillis();
        final String newLiveInode_1 = String.valueOf(currentTimeMillis);
        final String newWorkingInode_1 = String.valueOf(currentTimeMillis + 1);
        final String newIdentifier_1 = String.valueOf(currentTimeMillis + 2);

        csvWriter.write(newWorkingInode_1);
        csvWriter.write(newLiveInode_1);
        csvWriter.write(newIdentifier_1);
        csvWriter.write(identifier.getParentPath());
        csvWriter.write(((HTMLPageAsset) contentlet).getPageUrl());
        csvWriter.write(contentlet.getHost());
        csvWriter.write(String.valueOf(contentlet.getLanguageId()));
        csvWriter.endRecord();

        final String newLiveInode_2 = String.valueOf(currentTimeMillis + 3);
        final String newWorkingInode_2 = String.valueOf(currentTimeMillis + 4);
        final String newIdentifier_2 = String.valueOf(currentTimeMillis + 5);

        csvWriter.write(newWorkingInode_2);
        csvWriter.write(newLiveInode_2);
        csvWriter.write(newIdentifier_2);
        csvWriter.write("/any-path");
        csvWriter.write("folder-name");
        csvWriter.write(contentlet.getHost());
        csvWriter.write(String.valueOf(contentlet.getLanguageId()));
        csvWriter.endRecord();

        csvWriter.flush();
        csvWriter.close();

        IntegrityUtil.completeCheckIntegrity(endPointID);

        DotConnect dc = new DotConnect();
        dc.setSQL("select * from htmlpages_ir where endpoint_id = ?");
        dc.addObject(endPointID);
        final List<Map<String, Object>> results = dc.loadObjectResults();

        assertEquals(1, results.size());

        assertEquals(liveContentlet.getInode(), results.get(0).get("local_live_inode"));
        assertEquals(newLiveInode_1, results.get(0).get("remote_live_inode"));
        assertEquals(workingContentlet.getInode(), results.get(0).get("local_working_inode"));
        assertEquals(newWorkingInode_1, results.get(0).get("remote_working_inode"));
        assertEquals(liveContentlet.getIdentifier(), results.get(0).get("local_identifier"));
        assertEquals(newIdentifier_1, results.get(0).get("remote_identifier"));
        assertEquals(endPointID, results.get(0).get("endpoint_id"));
        assertEquals(language.getId(), results.get(0).get("language_id"));
    }

    /**
     * Method to test: {@link IntegrityUtil#completeCheckIntegrity(String)}
     * When: There is a Host with the same name but different inode ad identifier
     * Should: Insert a register in hosts_ir
     * @throws Exception
     */
    @Test
    public void hostFileWithConflicts() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        final Contentlet liveHost = ContentletDataGen.publish(host);
        final Contentlet checkout = ContentletDataGen.checkout(host);
        final Contentlet workingHost = ContentletDataGen.checkin(checkout);

        final Identifier identifier = APILocator.getIdentifierAPI().find(host.getIdentifier());
        final String endPointID = "endpointID_" + System.currentTimeMillis();

        final String path = createCSVFile(endPointID).get(IntegrityType.HOSTS);

        final CsvWriter csvWriter = new CsvWriter(path, '|', StandardCharsets.UTF_8);
        final long currentTimeMillis = System.currentTimeMillis();
        final String newLiveInode_1 = String.valueOf(currentTimeMillis);
        final String newWorkingInode_1 = String.valueOf(currentTimeMillis + 1);
        final String newIdentifier_1 = String.valueOf(currentTimeMillis + 2);

        csvWriter.write(newWorkingInode_1);
        csvWriter.write(newIdentifier_1);
        csvWriter.write(newWorkingInode_1);
        csvWriter.write(newLiveInode_1);
        csvWriter.write(String.valueOf(liveHost.getLanguageId()));
        csvWriter.write(String.valueOf(liveHost.getName()));
        csvWriter.endRecord();

        final String newLiveInode_2 = String.valueOf(currentTimeMillis + 3);
        final String newWorkingInode_2 = String.valueOf(currentTimeMillis + 4);
        final String newIdentifier_2 = String.valueOf(currentTimeMillis + 5);

        csvWriter.write(newWorkingInode_2);
        csvWriter.write(newIdentifier_2);
        csvWriter.write(newWorkingInode_2);
        csvWriter.write(newLiveInode_2);
        csvWriter.write(String.valueOf(liveHost.getLanguageId()));
        csvWriter.write("New Host");
        csvWriter.endRecord();

        csvWriter.flush();
        csvWriter.close();

        IntegrityUtil.completeCheckIntegrity(endPointID);

        DotConnect dc = new DotConnect();
        dc.setSQL("select * from hosts_ir where endpoint_id = ?");
        dc.addObject(endPointID);
        final List<Map<String, Object>> results = dc.loadObjectResults();

        assertEquals(1, results.size());

        assertEquals(liveHost.getInode(), results.get(0).get("local_live_inode"));
        assertEquals(newLiveInode_1, results.get(0).get("remote_live_inode"));
        assertEquals(workingHost.getInode(), results.get(0).get("local_working_inode"));
        assertEquals(newWorkingInode_1, results.get(0).get("remote_working_inode"));
        assertEquals(liveHost.getIdentifier(), results.get(0).get("local_identifier"));
        assertEquals(newIdentifier_1, results.get(0).get("remote_identifier"));
        assertEquals(endPointID, results.get(0).get("endpoint_id"));
        assertEquals(liveHost.getLanguageId(), ((Number)results.get(0).get("language_id")).longValue());
        assertEquals(liveHost.getName(), results.get(0).get("host"));
    }

    /**
     * Method to test: {@link IntegrityUtil#completeCheckIntegrity(String)}
     * When: There is a Role with the same name but different identifier
     * Should: Insert a register in roles_ir
     * @throws Exception
     */
    @Test
    public void roleFileWithConflicts() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final String endPointID = "endpointID_" + System.currentTimeMillis();

        final String path = createCSVFile(endPointID).get(IntegrityType.CMS_ROLES);

        final CsvWriter csvWriter = new CsvWriter(path, '|', StandardCharsets.UTF_8);
        final long currentTimeMillis = System.currentTimeMillis();
        final String newIdentifier_1 = String.valueOf(currentTimeMillis + 1);

        csvWriter.write(newIdentifier_1);
        csvWriter.write(role.getRoleKey());
        csvWriter.write(role.getName());
        csvWriter.write("role_parent id");
        csvWriter.write(role.getFQN());
        csvWriter.endRecord();

        final String newIdentifier_2 = String.valueOf(currentTimeMillis + 2);

        csvWriter.write(newIdentifier_2);
        csvWriter.write("role_key");
        csvWriter.write("role_name");
        csvWriter.write("role_parent id 2");
        csvWriter.write("role_parent name 2");
        csvWriter.endRecord();

        csvWriter.flush();
        csvWriter.close();

        IntegrityUtil.completeCheckIntegrity(endPointID);

        DotConnect dc = new DotConnect();
        dc.setSQL("select * from cms_roles_ir where endpoint_id = ?");
        dc.addObject(endPointID);
        final List<Map<String, Object>> results = dc.loadObjectResults();

        assertEquals(1, results.size());

        assertEquals(role.getId(), results.get(0).get("local_role_id"));
        assertEquals(newIdentifier_1, results.get(0).get("remote_role_id"));
        assertEquals(role.getDBFQN(), results.get(0).get("local_role_fqn"));
        assertEquals("role_parent id", results.get(0).get("remote_role_fqn"));
        assertEquals(role.getRoleKey(), results.get(0).get("role_key"));
        assertEquals(role.getName(), results.get(0).get("name"));
        assertEquals(endPointID, results.get(0).get("endpoint_id"));
    }

    /**
     * Method to test: {@link IntegrityUtil#completeCheckIntegrity(String)}
     * When: There is a ContentType with the same varName but different inode
     * Should: Insert a register in structres_ir
     * @throws Exception
     */
    @Test
    public void contentTypeWithConflicts() throws Exception {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final String endPointID = "endpointID_" + System.currentTimeMillis();

        final String path = createCSVFile(endPointID).get(IntegrityType.STRUCTURES);

        final CsvWriter csvWriter = new CsvWriter(path, '|', StandardCharsets.UTF_8);
        final long currentTimeMillis = System.currentTimeMillis();
        final String newInode_1 = String.valueOf(currentTimeMillis);

        csvWriter.write(newInode_1);
        csvWriter.write(contentType.variable());
        csvWriter.endRecord();

        final String newInode_2 = String.valueOf(currentTimeMillis + 1);

        csvWriter.write(newInode_2);
        csvWriter.write("new_content_type");
        csvWriter.endRecord();

        csvWriter.flush();
        csvWriter.close();

        IntegrityUtil.completeCheckIntegrity(endPointID);

        DotConnect dc = new DotConnect();
        dc.setSQL("select * from structures_ir where endpoint_id = ?");
        dc.addObject(endPointID);
        final List<Map<String, Object>> results = dc.loadObjectResults();

        assertEquals(1, results.size());

        assertEquals(contentType.variable(), results.get(0).get("velocity_name"));
        assertEquals(contentType.inode(), results.get(0).get("local_inode"));
        assertEquals(endPointID, results.get(0).get("endpoint_id"));
        assertEquals(newInode_1, results.get(0).get("remote_inode"));
    }


    /**
     * Method to test: {@link IntegrityUtil#fixConflicts(java.io.InputStream, String, IntegrityType)}
     * When: The uploaded roles-to-fix file carries a remote role id that contains a single quote
     * Should: Refuse the fix with the id in the message, keep the local role as it was and write no
     * row under the remote id
     */
    @Test
    public void roleFixWithQuotedRemoteIdIsRefused() throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        final String endPointID = "endpointID_" + System.currentTimeMillis();
        final String remoteRoleId = "r'" + UUID.randomUUID().toString().replace("-", "").substring(0, 30);

        try {
            final DotDataException error = assertThrows(DotDataException.class,
                    () -> fixRoleFromRemote(role, remoteRoleId, endPointID));
            assertTrue(error.getMessage(), error.getMessage().contains(remoteRoleId));

            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("select id, role_key from cms_role where id in (?, ?)")
                    .addParam(remoteRoleId)
                    .addParam(role.getId())
                    .loadObjectResults();
            assertEquals(1, rows.size());
            assertEquals(role.getId(), rows.get(0).get("id"));
            assertEquals(role.getRoleKey(), rows.get(0).get("role_key"));
        } finally {
            new DotConnect().setSQL("delete from cms_role where id = ?").addParam(remoteRoleId).loadResult();
            IntegrityUtil.completeDiscardConflicts(endPointID);
        }
    }

    /**
     * Method to test: {@link IntegrityUtil#fixConflicts(java.io.InputStream, String, IntegrityType)}
     * When: A role with a child role and a user is moved to a well-formed remote role id: a UUID, an
     * older id without dashes, or a fixed id made of letters and underscores
     * Should: Store the remote id with the original role key, and leave the moved role loadable with
     * its child and deletable through the Role API once the child is gone
     */
    @Test
    @UseDataProvider("wellFormedRemoteRoleIds")
    public void roleFixWithWellFormedRemoteIdKeepsRoleLoadableAndDeletable(final String remoteRoleId)
            throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final Role role = new RoleDataGen().nextPersisted();
        final Role child = new RoleDataGen().parent(role.getId()).nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();
        final String endPointID = "endpointID_" + System.currentTimeMillis();

        try {
            fixRoleFromRemote(role, remoteRoleId, endPointID);
            CacheLocator.getRoleCache().clearCache();

            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("select id, role_key from cms_role where id in (?, ?)")
                    .addParam(remoteRoleId)
                    .addParam(role.getId())
                    .loadObjectResults();
            assertEquals(1, rows.size());
            assertEquals(remoteRoleId, rows.get(0).get("id"));
            assertEquals(role.getRoleKey(), rows.get(0).get("role_key"));

            final Role moved = roleAPI.loadRoleById(remoteRoleId);
            assertNotNull(moved);
            assertEquals(List.of(child.getId()), moved.getRoleChildren());
            assertTrue(roleAPI.doesUserHaveRole(user, moved));

            roleAPI.delete(roleAPI.loadRoleById(child.getId()));
            roleAPI.delete(moved);

            assertTrue(new DotConnect().setSQL("select id from cms_role where id = ?")
                    .addParam(remoteRoleId).loadObjectResults().isEmpty());
            assertTrue(new DotConnect().setSQL("select id from users_cms_roles where role_id = ?")
                    .addParam(remoteRoleId).loadObjectResults().isEmpty());
        } finally {
            new DotConnect().setSQL("delete from cms_role where id = ?").addParam(child.getId()).loadResult();
            new DotConnect().setSQL("delete from users_cms_roles where role_id = ?").addParam(remoteRoleId).loadResult();
            new DotConnect().setSQL("delete from cms_role where id = ?").addParam(remoteRoleId).loadResult();
            UserDataGen.remove(user);
            CacheLocator.getRoleCache().clearCache();
            IntegrityUtil.completeDiscardConflicts(endPointID);
        }
    }

    /**
     * Remote role ids of the shapes dotCMS uses: a UUID, an older id without dashes, and a fixed id
     * made of letters and underscores.
     */
    @DataProvider
    public static Object[] wellFormedRemoteRoleIds() {
        final String suffix = UUID.randomUUID().toString().replace("-", "");
        return new Object[]{
                UUID.randomUUID().toString(),
                suffix,
                "LEGACY_ROLE_" + suffix.substring(0, 20)
        };
    }

    /**
     * Uploads a roles-to-fix file that moves the given local role to the given remote role id, the way
     * a push publishing end point sends it, and applies the fix.
     *
     * @param role the local role to move
     * @param remoteRoleId the id the remote end point uses for the role
     * @param endPointID the end point the conflict is recorded for
     * @throws Exception when the file cannot be written or the fix fails
     */
    private static void fixRoleFromRemote(final Role role, final String remoteRoleId,
            final String endPointID) throws Exception {
        final File zip = File.createTempFile("roles-to-fix", ".zip");
        try {
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
                out.putNextEntry(new ZipEntry(IntegrityType.CMS_ROLES.getDataToFixCSVName()));
                final String row = String.join("|", role.getName(), role.getRoleKey(), role.getId(),
                        remoteRoleId, role.getDBFQN(), remoteRoleId) + "\n";
                out.write(row.getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
            try (FileInputStream in = new FileInputStream(zip)) {
                new IntegrityUtil().fixConflicts(in, endPointID, IntegrityType.CMS_ROLES);
            }
        } finally {
            zip.delete();
        }
    }

    /**
     * Method to test: {@link IntegrityUtil#fixConflicts(String, IntegrityType)}
     * When: A content type conflict carries a remote inode, and the type has a contentlet and is the
     * default file type of a folder
     * Should: Move the content type, the contentlet's type (column and JSON) and the folder's default
     * file type to the remote inode
     */
    @Test
    public void contentTypeFixMovesReferencesToRemoteInode() throws Exception {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();
        final Folder folder = new FolderDataGen().defaultFileType(contentType.inode()).nextPersisted();
        final String endPointID = "endpointID_" + System.currentTimeMillis();
        final String remoteInode = UUID.randomUUID().toString();

        new DotConnect()
                .setSQL("insert into structures_ir (velocity_name, local_inode, remote_inode, endpoint_id) values (?,?,?,?)")
                .addParam(contentType.variable())
                .addParam(contentType.inode())
                .addParam(remoteInode)
                .addParam(endPointID)
                .loadResult();

        try {
            new IntegrityUtil().fixConflicts(endPointID, IntegrityType.STRUCTURES);

            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("select inode, velocity_var_name from structure where inode in (?, ?)")
                    .addParam(remoteInode)
                    .addParam(contentType.inode())
                    .loadObjectResults();
            assertEquals(1, rows.size());
            assertEquals(remoteInode, rows.get(0).get("inode"));
            assertEquals(contentType.variable(), rows.get(0).get("velocity_var_name"));

            final Map<String, Object> contentRow = new DotConnect()
                    .setSQL("select structure_inode, contentlet_as_json->>'contentType' as json_type"
                            + " from contentlet where inode = ?")
                    .addParam(contentlet.getInode())
                    .loadObjectResults().get(0);
            assertEquals(remoteInode, contentRow.get("structure_inode"));
            assertEquals(remoteInode, contentRow.get("json_type"));

            assertEquals(remoteInode, new DotConnect()
                    .setSQL("select default_file_type from folder where inode = ?")
                    .addParam(folder.getInode())
                    .loadObjectResults().get(0).get("default_file_type"));
        } finally {
            IntegrityUtil.completeDiscardConflicts(endPointID);
            Try.run(() -> FolderDataGen.remove(folder));
            Try.run(() -> ContentTypeDataGen.remove(
                    APILocator.getContentTypeAPI(APILocator.systemUser()).find(remoteInode), true));
        }
    }

    private String createCSVFile(final String endPointID, final IntegrityType integrityType) throws IOException {
        final String path = ConfigUtils.getIntegrityPath() + File.separator
                + endPointID + File.separator + integrityType.getDataToCheckCSVName();

        final File file = new File(path);
        file.getParentFile().mkdirs();
        file.createNewFile();
        return path;
    }

    private Map<IntegrityType, String> createCSVFile(final String endPointID) throws IOException {
        final Map<IntegrityType, String> files = new HashMap<>();

        for (IntegrityType integrityType : IntegrityType.values()) {
            final String csvFilePath = createCSVFile(endPointID, integrityType);
            files.put(integrityType, csvFilePath);
        }

        return files;
    }

    /**
     * Make sure test are executed under a contentlet stored as json and contentlet stored in column scenarios
     * @return
     * @throws Exception
     */
    @DataProvider
    public static Object[] getUseJsonTestCases() throws Exception {
        return new Object[]{
                true,
                false
        };
    }


}
