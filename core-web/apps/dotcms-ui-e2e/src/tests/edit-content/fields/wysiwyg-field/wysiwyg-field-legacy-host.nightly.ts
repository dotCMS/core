import { expect, test, type Page } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadTextField,
    createFakePayloadWYSIWYGField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

/**
 * dotCMS/private-issues#722 (TinyMCE consolidation) — the legacy JSP WYSIWYG editor has no
 * feasible Jest/Spectator harness (see plan.md's Test Strategy exception; manual verification is
 * this story's real PR-time gate). This is an **attempt** at automated browser coverage anyway,
 * not a replacement for that gate.
 *
 * ⚠️ NOT COLLECTED BY DEFAULT, ON PURPOSE. The `.nightly.ts` extension is the gate: Playwright's
 * default `testMatch` is `**` + `*.@(spec|test).ts`, so this file is invisible to `nx e2e` — per
 * ADR-0013, legacy-host e2e ships as nightly smoke, not a merge-queue gate. Modeled directly on
 * `block-editor-field/block-editor-legacy-host-selection.nightly.ts` (#36985), including its
 * `CONTENT_EDITOR2_ENABLED: false` + scoped-frame-locator pattern — but note that precedent file
 * is itself marked "UNVERIFIED, never completed successfully". This one has not been run against
 * a live instance from this environment either. Treat a green run here as a good sign, not proof.
 *
 * To run it: `npx playwright test --config apps/dotcms-ui-e2e/playwright.config.ts \
 *   apps/dotcms-ui-e2e/src/tests/edit-content/fields/wysiwyg-field/wysiwyg-field-legacy-host.nightly.ts`
 * against a running instance.
 */

const WYSIWYG_FIELD = 'wysiwygField';

/**
 * The legacy portlet iframe, scoped to the Dojo shell's viewport — see the precedent file's own
 * comment on why this is not `getLegacyFrame()` from `@utils/iframe` (that resolves `#detailFrame`
 * page-wide, and the content-edit route has two iframes carrying that id).
 */
const legacyPortletFrame = (page: Page) =>
    page.getByTestId('content-viewport').frameLocator('iframe[name="detailFrame"]');

let contentType: ContentType | null = null;

test.beforeEach(async ({ request }) => {
    contentType = await createFakeContentType(request, {
        name: `E2ELegacyWysiwyg${uniqueSuffix()}`,
        // Forces the legacy JSP contentlet editor — the only host this feature's TinyMCE
        // consolidation touches that has no automated test harness of its own.
        metadata: { CONTENT_EDITOR2_ENABLED: false },
        fields: [
            createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
            createFakePayloadWYSIWYGField({
                name: 'WYSIWYG Field',
                variable: WYSIWYG_FIELD,
                sortOrder: 2
            })
        ]
    });
});

test.afterEach(async ({ request }) => {
    if (contentType) {
        await deleteContentType(request, contentType.variable);
        contentType = null;
    }
});

test.describe('WYSIWYG field — legacy JSP editor host', () => {
    test('the 8.x editor loads, accepts typed text, and the text survives a save @nightly', async ({
        page
    }) => {
        await page.goto(`/dotAdmin/#/c/content/new/${contentType?.variable}`);

        const frame = legacyPortletFrame(page);

        // The legacy editor renders a raw `<textarea>` per field until `enableWYSIWYG()` swaps
        // in the real TinyMCE instance — `.mce-edit-area iframe` is TinyMCE's own wrapper once it
        // has actually booted, the tightest available signal here (there are no `data-testid`s in
        // the JSP/Dojo-rendered markup, unlike the modern editor). `frameLocator` (not
        // `.contentFrame()` on a `Locator`, unavailable in the pinned `@playwright/test@1.36.0`)
        // descends into the iframe's own document.
        const editorIframe = frame.frameLocator('.mce-edit-area iframe');
        await expect(frame.locator('.mce-edit-area iframe')).toBeVisible({ timeout: 30000 });

        const editorBody = editorIframe.locator('body');
        await editorBody.click();
        await editorBody.type('legacy host smoke test');
        await expect(editorBody).toContainText('legacy host smoke test');

        const titleInput = frame.getByRole('textbox', { name: /title/i });
        await titleInput.fill(`E2E Legacy WYSIWYG ${uniqueSuffix()}`);

        const saveButton = frame.getByRole('button', { name: /save/i }).first();
        await saveButton.click();

        await expect(frame.getByText(/wysiwyg field/i)).toBeVisible({ timeout: 15000 });
        const reloadedBody = frame.frameLocator('.mce-edit-area iframe').locator('body');
        await expect(reloadedBody).toContainText('legacy host smoke test', { timeout: 15000 });
    });
});
