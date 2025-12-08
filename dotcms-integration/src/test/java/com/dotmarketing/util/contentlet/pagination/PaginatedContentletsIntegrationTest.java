package com.dotmarketing.util.contentlet.pagination;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static reactor.core.publisher.Mono.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.contentet.pagination.PaginatedContentletBuilder;
import com.dotmarketing.util.contentet.pagination.PaginatedContentlets;
import java.util.Iterator;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class PaginatedContentletsIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link PaginatedContentlets}, really al the behavior as {@link Iterable}
     * When:
     * - Create 4 contentlets in the same Host.
     * - Set the ContentletsPaginated's perPage attribute to a really big amount
     * - Set the ContentletsPaginated's luceneQuery to "+conHost:[contentlet's host identifier]"
     * Should: get all the 4 Contentlets
     */
    @Test
    public void getContentletWithoutReallyPagination(){
        final Host host = new SiteDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final long currentTimeMillis = System.currentTimeMillis();
        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "A_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "B_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet3 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "C_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet4 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "D_" + currentTimeMillis)
                .nextPersisted();

        final List<Contentlet> expected = list(contentlet1, contentlet2, contentlet3, contentlet4);
        final PaginatedContentlets paginatedContentlets = new PaginatedContentletBuilder()
                .setLuceneQuery("+conHost:" + host.getIdentifier())
                .setUser(APILocator.systemUser())
                .setRespectFrontendRoles(false)
                .build();


        assertEquals("Should return all the Contentlets", expected.size(), paginatedContentlets.size());

        int i = 0;
        for (Contentlet contentlet : paginatedContentlets) {
            final Contentlet contentletExpected = expected.get(i++);

            assertEquals("The contentlets should be in order", contentletExpected.getIdentifier(), contentlet.getIdentifier());
            assertEquals("The contentlets should be in order", contentletExpected.getTitle(), contentlet.getTitle());
        }
    }


    /**
     * Method to test: {@link PaginatedContentlets}, really al the behavior as {@link Iterable}
     * When:
     * - Create 4 contentlets in the same Host.
     * - Set the ContentletsPaginated's perPage attribute to a really big amount
     * - Set the ContentletsPaginated's luceneQuery to "+conHost:[contentlet's host identifier]"
     * - remove each contentlet from the Iterator meanwhile go through it
     * Should: get all the 4 Contentlets
     */
    @Test
    public void remove(){
        final Host host = new SiteDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final long currentTimeMillis = System.currentTimeMillis();
        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "A_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "B_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet3 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "C_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet4 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "D_" + currentTimeMillis)
                .nextPersisted();

        final List<Contentlet> expected = list(contentlet1, contentlet2, contentlet3, contentlet4);
        final PaginatedContentlets paginatedContentlets = new PaginatedContentletBuilder()
                .setLuceneQuery("+conHost:" + host.getIdentifier())
                .setUser(APILocator.systemUser())
                .setRespectFrontendRoles(false)
                .build();


        assertEquals("Should return all the Contentlets", expected.size(), paginatedContentlets.size());

        int i = 0;
        final Iterator<Contentlet> iterator = paginatedContentlets.iterator();
        while(iterator.hasNext()){
            Contentlet contentlet = iterator.next();
            final Contentlet contentletExpected = expected.get(i++);

            assertEquals("The contentlets should be in order", contentletExpected.getIdentifier(), contentlet.getIdentifier());
            assertEquals("The contentlets should be in order", contentletExpected.getTitle(), contentlet.getTitle());
            iterator.remove();
        }
    }

    /**
     * Method to test: {@link PaginatedContentlets}, really al the behavior as {@link Iterable}
     * When:
     * - Create 4 contentlets in the same Host.
     * - Set the ContentletsPaginated's perPage attribute to 2
     * - Set the ContentletsPaginated's luceneQury to "+conHost:[contentlet's host identifier]"
     * Should: get all the 4 Contentlets
     */
    @Test
    public void getContentletWithReallyPagination(){
        final Host host = new SiteDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final long currentTimeMillis = System.currentTimeMillis();
        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "A_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "B_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet3 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "C_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet4 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "D_" + currentTimeMillis)
                .nextPersisted();

        final List<Contentlet> expected = list(contentlet1, contentlet2, contentlet3, contentlet4);
        final PaginatedContentlets paginatedContentlets = new PaginatedContentletBuilder()
                .setLuceneQuery("+conHost:" + host.getIdentifier())
                .setUser(APILocator.systemUser())
                .setRespectFrontendRoles(false)
                .setPerPage(2)
                .build();

        assertEquals("Should return all the Contentlets", expected.size(), paginatedContentlets.size());

        int i = 0;
        for (Contentlet contentlet : paginatedContentlets) {
            final Contentlet contentletExpected = expected.get(i++);

            assertEquals("The contentlets should be in order", contentletExpected.getIdentifier(), contentlet.getIdentifier());
            assertEquals("The contentlets should be in order", contentletExpected.getTitle(), contentlet.getTitle());
        }
    }

    /**
     * Method to test: {@link PaginatedContentlets}, really al the behavior as {@link Iterable}
     * When:
     * - Create 4 contentlets in the same Host.
     * - Set the ContentletsPaginated's perPage attribute to 2
     * - Set the ContentletsPaginated's luceneQury to "+conHost:[contentlet's host identifier]"
     * Should: get all the 4 Contentlets
     *
     * In this case we are using a Mock to be sure that really it did the pagination
     */
    @Test
    public void getContentletWithReallyPaginationAndMock()
            throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final long currentTimeMillis = System.currentTimeMillis();
        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "A_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "B_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet3 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "C_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet4 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "D_" + currentTimeMillis)
                .nextPersisted();

        final List<Contentlet> expected = list(contentlet1, contentlet2, contentlet3, contentlet4);

        final PaginatedArrayList<ContentletSearch> expectedFirstCall = new PaginatedArrayList<>();
        expectedFirstCall.add(createContentletSearch(contentlet1));
        expectedFirstCall.add(createContentletSearch(contentlet2));
        expectedFirstCall.setTotalResults(4);

        final PaginatedArrayList<ContentletSearch> expectedSecondCall = new PaginatedArrayList<>();
        expectedSecondCall.add(createContentletSearch(contentlet3));
        expectedSecondCall.add(createContentletSearch(contentlet4));
        expectedSecondCall.setTotalResults(4);

        final ContentletAPI contentletAPIMock = mock(ContentletAPI.class);
        final String luceneQuery = "+conHost:" + host.getIdentifier();

        // Mock indexCount to return total results (used to determine pagination strategy)
        when(contentletAPIMock.indexCount(luceneQuery, APILocator.systemUser(), false))
                .thenReturn(4L);

        when(contentletAPIMock.searchIndex(luceneQuery, 2, 0, "title asc",
                        APILocator.systemUser(), false))
                .thenReturn(expectedFirstCall);

        when(contentletAPIMock.searchIndex(luceneQuery, 2, 2, "title asc",
                APILocator.systemUser(), false))
                .thenReturn(expectedSecondCall);

        when(contentletAPIMock.find(contentlet1.getInode(), APILocator.systemUser(), false)).thenReturn(contentlet1);
        when(contentletAPIMock.find(contentlet2.getInode(), APILocator.systemUser(), false)).thenReturn(contentlet2);
        when(contentletAPIMock.find(contentlet3.getInode(), APILocator.systemUser(), false)).thenReturn(contentlet3);
        when(contentletAPIMock.find(contentlet4.getInode(), APILocator.systemUser(), false)).thenReturn(contentlet4);

        final PaginatedContentlets paginatedContentlets = new PaginatedContentletBuilder()
                .setLuceneQuery(luceneQuery)
                .setUser(APILocator.systemUser())
                .setRespectFrontendRoles(false)
                .setPerPage(2)
                .setContentletAPI(contentletAPIMock)
                .build();

        assertEquals("Should return all the Contentlets", expected.size(), paginatedContentlets.size());

        int i = 0;
        for (Contentlet contentlet : paginatedContentlets) {
            final Contentlet contentletExpected = expected.get(i++);

            assertEquals("The contentlets should be in order", contentletExpected.getIdentifier(), contentlet.getIdentifier());
            assertEquals("The contentlets should be in order", contentletExpected.getTitle(), contentlet.getTitle());
        }

        verify(contentletAPIMock).searchIndex(luceneQuery, 2, 0, "title asc",
                APILocator.systemUser(), false);

        verify(contentletAPIMock).searchIndex(luceneQuery, 2, 2, "title asc",
                APILocator.systemUser(), false);
    }

    private ContentletSearch createContentletSearch(Contentlet contentlet1) {
        final ContentletSearch contentletSearch_1 = new ContentletSearch();
        contentletSearch_1.setInode(contentlet1.getInode());
        return contentletSearch_1;
    }

    @Test
    public void getContentletWithNone(){
        final Host host = new SiteDataGen().nextPersisted();
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final long currentTimeMillis = System.currentTimeMillis();
        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "A_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "B_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet3 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "C_" + currentTimeMillis)
                .nextPersisted();

        final Contentlet contentlet4 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty("title", "D_" + currentTimeMillis)
                .nextPersisted();
        final PaginatedContentlets paginatedContentlets = new PaginatedContentletBuilder()
                .setLuceneQuery("+conHost:not_exists")
                .setUser(APILocator.systemUser())
                .setRespectFrontendRoles(false)
                .setPerPage(2)
                .build();

        assertEquals("Should not return any Contentlets", 0, paginatedContentlets.size());

    }

    /**
     * Method to test: {@link PaginatedContentlets} with Scroll API enabled
     * When:
     * - Create 3 contentlets in the same Host
     * - Temporarily set ES_SCROLL_API_THRESHOLD to 1 to force scroll API usage
     * - Create PaginatedContentlets with the query
     * Should:
     * - Use Scroll API (isUsingScrollApi() should return true)
     * - Return all 3 contentlets correctly
     * - Maintain proper order
     */
    @Test
    public void testScrollApiWithForcedThreshold() {
        // Store original threshold value to restore later
        final String originalThreshold = Config.getStringProperty("ES_SCROLL_API_THRESHOLD", "10000");

        try {
            // Create test data
            final Host host = new SiteDataGen().nextPersisted();
            final ContentType contentType = new ContentTypeDataGen().nextPersisted();

            final long currentTimeMillis = System.currentTimeMillis();
            final Contentlet contentlet1 = new ContentletDataGen(contentType)
                    .host(host)
                    .setProperty("title", "ScrollTest_A_" + currentTimeMillis)
                    .nextPersisted();

            final Contentlet contentlet2 = new ContentletDataGen(contentType)
                    .host(host)
                    .setProperty("title", "ScrollTest_B_" + currentTimeMillis)
                    .nextPersisted();

            final Contentlet contentlet3 = new ContentletDataGen(contentType)
                    .host(host)
                    .setProperty("title", "ScrollTest_C_" + currentTimeMillis)
                    .nextPersisted();

            final List<Contentlet> expected = list(contentlet1, contentlet2, contentlet3);

            // Temporarily lower threshold to force scroll API usage
            // Setting to 1 means any query with more than 1 result will use scroll
            Config.setProperty("ES_SCROLL_API_THRESHOLD", "1");

            // Create PaginatedContentlets with try-with-resources to ensure cleanup
            try (final PaginatedContentlets paginatedContentlets = new PaginatedContentletBuilder()
                    .setLuceneQuery("+conHost:" + host.getIdentifier())
                    .setUser(APILocator.systemUser())
                    .setRespectFrontendRoles(false)
                    .build()) {

                // Verify that Scroll API is being used
                assertTrue("Scroll API should be used when threshold is exceeded",
                        paginatedContentlets.isUsingScrollApi());

                // Verify total count
                assertEquals("Should return all 3 contentlets", expected.size(),
                        paginatedContentlets.size());

                // Verify all contentlets are returned in correct order
                int i = 0;
                for (Contentlet contentlet : paginatedContentlets) {
                    final Contentlet contentletExpected = expected.get(i++);

                    assertEquals("Contentlet identifiers should match in order",
                            contentletExpected.getIdentifier(), contentlet.getIdentifier());
                    assertEquals("Contentlet titles should match in order",
                            contentletExpected.getTitle(), contentlet.getTitle());
                }

                // Verify we iterated through all contentlets
                assertEquals("Should have iterated through all contentlets", expected.size(), i);
            }
            // try-with-resources ensures scroll context is cleaned up

        } finally {
            // Always restore original configuration
            Config.setProperty("ES_SCROLL_API_THRESHOLD", originalThreshold);
        }
    }

    /**
     * Method to test: {@link PaginatedContentlets} with Scroll API and pagination
     * When:
     * - Create 3 contentlets in the same Host
     * - Temporarily set ES_SCROLL_API_THRESHOLD to 1 to force scroll API usage
     * - Set perPage to 2 to force multiple batches
     * Should:
     * - Use Scroll API
     * - Return all 3 contentlets correctly across multiple batches
     * - Maintain proper order
     */
    @Test
    public void testScrollApiWithMultipleBatches() {
        final String originalThreshold = Config.getStringProperty("ES_SCROLL_API_THRESHOLD", "10000");

        try {
            // Create test data
            final Host host = new SiteDataGen().nextPersisted();
            final ContentType contentType = new ContentTypeDataGen().nextPersisted();

            final long currentTimeMillis = System.currentTimeMillis();
            final Contentlet contentlet1 = new ContentletDataGen(contentType)
                    .host(host)
                    .setProperty("title", "ScrollBatch_A_" + currentTimeMillis)
                    .nextPersisted();

            final Contentlet contentlet2 = new ContentletDataGen(contentType)
                    .host(host)
                    .setProperty("title", "ScrollBatch_B_" + currentTimeMillis)
                    .nextPersisted();

            final Contentlet contentlet3 = new ContentletDataGen(contentType)
                    .host(host)
                    .setProperty("title", "ScrollBatch_C_" + currentTimeMillis)
                    .nextPersisted();

            final List<Contentlet> expected = list(contentlet1, contentlet2, contentlet3);

            // Force scroll API usage with low threshold
            Config.setProperty("ES_SCROLL_API_THRESHOLD", "1");

            // Create PaginatedContentlets with small batch size to test pagination
            try (final PaginatedContentlets paginatedContentlets = new PaginatedContentletBuilder()
                    .setLuceneQuery("+conHost:" + host.getIdentifier())
                    .setUser(APILocator.systemUser())
                    .setRespectFrontendRoles(false)
                    .setPerPage(2) // Small batch size to force multiple scroll batches
                    .build()) {

                // Verify that Scroll API is being used
                assertTrue("Scroll API should be used when threshold is exceeded",
                        paginatedContentlets.isUsingScrollApi());

                // Verify total count
                assertEquals("Should return all 3 contentlets", expected.size(),
                        paginatedContentlets.size());

                // Verify all contentlets are returned correctly across batches
                int i = 0;
                for (Contentlet contentlet : paginatedContentlets) {
                    final Contentlet contentletExpected = expected.get(i++);

                    assertEquals("Contentlet identifiers should match in order (batch " + ((i-1)/2 + 1) + ")",
                            contentletExpected.getIdentifier(), contentlet.getIdentifier());
                    assertEquals("Contentlet titles should match in order (batch " + ((i-1)/2 + 1) + ")",
                            contentletExpected.getTitle(), contentlet.getTitle());
                }

                // Verify we iterated through all contentlets
                assertEquals("Should have iterated through all contentlets across batches",
                        expected.size(), i);
            }

        } finally {
            // Always restore original configuration
            Config.setProperty("ES_SCROLL_API_THRESHOLD", originalThreshold);
        }
    }
}
