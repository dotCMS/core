package com.dotmarketing.portlets.containers.business;

import com.dotcms.JUnit4WeldRunner;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotmarketing.beans.*;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.AssetUtil;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.commons.beanutils.BeanUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.Dependent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import static com.dotmarketing.util.Constants.CONTAINER_FOLDER_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This class will test operations related with interacting with Containers.
 *
 * @author Jorge Urdaneta
 * @since Aug 31st, 2012
 */
@Dependent
@RunWith(JUnit4WeldRunner.class)
public class ContainerAPITest extends ContentletBaseTest {

    @Test
    public void save() throws Exception {
    	HibernateUtil.startTransaction();
        Container c = new Container();
        c.setFriendlyName("test container");
        c.setTitle("this is the title");
        c.setMaxContentlets(5);
        c.setPreLoop("preloop code");
        c.setPostLoop("postloop code");

        Container cc = new Container();
        BeanUtils.copyProperties(cc, c);

        List<ContainerStructure> csList = new ArrayList<>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(contentTypeAPI.find("host").inode());
        cs.setCode("this is the code");
        csList.add(cs);

        cc = containerAPI.save(cc, csList, defaultHost, user, false);

        assertTrue(UtilMethods.isSet(cc.getInode()));
        assertTrue(UtilMethods.isSet(cc.getIdentifier()));

        cc = containerAPI.getWorkingContainerById(cc.getIdentifier(), user, false);

        assertTrue(UtilMethods.isSet(cc.getInode()));
        assertTrue(UtilMethods.isSet(cc.getIdentifier()));

        List<ContainerStructure> csListCopy = containerAPI.getContainerStructures(cc);
        ContainerStructure csCopy = csListCopy.get(0);
        assertEquals(csCopy.getCode(), cs.getCode());
        assertEquals(cc.getFriendlyName(), c.getFriendlyName());
        assertEquals(cc.getTitle(), c.getTitle());
        assertEquals(cc.getMaxContentlets(), c.getMaxContentlets());
        assertEquals(cc.getPreLoop(), c.getPreLoop());
        assertEquals(cc.getPostLoop(), c.getPostLoop());
        HibernateUtil.closeAndCommitTransaction();
    }

    @Test
    public void saveWithExistingIds() throws Exception {
        Container c = new Container();
        c.setFriendlyName("test container for existing inode/identifier");
        c.setTitle("this is the title for existing inode/identifier");
        c.setMaxContentlets(5);
        c.setPreLoop("preloop code");
        c.setPostLoop("postloop code");

        // here comes the existing inode/identifier
        String existingInode=UUIDGenerator.generateUuid();
        String existingIdentifier=UUIDGenerator.generateUuid();
        c.setInode(existingInode);
        c.setIdentifier(existingIdentifier);

        Container cc = new Container();
        BeanUtils.copyProperties(cc, c);

        List<ContainerStructure> csList = new ArrayList<>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(contentTypeAPI.find("host").inode());
        cs.setCode("this is the code");
        csList.add(cs);

        cc = containerAPI.save(cc, csList, defaultHost, user, false);

        assertTrue(UtilMethods.isSet(cc.getInode()));
        assertTrue(UtilMethods.isSet(cc.getIdentifier()));

        cc = containerAPI.getWorkingContainerById(cc.getIdentifier(), user, false);

        assertTrue(UtilMethods.isSet(cc.getInode()));
        assertTrue(UtilMethods.isSet(cc.getIdentifier()));

        // existing inode/identifier should match
        assertEquals(cc.getInode(),existingInode);
        assertEquals(cc.getIdentifier(), existingIdentifier);

        assertEquals(cc.getCode(),c.getCode());
        assertEquals(cc.getFriendlyName(),c.getFriendlyName());
        assertEquals(cc.getTitle(),c.getTitle());
        assertEquals(cc.getMaxContentlets(),c.getMaxContentlets());
        assertEquals(cc.getPreLoop(),c.getPreLoop());
        assertEquals(cc.getPostLoop(),c.getPostLoop());

        // now an update with existing inode
        String newInode=UUIDGenerator.generateUuid();
        cc.setPreLoop("new preloop");
        cc.setInode(newInode);
        cc = containerAPI.save(cc, csList, defaultHost, user, false);
        assertEquals(newInode, cc.getInode());
        assertEquals(existingIdentifier, cc.getIdentifier());
        cc = containerAPI.getWorkingContainerById(cc.getIdentifier(), user, false);
        assertEquals(newInode, cc.getInode());
        assertEquals(existingIdentifier, cc.getIdentifier());
        assertEquals(cc.getPreLoop(),"new preloop");
    }

    @Test
    public void delete() throws Exception {
        Container container = LocalTransaction.wrapReturnWithListeners(() -> {
            final Container saved = createContainer();

            if (containerAPI.delete(saved, user, false))
                return saved;
            else
                throw new DotDataException("An error occurred deleting container");
        });

        AssetUtil.assertDeleted(container.getInode(), container.getIdentifier(), Inode.Type.CONTAINERS.getValue());
    }

    @Test
    public void testCopy() throws Exception {
        LocalTransaction.wrap(() -> {
            try {
                copyContainer();
            } catch (DotSecurityException e) {
                throw new DotDataException(e);
            }
        });
    }

    private void copyContainer() throws DotSecurityException, DotDataException {
        final Container source = createContainer();

        final Container target = containerAPI.copy(source, defaultHost, user, false);

        assertNotNull(target);
        assertNotNull(target.getTitle());
        assertNotNull(target.getInode());
        assertTrue(target.getTitle().contains(source.getTitle()));
        assertNotEquals(source.getInode(), target.getInode());

    }

    @Test
    public void testFindContainersUnder() throws DotDataException {
        final List<Container> results = containerAPI.findContainersUnder(defaultHost);
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    public void testFindAllContainers() throws DotDataException, DotSecurityException {
        final List<Container> results = containerAPI.findAllContainers(user, false);
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    public void testFindContainers() throws DotDataException, DotSecurityException {
        final List<Container> results = containerAPI
                .findContainers(user, false, null, defaultHost.getIdentifier(), null, null, null, 0,
                        -1, null);
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    public void testFindContainersWithParent() throws DotDataException, DotSecurityException {

        final ContentType contentType = TestDataUtils.getBlogLikeContentType();
        final Container container = new ContainerDataGen().withContentType(contentType, "").nextPersisted();
        final Identifier identifier = APILocator.getIdentifierAPI().find(container.getIdentifier());

        final List<Container> results = containerAPI
                .findContainers(user, false, null, identifier.getHostId(), null, null,
                        contentType.id(), 0,
                        -1, null);
        assertTrue(container.toString(), UtilMethods.isSet(results));
    }

    private Container createContainer()
            throws DotSecurityException, DotDataException {

        final User user = APILocator.systemUser();
        final Host site = new SiteDataGen().nextPersisted();
        final ContentType contentType = APILocator.getContentTypeAPI(user).find("webPageContent");
        final String nameTitle = "anyTestContainer" + System.currentTimeMillis();

        return new ContainerDataGen()
                .site(site)
                .modUser(user)
                .friendlyName(nameTitle)
                .title(nameTitle)
                .withContentType(contentType, "$!{body}")
                .nextPersisted();
    }

    /**
     * Method to test: {@link ContainerAPI#save(Container, List, Host, User, boolean)}
     *
     * Given Scenario: Trying to save the System Container.
     *
     * Expected Result: An IllegalArgumentException must be thrown as the system Container cannot be saved.
     */
    @Test(expected = IllegalArgumentException.class)
    public void savingSystemContainer() throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();

        final Container systemContainer = containerAPI.systemContainer();
        containerAPI.save(systemContainer, new ArrayList<>(), defaultHost, systemUser, false);
    }

    /**
     * Method to test: {@link ContainerAPI#copy(Container, Host, User, boolean)}
     *
     * Given Scenario: Trying to copy the System Container.
     *
     * Expected Result: An IllegalArgumentException must be thrown as the system Container cannot be copied.
     */
    @Test(expected = IllegalArgumentException.class)
    public void copyingSystemContainer() throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();

        final Container systemContainer = containerAPI.systemContainer();
        containerAPI.copy(systemContainer, defaultHost, systemUser, false);
    }

    /**
     * Method to test: {@link ContainerAPI#delete(Container, User, boolean)}
     *
     * Given Scenario: Trying to delete the System Container.
     *
     * Expected Result: An IllegalArgumentException must be thrown as the system Container cannot be deleted.
     */
    @Test(expected = IllegalArgumentException.class)
    public void deletingSystemContainer() throws DotDataException, DotSecurityException {
        // Initialization
        final User systemUser = APILocator.systemUser();

        // Test data generation
        final Container systemContainer = containerAPI.systemContainer();
        containerAPI.delete(systemContainer, systemUser, false);

        // Assertions
        // See expected exception in this method's signature
    }

    /**
     * Method to test: {@link ContainerAPI#getParentHost(Container, User, boolean)}
     *
     * Given Scenario: Checking that the System Container belongs to the System Host only.
     *
     * Expected Result: Getting the parent Site from System Container must always point to System Host.
     */
    @Test
    public void gettingParentHostFromSystemContainer() throws DotDataException, DotSecurityException {
        // Initialization
        final User systemUser = APILocator.systemUser();
        final Container systemContainer = containerAPI.systemContainer();

        // Test data generation
        final Host parentSite = containerAPI.getParentHost(systemContainer, systemUser, false);

        // Assertions
        assertEquals("System Container must always belong to System Host!", Host.SYSTEM_HOST,
                parentSite.getIdentifier());
    }

    /**
     * Method to test: {@link ContainerAPI#findContainers(User, ContainerAPI.SearchParams)}
     *
     * Given Scenario: Searching for all Containers, making sure that the System Container is NOT returned.
     *
     * Expected Result: The System Container must NOT be returned.
     */
    @Test
    public void findContainersWithoutSystemContainer() throws DotDataException, DotSecurityException {
        // Initialization
        final boolean showSystemContainer = Boolean.FALSE;
        final ContainerAPI.SearchParams searchParams = ContainerAPI.SearchParams.newBuilder()
                .includeArchived(false)
                .includeSystemContainer(showSystemContainer).build();
        final List<Container> allContainers = containerAPI.findContainers(user, searchParams);

        // Test data generation
        Container systemContainer =
                allContainers.stream().filter(container -> Container.SYSTEM_CONTAINER.equals(container.getIdentifier()))
                        .findFirst().orElse(null);

        // Assertions
        assertTrue("System Container must NOT be returned in this test", null == systemContainer);
    }

    /**
     * Method to test: {@link ContainerAPI#findContainers(User, ContainerAPI.SearchParams)}
     *
     * Given Scenario: Searching for all Containers, making sure that the System Container IS returned.
     *
     * Expected Result: The System Container MUST be returned.
     */
    @Test
    public void findContainersWithSystemContainer() throws DotDataException, DotSecurityException {
        // Initialization
        final boolean showSystemContainer = Boolean.TRUE;
        final ContainerAPI.SearchParams searchParams = ContainerAPI.SearchParams.newBuilder()
                .includeArchived(false)
                .includeSystemContainer(showSystemContainer).build();
        final List<Container> allContainers = containerAPI.findContainers(user, searchParams);

        // Test data generation
        Container systemContainer =
                allContainers.stream().filter(container -> Container.SYSTEM_CONTAINER.equals(container.getIdentifier()))
                        .findFirst().orElse(null);

        // Assertions
        assertTrue("System Container MUST be returned in this test", null != systemContainer);
    }

    /**
     * Method to test: {@link ContainerAPI#findContainers(User, ContainerAPI.SearchParams)}
     *
     * Given Scenario: Creating a Container, and then deleting it.
     *
     * Expected Result: The total Container count must be different.
     */
    @Test
    public void compareCountDeletedContainer() throws DotDataException, DotSecurityException {
        // Initialization
        final ContentType contentGenericContentType = APILocator.getContentTypeAPI(user).find("webPageContent");
        final Container container = new ContainerDataGen()
                .title("My Test Container-" + System.currentTimeMillis())
                .withContentType(contentGenericContentType, "").nextPersisted();
        final boolean showSystemContainer = Boolean.FALSE;
        try {
            final ContainerAPI.SearchParams searchParams = ContainerAPI.SearchParams.newBuilder()
                    .includeArchived(false)
                    .includeSystemContainer(showSystemContainer).build();
            List<Container> allContainers = containerAPI.findContainers(user, searchParams);
            final int originalCount = allContainers.size();

            // Test data generation
            containerAPI.delete(container, user, false);
            allContainers = containerAPI.findContainers(user, searchParams);

            // Assertions
            assertTrue("Total Container count MUST be lower than the original count",
                    originalCount > allContainers.size());
        } finally {
            if (null != container) {
                containerAPI.delete(container, user, false);
            }
        }
    }

    /**
     * Method to test: {@link ContainerAPI#findContainers(User, ContainerAPI.SearchParams)}
     *
     * Given Scenario: Searching for all Containers that reference a specific test Content Type.
     *
     * Expected Result: Only one Container must be returned.
     */
    @Test
    public void findContainerUsedBySpecificContentType() throws DotDataException, DotSecurityException {
        // Initialization
        final ContentType testContentType = new ContentTypeDataGen()
                .name("My Test CT-" + System.currentTimeMillis())
                .nextPersisted();

        final Container container = new ContainerDataGen()
                .title("My Test Container-" + System.currentTimeMillis())
                .withContentType(testContentType, "").nextPersisted();
        final boolean showSystemContainer = Boolean.FALSE;
        try {
            // Test data generation
            final ContainerAPI.SearchParams searchParams = ContainerAPI.SearchParams.newBuilder()
                    .includeArchived(false)
                    .includeSystemContainer(showSystemContainer)
                    .contentTypeIdOrVar(testContentType.id()).build();
            final List<Container> allContainers = containerAPI.findContainers(user, searchParams);
            final int originalCount = allContainers.size();

            // Assertions
            assertEquals("There must only be ONE Container using the test Content Type", 1, originalCount);
        } finally {
            if (null != testContentType) {
                contentTypeAPI.delete(testContentType);
            }
            if (null != container) {
                containerAPI.delete(container, user, false);
            }
        }
    }

    /**
     * Method to test: {@link ContainerAPI#findContainers(User, ContainerAPI.SearchParams)}
     * Given Scenario: Searching for Containers with a given name.
     * Expected Result: At least one Container must be returned.
     */
    @Test
    public void findFileContainerByName() throws DotDataException, DotSecurityException {
        Host testSite = null;
        try {
            // Test data generation
            testSite = new SiteDataGen().nextPersisted();
            final String containerName = generateRandomName(10);
            final String metadataCode = "$dotJSON.put(\"title\", \"Test "
                    + containerName + " File Container\")\n"
                    + "$dotJSON.put(\"max_contentlets\", 25)";
            new ContainerAsFileDataGen()
                    .host(testSite)
                    .folderName("Test" + containerName + " File Container")
                    .metadataCode(metadataCode)
                    .nextPersisted();

            // Find by name
            final ContainerAPI.SearchParams searchParams = ContainerAPI.SearchParams.newBuilder()
                    .includeArchived(false)
                    .includeSystemContainer(false)
                    .siteId(testSite.getIdentifier())
                    .filteringCriterion(Map.of("title", containerName.toLowerCase())).build();
            final List<Container> allContainers = containerAPI.findContainers(user, searchParams);

            // Assertions
            assertFalse("There must be at least one Container with the name 'File Container'", allContainers.isEmpty());
        } finally {
            // Clean up
            if (null != testSite) {
                APILocator.getHostAPI().archive(testSite, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(testSite, APILocator.systemUser(), false);
            }
        }
    }

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Random RANDOM = new Random();

    /**
     * Generates a random name with the given length.
     *
     * @param length the length of the name to generate
     * @return the generated name
     */
    private static String generateRandomName(int length) {
        StringBuilder name = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            name.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return name.toString();
    }
}
