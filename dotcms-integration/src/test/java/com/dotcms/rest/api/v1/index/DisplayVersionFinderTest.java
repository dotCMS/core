package com.dotcms.rest.api.v1.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.UUIDGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the lookup that gives each failed reindex record its display data —
 * inode, language, title and content type — for {@code GET /api/v1/index/failed} (review of
 * #37924).
 *
 * <h2>What is at stake</h2>
 * <p>The failed records are often the oversized contentlets #37269 withholds. Loading each one
 * as a full {@link Contentlet} to read three values put tens of megabytes per record on the heap
 * and left them in the contentlet cache. The lookup reads only the needed columns, never the
 * contentlet's JSON, for all identifiers in a few batched queries, and keeps the version rule of
 * {@code findContentletByIdentifierAnyLanguage}: a non-archived working version of any language,
 * default variant.</p>
 */
public class DisplayVersionFinderTest {

    private static ContentType contentType;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        contentType = new ContentTypeDataGen()
                .field(new FieldDataGen().type(TextField.class).velocityVarName("title").next())
                .nextPersisted();
    }

    /**
     * Given Scenario: A contentlet with a title.
     * Expected Result: The lookup returns its working inode, language, title and content type
     * variable.
     */
    @Test
    public void test_find_returnsWorkingVersionDisplayData() throws Exception {
        final Contentlet contentlet = newContentlet("Display title");

        final Map<String, DisplayVersion> found = new DisplayVersionFinder()
                .find(List.of(contentlet.getIdentifier()));

        final DisplayVersion display = found.get(contentlet.getIdentifier());
        assertEquals(contentlet.getInode(), display.inode());
        assertEquals(contentlet.getLanguageId(), display.languageId());
        assertEquals("Display title", display.title());
        assertEquals(contentType.variable(), display.contentTypeVariable());
    }

    /**
     * Given Scenario: A contentlet that is not in the contentlet cache.
     * Expected Result: After the lookup it is still not in the cache — the contentlet itself is
     * never loaded.
     */
    @Test
    public void test_find_doesNotLoadContentletIntoCache() throws Exception {
        final Contentlet contentlet = newContentlet("Not cached");
        CacheLocator.getContentletCache().remove(contentlet.getInode());

        new DisplayVersionFinder().find(List.of(contentlet.getIdentifier()));

        assertNull("the lookup must not load the contentlet",
                CacheLocator.getContentletCache().get(contentlet.getInode()));
    }

    /**
     * Given Scenario: An archived contentlet and an identifier that does not exist.
     * Expected Result: Neither is returned, as {@code findContentletByIdentifierAnyLanguage}
     * returns nothing for them today.
     */
    @Test
    public void test_find_archivedOrMissing_notReturned() throws Exception {
        final Contentlet archived = newContentlet("Archived");
        ContentletDataGen.archive(archived);
        final String missing = UUIDGenerator.generateUuid();

        final Map<String, DisplayVersion> found = new DisplayVersionFinder()
                .find(List.of(archived.getIdentifier(), missing));

        assertFalse("an archived contentlet is not resolved",
                found.containsKey(archived.getIdentifier()));
        assertFalse("a missing identifier is not resolved", found.containsKey(missing));
    }

    /**
     * Given Scenario: A contentlet whose stored {@code title} column is empty.
     * Expected Result: Its title is {@code null}; the lookup does not fall back to loading the
     * contentlet to compute one.
     */
    @Test
    public void test_find_emptyTitleColumn_titleIsNull() throws Exception {
        final Contentlet contentlet = newContentlet("To be blanked");
        new DotConnect().setSQL("update contentlet set title = '' where inode = ?")
                .addParam(contentlet.getInode()).loadResult();

        final DisplayVersion display = new DisplayVersionFinder()
                .find(List.of(contentlet.getIdentifier())).get(contentlet.getIdentifier());

        assertEquals(contentlet.getInode(), display.inode());
        assertNull("an empty title column is reported as null", display.title());
    }

    /**
     * Given Scenario: More identifiers than one query batch holds (batch size 2, five
     * contentlets).
     * Expected Result: Every identifier is resolved.
     */
    @Test
    public void test_find_moreIdentifiersThanOneBatch_resolvesAll() throws Exception {
        final List<Contentlet> contentlets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            contentlets.add(newContentlet("Batch " + i));
        }
        final List<String> identifiers = contentlets.stream().map(Contentlet::getIdentifier)
                .collect(Collectors.toList());

        final Map<String, DisplayVersion> found = new DisplayVersionFinder(2).find(identifiers);

        assertEquals(5, found.size());
        for (final Contentlet contentlet : contentlets) {
            assertEquals(contentlet.getInode(), found.get(contentlet.getIdentifier()).inode());
        }
    }

    /**
     * Given Scenario: No identifiers.
     * Expected Result: An empty result, without querying.
     */
    @Test
    public void test_find_noIdentifiers_empty() throws Exception {
        assertTrue(new DisplayVersionFinder().find(List.of()).isEmpty());
    }

    private static Contentlet newContentlet(final String title) {
        return new ContentletDataGen(contentType)
                .setProperty("title", title)
                .setPolicy(IndexPolicy.DEFER)
                .nextPersisted();
    }
}
