package com.dotcms.util.pagination;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerAsFileDataGen;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerView;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.apache.commons.beanutils.BeanUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Jose Castro
 * @since Apr 26th, 2022
 */
public class ContainerPaginatorTest extends ContentletBaseTest {

    /**
     * Method to test: {@link ContainerPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     *
     * Given Scenario: Get a paginated list of Containers based on specific filtering criteria. This method creates
     * three test Containers, but sets an offset value of 2.
     *
     * Expected Result: The list of paginated Containers with an offset of 2, which leaves 2 of the 3 test Containers
     * out of the result set. This will confirm that the pagination works.
     */
    @Test
    public void getPaginatedContainers() throws DotDataException, DotSecurityException {
        // Initialization
        final String ORDER_BY = "title";
        final String timestamp = String.valueOf(System.currentTimeMillis());
        final String filter = "Container Paginator Test " + timestamp;
        final int limit = 20;
        final int offset = 2;
        Container containerOne = null;
        Container containerTwo = null;
        Container containerThree = null;
        // Need to filter to this test.  There could be more than 20 containers in the system.
        final ContainerAPI.SearchParams searchParams = ContainerAPI.SearchParams.newBuilder()
                .siteId(defaultHost.getIdentifier())
                .filteringCriteria("title", filter)
                .orderBy(ORDER_BY).build();
        List<Container> containerList = containerAPI.findContainers(user, searchParams);
        final int initialContainerCount = containerList.size();

        try {
            // Test data generation
            containerOne = new ContainerDataGen().site(defaultHost).title(filter+" 1").nextPersisted();
            containerTwo = new ContainerDataGen().site(defaultHost).title(filter+" 2").nextPersisted();
            containerThree = new ContainerDataGen().site(defaultHost).title(filter+" 3").nextPersisted();


            TestDataUtils.waitForEmptyQueue();

            final ContainerPaginator containerPaginator = new ContainerPaginator();
            final PaginatedArrayList<ContainerView> containersViews = containerPaginator.getItems(user, filter, limit, offset, ORDER_BY,
                    OrderDirection.ASC, Map.of(ContainerPaginator.HOST_PARAMETER_ID, defaultHost.getIdentifier()));
            containerList = containersViews.stream()
                    .map(containerView -> containerView.getContainer())
                    .collect(Collectors.toList());
            final int finalContainerCount = containerList.size();

            // Assertions
            assertEquals("There must be a difference of only 1 Container after comparing results!", 1,
                    finalContainerCount - initialContainerCount);
        } finally {
            // Data cleanup
            if (null != containerOne) {
                containerAPI.delete(containerOne, user, false);
            }
            if (null != containerTwo) {
                containerAPI.delete(containerTwo, user, false);
            }
            if (null != containerThree) {
                containerAPI.delete(containerThree, user, false);
            }
        }
    }


    /**
     * Method to test: {@link ContainerPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     * Given Scenario: Two Containers use the same Content Type. A back-end user can read the Content Type and
     * the first Container, but has no permission on the second one.
     * Expected Result: Filtering by that Content Type returns only the first Container.
     */
    @Test
    public void getItems_byContentType_returnsOnlyContainersTheUserCanRead() throws Exception {
        final ContentTypeUsage usage = new ContentTypeUsage();
        try {
            usage.create();
            usage.grantRead(usage.firstContainer);

            assertEquals(Set.of(usage.firstContainer.getIdentifier()), usage.identifiersFound());
        } finally {
            usage.cleanUp();
        }
    }

    /**
     * Method to test: {@link ContainerPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     * Given Scenario: Two Containers use the same Content Type, and a back-end user can read the Content Type
     * and both Containers.
     * Expected Result: Filtering by that Content Type returns both Containers.
     */
    @Test
    public void getItems_byContentType_returnsEveryReadableContainer() throws Exception {
        final ContentTypeUsage usage = new ContentTypeUsage();
        try {
            usage.create();
            usage.grantRead(usage.firstContainer);
            usage.grantRead(usage.secondContainer);

            assertEquals(Set.of(usage.firstContainer.getIdentifier(), usage.secondContainer.getIdentifier()),
                    usage.identifiersFound());
        } finally {
            usage.cleanUp();
        }
    }

    /**
     * Method to test: {@link ContainerPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     * Given Scenario: Two Containers use the same Content Type, a back-end user can read both, and the second
     * one is archived.
     * Expected Result: Filtering by that Content Type returns the archived Container only when archived
     * Containers are requested.
     */
    @Test
    public void getItems_byContentTypeWithArchived_returnsArchivedContainer() throws Exception {
        final ContentTypeUsage usage = new ContentTypeUsage();
        try {
            usage.create();
            usage.grantRead(usage.firstContainer);
            usage.grantRead(usage.secondContainer);
            containerAPI.archive(usage.secondContainer, APILocator.systemUser(), false);

            assertEquals(Set.of(usage.firstContainer.getIdentifier()), usage.identifiersFound());
            assertEquals(Set.of(usage.firstContainer.getIdentifier(), usage.secondContainer.getIdentifier()),
                    usage.identifiersFound(true));
        } finally {
            usage.cleanUp();
        }
    }

    /**
     * Method to test: {@link ContainerPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     * Given Scenario: Two Containers use a Content Type, and a back-end user has no permission on that
     * Content Type.
     * Expected Result: Filtering by that Content Type fails with a permission error.
     */
    @Test
    public void getItems_byContentTypeTheUserCannotRead_failsWithPermissionError() throws Exception {
        final ContentTypeUsage usage = new ContentTypeUsage();
        try {
            usage.create();
            final User outsider = usage.createOutsider();

            final PaginationException error = assertThrows(PaginationException.class,
                    () -> usage.search(outsider, usage.contentType.id()));
            assertTrue(ExceptionUtil.causedBy(error, DotSecurityException.class));
        } finally {
            usage.cleanUp();
        }
    }

    /**
     * Method to test: {@link ContainerPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     * Given Scenario: No Content Type exists with the requested variable.
     * Expected Result: Filtering by it fails with a not-found error.
     */
    @Test
    public void getItems_byUnknownContentType_failsWithNotFoundError() {
        final PaginationException error = assertThrows(PaginationException.class,
                () -> new ContainerPaginator().getItems(APILocator.systemUser(), "", 40, 0, "title",
                        OrderDirection.ASC,
                        Map.of(ContainerPaginator.CONTENT_TYPE, "unknownType" + System.currentTimeMillis())));
        assertTrue(ExceptionUtil.causedBy(error, NotFoundInDbException.class));
    }

    /**
     * Method to test: {@link ContainerPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     * Given Scenario: A Container used a Content Type in an earlier version, and its current version uses a
     * different Content Type only.
     * Expected Result: Filtering by the first Content Type does not return that Container.
     */
    @Test
    public void getItems_byContentType_skipsContainersThatOnlyUsedItInAnOlderVersion() throws Exception {
        final ContentTypeUsage usage = new ContentTypeUsage();
        try {
            usage.create();
            usage.grantRead(usage.firstContainer);
            usage.grantRead(usage.secondContainer);
            usage.saveNewVersionWithout(usage.secondContainer);

            assertEquals(Set.of(usage.firstContainer.getIdentifier()), usage.identifiersFound());
        } finally {
            usage.cleanUp();
        }
    }

    /**
     * Method to test: {@link ContainerPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     * Given Scenario: A file-based Container that uses a Content Type cannot be loaded, because its
     * {@code container.vtl} exists only in a language other than the default one.
     * Expected Result: Filtering by that Content Type fails instead of returning a list without that
     * Container, and listing Containers without a Content Type still works.
     */
    @Test
    public void getItems_byContentTypeWhenAFileContainerCannotBeLoaded_fails() throws Exception {
        final ContentType contentType = new ContentTypeDataGen().host(defaultHost).nextPersisted();
        final Language otherLanguage = new LanguageDataGen().nextPersisted();
        final ContainerAsFileDataGen fileContainerGen = new ContainerAsFileDataGen().host(defaultHost)
                .ignoreDefaultContentTypes().contentType(contentType, "$!{body}");
        final FileAssetContainer fileContainer = fileContainerGen.nextPersisted();
        final long defaultLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        try {
            moveToLanguage(fileContainer.getIdentifier(), otherLanguage.getId());

            assertThrows(DotRuntimeException.class, () -> new ContainerPaginator().getItems(
                    APILocator.systemUser(), "", 40, 0, "title", OrderDirection.ASC,
                    Map.of(ContainerPaginator.CONTENT_TYPE, contentType.id())));
            new ContainerPaginator().getItems(APILocator.systemUser(), "", 40, 0, "title", OrderDirection.ASC,
                    Map.of());
        } finally {
            moveToLanguage(fileContainer.getIdentifier(), defaultLanguageId);
            APILocator.getFolderAPI().delete(APILocator.getFolderAPI().find(
                            fileContainerGen.getMetaData().getFolder(), APILocator.systemUser(), false),
                    APILocator.systemUser(), false);
            ContentTypeDataGen.remove(contentType);
            LanguageDataGen.remove(otherLanguage);
        }
    }

    /** Moves every version of a Contentlet to the given language, and flushes the caches that hold it. */
    private static void moveToLanguage(final String identifier, final long languageId) throws DotDataException {
        new DotConnect().executeUpdate("update contentlet set language_id = ? where identifier = ?",
                languageId, identifier);
        new DotConnect().executeUpdate("update contentlet_version_info set lang = ? where identifier = ?",
                languageId, identifier);
        CacheLocator.getCacheAdministrator().flushAll();
    }

    /**
     * A Content Type used by two Containers, and a back-end user who can read the Content Type. Each
     * Container starts with an individual permission for a separate role only, so it does not inherit any
     * permission from its Site and the user can read it only when {@link #grantRead} is called.
     * {@link #cleanUp()} removes whatever {@link #create()} got to create.
     */
    private static class ContentTypeUsage {

        private Role ownerRole;
        private User reader;
        private Role readerRole;
        private ContentType contentType;
        private Container firstContainer;
        private Container secondContainer;
        private User outsider;

        void create() throws DotDataException, DotSecurityException {
            ownerRole = new RoleDataGen().nextPersisted();
            reader = new UserDataGen()
                    .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                    .nextPersisted();
            readerRole = APILocator.getRoleAPI().getUserRole(reader);
            contentType = new ContentTypeDataGen().host(defaultHost).nextPersisted();
            savePermission(contentType, readerRole);
            firstContainer = newContainer();
            savePermission(firstContainer, ownerRole);
            secondContainer = newContainer();
            savePermission(secondContainer, ownerRole);
        }

        private Container newContainer() {
            return new ContainerDataGen().site(defaultHost)
                    .withContentType(contentType, "$!{body}").nextPersisted();
        }

        void grantRead(final Container container) throws DotDataException, DotSecurityException {
            savePermission(container, readerRole);
        }

        /** Saves a new working version of the Container that uses only the Page Content Type. */
        void saveNewVersionWithout(final Container container) throws Exception {
            final Container nextVersion = new Container();
            BeanUtils.copyProperties(nextVersion, container);
            nextVersion.setInode(null);
            final ContainerStructure pageUsage = new ContainerStructure();
            pageUsage.setStructureId(APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find("htmlpageasset").id());
            pageUsage.setCode("$!{title}");
            containerAPI.save(nextVersion, List.of(pageUsage), defaultHost, APILocator.systemUser(), false);
        }

        Set<String> identifiersFound() {
            return identifiersFound(false);
        }

        Set<String> identifiersFound(final boolean includeArchived) {
            return new ContainerPaginator().getItems(reader, "", 40, 0, "title", OrderDirection.ASC,
                            Map.of(ContainerPaginator.CONTENT_TYPE, contentType.id(),
                                    ContainerPaginator.ARCHIVE_PARAMETER_NAME, includeArchived)).stream()
                    .map(containerView -> containerView.getContainer().getIdentifier())
                    .collect(Collectors.toSet());
        }

        /** Creates a back-end user with no permission on the Content Type or its Containers. */
        User createOutsider() throws DotDataException {
            outsider = new UserDataGen()
                    .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                    .nextPersisted();
            return outsider;
        }

        void search(final User user, final String contentTypeIdOrVar) {
            new ContainerPaginator().getItems(user, "", 40, 0, "title", OrderDirection.ASC,
                    Map.of(ContainerPaginator.CONTENT_TYPE, contentTypeIdOrVar));
        }

        private static void savePermission(final Permissionable permissionable, final Role role)
                throws DotDataException, DotSecurityException {
            permissionAPI.save(new Permission(permissionable.getPermissionId(), role.getId(),
                    PermissionAPI.PERMISSION_READ, true), permissionable, APILocator.systemUser(), false);
        }

        /**
         * Removes the test data in reverse order of creation. Every step runs even when an earlier one
         * fails; the first failure is thrown at the end, with the others attached as suppressed.
         */
        void cleanUp() throws Exception {
            final List<Exception> failures = new ArrayList<>();
            removeIfCreated(failures, secondContainer, ContainerDataGen::remove);
            removeIfCreated(failures, firstContainer, ContainerDataGen::remove);
            removeIfCreated(failures, contentType, ContentTypeDataGen::remove);
            removeIfCreated(failures, outsider, user -> APILocator.getUserAPI()
                    .delete(user, TestUserUtils.getAdminUser(), APILocator.systemUser(), false));
            removeIfCreated(failures, reader, user -> APILocator.getUserAPI()
                    .delete(user, TestUserUtils.getAdminUser(), APILocator.systemUser(), false));
            removeIfCreated(failures, ownerRole, RoleDataGen::remove);
            if (!failures.isEmpty()) {
                final Exception first = failures.get(0);
                failures.subList(1, failures.size()).forEach(first::addSuppressed);
                throw first;
            }
        }

        private static <T> void removeIfCreated(final List<Exception> failures, final T created,
                final Removal<T> removal) {
            if (null == created) {
                return;
            }
            try {
                removal.remove(created);
            } catch (final Exception e) {
                failures.add(e);
            }
        }

        /** Removes one piece of test data. */
        @FunctionalInterface
        private interface Removal<T> {
            void remove(T created) throws Exception;
        }
    }

}
