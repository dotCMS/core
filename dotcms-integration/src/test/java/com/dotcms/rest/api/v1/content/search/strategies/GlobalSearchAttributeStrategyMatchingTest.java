package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.content.ContentSearchForm;
import com.dotcms.rest.api.v1.content.search.LuceneQueryBuilder;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression test for issue #36791 — {@link GlobalSearchAttributeStrategy}'s mandatory
 * {@code catchall} PREFIX gate misses mid-token terms and even a content's own exact full name
 * (e.g. a FileAsset named {@code IMG_1004.jpeg} tokenizes to {@code img_1004}+{@code jpeg}, so
 * neither {@code 1004} nor the full name satisfy a catchall-only prefix gate).
 *
 * <p>The fix widens the mandatory gate to {@code +(catchall:kw* OR fieldName_dotraw:*kw*)} — the
 * {@code _dotraw} alternative is scoped to a single field (unlike {@code catchall}, which
 * aggregates every field of the document), so it recovers mid-token/exact-name matches without
 * reintroducing the unscoped, whole-document wildcard that issue #36688 deliberately removed.</p>
 *
 * <p>Runs the exact production mechanism: {@link LuceneQueryBuilder} (the class
 * {@code ContentResource.java} instantiates for the Content Search portlet) building the query
 * from a {@link ContentSearchForm}, executed via {@link com.dotmarketing.portlets.contentlet.business.ContentletAPI#search}.</p>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class GlobalSearchAttributeStrategyMatchingTest extends IntegrationTestBase {

    private static User systemUser;
    private static final String FILE_NAME = "IMG_1004.jpeg";
    private static String fileInode;

    // An unrelated content item whose BODY (not title) contains the searched substring — proves
    // the fix stays scoped to title/name and does not reintroduce whole-document body matching.
    private static ContentType bodyType;
    private static String unrelatedBodyInode;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.getUserAPI().getSystemUser();

        final String uniqueId = System.currentTimeMillis() + "";
        final Host site = new SiteDataGen().name("gsearch-" + uniqueId + ".local").nextPersisted();
        final Folder folder = new FolderDataGen().name("gsearchFolder_" + uniqueId).site(site).nextPersisted();

        final File tmpDir = Files.createTempDirectory("gsearch-" + uniqueId).toFile();
        final File imgFile = new File(tmpDir, FILE_NAME);
        Files.writeString(imgFile.toPath(), "global search strategy regression test content");
        final Contentlet fileAsset = new FileAssetDataGen(folder, imgFile)
                .languageId(1)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();
        fileInode = fileAsset.getInode();

        // A plain CONTENT item whose title has nothing to do with "1004", but whose body text
        // mentions it — this must NOT be returned by a "1004" search once the catchall-only gate
        // is widened; only the fieldName_dotraw (title-scoped) alternative should let docs in.
        bodyType = new ContentTypeDataGen()
                .name("GSearchBodyType_" + uniqueId)
                .velocityVarName("gSearchBodyType_" + uniqueId)
                .baseContentType(BaseContentType.CONTENT)
                .host(site)
                .nextPersisted();
        final Contentlet unrelatedBody = new ContentletDataGen(bodyType.id())
                .setProperty("title", "Unrelated report " + uniqueId)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();
        unrelatedBodyInode = unrelatedBody.getInode();

        Logger.info(GlobalSearchAttributeStrategyMatchingTest.class, String.format(
                "Seeded FileAsset '%s' (inode %s) and unrelated item (inode %s)",
                FILE_NAME, fileInode, unrelatedBodyInode));
    }

    /** Runs the real Content Search production mechanism for a given global-search term. */
    private List<Contentlet> search(final String term) throws DotDataException, DotSecurityException {
        final Map<String, Object> systemSearchableFields = new HashMap<>();
        systemSearchableFields.put("languageId", 1);
        final ContentSearchForm contentSearchForm = new ContentSearchForm.Builder()
                .globalSearch(term)
                .systemSearchableFields(systemSearchableFields)
                .perPage(100)
                .page(1)
                .build();
        final LuceneQueryBuilder luceneQueryBuilder = new LuceneQueryBuilder(contentSearchForm, systemUser);
        final String luceneQuery = luceneQueryBuilder.build();
        Logger.info(this.getClass(), String.format("term='%s' → query: %s", term, luceneQuery));
        return APILocator.getContentletAPI().search(luceneQuery, 100, 0, null, systemUser, false);
    }

    private static boolean containsInode(final List<Contentlet> contentlets, final String inode) {
        return contentlets.stream().anyMatch(c -> inode.equals(c.getInode()));
    }

    /** The reported gap: mid-token term "1004" (inside "img_1004") now finds the file. */
    @Test
    public void midTokenTerm_findsContent() throws DotDataException, DotSecurityException {
        assertTrue("Searching '1004' must return " + FILE_NAME, containsInode(search("1004"), fileInode));
    }

    /** The reported gap: the exact full filename, split across a tokenizer boundary, now matches. */
    @Test
    public void exactFullName_findsContent() throws DotDataException, DotSecurityException {
        assertTrue("Searching the exact filename must return it",
                containsInode(search(FILE_NAME), fileInode));
    }

    /** Boundary-spanning substring (crosses the '.' token split) now matches too. */
    @Test
    public void boundarySpanningTerm_findsContent() throws DotDataException, DotSecurityException {
        assertTrue("Searching '1004.jpeg' must return " + FILE_NAME,
                containsInode(search("1004.jpeg"), fileInode));
    }

    /** Prefix-style terms (already working before this fix) keep working. */
    @Test
    public void prefixTerms_stillFindContent() throws DotDataException, DotSecurityException {
        for (final String term : List.of("IMG", "img", "IMG_1004", "jpeg")) {
            assertTrue("Searching '" + term + "' must return " + FILE_NAME,
                    containsInode(search(term), fileInode));
        }
    }

    /**
     * Regression guard for #36688: a "1004" search must NOT return an unrelated item just because
     * some other content elsewhere contains "1004" in an unrelated way. The unrelated item seeded
     * here has no "1004" anywhere (title or body) — it is a negative control confirming the fix
     * doesn't turn "1004" into an unbounded match-everything query.
     */
    @Test
    public void unrelatedContent_isNotReturned() throws DotDataException, DotSecurityException {
        assertFalse("An unrelated item with no '1004' anywhere must not be returned",
                containsInode(search("1004"), unrelatedBodyInode));
    }
}
