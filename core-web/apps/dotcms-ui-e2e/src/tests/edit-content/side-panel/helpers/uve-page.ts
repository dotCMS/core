import { type APIRequestContext } from '@playwright/test';
import { createContainer, deleteContainer } from '@requests/containers';
import {
    actionsPageWorkflow,
    addContentToPage,
    createPage,
    type Page as PageContentlet
} from '@requests/pages';
import { createTemplate, deleteTemplate } from '@requests/templates';

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
    containerIdentifier: string;
}

/** What {@link createUvePage} builds the page with. */
export interface UvePageContent {
    /** The content types the page's container accepts and renders. */
    contentTypeIds: string[];
    /** What to place in the container, in order; empty leaves it empty. */
    contentletIdentifiers?: string[];
}

/**
 * Creates a page on a one-row, full-width template holding a single container of its own, which
 * accepts the given content types and renders each contentlet's title as a heading, and places the
 * given contentlets in it, in order.
 *
 * A container of its own, not `SYSTEM_CONTAINER`: the server caches the system container's
 * rendered code, with its list of types, from its first render, and a type created after that is
 * neither accepted at a drop nor rendered (its contentlets show as empty wrappers). The tests'
 * types are always created after it.
 *
 * Full width on purpose: the UVE's contentlet toolbar lays its buttons out in a row only on
 * contentlets at least 600px wide, which `UveEditorPage.openFullEditor()` relies on.
 *
 * @param request - Playwright APIRequestContext
 * @param suffix - Makes the container, template and page unique to the test
 * @param content - The types the container accepts and the contentlets to place in it
 */
export async function createUvePage(
    request: APIRequestContext,
    suffix: string,
    { contentTypeIds, contentletIdentifiers = [] }: UvePageContent
): Promise<UvePage> {
    const container = await createContainer(request, {
        title: `e2e side-panel container ${suffix}`,
        contentTypeIds
    });
    const containerIdentifier = container.identifier;

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
                                    { identifier: containerIdentifier, uuid: CONTAINER_UUID }
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
                identifier: containerIdentifier,
                uuid: CONTAINER_UUID,
                contentletsId: contentletIdentifiers
            }
        ]);
    }

    return { page, templateIdentifier: template.identifier, containerIdentifier };
}

/**
 * The page's path for `UveEditorPage.open()`, with exactly one leading slash: the create response's
 * `url` is not consistent about carrying one.
 */
export function uvePagePath(uvePage: UvePage): string {
    return `/${uvePage.page.url.replace(/^\/+/, '')}`;
}

/**
 * Removes a page made by {@link createUvePage}, its template and its container. Best effort:
 * teardown must not turn a failing test into a second, misleading error.
 */
export async function removeUvePage(request: APIRequestContext, uvePage: UvePage): Promise<void> {
    await actionsPageWorkflow(request, uvePage.page.inode, [
        'Unpublish',
        'Archive',
        'Destroy'
    ]).catch(() => undefined);
    await deleteTemplate(request, [uvePage.templateIdentifier]).catch(() => undefined);
    // After the template that uses it.
    await deleteContainer(request, uvePage.containerIdentifier);
}
