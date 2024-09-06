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
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.contentet.pagination.PaginatedContentletBuilder;
import com.dotmarketing.util.contentet.pagination.PaginatedContentlets;
import java.util.Iterator;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
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
}
