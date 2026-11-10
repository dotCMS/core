import { LegacyEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { Contentlet, createContentlet, deleteContentlets } from '@requests/contentlets';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { createFieldVariable } from '@requests/field-variables';
import {
    createFakePayloadRelationshipField,
    createFakePayloadTextField,
    createFakePayloadWYSIWYGField
} from '@utils/dot-content-types.mock';
import { uniqueSuffix } from '@utils/utils';

/** Markup-looking text typed into a plain text field: it has to be shown exactly as typed. */
const PLAIN_VALUE = '<admin> &copy; Smith';

/**
 * A WYSIWYG value: the table has to show its words, not its tags, and words in neighbouring or
 * nested blocks (`</p><div>`, `big<div>`) stay separate words instead of running together.
 */
const WYSIWYG_VALUE = '<p>Hello</p><div>big<div><b>world</b> &amp; co</div></div>';

const RELATIONSHIP_VARIABLE = 'related';

test.describe('Legacy editor — relationship field table', () => {
    let relatedType: ContentType | null = null;
    let parentType: ContentType | null = null;
    let relatedContentlet: Contentlet | null = null;
    let parentContentlet: Contentlet | null = null;

    test.beforeEach(async ({ request }) => {
        const suffix = uniqueSuffix();

        // The related type: a plain text field and a WYSIWYG one, both listed so the legacy
        // listing endpoint returns their values.
        relatedType = await createFakeContentType(request, {
            name: `E2E Legacy Related ${suffix}`,
            fields: [
                createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
                createFakePayloadTextField({
                    name: 'Label',
                    variable: 'label',
                    sortOrder: 2,
                    listed: true,
                    indexed: true
                }),
                createFakePayloadWYSIWYGField({
                    name: 'Body',
                    variable: 'body',
                    sortOrder: 3,
                    listed: true,
                    indexed: true
                })
            ]
        });

        // The parent type, in the legacy editor (false -> the Dojo/JSP form).
        parentType = await createFakeContentType(request, {
            name: `E2E Legacy Parent ${suffix}`,
            metadata: { CONTENT_EDITOR2_ENABLED: false },
            fields: [
                createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
                createFakePayloadRelationshipField({
                    name: 'Related',
                    variable: RELATIONSHIP_VARIABLE,
                    sortOrder: 2,
                    // many-to-many
                    relationships: { velocityVar: relatedType.variable, cardinality: 1 }
                })
            ]
        });

        const relationshipField = parentType.fields.find(
            (field) => field.variable === RELATIONSHIP_VARIABLE
        );
        expect(relationshipField?.id, 'relationship field id').toBeTruthy();

        // Showing the related fields as columns is what routes the table through the code
        // that writes each value into a cell.
        await createFieldVariable(
            request,
            parentType.id,
            relationshipField?.id as string,
            'showFields',
            'label,body'
        );

        relatedContentlet = await createContentlet(request, {
            contentType: relatedType.variable,
            title: `Related ${suffix}`,
            label: PLAIN_VALUE,
            body: WYSIWYG_VALUE
        });

        parentContentlet = await createContentlet(request, {
            contentType: parentType.variable,
            title: `Parent ${suffix}`,
            [RELATIONSHIP_VARIABLE]: relatedContentlet.identifier
        });
    });

    test.afterEach(async ({ request }) => {
        const identifiers = [parentContentlet, relatedContentlet]
            .filter((contentlet): contentlet is Contentlet => contentlet !== null)
            .map((contentlet) => contentlet.identifier);

        if (identifiers.length) {
            await deleteContentlets(request, identifiers);
        }

        parentContentlet = null;
        relatedContentlet = null;

        if (parentType) {
            await deleteContentType(request, parentType.id);
            parentType = null;
        }

        if (relatedType) {
            await deleteContentType(request, relatedType.id);
            relatedType = null;
        }
    });

    /**
     * Each related value is written into the table with `innerHTML`, so it is encoded first. Only
     * a WYSIWYG value is reduced to its text beforehand; a plain value has to keep the characters
     * that were typed, or an editor reading `<admin>` sees a blank cell.
     */
    test('shows plain values as typed and reduces a WYSIWYG value to its text @critical', async ({
        page
    }) => {
        const formPage = new LegacyEditContentFormPage(page);
        await formPage.goToLegacyEdit(parentType?.variable as string);

        const legacyFrame = await formPage.getLegacyContentFrame();
        const rows = legacyFrame.locator('tbody[id^="rel_"] tr[class^="dataRow"]');
        await expect(rows).toHaveCount(1, { timeout: 20000 });

        const row = rows.first();

        // Plain text field: the literal characters, nothing parsed.
        await expect(row).toContainText(PLAIN_VALUE);

        // WYSIWYG field: the words kept apart, without the tags and with the entity decoded once.
        await expect(row).toContainText('Hello big world & co');
        await expect(row).not.toContainText('<p>');
        await expect(row).not.toContainText('<b>');
    });
});
