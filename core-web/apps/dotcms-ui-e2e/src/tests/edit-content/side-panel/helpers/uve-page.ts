import { type APIRequestContext } from '@playwright/test';
import {
    actionsPageWorkflow,
    addContentToPage,
    createPage,
    type Page as PageContentlet
} from '@requests/pages';
import { createTemplate, deleteTemplate } from '@requests/templates';

/** The container every content type can go in, rendering each contentlet's title as a heading. */
const SYSTEM_CONTAINER = 'SYSTEM_CONTAINER';

/** The one container instance on the template below. */
const CONTAINER_UUID = '1';

/**
 * A real folder every instance keeps, which `TemplateResource` requires a theme to be. See
 * `tests/edit-page/layout-concurrent-save.spec.ts` for why `SYSTEM_THEME` would not do.
 */
const THEME = 'SYSTEM_FOLDER';

/** A page the UVE can open, and what has to go when the test is done with it. */
export interface UvePage {
    page: PageContentlet;
    templateIdentifier: string;
}

/**
 * Creates a page on a one-row, full-width template holding a single `SYSTEM_CONTAINER`, and places
 * the given contentlets in it, in order.
 *
 * Full width on purpose: the UVE's contentlet toolbar lays its buttons out in a row only on
 * contentlets at least 600px wide, which `UveEditorPage.openFullEditor()` relies on.
 *
 * @param request - Playwright APIRequestContext
 * @param suffix - Makes the template title and page URL unique to the test
 * @param contentletIdentifiers - What to place in the container; empty leaves it empty
 */
export async function createUvePage(
    request: APIRequestContext,
    suffix: string,
    contentletIdentifiers: string[] = []
): Promise<UvePage> {
    const template = await createTemplate(request, {
        title: `anonymous_layout_${suffix}`,
        friendlyName: `e2e side-panel template ${suffix}`,
        drawed: true,
        body: 'placeholder',
        theme: THEME,
        image: '',
        layout: {
            width: '100%',
            title: `e2e side-panel template ${suffix}`,
            header: false,
            footer: false,
            body: {
                rows: [
                    {
                        columns: [
                            {
                                containers: [
                                    { identifier: SYSTEM_CONTAINER, uuid: CONTAINER_UUID }
                                ],
                                width: 12,
                                leftOffset: 1
                            }
                        ]
                    }
                ]
            },
            sidebar: { containers: [], location: '', width: '' }
        }
    });

    const title = `e2e-side-panel-${suffix}`;
    const page = await createPage(request, {
        title,
        url: title,
        friendlyName: title,
        template: template.identifier,
        contentType: 'htmlpageasset',
        cachettl: 0
    });

    if (contentletIdentifiers.length) {
        await addContentToPage(request, page.identifier, [
            {
                identifier: SYSTEM_CONTAINER,
                uuid: CONTAINER_UUID,
                contentletsId: contentletIdentifiers
            }
        ]);
    }

    return { page, templateIdentifier: template.identifier };
}

/**
 * The page's path for `UveEditorPage.open()`, with exactly one leading slash: the create response's
 * `url` is not consistent about carrying one.
 */
export function uvePagePath(uvePage: UvePage): string {
    return `/${uvePage.page.url.replace(/^\/+/, '')}`;
}

/**
 * Removes a page made by {@link createUvePage} and its template. Best effort: teardown must not
 * turn a failing test into a second, misleading error.
 */
export async function removeUvePage(request: APIRequestContext, uvePage: UvePage): Promise<void> {
    await actionsPageWorkflow(request, uvePage.page.inode, [
        'Unpublish',
        'Archive',
        'Destroy'
    ]).catch(() => undefined);
    await deleteTemplate(request, [uvePage.templateIdentifier]).catch(() => undefined);
}
