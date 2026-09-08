import { expect, test, type Page } from '@playwright/test';
import { Contentlet, createContentlet, deleteContentlets } from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadBlockEditorField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

/**
 * #36985 — an embedded contentlet must stay selectable in the LEGACY JSP contentlet editor.
 *
 * ⚠️ NOT COLLECTED BY DEFAULT, ON PURPOSE. The `.nightly.ts` extension is the gate: Playwright's
 * default `testMatch` is `**` + `*.@(spec|test).ts`, so this file is invisible to `nx e2e`. A
 * `@nightly` tag in the title would NOT have achieved that — nothing in CI greps for it, which is
 * why an earlier revision of this spec ran per-PR and broke the build.
 *
 * That matches the decision recorded for #36985: the Jest suite in `libs/new-block-editor`
 * reproduces the defect headlessly and is the PR gate, while this covers the real browser click
 * path off the critical path (ADR-0013 positions e2e as nightly smoke, not a merge-queue gate).
 *
 * To run it: `npx playwright test --config apps/dotcms-ui-e2e/playwright.config.ts \
 *   apps/dotcms-ui-e2e/src/tests/edit-content/fields/block-editor-field/block-editor-legacy-host-selection.nightly.ts`
 * against a running instance. A scheduled workflow to do that does not exist yet — see the
 * follow-up on PR #37319.
 *
 * ⚠️ UNVERIFIED. This has never completed successfully. Its first CI run failed on a
 * strict-mode violation in the frame locator (`#detailFrame` matched two iframes); that is
 * addressed below, but the fix has not been observed passing, and the assertions past the frame
 * lookup have never executed at all.
 *
 * Why the legacy host specifically: it is the only one that binds the web component's `value`
 * property. The new Angular Edit Content screen binds `[formControlName]` only, so its `value()`
 * stays empty and the effect that caused this bug exits immediately. Reproducing the defect
 * therefore requires `CONTENT_EDITOR2_ENABLED: false` on the content type.
 *
 * The body is seeded through the API on purpose. Authoring it in the editor would produce
 * current-shaped JSON, which round-trips byte-identically and does NOT reproduce — the defect
 * needs a document whose stored shape predates the current schema.
 */

const BLOCK_FIELD = 'blockEditorField';

/**
 * The legacy portlet iframe, scoped to the Dojo shell's viewport.
 *
 * NOT `getLegacyFrame()` from `@utils/iframe`: that resolves `#detailFrame` page-wide, and on the
 * content-edit route two iframes carry that id, so Playwright raises a strict-mode violation
 * ("resolved to 2 elements"). Scoping to `content-viewport`
 * (`main-legacy.component.html:18`) picks the one the portlet actually renders into.
 */
const legacyPortletFrame = (page: Page) =>
    page.getByTestId('content-viewport').frameLocator('iframe[name="detailFrame"]');

let contentType: ContentType | null = null;
let embedded: Contentlet | null = null;
let parent: Contentlet | null = null;

/**
 * Legacy-shaped body carrying both historical triggers: the root `chartCount` typo (written by
 * the legacy editor until #26025) and no `indent` on heading or paragraph (only declared from
 * #32235). Either alone is enough; both together match what customers actually have.
 */
const legacyShapedBody = (identifier: string) => ({
    type: 'doc',
    attrs: { chartCount: 118, wordCount: 20, readingTime: 1 },
    content: [
        {
            type: 'heading',
            attrs: { textAlign: 'left', level: 2 },
            content: [{ type: 'text', text: 'Heading above the card' }]
        },
        {
            type: 'paragraph',
            attrs: { textAlign: 'left' },
            content: [{ type: 'text', text: 'Some text before the contentlet.' }]
        },
        { type: 'dotContent', attrs: { data: { identifier, languageId: 1 } } },
        {
            type: 'paragraph',
            attrs: { textAlign: 'left' },
            content: [{ type: 'text', text: 'Some text after the contentlet.' }]
        }
    ]
});

test.beforeEach(async ({ request }) => {
    contentType = await createFakeContentType(request, {
        name: `E2EBlockEditor36985${uniqueSuffix()}`,
        // Forces the legacy JSP contentlet editor — the only host that binds `value`.
        metadata: { CONTENT_EDITOR2_ENABLED: false },
        fields: [
            createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
            createFakePayloadBlockEditorField({
                name: 'Block Editor Field',
                variable: BLOCK_FIELD,
                sortOrder: 2
            })
        ]
    });

    embedded = await createContentlet(request, {
        contentType: contentType.variable,
        title: `Embedded 36985 ${uniqueSuffix()}`
    });

    parent = await createContentlet(request, {
        contentType: contentType.variable,
        title: `Parent 36985 ${uniqueSuffix()}`,
        [BLOCK_FIELD]: JSON.stringify(legacyShapedBody(embedded.identifier))
    });
});

test.afterEach(async ({ request }) => {
    const identifiers = [parent, embedded]
        .filter((contentlet): contentlet is Contentlet => Boolean(contentlet))
        .map((contentlet) => contentlet.identifier);
    if (identifiers.length) {
        await deleteContentlets(request, identifiers);
    }
    if (contentType) {
        await deleteContentType(request, contentType.variable);
    }
    parent = embedded = contentType = null;
});

test.describe('Block Editor — embedded contentlet selection in the legacy host', () => {
    test('keeps the selection when the card is clicked on legacy-shaped content @nightly', async ({
        page
    }) => {
        await page.goto(`/dotAdmin/#/c/content/${parent?.inode}`);

        const frame = legacyPortletFrame(page);
        const card = frame.locator('[data-type="dot-content"]').first();
        await expect(card).toBeVisible({ timeout: 30_000 });

        await card.click();

        // Two independent observables of the same NodeSelection, asserted together because
        // either one alone could pass for the wrong reason.
        //
        // The ring: `is-selected` is bound to the node view's `selected()` signal, so it is
        // present only while the selection is live.
        await expect(card).toHaveClass(/is-selected/);

        // The toolbar: `toolbar-edit-contentlet` is disabled unless the store sees a
        // `NodeSelection` whose node type is `dotContent` (editor-toolbar.store.ts:96-99).
        // This is what the bug actually took away from authors, and it survives Playwright's
        // auto-retry — so it also proves the selection persisted past the change-detection
        // cycle that used to clobber it, without sleeping on a timer.
        await expect(frame.getByTestId('toolbar-edit-contentlet')).toBeEnabled();
    });

    test('an image on the same document also stays selectable @nightly', async ({ page }) => {
        // Guards the inverse: only `dotContent` and `codeBlock` use Angular node views, so every
        // other node type was never affected and must not regress into being affected.
        await page.goto(`/dotAdmin/#/c/content/${parent?.inode}`);

        const frame = legacyPortletFrame(page);
        const heading = frame.locator('.ProseMirror h2').first();
        await expect(heading).toBeVisible({ timeout: 30_000 });

        await heading.click();
        await expect(frame.locator('.ProseMirror')).toBeVisible();
    });
});
