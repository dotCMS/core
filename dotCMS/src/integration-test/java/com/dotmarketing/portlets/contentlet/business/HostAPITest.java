package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.StructureDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.HostAssetsJobProxy;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.quartz.job.HostCopyOptions;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.quartz.Trigger;

import static com.dotmarketing.portlets.templates.model.Template.ANONYMOUS_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class will test operations related with interacting with hosts: Deleting
 * a host, marking a host as default, etc.
 *
 * @author Jorge Urdaneta
 * @since Sep 5, 2013
 *
 */
public class HostAPITest extends IntegrationTestBase  {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        LicenseTestUtil.getLicense();
        DotInitScheduler.start();
    }

    /**
     * This test validates the Content Type under the deleted host is also deleted with the host
     */
    @Test
    public void delete_host_with_content_type() throws Exception {
        deleteHostWithContentType(false, false);
    }

    /**
     * This test validates the Content Type under the deleted host is NOT deleted as it is a default
     * Content Type but the host is changed to SYSTEM_HOST
     */
    @Test
    public void delete_host_with_default_type_content_type() throws Exception {
        deleteHostWithContentType(true, false);
    }

    /**
     * This test validates the Content Type under the deleted host is NOT deleted as it is a system
     * Content Type but the host is changed to SYSTEM_HOST
     */
    @Test
    public void delete_host_with_system_content_type() throws Exception {
        deleteHostWithContentType(false, true);
    }

    @Test
    public void testDeleteHostCleanUpTemplates() throws Exception {



        final User user = APILocator.getUserAPI().getSystemUser();
        final Host host = new SiteDataGen().nextPersisted();

        final DotConnect dc = new DotConnect();
        final String query = "select inode.inode from inode left outer join template on template.inode = inode.inode where template.inode is null and inode.type='template'";

        //Verifies that the environment is clean
        dc.setSQL(query);
        assertTrue(dc.loadObjectResults().isEmpty());

        final String body = "<html><body> I'm mostly empty </body></html>";
        final String title = ANONYMOUS_PREFIX + UUIDGenerator.generateUuid();

        Template template = new Template();
        template.setTitle(title);
        template.setBody(body);
        template = APILocator.getTemplateAPI().saveTemplate(template, host, user, false);

        assertNotNull(template);
        assertNotNull(template.getInode());

        assertFalse(template.isShowOnMenu());

        archiveHost(host, user);
        deleteHost(host, user);

        dc.setSQL(query);

        assertTrue(dc.loadObjectResults().isEmpty());

    }

    @Test
    public void testDeleteHost() throws Exception {

        User user = APILocator.getUserAPI().getSystemUser();

        Host source = new SiteDataGen().nextPersisted();
        final ContentType blogContentType = TestDataUtils
                .getBlogLikeContentType("Blog" + System.currentTimeMillis(), source);
        final ContentType employeeContentType = TestDataUtils
                .getEmployeeLikeContentType("Employee" + System.currentTimeMillis(), source);
        final ContentType newsContentType = TestDataUtils
                .getNewsLikeContentType("News" + System.currentTimeMillis(), source);

        TestDataUtils.getBlogContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                blogContentType.id(), source);
        TestDataUtils.getBlogContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                blogContentType.id(), source);
        TestDataUtils
                .getEmployeeContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        employeeContentType.id(), source);
        TestDataUtils
                .getEmployeeContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        employeeContentType.id(), source);
        TestDataUtils.getNewsContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                newsContentType.id(), source);
        TestDataUtils.getNewsContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                newsContentType.id(), source);

        TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                source);
        TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                source);
        TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                source);

        //Create a new test host
        Host host = createHost("copy" + System.currentTimeMillis() + ".demo.dotcms.com", user);

        Thread.sleep(5000);
        String newHostIdentifier = host.getIdentifier();
        String newHostName = host.getHostname();

        // mocking JobExecutionContext to execute HostAssetsJobProxy
        final JobDetail jobDetail = mock(JobDetail.class);
        final JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
        final Trigger trigger = mock(Trigger.class);
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
        when(jobExecutionContext.getJobDetail().getName())
                .thenReturn("setup-host-" + host.getIdentifier());
        when(jobExecutionContext.getJobDetail().getGroup()).thenReturn("setup-host-group");

        final Map dataMap = new HashMap<>();
        dataMap.put(HostAssetsJobProxy.USER_ID, user.getUserId());
        dataMap.put(HostAssetsJobProxy.SOURCE_HOST_ID, source.getIdentifier());
        dataMap.put(HostAssetsJobProxy.DESTINATION_HOST_ID, host.getIdentifier());
        dataMap.put(HostAssetsJobProxy.COPY_OPTIONS, new HostCopyOptions(true));
        when(jobExecutionContext.getTrigger()).thenReturn(trigger);

        HostAssetsJobProxy hostAssetsJobProxy = Mockito.spy(new HostAssetsJobProxy());
        doReturn(dataMap).when(hostAssetsJobProxy).getExecutionData(trigger);
        hostAssetsJobProxy.execute(jobExecutionContext);

        Thread.sleep(600); // wait a bit for the index

        //Archive the just created host in order to be able to delete it
        archiveHost(host, user);

        //Delete the just created host
        deleteHost(host, user);

        //Make sure the host was deleted properly
        hostDoesNotExistCheck(newHostIdentifier, newHostName, user);
    }

    @Test
    public void makeDefault() throws Exception {

        User user = APILocator.getUserAPI().getSystemUser();

        //Getting the default host
        Host defaultHost = APILocator.getHostAPI().findDefaultHost(user, false);
        defaultHost.setIndexPolicy(IndexPolicy.WAIT_FOR);
        if (!defaultHost.isLive()) {
            APILocator.getHostAPI().publish(defaultHost, user, false);
        }

        //Create a new test host
        final String newHostName = "test" + System.currentTimeMillis() + ".dotcms.com";
        Host host = createHost(newHostName, user);
        String newHostIdentifier = host.getIdentifier();

        //Publish the host
        host.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getHostAPI().publish(host, user, false);
        //And make it default
        host.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getHostAPI().makeDefault(host, user, false);

        host = APILocator.getHostAPI().find(host.getIdentifier(), user, false);
        defaultHost = APILocator.getHostAPI().find(defaultHost.getIdentifier(), user, false);
        assertNotNull(host);
        assertNotNull(defaultHost);

        /*
         * Validate if the previous default host. Is live and not default
         */
        assertTrue(defaultHost.isLive());
        assertFalse(defaultHost.isDefault());

        /*
         * get Back to default the previous host
         */
        APILocator.getHostAPI().makeDefault(defaultHost, user, false);

        host = APILocator.getHostAPI().find(host.getIdentifier(), user, false);
        defaultHost = APILocator.getHostAPI().find(defaultHost.getIdentifier(), user, false);
        assertNotNull(host);
        assertNotNull(defaultHost);

        /*
         * Validate if the new host is not default anymore and if its live
         */
        assertTrue(host.isLive());
        assertFalse(host.isDefault());

        assertTrue(defaultHost.isLive());
        assertTrue(defaultHost.isDefault());

        //Unpublish, archive and delete the host
        unpublishHost(host, user);
        archiveHost(host, user);
        deleteHost(host, user);

        //Make sure the host was deleted properly
        hostDoesNotExistCheck(newHostIdentifier, newHostName, user);

        /*
         * Validate if the current Original default host is the current default one
         */
        host = APILocator.getHostAPI().findDefaultHost(user, false);
        Assert.assertEquals(defaultHost.getIdentifier(), host.getIdentifier());
    }

    @Test
    public void givenSearch_whenNewHost_thenFindsNewHost() throws Exception {

        User user = APILocator.getUserAPI().getSystemUser();

        new SiteDataGen().name("demo.test2" + System.currentTimeMillis() + ".dotcms.com")
                .nextPersisted();

        final String newHostName = "demo.test" + System.currentTimeMillis() + ".dotcms.com";
        //Create a new test host
        Host host = createHost(newHostName, user);
        final String newHostIdentifier = host.getIdentifier();

        //Publish the host
        host.setIndexPolicy(IndexPolicy.FORCE);
        APILocator.getHostAPI().publish(host, user, false);

        PaginatedArrayList<Host> hosts = APILocator.getHostAPI()
                .search("demo", Boolean.FALSE, Boolean.FALSE, 0, 0, user, Boolean.TRUE);
        //Validate if the search is bringing the right amount of results
        assertTrue(hosts.size() >= 2 && hosts.getTotalResults() >= 2);
        assertTrue(hosts.contains(host));

        //Do a more specific search
        hosts = APILocator.getHostAPI()
                .search(newHostName, Boolean.FALSE, Boolean.FALSE, 0, 0, user, Boolean.TRUE);
        //Validate if the search is bringing the right amount of results
        assertTrue(hosts.size() == 1 && hosts.getTotalResults() == 1);
        Assert.assertEquals(hosts.get(0).getHostname(), newHostName);

        //Unpublish, archive and delete the host
        unpublishHost(host, user);
        archiveHost(host, user);
        deleteHost(host, user);

        //Make sure the host was deleted properly
        hostDoesNotExistCheck(newHostIdentifier, newHostName, user);

        hosts = APILocator.getHostAPI()
                .search("nothing", Boolean.FALSE, Boolean.FALSE, 0, 0, user, Boolean.TRUE);
        //Validate if the search doesn't bring results
        assertTrue(hosts.size() == 0 && hosts.getTotalResults() == 0);
    }

    /**
     * Utility method to verify if Content Types under a just deleted host are also deleted or no,
     * the idea is to validate that Content Types under the deleted host are deleted also EXCEPT for
     * Default or System Content Types. If the deleted host have System or Default content types we
     * need to make sure the host for those Content Types is changed to SYSTEM_HOST.
     */
    private void deleteHostWithContentType(boolean defaultType, boolean system)
            throws DotDataException, DotSecurityException, ExecutionException, InterruptedException {

        User user = APILocator.getUserAPI().getSystemUser();

        //Get the current default content type
        ContentType existingDefaultContentType = null;
        if (defaultType) {
            existingDefaultContentType = APILocator.getContentTypeAPI(user).findDefault();
        }

        final String newHostName = "test" + System.currentTimeMillis() + ".dotcms.com";

        //Create a new test host
        Host host = createHost(newHostName, user);
        String newHostIdentifier = host.getIdentifier();

        //Archive the just created host in order to be able to delete it
        archiveHost(host, user);

        //Create a test content type
        final ContentType testContentType = createContentType(host, defaultType, system, user);

        //Delete the just created host
        deleteHost(host, user);

        //Make sure the host was deleted properly
        hostDoesNotExistCheck(newHostIdentifier, newHostName, user);

        if (defaultType || system) {

            //Make sure the content type was NOT deleted
            try {
                final ContentType foundContentType = APILocator.getContentTypeAPI(user)
                        .find(testContentType.variable());
                assertNotNull(
                        foundContentType);
                Assert.assertEquals(system, foundContentType.system());
                Assert.assertEquals(defaultType, foundContentType.defaultType());
                Assert.assertEquals(testContentType.system(), foundContentType.system());
                Assert.assertEquals(testContentType.defaultType(),
                        foundContentType.defaultType());
                Assert.assertEquals(testContentType.id(), foundContentType.id());
                Assert.assertEquals(testContentType.variable(),
                        foundContentType.variable());

                //Make sure the host was changed to SYSTEM_HOST
                Assert.assertEquals(APILocator.getHostAPI().findSystemHost().getIdentifier(),
                        foundContentType.host());
            } catch (Exception e) {
                Assert.fail(String.format("Unable to create delete test content type [%s] [%s]",
                        testContentType.id(),
                        e.getMessage()));
            } finally {

                //Cleaning up the test data
                try {
                    if (defaultType && null != existingDefaultContentType) {
                        APILocator.getContentTypeAPI(user).setAsDefault(existingDefaultContentType);
                    }
                } catch (Exception e) {
                    //Do nothing...
                }

                try {
                    ContentType clonedContentType = ContentTypeBuilder.builder(testContentType)
                            .system(false).defaultType(false).build();
                    APILocator.getContentTypeAPI(user).delete(clonedContentType);
                } catch (Exception e) {
                    //Do nothing...
                }
            }
        } else {

            //Make sure the content type was deleted also
            try {
                final ContentType foundContentType = APILocator.getContentTypeAPI(user)
                        .find(testContentType.variable());
                Assert.assertNull(
                        foundContentType);//The find should throw NotFoundInDbException but just in case
            } catch (NotFoundInDbException e) {
                //Expected, the content type should be deleted
            } catch (Exception e) {
                Assert.fail(String.format("Unable to create delete test content type [%s] [%s]",
                        testContentType.id(),
                        e.getMessage()));
            }
        }
    }

    /**
     * Creates a test content type for a given host
     */
    private ContentType createContentType(final Host host, final boolean defaultType,
            final boolean system,
            final User user)
            throws DotDataException, DotSecurityException {

        Structure structure = new StructureDataGen()
                .structureType(BaseContentType.CONTENT)
                .system(system)
                .host(host)
                .nextPersisted();

        ContentType contentType = new StructureTransformer(structure).from();
        if (defaultType) {
            contentType = APILocator.getContentTypeAPI(user).setAsDefault(contentType);
        }

        final String structureId = structure.id();
        final String structureVarName = structure.getVelocityVarName();
        assertNotNull(structureId);
        assertNotNull(structureVarName);

        //Make sure was created properly
        ContentType foundContentType = APILocator.getContentTypeAPI(user).find(structureVarName);
        assertNotNull(foundContentType);
        Assert.assertEquals(structureId, foundContentType.id());
        Assert.assertEquals(defaultType, foundContentType.defaultType());
        Assert.assertEquals(system, foundContentType.system());
        Assert.assertEquals(structureVarName, foundContentType.variable());
        Assert.assertEquals(host.getIdentifier(), foundContentType.host());

        return contentType;
    }

    /**
     * Creates a test host with a given host name
     */
    private Host createHost(final String hostName, final User user) throws DotHibernateException {

        Host host = new Host();
        host.setHostname(hostName);
        host.setDefault(false);
        try {
            HibernateUtil.startTransaction();
            host.setIndexPolicy(IndexPolicy.FORCE);
            host = APILocator.getHostAPI().save(host, user, false);
            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(HostAPITest.class, e.getMessage(), e);
            Assert.fail(String.format("Unable to create test host [%s] [%s]", hostName,
                    e.getMessage()));
        }

        return host;
    }

    /**
     * Unpublish a given host
     */
    private void unpublishHost(final Host host, final User user) throws DotHibernateException {

        try {
            HibernateUtil.startTransaction();
            host.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getHostAPI().unpublish(host, user, false);
            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(HostAPITest.class, e.getMessage(), e);
            Assert.fail(String.format("Unable to unpublish test host [%s] [%s]", host.getHostname(),
                    e.getMessage()));
        }
    }

    /**
     * Archives a given host
     */
    private void archiveHost(final Host host, final User user) throws DotHibernateException {

        try {
            HibernateUtil.startTransaction();
            host.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getHostAPI().archive(host, user, false);
            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(HostAPITest.class, e.getMessage(), e);
            Assert.fail(String.format("Unable to archive test host [%s] [%s]", host.getHostname(),
                    e.getMessage()));
        }
    }

    /**
     * Deletes a given host
     */
    private void deleteHost(final Host host, final User user)
            throws DotHibernateException, InterruptedException, ExecutionException {

        Optional<Future<Boolean>> hostDeleteResult = Optional.empty();
        try {
            HibernateUtil.startTransaction();
            host.setIndexPolicy(IndexPolicy.FORCE);
            hostDeleteResult = APILocator.getHostAPI().delete(host, user, false, true);
            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(HostAPITest.class, e.getMessage());
            Assert.fail(String.format("Unable to delete test host [%s] [%s]", host.getHostname(),
                    e.getMessage()));
        }

        if (!hostDeleteResult.isPresent()) {
            Thread.sleep(6000); // wait a bit for the index
        } else {
            hostDeleteResult.get().get();
        }
    }

    /**
     * Verifies if a just deleted host was properly deleted
     */
    private void hostDoesNotExistCheck(final String identifier, final String name, final User user)
            throws DotSecurityException, DotDataException {

        //Verify the Host does not exist any more
        Host host = APILocator.getHostAPI().find(identifier, user, false);
        Assert.assertNull(host);

        host = APILocator.getHostAPI().findByName(name, user, false);
        Assert.assertNull(host);
    }

    /**
     * Method to test: {@link HostAPI#resolveHostNameWithoutDefault(String, User, boolean)}
     * When a host exist and the user have permission
     * Should return it
     */
    @Test
    public void shouldReturnExistingHost() throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        this.addPermission(role, host);

        final Host hostReturned = APILocator.getHostAPI().resolveHostNameWithoutDefault(host.getHostname(), user, false).get();
        assertEquals(host, hostReturned);
    }

    /**
     * Method to test: {@link HostAPI#resolveHostNameWithoutDefault(String, User, boolean)}
     * When a host exist but the user does not have permission
     * Should throw a {@link DotSecurityException}
     */
    @Test(expected = DotSecurityException.class)
    public void shouldThrowDotSecurityExceptionWhenUserNotHavePermission() throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        APILocator.getHostAPI().resolveHostNameWithoutDefault(host.getHostname(), user, false);
    }

    /**
     * Method to test: {@link HostAPI#resolveHostNameWithoutDefault(String, User, boolean)}
     * When the host does not exist
     * Should return null
     */
    @Test
    public void shouldReturnNull() throws DotSecurityException, DotDataException {

        final Optional<Host> optional = APILocator.getHostAPI().resolveHostNameWithoutDefault(
                "not_exists_host", APILocator.systemUser(), false);
        assertFalse(optional.isPresent());
    }

    /**
     * Method to test: {@link HostAPI#resolveHostName(String, User, boolean)}
     * When the host does not exist
     * Should return the default host and store it into host cache
     */
    @Test
    public void shouldStoreDefaultHostIntoCache() throws DotSecurityException, DotDataException {
        final String hostName = "not_exists_host";
        final Host notExistsHost = APILocator.getHostAPI().resolveHostName(hostName
                , APILocator.systemUser(), false);
        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), true);
        assertEquals(notExistsHost.getIdentifier(), defaultHost.getIdentifier());

        final HostCache hostCache = CacheLocator.getHostCache();
        final Host hostByAlias = hostCache.getHostByAlias(hostName);

        assertEquals(hostByAlias.getIdentifier(), defaultHost.getIdentifier());
    }

    /**
     * Method to test: {@link HostAPI#resolveHostNameWithoutDefault(String, User, boolean)}
     * When a host exist but the user does not have permission and respectFrontendRoles is true
     * Should return the host
     */
    @Test()
    public void shouldReturnHostWhenRespectFrontendRolesIsTrue() throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        final Host hostReturned = APILocator.getHostAPI().resolveHostNameWithoutDefault(host.getHostname(), user, true).get();
        assertEquals(host, hostReturned);
    }

    private void addPermission(final Role role, final Host host)
            throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();

        final Permission permission = new Permission();
        permission.setInode(host.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(PermissionAPI.PERMISSION_READ);

        APILocator.getPermissionAPI().save(CollectionsUtils.list(permission), host, systemUser, false);
    }

    /**
     * Method to test: {@link HostAPI#findByAlias(String, User, boolean)}
     * When create two host: first one with alias equals to demo.dotcms.com and second  one with alias equals to not-demo.dotcms.com
     *      and find by  demo.dotcms.com
     * Should return the first one
     */
    @Test
    public void shouldReturnHostByAlias() throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().aliases("demo.dotcms.com").nextPersisted();
        final Host host_2 = new SiteDataGen().aliases("not-demo.dotcms.com").nextPersisted();

        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        this.addPermission(role, host);
        this.addPermission(role, host_2);

        final Host hostReturned = APILocator.getHostAPI().findByAlias("demo.dotcms.com", user, false);
        assertEquals(host, hostReturned);
        assertNotEquals(host_2, hostReturned);
    }

    /**
     * Method to test: {@link HostAPI#findByAlias(String, User, boolean)}
     * When create one host with multiple alias
     * Should return thehost by alias
     */
    @Test
    public void whenHostHasMultipleAliasshouldReturnHostByAlias() throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().aliases("demo.dotcms.com\r\ntest.dotcms.com").nextPersisted();

        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        this.addPermission(role, host);

        final Host hostReturned = APILocator.getHostAPI().findByAlias("test.dotcms.com", user, false);
        assertEquals(host, hostReturned);
    }

    /**
     * Method to test: {@link HostAPI#findByAlias(String, User, boolean)}
     * When create two host with alias that both start by prod-
     * Should return the right host by alias
     */
    @Test
    public void whenBothAliasStartByProd() throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().aliases("prod-client.dotcms.com").nextPersisted();
        final Host host_2 = new SiteDataGen().aliases("prod-anotherclient.dotcms.com").nextPersisted();

        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        this.addPermission(role, host);
        this.addPermission(role, host_2);

        final Host hostReturned = APILocator.getHostAPI().findByAlias("prod-client.dotcms.com", user, false);
        assertEquals(host, hostReturned);
        assertNotEquals(host_2, hostReturned);

        final Host hostReturned2 = APILocator.getHostAPI().findByAlias("prod-anotherclient.dotcms.com", user, false);
        assertNotEquals(host, hostReturned2);
        assertEquals(host_2, hostReturned2);
    }

    /**
     * Method to test: {@link HostAPI#findAllFromCache(User, boolean)}
     * This verifies that after creating and removing a host the method continues to return accurate results.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void Test_findAllCache() throws DotSecurityException, DotDataException {
        final User systemUser = APILocator.systemUser();
        final HostAPI hostAPI = APILocator.getHostAPI();
        final List<Host> allFromDB1 = hostAPI.findAllFromDB(systemUser, false);
        final List<Host> allFromCache1 = hostAPI.findAllFromCache(systemUser, false);
        Assert.assertEquals(allFromDB1, allFromCache1);
        final Host host1 = new SiteDataGen().aliases("any.client.dotcms.com").nextPersisted();
        final List<Host> allFromCache2 = hostAPI.findAllFromCache(systemUser, false);
        assertTrue(allFromCache1.size() < allFromCache2.size());
        hostAPI.archive(host1, systemUser, false);
        hostAPI.delete(host1, systemUser, false);
        final List<Host> allFromCache3 = hostAPI.findAllFromCache(systemUser, false);
        assertEquals(allFromCache3.size() , allFromCache1.size());
    }

    /**
     * Host must be Language independent.
     * Even though they can be assigned with a language we should not relay on the lang id to find it.
     * Method to test: {@link HostAPI#DBSearch(String, User, boolean)}
     * This Test is meant to verify a changed introduced in DBSearch to find ContentletVersionInfo regardless of the language
     * Also it verifies there is always only one entry on version-info for every contentlet of type Host.
     * Regardless of the operation we perform.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Host_With_Multiple_Lang_Versions_Return_Default_Lang_OtherWise_First_Occurrence()
            throws DotDataException, DotSecurityException {

        final LanguageAPI languageAPI = APILocator.getLanguageAPI();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final HostAPI hostAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();
        final VersionableAPI versionableAPI = APILocator.getVersionableAPI();

        final SiteDataGen siteDataGen = new SiteDataGen();

        Host host = siteDataGen.name("xyx" + System.currentTimeMillis())
                .aliases("xyz.dotcms.com").next();

        host.setLanguageId(0);
        host = siteDataGen.persist(host, false);

        //Host are created by default under the default language.
        Assert.assertEquals(languageAPI.getDefaultLanguage().getId(), host.getLanguageId());

        List<ContentletVersionInfo> verInfos = versionableAPI
                .findContentletVersionInfos(host.getIdentifier());

        Assert.assertEquals("There should be only one entry.", 1, verInfos.size());

        Host dbSearch = hostAPI.DBSearch(host.getIdentifier(), systemUser, false);
        Assert.assertNotNull(dbSearch);

        Assert.assertEquals(verInfos.get(0).getWorkingInode(), dbSearch.getInode());

        Assert.assertNull("There shouldn't be a live version yet.", verInfos.get(0).getLiveInode());

        contentletAPI.publish(host, systemUser, false);

        verInfos = versionableAPI.findContentletVersionInfos(host.getIdentifier());

        Assert.assertNotNull("There should be a live version now.", verInfos.get(0).getLiveInode());

        //This should create another version of the host in a different language.
        final Language newLanguage = new LanguageDataGen().languageName("ES").nextPersisted();

        host.setLanguageId(newLanguage.getId());
        hostAPI.save(host, systemUser, false);

        verInfos = versionableAPI
                .findContentletVersionInfos(host.getIdentifier());
        Assert.assertEquals("There should be two entries one for each language.", 2, verInfos.size());

        final long newRandomDefaultLangId = -1;
        final Language mockDefaultLang = mock(Language.class);
        when(mockDefaultLang.getId()).thenReturn(newRandomDefaultLangId);

        final HostAPI hostAPIWithMockedLangAPI = new HostAPIImpl(APILocator.getSystemEventsAPI());
        final Host dbSearchNoDefaultLang = hostAPIWithMockedLangAPI.DBSearch(host.getIdentifier(), systemUser, false);

        //Since Default language is changed now we still get something here.
        assertNotNull(dbSearchNoDefaultLang);
        //And System-host should still be system host
        assertNotNull(hostAPIWithMockedLangAPI.findSystemHost());

    }

    @Test(expected = RuntimeException.class)
    public void Test_SiteKey_InValid_DNS_Format_Expect_Failure()  {
        final String propName = "site.key.dns.validation";
        final boolean propValue = Config.getBooleanProperty(propName, false);
        Config.setProperty(propName, true);
        try {
            final String testHost = "test_host_" + System.currentTimeMillis() + ".dotcms.com";
            new SiteDataGen().name(testHost).nextPersisted();
        }finally {
            Config.setProperty(propName, propValue);
        }
    }

    @Test
    public void Test_SiteKey_Valid_DNS_Format_No_Failure_Expected()  {
        final String propName = "site.key.dns.validation";
        final boolean propValue = Config.getBooleanProperty(propName, false);
        Config.setProperty(propName, true);
        try {
            final String testHost = "test-host-" + System.currentTimeMillis() + ".dotcms.com";
            final Host site = new SiteDataGen().name(testHost).nextPersisted();
            assertNotNull(site);
        }finally {
            Config.setProperty(propName, propValue);
        }
    }

    /**
     * Method to test: {@link HostAPI#find(Contentlet, User, boolean)}
     *
     * Given Scenario: Finds the Site the is being referecned by a Contentlet object.
     *
     * Expected Result: By passing the Contentlet object down to the API, an instance of the Host object that holds such
     * a Contentlet must be returned.
     */
    @Test
    public void getSitebyItsIdFromContentlet() throws Exception {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();
        Host originalSite = new Host();
        ContentType contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass()).name("testContentType").build();

        // Test data
        try {
            originalSite = new SiteDataGen().nextPersisted();
            contentType = new ContentTypeDataGen().host(originalSite).nextPersisted();
            final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType);
            contentletDataGen.host(originalSite);
            final Contentlet testContentlet = contentletDataGen.nextPersisted();
            final Host siteFromContentlet = hostAPI.find(testContentlet, systemUser, false);

            // Assertions
            assertTrue("Site ID from Contentlet does NOT match!", originalSite.getIdentifier().equals(siteFromContentlet.getIdentifier()));
        } finally {
            // Cleanup
            unpublishHost(originalSite, systemUser);
            archiveHost(originalSite, systemUser);
            deleteHost(originalSite, systemUser);
            deleteContentType(contentType);
        }
    }

    /**
     * Method to test: {@link HostAPI#find(String, User, boolean)}
     *
     * Given Scenario: Create a new Site and then look it up by its Identifier.
     *
     * Expected Result: By passing down the Site's Identifier as a String, an instance of the Host object must be
     * returned.
     */
    @Test
    public void getSitebyId() throws Exception {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();
        Host originalSite = new Host();

        // Test data generation
        try {
            originalSite = new SiteDataGen().nextPersisted();
            final Host siteFromAPI = hostAPI.find(originalSite.getIdentifier(), systemUser, false);

            // Assertions
            assertTrue("Site ID does NOT match!", originalSite.getIdentifier().equals(siteFromAPI.getIdentifier()));
        } finally {
            // Cleanup
            unpublishHost(originalSite, systemUser);
            archiveHost(originalSite, systemUser);
            deleteHost(originalSite, systemUser);
        }
    }

    /**
     * Utility method used to delete test Content Types.
     *
     * @param type
     * @throws Exception
     */
    private void deleteContentType(ContentType type) throws Exception {
        final User systemUser = APILocator.systemUser();
        APILocator.getContentTypeAPI(systemUser).delete(type);
    }

    /**
     * Method to test: {@link HostAPI#findAll(User, int, int, String, boolean)}
     *
     * Given Scenario: Get the initial list of all Sites in the repository, add a new Site, and get a new count.
     *
     * Expected Result: If the size difference between the first read and the second read equals 1, the test
     */
    @Test
    public void getAllSites() throws DotSecurityException, DotDataException, ExecutionException, InterruptedException {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();
        Host testSite = new Host();

        try {
            // Test data generation
            final List<Host> initialSiteList = hostAPI.findAll(systemUser, 0, 0, null, false);
            testSite = new SiteDataGen().nextPersisted();
            final List<Host> finalSiteList = hostAPI.findAll(systemUser, 0, 0, null, false);

            // Assertions
            assertEquals("Site API is returning more Sites than expected", 1, (finalSiteList.size() - initialSiteList.size()));
        } finally {
            // Cleanup
            unpublishHost(testSite, systemUser);
            archiveHost(testSite, systemUser);
            deleteHost(testSite, systemUser);
        }
    }

    /**
     * Method to test: {@link HostAPI#findDefaultHost(User, boolean)}
     *
     * Given Scenario: Create a new Site and mark it as default. Then, go back to marking the original default Site as
     * default
     *
     * Expected Result: If the new Site is NOT returned as the default one, the test will fail.
     */
    @Test
    public void updateDefaultSite() throws DotSecurityException, DotDataException, ExecutionException,
            InterruptedException {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();
        final Host originalDefaultSite = hostAPI.findDefaultHost(systemUser, false);
        Host newDefaultSite = new Host();

        try {
            // Test data generation
            newDefaultSite = new SiteDataGen().setDefault(true).nextPersisted();
            hostAPI.updateDefaultHost(newDefaultSite, systemUser, false);
            final Host updatedDefaultSite = hostAPI.findDefaultHost(systemUser, false);

            // Assertions
            assertTrue("Default Site and New Default Site are NOT the same", newDefaultSite.getIdentifier().equals(updatedDefaultSite.getIdentifier()));
        } finally {
            hostAPI.updateDefaultHost(originalDefaultSite, systemUser, false);
            unpublishHost(newDefaultSite, systemUser);
            archiveHost(newDefaultSite, systemUser);
            deleteHost(newDefaultSite, systemUser);
        }
    }

    /**
     * Method to test: {@link HostAPI#getHostsWithPermission(int, User, boolean)}
     *
     * Given Scenario: Create a User with READ permission on a new Site, and check that the API returns it
     *
     * Expected Result: If the list of Sites that can be read by the new User is NOT one, the test will fail.
     */
    @Test
    public void getSitesWithPermission() throws DotSecurityException, DotDataException, ExecutionException,
            InterruptedException {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final UserAPI userAPI = APILocator.getUserAPI();
        Host testSite = new Host();
        Role testRole = new Role();
        User dummyUser = new User();

        try {
            // Test data generation
            testSite = new SiteDataGen().nextPersisted();
            testRole = new RoleDataGen().nextPersisted();
            dummyUser = new UserDataGen().roles(testRole).nextPersisted();
            this.addPermission(testRole, testSite);
            final List<Host> permissionedSites = hostAPI.getHostsWithPermission(PermissionAPI.PERMISSION_READ,
                    dummyUser, false);

            // Assertions
            assertEquals("Only one permissioned Site should've been retrieved", 1, permissionedSites.size());
            assertTrue("Permissioned Site ID does not match!", testSite.getIdentifier().equals(permissionedSites.get(0).getIdentifier()));
        } finally {
            // Cleanup
            unpublishHost(testSite, systemUser);
            archiveHost(testSite, systemUser);
            deleteHost(testSite, systemUser);
            userAPI.delete(dummyUser, systemUser, false);
            roleAPI.delete(testRole);
        }
    }

    /**
     * Method to test: {@link HostAPI#getHostsWithPermission(int, boolean, User, boolean)}
     *
     * Given Scenario: Non-live/archived contents cannot be checked for permissions. So, any query to archived Sites
     * will always return an empty list of results.
     *
     * Expected Result: Checking permissions on an archived Site must return NO results.
     */
    @Test
    public void getArchivedSitesWithPermission() throws DotSecurityException, DotDataException, ExecutionException,
            InterruptedException {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final UserAPI userAPI = APILocator.getUserAPI();
        Host testSite = new Host();
        Role testRole = new Role();
        User dummyUser = new User();

        try {
            // Test data generation
            testSite = new SiteDataGen().nextPersisted();
            testRole = new RoleDataGen().nextPersisted();
            dummyUser = new UserDataGen().roles(testRole).nextPersisted();
            this.addPermission(testRole, testSite);
            unpublishHost(testSite, systemUser);
            archiveHost(testSite, systemUser);
            final List<Host> permissionedSites = hostAPI.getHostsWithPermission(PermissionAPI.PERMISSION_READ, true,
                    dummyUser, false);

            // Assertions
            assertEquals("Non-live/archived Sites cannot be verified for permissions", 0, permissionedSites.size());
        } finally {
            // Cleanup
            deleteHost(testSite, systemUser);
            userAPI.delete(dummyUser, systemUser, false);
            roleAPI.delete(testRole);
        }
    }

    /**
     * Method to test: {@link HostAPI#findSystemHost()}
     *
     * Given Scenario: Retrieve the System Host object.
     *
     * Expected Result: The API must return the System Host.
     */
    @Test
    public void findSystemHost() throws DotDataException {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();

        // Test data generation
        final Host systemHost = hostAPI.findSystemHost();

        // Assertions
        assertEquals("System Host object NOT found!", "SYSTEM_HOST", systemHost.getIdentifier());
    }

    /**
     * Method to test: {@link HostAPI#findSystemHost(User, boolean)}
     *
     * Given Scenario: Have a limited User retrieve the System Host
     *
     * Expected Result: If a limited User cannot get a reference to the System Host, the test will fail.
     */
    @Test
    public void findSystemHostWithLimitedUser() throws DotDataException, DotSecurityException {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final UserAPI userAPI = APILocator.getUserAPI();
        Role testRole = new Role();
        User dummyUser = new User();

        try {
            // Test data generation
            testRole = new RoleDataGen().nextPersisted();
            dummyUser = new UserDataGen().roles(testRole).nextPersisted();
            final Host systemHost = hostAPI.findSystemHost(dummyUser, true);

            // Assertions
            assertEquals("System Host object NOT found!", "SYSTEM_HOST", systemHost.getIdentifier());
        } finally {
            // Cleanup
            userAPI.delete(dummyUser, APILocator.systemUser(), false);
            roleAPI.delete(testRole);
        }
    }

    /**
     * Method to test: {@link HostAPI#parseHostAliases(Host)}
     *
     * Given Scenario: There three ways of entering Site Aliases: (1) Separated by blank spaces, (2) by commas, or
     * (3) by line breaks.
     *
     * Expected Result: Site Aliases can only be parseable under the three scenarios explained above.
     */
    @Test
    public void parseHostAliases() throws DotHibernateException, ExecutionException, InterruptedException {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final SiteDataGen siteDataGen = new SiteDataGen();
        final User systemUser = APILocator.systemUser();
        Host testSite = new Host();

        try {
            // Test data generation #1
            testSite = siteDataGen.aliases("first.dotcms.com second.dotcms.com third.dotcms.com")
                    .nextPersisted();
            List<String> aliasList = hostAPI.parseHostAliases(testSite);

            // Assertions
            assertEquals("There must be three Site aliases separated by blank spaces!", 3, aliasList.size());

            // Test data generation #2
            testSite.setAliases("first.dotcms.com,second.dotcms.com,third.dotcms.com");
            siteDataGen.persist(testSite, true);
            aliasList = hostAPI.parseHostAliases(testSite);

            // Assertions
            assertEquals("There must be three Site aliases separated by commas!", 3, aliasList.size());

            // Test data generation #3
            testSite.setAliases("first.dotcms.com\nsecond.dotcms.com\nthird.dotcms.com");
            siteDataGen.persist(testSite, true);
            aliasList = hostAPI.parseHostAliases(testSite);

            // Assertions
            assertEquals("There must be three Site aliases separated by line breaks!", 3, aliasList.size());
        } finally {
            // Cleanup
            unpublishHost(testSite, systemUser);
            archiveHost(testSite, systemUser);
            deleteHost(testSite, systemUser);
        }
    }

    /**
     * Method to test: {@link HostAPI#retrieveHostsPerTagStorage(String, User)}
     *
     * Given Scenario: Create a parent Site, and a child Site. Then, have the Tag Storage of the child Site point to the
     * parent Site. Finally, delete the parent Site.
     *
     * Expected Result: When the parent Site is deleted, the Tag Storage of the child Site will point to the child Site
     * itself.
     */
    @Test
    public void retrieveHostsPerTagStorage() throws DotHibernateException, ExecutionException, InterruptedException {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final SiteDataGen siteDataGen = new SiteDataGen();
        final User systemUser = APILocator.systemUser();
        Host siteOne = new Host();
        Host siteTwo = new Host();

        try {
            // Test data generation
            siteOne = siteDataGen.name("parenttagstorage" + System.currentTimeMillis() + "dotcms.com")
                    .nextPersisted(true);
            final String parentTagStorageId = siteOne.getIdentifier();
            siteTwo = siteDataGen.name("childtagstorage" + System.currentTimeMillis() + "dotcms.com").next();
            siteTwo.setTagStorage(parentTagStorageId);
            siteTwo = siteDataGen.persist(siteTwo);
            final String expectedStorageId = siteTwo.getIdentifier();
            final String siteTwoName = siteTwo.getHostname();
            unpublishHost(siteOne, systemUser);
            archiveHost(siteOne, systemUser);
            final PaginatedArrayList<Host> updatedSite = hostAPI.search(siteTwoName, false, false, 0, 0, systemUser, false);


            // Assertions
            assertEquals("Only one Site should've been returned", 1, updatedSite.size());
            assertTrue("Tag Storage ID does NOT match the Site ID", expectedStorageId.equals(updatedSite.get(0)
                    .getTagStorage()));
        } finally {
            // Cleanup
            deleteHost(siteOne, systemUser);
            unpublishHost(siteTwo, systemUser);
            archiveHost(siteTwo, systemUser);
            deleteHost(siteTwo, systemUser);
        }
    }

    /**
     * Method to test: {@link HostAPI#searchByStopped(String, boolean, boolean, int, int, User, boolean)}
     *
     * Given Scenario: Create a test Site and stop it. Then, create another Site, then stop it and archive it. Finally,
     * compare the total count of stopped Sites.
     *
     * Expected Result: When compared to the initial stopped Sites count, after stopping the new Site, the count must
     * increase by one. After stopping and archivnig the second Site, the count must be increased by two because
     * archived Sites are also considered "stopped Sites".
     */
    @Test
    public void searchByStopped() throws DotHibernateException, ExecutionException, InterruptedException {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        SiteDataGen siteDataGen = new SiteDataGen();
        final User systemUser = APILocator.systemUser();
        Host testSite = new Host();
        Host testSiteTwo = new Host();

        try {
            // Test data generation
            final PaginatedArrayList<Host> stoppedSites = hostAPI.searchByStopped(null, true, false, 0, 0, systemUser,
                    false);
            testSite = siteDataGen.nextPersisted();
            unpublishHost(testSite, systemUser);
            final PaginatedArrayList<Host> updatedStoppedSites = hostAPI.searchByStopped(null, true, false, 0, 0,
                    systemUser, false);

            // Assertions
            assertEquals("Stopped Sites count difference MUST be one", 1, updatedStoppedSites.size() - stoppedSites
                    .size());

            // Test data generation #2
            siteDataGen = new SiteDataGen();
            testSiteTwo = siteDataGen.nextPersisted();
            unpublishHost(testSiteTwo, systemUser);
            archiveHost(testSiteTwo, systemUser);
            final PaginatedArrayList<Host> updatedStoppedAndArchivedSites = hostAPI.searchByStopped(null, true,
                    false, 0, 0, systemUser, false);

            // Assertions #2
            assertEquals("Stopped and Archived Sites count difference MUST be two", 2, updatedStoppedAndArchivedSites.size() - stoppedSites.size());
        } finally {
            // Cleanup
            archiveHost(testSite, systemUser);
            deleteHost(testSite, systemUser);
            deleteHost(testSiteTwo, systemUser);
        }
    }

    /**
     * Method to test: {@link HostAPI#search(String, boolean, boolean, boolean, int, int, User, boolean)}
     *
     * Given Scenario: Create an archived Site, and retrieve it via the API based on a combination of parameters passed
     * down to the method.
     *
     * Expected Result: After archiving a Site, the total archived Site count must be increased by one.
     */
    @Test
    public void searchByArchived() throws DotHibernateException, InterruptedException, ExecutionException {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final SiteDataGen siteDataGen = new SiteDataGen();
        final User systemUser = APILocator.systemUser();
        Host testSite = new Host();

        try {
            // Test data generation
            final PaginatedArrayList<Host> archivedSites = hostAPI.search(null, true, true, false, 0, 0, systemUser, false);

            testSite = siteDataGen.nextPersisted();
            unpublishHost(testSite, systemUser);
            archiveHost(testSite, systemUser);
            final PaginatedArrayList<Host> updatedArchivedSites = hostAPI.search(null, true, true, false, 0, 0,
                    systemUser, false);

            // Assertions
            assertEquals("Archived Sites count difference MUST one", 1, updatedArchivedSites.size() - archivedSites
                    .size());
        } finally {
            // Cleanup;
            deleteHost(testSite, systemUser);
        }
    }

    /**
     * Method to test: {@link HostAPI#search(String, boolean, int, int, User, boolean)}
     *
     * Given Scenario: Create a Site, and look it up by its Site Name.
     *
     * Expected Result: Looking up a Site by its name must return at least one result, as the filter is internally set
     * with leading and trailing wildcards so more than one result can be returned -- if applicable.
     */
    @Test
    public void searchByFilter() throws DotHibernateException, ExecutionException, InterruptedException {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final String siteName = "uniquenamefilteredsite.dotcms.com" + System.currentTimeMillis();
        final SiteDataGen siteDataGen = new SiteDataGen().name(siteName);
        final User systemUser = APILocator.systemUser();
        Host testSite = new Host();

        try {
            // Test data generation
            testSite = siteDataGen.nextPersisted();
            final PaginatedArrayList<Host> filteredSites = hostAPI.search(siteName, false, 0, 0, systemUser, false);

            // Assertions
            assertEquals("Name-filtered Site count is NOT one", 1, filteredSites.size());
        } finally {
            // Cleanup;
            unpublishHost(testSite, systemUser);
            archiveHost(testSite, systemUser);
            deleteHost(testSite, systemUser);
        }
    }

    /**
     * Method to test: {@link HostAPI#count(User, boolean)}
     *
     * Given Scenario: Create a new Site, and get the total count of Sites in the current repository.
     *
     * Expected Result: If the total Site count is increased by one, the test is successful.
     */
    @Test
    public void count() throws DotHibernateException, ExecutionException, InterruptedException {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final SiteDataGen siteDataGen = new SiteDataGen();
        final User systemUser = APILocator.systemUser();
        final long initialCount = hostAPI.count(systemUser, false);
        Host testSite = new Host();

        try {
            // Test data generation
            testSite = siteDataGen.nextPersisted();
            final long newCount = hostAPI.count(systemUser, false);

            // Assertions
            assertEquals("Site count difference is NOT one", 1, newCount - initialCount);
        } finally {
            // Cleanup;
            unpublishHost(testSite, systemUser);
            archiveHost(testSite, systemUser);
            deleteHost(testSite, systemUser);
        }
    }

}
