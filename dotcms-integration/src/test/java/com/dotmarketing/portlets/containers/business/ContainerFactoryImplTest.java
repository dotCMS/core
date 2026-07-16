package com.dotmarketing.portlets.containers.business;

import static org.junit.Assert.*;

import com.dotmarketing.business.DotStateException;
import java.util.Date;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.util.SQLUtilTest;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

public class ContainerFactoryImplTest {

    private static HostAPI hostAPI;
    private static User user;
    private static UserAPI userAPI;
    private static Host host;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        hostAPI = APILocator.getHostAPI();
        userAPI = APILocator.getUserAPI();
        user = userAPI.getSystemUser();
        host = hostAPI.findDefaultHost(user, false);
    }

    /**
     * this tests whether we properly escaping the orderby clause in the
     * find containers method
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void test_container_find_by_query_no_SQL_injection_in_orderby() throws DotDataException, DotSecurityException {

        final Host daHost = new SiteDataGen().nextPersisted();

        Container container =  new ContainerDataGen()
                .site(daHost)
                .title(UUIDGenerator.generateUuid() + SQLUtilTest.MALICIOUS_SQL_CONDITION)
                .nextPersisted();

        assertNotNull(container);
        assertNotNull(container.getInode());
        Container containerFromDB = APILocator.getContainerAPI().find(container.getInode(), APILocator.systemUser(), false);

        // get normally
        List<Container> containers = FactoryLocator.getContainerFactory().findContainers(user, false, ImmutableMap.of("title",container.getTitle() ), daHost.getIdentifier(), null, null, null, 0, 100, "mod_date");

        assertFalse("Containers list should not be empty with normal orderBy", containers.isEmpty());
        assertTrue("Containers list should contain the created container with normal orderBy", containers.contains(containerFromDB));

        // get with a malicious SQL order by
        containers = FactoryLocator.getContainerFactory().findContainers(user, false, ImmutableMap.of("title",container.getTitle() ), daHost.getIdentifier(), null, null, null, 0, 100, SQLUtilTest.MALICIOUS_SQL_ORDER_BY);

        assertFalse("Containers list should not be empty even with malicious orderBy", containers.isEmpty());
        assertTrue("Containers list should contain the created container even with malicious orderBy", containers.contains(containerFromDB));
    }

    @Test
    public void test_find_inodeInputParameter_shouldReturnContainerByInode() throws DotDataException, DotSecurityException {

        final Host daHost = new SiteDataGen().nextPersisted();

        Container newContainer =  new ContainerDataGen()
                .site(daHost)
                .title(UUIDGenerator.generateUuid())
                .nextPersisted();

        assertNotNull(newContainer);
        assertNotNull(newContainer.getInode());

        Container containerFromDB = APILocator.getContainerAPI().find(newContainer.getInode(), APILocator.systemUser(), false);

        final Container container = FactoryLocator.getContainerFactory().find(newContainer.getInode());

        assertNotNull(container);
        assertNotNull(container.getInode());

        APILocator.getContainerAPI().delete(container, APILocator.systemUser(), false);
        assertNull(FactoryLocator.getContainerFactory().find(newContainer.getInode()));
    }
    @Test
    public void test_save_containerWithoutIdentifier_shouldThrowException() throws DotDataException, DotSecurityException {

        Container container = new Container();

        container.setMaxContentlets(0);
        container.setNotes("some notes");
        container.setPreLoop("preLoop xxxx");
        container.setPostLoop("postLoop xxxx");
        container.setFriendlyName("Test container");
        container.setModDate(new Date());
        container.setModUser(user.getUserId());
        container.setOwner(user.getUserId());
        container.setTitle("Test container");
        container.setInode(UUIDGenerator.generateUuid());

        assertThrows(DotStateException.class,
                ()->{
                    FactoryLocator.getContainerFactory().save(container);
                });
    }
    @Test
    public void test_save_inputNewContainerData_shouldInsertNewContainer() throws DotDataException, DotSecurityException {

        final String newContainerInode = UUIDGenerator.generateUuid();
        final String newContainerIdentifier = UUIDGenerator.generateUuid();

        Container container = new Container();

        container.setMaxContentlets(0);
        container.setNotes("some notes");
        container.setPreLoop("preLoop xxxx");
        container.setPostLoop("postLoop xxxx");
        container.setFriendlyName("Test container");
        container.setModDate(new Date());
        container.setModUser(user.getUserId());
        container.setOwner(user.getUserId());
        container.setTitle("Test container");
        container.setInode(newContainerInode);
        container.setIdentifier(newContainerIdentifier);

        final Container nonExistingContainer = FactoryLocator.getContainerFactory().find(newContainerInode);
        assertNull(nonExistingContainer);

        APILocator.getIdentifierAPI().createNew(container, host, container.getIdentifier());
        FactoryLocator.getContainerFactory().save(container);

        final Container existingContainer = FactoryLocator.getContainerFactory().find(newContainerInode);
        assertNotNull(existingContainer);

        APILocator.getContainerAPI().delete(existingContainer, APILocator.systemUser(), false);
        assertNull(FactoryLocator.getContainerFactory().find(existingContainer.getInode()));
    }
    @Test
    public void test_save_inputExistingContainerData_shouldUpdateExistingContainer() throws DotDataException, DotSecurityException {

        final String newContainerInode = UUIDGenerator.generateUuid();
        final String newContainerIdentifier = UUIDGenerator.generateUuid();

        Container container = new Container();

        container.setMaxContentlets(0);
        container.setNotes("some notes");
        container.setPreLoop("preLoop xxxx");
        container.setPostLoop("postLoop xxxx");
        container.setFriendlyName("Test container");
        container.setModDate(new Date());
        container.setModUser(user.getUserId());
        container.setOwner(user.getUserId());
        container.setTitle("Test container");
        container.setInode(newContainerInode);
        container.setIdentifier(newContainerIdentifier);

        final Container nonExistingContainer = FactoryLocator.getContainerFactory().find(newContainerInode);
        assertNull(nonExistingContainer);

        APILocator.getIdentifierAPI().createNew(container, host, container.getIdentifier());
        FactoryLocator.getContainerFactory().save(container);

        Container existingContainer = FactoryLocator.getContainerFactory().find(newContainerInode);
        assertNotNull(existingContainer);

        existingContainer.setTitle("Updated title");
        existingContainer.setFriendlyName("Updated friendly name");

        FactoryLocator.getContainerFactory().save(existingContainer);

        existingContainer = FactoryLocator.getContainerFactory().find(newContainerInode);

        assertEquals("Updated title",existingContainer.getTitle());
        assertEquals("Updated friendly name",existingContainer.getFriendlyName());

        APILocator.getContainerAPI().delete(existingContainer, APILocator.systemUser(), false);
        assertNull(FactoryLocator.getContainerFactory().find(existingContainer.getInode()));
    }
}