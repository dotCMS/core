package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.JUnit4WeldRunner;
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
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableFactory;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;

import javax.enterprise.context.Dependent;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.dotmarketing.portlets.templates.model.Template.ANONYMOUS_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class will test operations related with interacting with Sites: Deleting a site, marking a
 * Site as default, and any other piece of information exposed by the {@link HostAPI}.
 *
 * @author Jorge Urdaneta
 * @since Sep 5, 2013
 */
@Dependent
@RunWith(JUnit4WeldRunner.class)
public class HostAPITest extends IntegrationTestBase  {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
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

    /**
     * This test validates the site variables are deleted when the host is deleted
     */
    @Test
    public void testDeleteSiteCleanUpSiteVariables() throws Exception {

        final HostVariableFactory hostVariableFactory = FactoryLocator.getHostVariableFactory();
        final HostVariableAPI hostVariableAPI = APILocator.getHostVariableAPI();

        final User user = APILocator.getUserAPI().getSystemUser();
        final Host site = new SiteDataGen().nextPersisted();

        // Check the current variables
        var siteVariables = hostVariableFactory.getVariablesForHost(site.getIdentifier());
        assertEquals(0, siteVariables.size());

        // Save some variables to the site
        hostVariableAPI.save(List.of(
                createSiteVariable(site, user, 1),
                createSiteVariable(site, user, 2),
                createSiteVariable(site, user, 3),
                createSiteVariable(site, user, 4),
                createSiteVariable(site, user, 5)
        ), site.getIdentifier(), user, false);

        // Make sure we properly saved the site variables
        siteVariables = hostVariableFactory.getVariablesForHost(site.getIdentifier());
        assertEquals(5, siteVariables.size());

        // Delete the site
        archiveHost(site, user);
        deleteHost(site, user);

        // And validate that the site variables were deleted
        siteVariables = hostVariableFactory.getVariablesForHost(site.getIdentifier());
        assertEquals(0, siteVariables.size());
    }

    @Test
    public void testDeleteSite() throws Exception {

        User user = APILocator.getUserAPI().getSystemUser();
        Host sourceHost = null;

        try {
            sourceHost = new SiteDataGen().nextPersisted();
            final ContentType blogContentType = TestDataUtils
                    .getBlogLikeContentType("Blog" + System.currentTimeMillis(), sourceHost);
            final ContentType employeeContentType = TestDataUtils
                    .getEmployeeLikeContentType("Employee" + System.currentTimeMillis(), sourceHost);
            final ContentType newsContentType = TestDataUtils
                    .getNewsLikeContentType("News" + System.currentTimeMillis(), sourceHost);

            TestDataUtils.getBlogContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                    blogContentType.id(), sourceHost);
            TestDataUtils.getBlogContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                    blogContentType.id(), sourceHost);
            TestDataUtils
                    .getEmployeeContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            employeeContentType.id(), sourceHost);
            TestDataUtils
                    .getEmployeeContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            employeeContentType.id(), sourceHost);
            TestDataUtils.getNewsContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                    newsContentType.id(), sourceHost);
            TestDataUtils.getNewsContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                    newsContentType.id(), sourceHost);

            TestDataUtils.getGenericContentContent(true,
                    APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                    sourceHost);
            TestDataUtils.getGenericContentContent(true,
                    APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                    sourceHost);
            TestDataUtils.getGenericContentContent(true,
                    APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                    sourceHost);

            TestDataUtils.waitForEmptyQueue();

            //Create a new test host
            Host host = createSite("copy" + System.currentTimeMillis() + ".demo.dotcms.com", user);
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

            final Map<Object,Object> dataMap = new HashMap<>();
            dataMap.put(HostAssetsJobProxy.USER_ID, user.getUserId());
            dataMap.put(HostAssetsJobProxy.SOURCE_HOST_ID, sourceHost.getIdentifier());
            dataMap.put(HostAssetsJobProxy.DESTINATION_HOST_ID, host.getIdentifier());
            dataMap.put(HostAssetsJobProxy.COPY_OPTIONS, new HostCopyOptions(true));
            when(jobExecutionContext.getTrigger()).thenReturn(trigger);

            HostAssetsJobProxy hostAssetsJobProxy = Mockito.spy(new HostAssetsJobProxy());
            doReturn(dataMap).when(hostAssetsJobProxy).getExecutionData(trigger);
            hostAssetsJobProxy.execute(jobExecutionContext);

            TestDataUtils.waitForEmptyQueue(); // wait a bit for the index

            //Archive the just created host in order to be able to delete it
            archiveHost(host, user);

            //Delete the just created host
            deleteHost(host, user);

            //Make sure the host was deleted properly
            hostDoesNotExistCheck(newHostIdentifier, newHostName, user);
        } finally {
            // Cleanup
            if (sourceHost != null) {
                unpublishHost(sourceHost, user);
                archiveHost(sourceHost, user);
                deleteHost(sourceHost, user);
            }
        }
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
        Host host = createSite(newHostName, user);
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
        assertEquals(defaultHost.getIdentifier(), host.getIdentifier());
    }

    @Test
    public void givenSearch_whenNewHost_thenFindsNewHost() throws Exception {

        User user = APILocator.getUserAPI().getSystemUser();

        new SiteDataGen().name("demo.test2" + System.currentTimeMillis() + ".dotcms.com")
                .nextPersisted();

        final String newHostName = "demo.test" + System.currentTimeMillis() + ".dotcms.com";

        //Create a new test host
        Host host = createSite(newHostName, user);
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
        assertEquals(hosts.get(0).getHostname(), newHostName);

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
     * Creates a site variable.
     *
     * @param host  The host associated with the site variable.
     * @param user  The user who is creating the site variable.
     * @param index The index used to generate unique names, keys, and values for the site
     *              variable.
     * @return The created site variable.
     */
    private HostVariable createSiteVariable(Host host, User user, int index) {

        var siteVariable = new HostVariable();
        siteVariable.setHostId(host.getIdentifier());
        siteVariable.setName(String.format("var%dName", index));
        siteVariable.setKey(String.format("var%dKey", index));
        siteVariable.setValue(String.format("var%dValue", index));
        siteVariable.setLastModifierId(user.getUserId());
        siteVariable.setLastModDate(new Date());

        return siteVariable;
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
        Host host = createSite(newHostName, user);
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
                assertEquals(system, foundContentType.system());
                assertEquals(defaultType, foundContentType.defaultType());
                assertEquals(testContentType.system(), foundContentType.system());
                assertEquals(testContentType.defaultType(),
                        foundContentType.defaultType());
                assertEquals(testContentType.id(), foundContentType.id());
                assertEquals(testContentType.variable(),
                        foundContentType.variable());

                //Make sure the host was changed to SYSTEM_HOST
                assertEquals(APILocator.getHostAPI().findSystemHost().getIdentifier(),
                        foundContentType.host());
            } catch (Exception e) {
                fail(String.format("Unable to create delete test content type [%s] [%s]",
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
                fail(String.format("Unable to create delete test content type [%s] [%s]",
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
        assertEquals(structureId, foundContentType.id());
        assertEquals(defaultType, foundContentType.defaultType());
        assertEquals(system, foundContentType.system());
        assertEquals(structureVarName, foundContentType.variable());
        assertEquals(host.getIdentifier(), foundContentType.host());

        return contentType;
    }

    /**
     * Creates a new site with the given siteName and user.
     *
     * @param siteName the name to assign to the new host
     * @param user     the user performing the creation operation
     *
     * @return the newly created Host object
     *
     * @throws DotHibernateException if an error occurs during the database transaction
     */
    private Host createSite(final String siteName, final User user) throws DotHibernateException {
        Host site = new Host();
        site.setHostname(siteName);
        site.setDefault(false);
        site.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        try {
            HibernateUtil.startTransaction();
            site.setIndexPolicy(IndexPolicy.FORCE);
            site = APILocator.getHostAPI().save(site, user, false);
            HibernateUtil.closeAndCommitTransaction();
        } catch (final Exception e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(HostAPITest.class, e.getMessage(), e);
            Assert.fail(String.format("Unable to create test site [%s] [%s]", siteName,
                    e.getMessage()));
        }
        return site;
    }

    /**
     * Unpublish a given host
     */
    private void unpublishHost(final Host host, final User user) throws DotHibernateException {

        try {
            HibernateUtil.startTransaction();
            host.setIndexPolicy(IndexPolicy.WAIT_FOR);
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
        if (null == host || null == user) {
            return;
        }
        try {
            HibernateUtil.startTransaction();
            host.setIndexPolicy(IndexPolicy.WAIT_FOR);
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
        if (null == host || null == user) {
            return;
        }
        Optional<Future<Boolean>> hostDeleteResult = Optional.empty();
        try {
            HibernateUtil.startTransaction();
            host.setIndexPolicy(IndexPolicy.WAIT_FOR);
            hostDeleteResult = APILocator.getHostAPI().delete(host, user, false, true);
            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(HostAPITest.class, e.getMessage());
            Assert.fail(String.format("Unable to delete test host [%s] [%s]", host.getHostname(),
                    e.getMessage()));
        }

        if (hostDeleteResult.isEmpty()) {
            TestDataUtils.waitForEmptyQueue(); // wait a bit for the index
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
    public void shouldReturnExistingHost() throws Exception {
        Host host = null;
        Role role = null;
        User user = null;
        try {
            host = new SiteDataGen().nextPersisted();
            role = new RoleDataGen().nextPersisted();
            user = new UserDataGen().roles(role).nextPersisted();

            this.addPermission(role, host);

            final Host hostReturned = APILocator.getHostAPI().resolveHostNameWithoutDefault(host.getHostname(), user, false).get();
            assertEquals(host, hostReturned);
        } finally {
            // Cleanup
            final User systemUser = APILocator.systemUser();
            if (host != null) {
                unpublishHost(host, systemUser);
                archiveHost(host, systemUser);
                deleteHost(host, systemUser);
            }
            if (user != null) {
                UserDataGen.remove(user);
            }
            if (role != null) {
                RoleDataGen.remove(role);
            }
        }
    }

    /**
     * Method to test: {@link HostAPI#resolveHostNameWithoutDefault(String, User, boolean)}
     * When a host exist but the user does not have permission
     * Should throw a {@link DotSecurityException}
     */
    @Test(expected = DotSecurityException.class)
    public void shouldThrowDotSecurityExceptionWhenUserNotHavePermission() throws Exception {
        Host host = null;
        Role role = null;
        User user = null;
        try {
            host = new SiteDataGen().nextPersisted();
            role = new RoleDataGen().nextPersisted();
            user = new UserDataGen().roles(role).nextPersisted();

            APILocator.getHostAPI().resolveHostNameWithoutDefault(host.getHostname(), user, false);
        } finally {
            // Cleanup
            final User systemUser = APILocator.systemUser();
            if (host != null) {
                unpublishHost(host, systemUser);
                archiveHost(host, systemUser);
                deleteHost(host, systemUser);
            }
            if (user != null) {
                UserDataGen.remove(user);
            }
            if (role != null) {
                RoleDataGen.remove(role);
            }
        }
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
     * Should return the default host and store 404 into host cache
     */
    @Test
    public void shouldReturnDefaultHostForNonExistingHost() throws DotSecurityException, DotDataException {
        final String hostName = "not_exists_host";
        final Host notExistsHost = APILocator.getHostAPI().resolveHostName(hostName
                , APILocator.systemUser(), false);
        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), true);
        assertEquals(defaultHost.getIdentifier(), notExistsHost.getIdentifier());

        final HostCache hostCache = CacheLocator.getHostCache();
        final Host hostByAlias = hostCache.getHostByAlias(hostName, false);

        assertEquals(HostCache.CACHE_404_HOST, hostByAlias.getIdentifier());
    }

    /**
     * Method to test: {@link HostAPI#resolveHostNameWithoutDefault(String, User, boolean)}
     * When a host exist but the user does not have permission and respectFrontendRoles is true
     * Should return the host
     */
    @Test()
    public void shouldReturnHostWhenRespectFrontendRolesIsTrue() throws Exception {
        Host host = null;
        Role role = null;
        User user = null;
        try {
            host = new SiteDataGen().nextPersisted();
            role = new RoleDataGen().nextPersisted();
            user = new UserDataGen().roles(role).nextPersisted();

            final Host hostReturned = APILocator.getHostAPI().resolveHostNameWithoutDefault(host.getHostname(), user, true).get();
            assertEquals(host, hostReturned);
        } finally {
            // Cleanup
            final User systemUser = APILocator.systemUser();
            if (host != null) {
                unpublishHost(host, systemUser);
                archiveHost(host, systemUser);
                deleteHost(host, systemUser);
            }
            if (user != null) {
                UserDataGen.remove(user);
            }
            if (role != null) {
                RoleDataGen.remove(role);
            }
        }
    }

    /**
     * Method to test: {@link HostAPI#resolveHostNameWithoutDefault(String, User, boolean)}
     * When a host has live and working versions and the user has a front end role
     * Should return the live version
     */
    @Test
    public void shouldReturnLiveVersionWhenFrontendRolesIsTrue() throws Exception {
        Host host = null;
        Role role = null;
        User user = null;
        try {
            host = new SiteDataGen().nextPersisted();
            role = new RoleDataGen().nextPersisted();
            user = new UserDataGen().roles(role).nextPersisted();

            // Create a working version
            final Host workingHost = new Host(APILocator.getContentletAPI().checkout(
                    host.getInode(), APILocator.systemUser(), false));
            final String workingHostAlias = "working." + workingHost.getHostname();
            workingHost.setAliases(workingHostAlias);
            final Host savedHost = new Host(APILocator.getContentletAPI().checkin(
                    workingHost, APILocator.systemUser(), false));
            assertNotEquals(host.getInode(), savedHost.getInode());

            // Resolve host by name
            final Optional<Host> optionalHost = APILocator.getHostAPI().resolveHostNameWithoutDefault(
                    host.getHostname(), user, true);
            assertTrue(optionalHost.isPresent());
            final Host resolvedHost = optionalHost.get();

            assertTrue(resolvedHost.isLive());
            assertNull(resolvedHost.getAliases());
            assertEquals(host, resolvedHost);

            // Resolve host by alias
            final Optional<Host> optionalHostByAlias = APILocator.getHostAPI()
                    .resolveHostNameWithoutDefault(workingHostAlias, user, true);
            assertTrue(optionalHostByAlias.isPresent());
            final Host resolvedHostByAlias = optionalHostByAlias.get();

            assertTrue(resolvedHostByAlias.isLive());
            assertEquals(host, resolvedHostByAlias);

        } finally {
            // Cleanup
            final User systemUser = APILocator.systemUser();
            if (host != null) {
                unpublishHost(host, systemUser);
                archiveHost(host, systemUser);
                deleteHost(host, systemUser);
            }
            if (user != null) {
                UserDataGen.remove(user);
            }
            if (role != null) {
                RoleDataGen.remove(role);
            }
        }
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
    public void shouldReturnHostByAlias() throws Exception {
        Host host = null;
        Host host_2 = null;
        Role role = null;
        User user = null;
        try {
            final long currentTime = System.currentTimeMillis();
            final String demoAlias = "demo-" + currentTime + ".dotcms.com";
            host = new SiteDataGen().aliases(demoAlias).nextPersisted();
            final String notDemoAlias = "not-demo-" + currentTime + ".dotcms.com";
            host_2 = new SiteDataGen().aliases(notDemoAlias).nextPersisted();

            role = new RoleDataGen().nextPersisted();
            user = new UserDataGen().roles(role).nextPersisted();

            this.addPermission(role, host);
            this.addPermission(role, host_2);

            final Host hostReturned = APILocator.getHostAPI().findByAlias(
                    demoAlias, user, false);
            assertEquals(host, hostReturned);
            assertNotEquals(host_2, hostReturned);
        } finally {
            // Cleanup
            final User systemUser = APILocator.systemUser();
            if (host != null) {
                unpublishHost(host, systemUser);
                archiveHost(host, systemUser);
                deleteHost(host, systemUser);
            }
            if (host_2 != null) {
                unpublishHost(host_2, systemUser);
                archiveHost(host_2, systemUser);
                deleteHost(host_2, systemUser);
            }
            if (user != null) {
                UserDataGen.remove(user);
            }
            if (role != null) {
                RoleDataGen.remove(role);
            }
        }
    }

    /**
     * Method to test: {@link HostAPI#findByAlias(String, User, boolean)}
     * When create one host with multiple alias
     * Should return thehost by alias
     */
    @Test
    public void whenHostHasMultipleAliasshouldReturnHostByAlias() throws Exception {
        Host host = null;
        Role role = null;
        User user = null;
        try {
            final long currentTime = System.currentTimeMillis();
            final String demoAlias = "demo-" + currentTime + ".dotcms.com";
            final String testAlias = "test-" + currentTime + ".dotcms.com";
            host = new SiteDataGen().aliases(String.format("%s%n%s",
                    demoAlias, testAlias)).nextPersisted();

            role = new RoleDataGen().nextPersisted();
            user = new UserDataGen().roles(role).nextPersisted();

            this.addPermission(role, host);

            final Host hostReturned = APILocator.getHostAPI().findByAlias(testAlias, user, false);
            assertEquals(host, hostReturned);
        } finally {
            // Cleanup
            final User systemUser = APILocator.systemUser();
            if (host != null) {
                unpublishHost(host, systemUser);
                archiveHost(host, systemUser);
                deleteHost(host, systemUser);
            }
            if (user != null) {
                UserDataGen.remove(user);
            }
            if (role != null) {
                RoleDataGen.remove(role);
            }
        }
    }

    /**
     * Method to test: {@link HostAPI#findByAlias(String, User, boolean)}
     * When create two host with alias that both start by prod-
     * Should return the right host by alias
     */
    @Test
    public void whenBothAliasStartByProd() throws Exception {
        Host host = null;
        Host host_2 = null;
        User user = null;
        Role role = null;
        final User systemUser = APILocator.systemUser();
        try {
            final long currentTime = System.currentTimeMillis();
            final String prodAlias = "prod-client-" + currentTime + ".dotcms.com";
            host = new SiteDataGen().aliases(prodAlias).nextPersisted();
            final String prodAlias_2 = "prod-anotherclient-" + currentTime + ".dotcms.com";
            host_2 = new SiteDataGen().aliases(prodAlias_2).nextPersisted();

            role = new RoleDataGen().nextPersisted();
            user = new UserDataGen().roles(role).nextPersisted();

            this.addPermission(role, host);
            this.addPermission(role, host_2);

            final Host hostReturned = APILocator.getHostAPI().findByAlias(prodAlias, user, false);
            assertEquals(host, hostReturned);
            assertNotEquals(host_2, hostReturned);

            final Host hostReturned2 = APILocator.getHostAPI().findByAlias(prodAlias_2, user, false);
            assertNotEquals(host, hostReturned2);
            assertEquals(host_2, hostReturned2);
        } finally {
            // Cleanup
            if (host != null) {
                unpublishHost(host, systemUser);
                archiveHost(host, systemUser);
                deleteHost(host, systemUser);
            }
            if (host_2 != null) {
                unpublishHost(host_2, systemUser);
                archiveHost(host_2, systemUser);
                deleteHost(host_2, systemUser);
            }
            if (user != null) {
                UserDataGen.remove(user);
            }
            if (role != null) {
                RoleDataGen.remove(role);
            }
        }
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
        final List<Host> allFromDB1 = hostAPI.findAllFromDB(systemUser,
                HostAPI.SearchType.INCLUDE_SYSTEM_HOST);
        final List<Host> allFromCache1 = hostAPI.findAllFromCache(systemUser, false);
        Assert.assertTrue( allFromDB1.size() == allFromCache1.size() &&
                allFromDB1.containsAll(allFromCache1) && allFromCache1.containsAll(allFromDB1));
        final Host host1 = new SiteDataGen().aliases("any.client"
                + System.currentTimeMillis() + ".dotcms.com").nextPersisted();
        final List<Host> allFromCache2 = hostAPI.findAllFromCache(systemUser, false);
        assertTrue(allFromCache1.size() < allFromCache2.size());
        hostAPI.archive(host1, systemUser, false);
        hostAPI.delete(host1, systemUser, false);
        final List<Host> allFromCache3 = hostAPI.findAllFromCache(systemUser, false);
        assertEquals(allFromCache3.size() , allFromCache1.size());
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link HostAPI#DBSearch(String, User, boolean)}</li>
     *     <li><b>Given Scenario: </b>Sites must be Language independent. Even though they can be
     *     assigned with a language, we should not relay on the lang id to find them.</li>
     *     <li><b>Expected Result: </b>This test is meant to verify a change introduced in the
     *     DBSearch method to find the {@link ContentletVersionInfo} regardless of the language.
     *     Also, it verifies there is always only one entry on the version-info for every contentlet
     *     of type Host, regardless of the operation we perform.</li>
     * </ul>
     */
    @Test
    public void Test_Host_With_Multiple_Lang_Versions_Return_Default_Lang_OtherWise_First_Occurrence()
            throws Exception {

        final LanguageAPI languageAPI = APILocator.getLanguageAPI();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final HostAPI hostAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();
        final VersionableAPI versionableAPI = APILocator.getVersionableAPI();

        Host site = null;
        try {
            final SiteDataGen siteDataGen = new SiteDataGen();
            site = siteDataGen.name("xyx" + System.currentTimeMillis())
                    .aliases("xyz" + System.currentTimeMillis() + ".dotcms.com").next();
            site.setLanguageId(languageAPI.getDefaultLanguage().getId());
            site = siteDataGen.persist(site, false);

            // Sites are created by default under the default language
            assertEquals(languageAPI.getDefaultLanguage().getId(), site.getLanguageId());

            List<ContentletVersionInfo> verInfos = versionableAPI
                    .findContentletVersionInfos(site.getIdentifier());

            assertEquals("There should be only one entry.", 1, verInfos.size());

            final Host dbSearch = hostAPI.DBSearch(site.getIdentifier(), systemUser, false);
            assertNotNull("The Site queried directly in the database cannot be null", dbSearch);

            assertEquals(verInfos.get(0).getWorkingInode(), dbSearch.getInode());

            Assert.assertNull("There shouldn't be a live version yet.", verInfos.get(0).getLiveInode());

            contentletAPI.publish(site, systemUser, false);

            verInfos = versionableAPI.findContentletVersionInfos(site.getIdentifier());

            assertNotNull("There should be a live version now.", verInfos.get(0).getLiveInode());

            //This should create another version of the host in a different language.
            final Language newLanguage = new LanguageDataGen().languageName("ES").nextPersisted();

            site.setLanguageId(newLanguage.getId());
            hostAPI.save(site, systemUser, false);

            verInfos = versionableAPI
                    .findContentletVersionInfos(site.getIdentifier());
            assertEquals("There should be two entries one for each language.", 2, verInfos.size());

            final long newRandomDefaultLangId = -1;
            final Language mockDefaultLang = mock(Language.class);
            when(mockDefaultLang.getId()).thenReturn(newRandomDefaultLangId);

            final HostAPI hostAPIWithMockedLangAPI = new HostAPIImpl(APILocator.getSystemEventsAPI());
            final Host dbSearchNoDefaultLang = hostAPIWithMockedLangAPI.DBSearch(site.getIdentifier(), systemUser, false);

            //Since the Default language is changed now we still get something here.
            assertNotNull(dbSearchNoDefaultLang);
            //And System-host should still be system host
            assertNotNull("The System Host must have been returned", hostAPIWithMockedLangAPI.findSystemHost());
        } finally {
            // Cleanup
            if (null != site) {
                site.setIndexPolicy(IndexPolicy.WAIT_FOR);
                contentletAPI.unpublish(site, systemUser, false);
                contentletAPI.archive(site, systemUser, false);
                this.deleteHost(site, systemUser);
            }
        }

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
     * Method to test: {@link HostAPI#findAllFromCache(User, boolean)}
     *
     * Given Scenario: For a Frontend User, get the list of all Sites in the repository
     * when a given host has a live and a working version
     * Expected Result: The API should return the live version of the host
     */
    @Test
    public void getAllSitesShouldReturnLiveVersionForFrontendUser() throws Exception {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();
        Host host = null;
        Role role = null;
        User user = null;

        try {
            // Test data generation
            host = new SiteDataGen().nextPersisted();
            role = new RoleDataGen().nextPersisted();
            user = new UserDataGen().roles(role).nextPersisted();

            // Create a working version of the host
            final Host workingHost = new Host(APILocator.getContentletAPI().checkout(
                    host.getInode(), APILocator.systemUser(), false));
            final String workingHostAlias = "working." + workingHost.getHostname();
            workingHost.setAliases(workingHostAlias);
            final Host savedHost = new Host(APILocator.getContentletAPI().checkin(
                    workingHost, systemUser, false));
            assertNotEquals(host.getInode(), savedHost.getInode());

            final List<Host> allSites = hostAPI.findAllFromCache(user, true);

            // Assertions
            assertFalse("No Sites were returned", allSites.isEmpty());
            final String hostId = host.getIdentifier();

            Optional<Host> optionalHost = allSites.stream().filter(
                    h -> h.getIdentifier().equals(hostId)).findFirst();
            assertTrue("Host was NOT returned", optionalHost.isPresent());
            final Host returnedHost = optionalHost.get();

            assertTrue("Host is NOT live", returnedHost.isLive());
            assertNull("Host has aliases", returnedHost.getAliases());
            assertEquals("Host does NOT match", host, returnedHost);
        } finally {
            // Cleanup
            if (host != null) {
                unpublishHost(host, systemUser);
                archiveHost(host, systemUser);
                deleteHost(host, systemUser);
            }
            if (user != null) {
                UserDataGen.remove(user);
            }
            if (role != null) {
                RoleDataGen.remove(role);
            }
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
     * Method to test: {@link HostAPI#findDefaultHost(User, boolean)}
     * Given scenario: When the default host has live and working versions and the user has a front end role
     * Expected result: The API should return the live version of the host
     */
    @Test
    public void findDefaultHostShouldReturnLiveVersionForFrontedUser() throws Exception {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();
        final Host originalDefaultSite = hostAPI.findDefaultHost(systemUser, false);

        Host host = null;
        Role role = null;
        User user = null;

        try {
            // Test data generation
            host = new SiteDataGen().setDefault(true).nextPersisted();
            role = new RoleDataGen().nextPersisted();
            user = new UserDataGen().roles(role).nextPersisted();
            hostAPI.updateDefaultHost(host, systemUser, false);

            // Save working version of the default host
            final Host workingHost = new Host(APILocator.getContentletAPI().checkout(
                    host.getInode(), APILocator.systemUser(), false));
            final String workingHostAlias = "working." + workingHost.getHostname();
            workingHost.setAliases(workingHostAlias);
            final Host savedHost = new Host(APILocator.getContentletAPI().checkin(
                    workingHost, systemUser, false));
            assertNotEquals(host.getInode(), savedHost.getInode());

            final Host defaultHost = hostAPI.findDefaultHost(user, true);

            // Assertions
            assertTrue("Default Host is NOT live", defaultHost.isLive());
            assertNull("Default Host has aliases", defaultHost.getAliases());
            assertEquals("Default Host does NOT match", host, defaultHost);

        } finally {
            // Cleanup
            hostAPI.updateDefaultHost(originalDefaultSite, systemUser, false);
            if (host != null) {
                unpublishHost(host, systemUser);
                archiveHost(host, systemUser);
                deleteHost(host, systemUser);
            }
            if (user != null) {
                UserDataGen.remove(user);
            }
            if (role != null) {
                RoleDataGen.remove(role);
            }
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
        Host testSite = null;
        Role testRole = null;
        User dummyUser = null;

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
            if (testSite != null) {
                unpublishHost(testSite, systemUser);
                archiveHost(testSite, systemUser);
                deleteHost(testSite, systemUser);
            }
            if (dummyUser != null) {
                UserDataGen.remove(dummyUser);
            }
            if (testRole != null) {
                RoleDataGen.remove(testRole);
            }
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
        Host testSite = null;
        Role testRole = null;
        User dummyUser = null;

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
            if (testSite != null) {
                deleteHost(testSite, systemUser);
            }
            if (dummyUser != null) {
                UserDataGen.remove(dummyUser);
            }
            if (testRole != null) {
                RoleDataGen.remove(testRole);
            }
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
        Role testRole = null;
        User dummyUser = null;

        try {
            // Test data generation
            testRole = new RoleDataGen().nextPersisted();
            dummyUser = new UserDataGen().roles(testRole).nextPersisted();
            final Host systemHost = hostAPI.findSystemHost(dummyUser, true);

            // Assertions
            assertEquals("System Host object NOT found!", "SYSTEM_HOST", systemHost.getIdentifier());
        } finally {
            // Cleanup
            if (dummyUser != null) {
                UserDataGen.remove(dummyUser);
            }
            if (testRole != null) {
                RoleDataGen.remove(testRole);
            }
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
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link HostAPI#searchByStopped(String, boolean, boolean, int, int, User, boolean)}</li>
     *     <li><b>Given Scenario: </b>Create a test Site and stop it. Then, create another Site, then stop it and
     *     archive it. Finally, compare the total count of stopped Sites.</li>
     *     <li><b>Expected Result: </b>When compared to the initial stopped Sites count, after stopping the new Site,
     *     the count must be increased by one. After stopping AND archiving the second Site, the total count difference
     *     must still be 1 because archived Sites are not returned by searchByStopped method.</li>
     * </ul>
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
            this.unpublishHost(testSite, systemUser);
            final PaginatedArrayList<Host> updatedStoppedSites = hostAPI.searchByStopped(null, true, false, 0, 0,
                    systemUser, false);

            // Assertions
            assertEquals("Stopped Sites count difference MUST be one", 1, updatedStoppedSites.size() - stoppedSites
                    .size());

            // Test data generation #2
            siteDataGen = new SiteDataGen();
            testSiteTwo = siteDataGen.nextPersisted();
            this.unpublishHost(testSiteTwo, systemUser);
            this.archiveHost(testSiteTwo, systemUser);
            final PaginatedArrayList<Host> updatedStoppedAndArchivedSites = hostAPI.searchByStopped(null, true,
                    false, 0, 0, systemUser, false);

            // Assertions #2
            assertEquals("After create and archive a site, Stopped Sites count difference MUST be one", 1, updatedStoppedAndArchivedSites.size() - stoppedSites.size());
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
     * Method to test: {@link HostAPI#search(String, boolean, boolean, boolean, int, int, User, boolean)}
     * Given Scenario: look for a site when there is a live and a working version.
     * Expected Result: The API should return the live version of the site for a front-end user.
     */
    @Test
    public void searchShouldReturnLiveVersionForFrontendUser() throws Exception {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();

        Host host = null;
        Role role = null;
        User user = null;

        try {
            // Test data generation
            final String siteName = "returnliveversion.dotcms.com" + System.currentTimeMillis();
            host = new SiteDataGen().name(siteName).nextPersisted();
            role = new RoleDataGen().nextPersisted();
            user = new UserDataGen().roles(role).nextPersisted();

            // Save working version of the host
            final Host workingHost = new Host(APILocator.getContentletAPI().checkout(
                    host.getInode(), APILocator.systemUser(), false));
            final String workingHostAlias = "working." + workingHost.getHostname();
            workingHost.setAliases(workingHostAlias);
            final Host savedHost = new Host(APILocator.getContentletAPI().checkin(
                    workingHost, systemUser, false));
            assertNotEquals(host.getInode(), savedHost.getInode());

            final List<Host> returnedSites = hostAPI.search(siteName,
                    false, true, false, 0, 0,
                    user, true);
            assertFalse("No Sites were returned", returnedSites.isEmpty());
            final Host returnedHost = returnedSites.get(0);

            // Assertions
            assertTrue("Returned Host is NOT live", returnedHost.isLive());
            assertNull("Returned Host has aliases", returnedHost.getAliases());
            assertEquals("Returned Host does NOT match", host, returnedHost);

        } finally {
            // Cleanup
            if (host != null) {
                unpublishHost(host, systemUser);
                archiveHost(host, systemUser);
                deleteHost(host, systemUser);
            }
            if (user != null) {
                UserDataGen.remove(user);
            }
            if (role != null) {
                RoleDataGen.remove(role);
            }
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

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link HostAPI#findAllFromDB(User, HostAPI.SearchType...)}</li>
     *     <li><b>Given Scenario: </b>Create a test Site and call the {@findAllFromDB} method that allows you to include
     *     or exclude the System Host.</li>
     *     <li><b>Expected Result: </b>When calling the method with the {@code includeSystemHost} parameter as
     *     {@code true}, the System Host must be included. Otherwise, it must be left out.</li>
     * </ul>
     */
    @Test
    public void findAllFromDB() throws DotDataException, DotSecurityException {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();

        // Test data generation
        final List<Host> siteList = hostAPI.findAllFromDB(systemUser);
        final List<Host> siteListWithSystemHost = hostAPI.findAllFromDB(systemUser,
                HostAPI.SearchType.INCLUDE_SYSTEM_HOST);

        // Assertions
        assertEquals("The size difference between both Site lists MUST be 1", 1,
                siteListWithSystemHost.size() - siteList.size());
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link HostAPI#findByIdOrKey(String, User, boolean)}</li>
     *     <li><b>Given Scenario: </b>Find the Default Site using either its Identifier or its
     *     Key.</li>
     *     <li><b>Expected Result: </b>The Site API must be able to find the specified Site using
     *     either the Identifier or the Site Key.</li>
     * </ul>
     */
    @Test
    public void findSiteByIdOrKey() throws DotDataException, DotSecurityException {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final HostAPI siteAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();
        final Host defaultSite = siteAPI.findDefaultHost(systemUser, false);
        final String defaultSiteId = defaultSite.getIdentifier();
        final String defaultSiteKey = defaultSite.getHostname();

        // ╔════════════════════════╗
        // ║  Generating Test data  ║
        // ╚════════════════════════╝
        final Optional<Host> siteById = siteAPI.findByIdOrKey(defaultSiteKey, systemUser, false);
        final Optional<Host> siteByKey = siteAPI.findByIdOrKey(defaultSiteId, systemUser, false);

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        assertTrue("The Site by ID was NOT found!", siteById.isPresent());
        assertTrue("The Site by Key was NOT found!", siteByKey.isPresent());
        assertEquals("Site by ID and Site by Key must point to the Default Site",
                siteById.get().getIdentifier(), siteByKey.get().getIdentifier());
        assertEquals("Site by ID must point to the Default Site",
                siteById.get().getIdentifier(), defaultSiteId);
        assertEquals("Site by Key must point to the Default Site",
                siteByKey.get().getIdentifier(), defaultSiteId);
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link HostAPI#findByName(String, User, boolean)}</li>
     *     <li><b>Given Scenario: </b>Call {@code findByName} with a site's UUID identifier instead
     *     of its name. This tests the fallback mechanism in {@link HostFactoryImpl#findSiteByIdIfUUID}
     *     that checks if the provided "name" is actually a UUID and attempts to find the site by ID.</li>
     *     <li><b>Expected Result: </b>The Site API must successfully find the site by its UUID
     *     identifier even when using the findByName method, returning the same site as would be
     *     returned by {@code find(id, user, false)}.</li>
     * </ul>
     */
    @Test
    public void testFindByName_whenPassingUUID_shouldReturnSiteById() throws Exception {
        // Initialization
        final HostAPI hostAPI = APILocator.getHostAPI();
        final User systemUser = APILocator.systemUser();
        Host testSite = null;

        try {
            // Test data generation - create a new site
            testSite = new SiteDataGen().nextPersisted();
            final String siteId = testSite.getIdentifier();
            final String siteName = testSite.getHostname();

            // Call findByName with the site's name (normal scenario)
            final Host siteFoundByName = hostAPI.findByName(siteName, systemUser, false);
            assertNotNull("Site should be found by name", siteFoundByName);
            assertEquals("Site found by name should match original site",
                    siteId, siteFoundByName.getIdentifier());

            // Call findByName with the site's UUID identifier (new scenario being tested)
            final Host siteFoundByUUID = hostAPI.findByName(siteId, systemUser, false);
            assertNotNull("Site should be found when passing UUID to findByName", siteFoundByUUID);
            assertEquals("Site found by UUID should match original site",
                    siteId, siteFoundByUUID.getIdentifier());
            assertEquals("Site found by UUID should be the same as site found by name",
                    siteFoundByName.getIdentifier(), siteFoundByUUID.getIdentifier());

            // Verify that the fallback mechanism in findSiteByIdIfUUID was used
            // by confirming both methods return the same site
            final Host siteFoundById = hostAPI.find(siteId, systemUser, false);
            assertNotNull("Site should be found by ID", siteFoundById);
            assertEquals("Site found by UUID in findByName should match site found by find(id)",
                    siteFoundById.getIdentifier(), siteFoundByUUID.getIdentifier());
        } finally {
            // Cleanup
            if (testSite != null) {
                unpublishHost(testSite, systemUser);
                archiveHost(testSite, systemUser);
                deleteHost(testSite, systemUser);
            }
        }
    }

}
