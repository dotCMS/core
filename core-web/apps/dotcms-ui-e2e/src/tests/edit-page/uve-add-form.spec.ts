import { UveEditorPage } from '@pages';
import { expect, Response, test } from '@playwright/test';
import { deleteContentlets } from '@requests/contentlets';
import { ContentType, createContentType, deleteContentType } from '@requests/contentType';
import { createPage, Page as PageContentlet } from '@requests/pages';
import { createTemplate, deleteTemplate } from '@requests/templates';

/**
 * Adding a form to a page in the UVE.
 *
 * A form is placed through its widget. The editor asks for the widget with a GET; the first time a
 * form is placed it has none, the GET answers 404 and the editor asks for it to be created with a
 * POST. Placing the same form again finds the widget with the GET alone.
 */

const SYSTEM_CONTAINER = 'SYSTEM_CONTAINER';

// TemplateResource requires `theme` to be a real folder; SYSTEM_FOLDER always is (see
// layout-concurrent-save.spec.ts for the details).
const THEME = 'SYSTEM_FOLDER';

let templateIdentifier: string;
let pageContentlet: PageContentlet;
let form: ContentType;
let widgetIdentifiers: string[];

test.beforeEach(async ({ request }) => {
    const suffix = Date.now();
    widgetIdentifiers = [];

    const template = await createTemplate(request, {
        title: `e2e add-form template ${suffix}`,
        friendlyName: 'e2e add-form template',
        drawed: true,
        body: 'placeholder',
        theme: THEME,
        image: '',
        layout: {
            width: '100%',
            title: `e2e add-form template ${suffix}`,
            header: true,
            footer: true,
            body: {
                rows: [
                    {
                        columns: [
                            {
                                containers: [{ identifier: SYSTEM_CONTAINER, uuid: '1' }],
                                width: 6,
                                leftOffset: 1
                            },
                            {
                                containers: [{ identifier: SYSTEM_CONTAINER, uuid: '2' }],
                                width: 6,
                                leftOffset: 7
                            }
                        ]
                    }
                ]
            },
            sidebar: { containers: [], location: '', width: '' }
        }
    });
    templateIdentifier = template.identifier;

    const title = `e2e-add-form-${suffix}`;
    pageContentlet = await createPage(request, {
        title,
        url: title,
        friendlyName: title,
        template: templateIdentifier,
        contentType: 'htmlpageasset',
        cachettl: 0
    });

    form = await createContentType(request, {
        clazz: 'com.dotcms.contenttype.model.type.ImmutableFormContentType',
        name: `e2e add-form ${suffix}`,
        variable: `e2eAddForm${suffix}`,
        host: 'SYSTEM_HOST',
        folder: 'SYSTEM_FOLDER'
    });
});

test.afterEach(async ({ request }) => {
    // Best effort: teardown must not turn an already-failing test into a second error.
    await deleteContentlets(request, widgetIdentifiers).catch(() => undefined);
    await deleteContentlets(request, [pageContentlet.identifier]).catch(() => undefined);
    await deleteTemplate(request, [templateIdentifier]).catch(() => undefined);
    await deleteContentType(request, form.id).catch(() => undefined);
});

test('add a form to a page, then add it again @critical', async ({ page }) => {
    const uve = new UveEditorPage(page);
    const isFormRequest = (url: string) => url.includes(`/api/v1/containers/form/${form.id}`);
    const formRequests: string[] = [];
    page.on('request', (request) => {
        if (isFormRequest(request.url())) {
            formRequests.push(request.method());
        }
    });
    const formResponse = (method: string) =>
        page.waitForResponse(
            (response: Response) =>
                isFormRequest(response.url()) && response.request().method() === method
        );

    await uve.open(pageContentlet.url);

    // First time: the GET finds no widget and the editor creates it.
    const created = formResponse('POST');
    await uve.addForm(0, form.name);
    const createdResponse = await created;
    expect(createdResponse.status()).toBe(200);

    // The widget's identifier is read from the page, not from the response body. Chromium drops the
    // body of these responses within milliseconds, so `response.json()` fails with "No data found
    // for resource with given identifier" however early it is called.
    const placed = uve.container(0).locator('[data-dot-object="contentlet"]');
    await expect(placed).toHaveCount(1);
    const widgetIdentifier = await placed.getAttribute('data-dot-identifier');
    expect(widgetIdentifier).toBeTruthy();
    widgetIdentifiers.push(widgetIdentifier as string);

    await expect(uve.contentlet(widgetIdentifier as string)).toHaveCount(1);
    expect(formRequests).toEqual(['GET', 'POST']);

    // Again, in the other container: the GET finds the widget and nothing is created. Placing the
    // same identifier a second time is what shows the GET found it.
    const found = formResponse('GET');
    await uve.addForm(1, form.name);
    expect((await found).status()).toBe(200);

    await expect(uve.contentlet(widgetIdentifier as string)).toHaveCount(2);
    await expect(
        uve.container(1).locator(`[data-dot-identifier="${widgetIdentifier}"]`)
    ).toHaveCount(1);
    expect(formRequests).toEqual(['GET', 'POST', 'GET']);
});
