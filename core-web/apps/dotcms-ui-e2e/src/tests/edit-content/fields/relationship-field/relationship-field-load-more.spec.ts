import { NewEditContentFormPage } from '@pages';

import { RelationshipField } from './helpers/relationship-field';

import { CARDINALITY, test } from '../../../../fixtures/relationship.fixture';

/**
 * FR-021 to FR-024 — the related-content list has **no paging control**.
 *
 * Its own file rather than a describe inside the table spec, for two reasons. It is named for what
 * it asserts: the list stops at 40 rows and reveals the rest through "Load more", which is what
 * replaced the paginator this change removed — a block called "Table Pagination" now describes the
 * opposite of what it tests.
 *
 * And it is the only relationship test that creates more than forty contentlets. Playwright shards
 * by file, so keeping it here stops that burst competing for the same backend as the search tests
 * it used to sit beside — which were timing out in their own setup while this one ran.
 */
test.describe('Load more (no paging control)', () => {
    /**
     * Longer than the 60s default. Proving the list stops at 40 means creating more than 40
     * contentlets and then opening a contentlet that relates all of them, which resolves every one
     * of them through `?depth=2`. That is real work even without the index wait, against a default
     * sized for tests that create three.
     */
    test.describe.configure({ timeout: 120_000 });

    let authorTypeVariable: string;
    let blogTypeVariable: string;

    test.beforeEach(async ({ apiHelpers, testSuffix }) => {
        const authorType = await apiHelpers.createContentType(
            apiHelpers.authorPayload(`TablePag_${testSuffix}`)
        );
        authorTypeVariable = authorType.variable;

        const blogType = await apiHelpers.createContentType(
            apiHelpers.blogPayload(
                `TablePag_${testSuffix}`,
                'E2E_Blog_TablePag',
                'E2EBlogTablePag',
                authorTypeVariable,
                'authors',
                CARDINALITY.MANY_TO_MANY
            )
        );
        blogTypeVariable = blogType.variable;
    });

    /**
     * FR-021 / FR-022 — paging the related list was **removed**, not resized.
     *
     * Drag-reorder is the reason. With the list paged, a row could only be dragged within its own
     * page, so moving an item from page 2 to position 1 was impossible: the operation the column
     * exists for did not work across the set it was ordering. The list now renders the first 40 and
     * reveals the next 40 on demand, which keeps every row in one drag surface.
     */
    test('reveals the first 40 related items with a Load more @smoke', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const authors = await apiHelpers.createContentlets(
            authorTypeVariable,
            // 41, not a round 45: one past the threshold proves both halves — 40 rendered and the
            // rest revealed — and every extra row is another publish on the runner.
            Array.from({ length: 41 }, (_, j) => {
                const i = j + 1;
                return {
                    title: `LoadMore Author ${String(i).padStart(2, '0')} ${testSuffix}`,
                    bio: `Bio ${i}`
                };
            }),
            // No index wait. These rows are only ever read back through the blog they are related
            // to, and the related-content list resolves from the parent through `?depth=2` — out of
            // the database, not the index. Waiting cost 41 rounds of indexing and bought nothing:
            // it is what pushed this test past 60s, then past 180s, and took its neighbours in the
            // shard down with it.
            false
        );

        const blog = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog LoadMore Test ${testSuffix}` },
            { authors: authors.map((a) => a.identifier).join(',') }
        );

        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blog.inode);

        const relationshipField = new RelationshipField(adminPage);

        await relationshipField.expectRowCount(40);
        await relationshipField.expectPaginationHidden();
        await relationshipField.expectLoadMoreVisible();

        await relationshipField.clickLoadMore();

        // The remainder, appended — not a second page replacing the first.
        await relationshipField.expectRowCount(41);
        await relationshipField.expectLoadMoreHidden();
    });

    test('shows every related item and no Load more below the threshold', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const authors = await apiHelpers.createContentlets(
            authorTypeVariable,
            Array.from({ length: 5 }, (_, j) => {
                const i = j + 1;
                return {
                    title: `NoPag Author ${i} ${testSuffix}`,
                    bio: `Bio ${i}`
                };
            })
        );

        const blog = await apiHelpers.createContentletWithRelationship(
            blogTypeVariable,
            { title: `Blog NoPag Test ${testSuffix}` },
            { authors: authors.map((a) => a.identifier).join(',') }
        );

        const formPage = new NewEditContentFormPage(adminPage);
        await formPage.goToContent(blog.inode);

        const relationshipField = new RelationshipField(adminPage);

        await relationshipField.expectRowCount(5);
        await relationshipField.expectPaginationHidden();
        await relationshipField.expectLoadMoreHidden();
    });
});
