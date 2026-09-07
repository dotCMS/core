import { ContentTypeBuilderPage } from '@pages';
import { test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';

/**
 * Content-type builder — add / edit / save fields.
 *
 * Drives the real builder UI (drag-drop add, click-to-edit) and verifies persistence by
 * reopening the content type. Guards the `DotFieldService` migration
 * (`saveFields` / `updateField`) and the "field-editor disables saving" regression.
 *
 * The content type shell is seeded via API; only the field operations go through the UI.
 */
test.describe('content type builder — fields', () => {
    let contentType: ContentType | null = null;
    let contentTypeId = '';

    test.beforeEach(async ({ request }) => {
        const suffix = crypto.randomUUID().slice(0, 8);
        contentType = await createFakeContentType(request, {
            name: `E2EBuilderFields${suffix}`
        });
        contentTypeId = contentType.id;
    });

    test.afterEach(async ({ request }) => {
        if (contentType) {
            await deleteContentType(request, contentType.id);
            contentType = null;
        }
    });

    test('add text and site/folder fields persist after reopening @critical', async ({ page }) => {
        const suffix = Date.now();
        const textName = `E2E Text ${suffix}`;
        const siteName = `E2E Site ${suffix}`;

        const builder = new ContentTypeBuilderPage(page);
        await builder.goToBuilder(contentTypeId);

        await builder.addField({ title: textName, fieldType: 'text' });
        await builder.addField({ title: siteName, fieldType: 'siteOrFolder' });

        // Reopen the builder — fields must come back from the server, not local state.
        await builder.goToBuilder(contentTypeId);

        await builder.expectFieldPresent(textName);
        await builder.expectFieldPresent(siteName);
    });

    test('edit a field name persists after reopening @critical', async ({ page }) => {
        const suffix = Date.now();
        const originalName = `E2E Original ${suffix}`;
        const renamedName = `E2E Renamed ${suffix}`;

        const builder = new ContentTypeBuilderPage(page);
        await builder.goToBuilder(contentTypeId);

        await builder.addField({ title: originalName, fieldType: 'text' });
        await builder.editField(originalName, renamedName);

        // Reopen the builder and confirm the rename survived the round-trip.
        await builder.goToBuilder(contentTypeId);

        await builder.expectFieldPresent(renamedName);
        await builder.expectFieldAbsent(originalName);
    });
});
