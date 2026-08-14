import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { formatDate } from '@angular/common';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPageBrowserFolder,
    DotPageBrowserPage,
    DotPageBrowserState,
    DotPagesBrowserService
} from '@dotcms/data-access';
import {
    DOT_FOLDER_TREE_PAGE_SIZE,
    DotExperiment,
    DotExperimentStatus
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsSelectPageDialogComponent } from './dot-experiments-select-page-dialog.component';

import { SEARCH_DEBOUNCE_MS } from '../../../shared/constants';

const SITE = { identifier: 'site-1', hostname: 'demo.dotcms.com' };

const ROW_DISABLED_TOOLTIP_KEY = 'experiments.configure.select-page.row.disabled.tooltip';

/** Fixed epoch so the Modified column can be asserted against a formatted value. */
const MOD_DATE_EPOCH = Date.UTC(2024, 4, 17, 12, 0, 0);

const folderOf = (name: string, hasChildren = false): DotPageBrowserFolder => ({
    id: `folder-${name}`,
    inode: `inode-${name}`,
    name,
    path: `/${name}/`,
    hostname: SITE.hostname,
    hasChildren
});

const FOLDERS = [folderOf('about-us', true), folderOf('blog')];

const pageOf = (
    page: Partial<DotPageBrowserPage> & { identifier: string }
): DotPageBrowserPage => ({
    inode: `inode-${page.identifier}`,
    title: 'Untitled',
    url: '/index',
    path: '/index',
    hostname: SITE.hostname,
    hostId: SITE.identifier,
    templateId: 'template-1',
    modDate: String(MOD_DATE_EPOCH),
    languageId: 1,
    state: DotPageBrowserState.PUBLISHED,
    ...page
});

const HOME_PAGE = pageOf({
    identifier: 'page-1',
    title: 'Home',
    url: '/index',
    path: '/index',
    templateId: 'e4b4f0c2-1111-2222-3333-444455556666'
});

const BLOG_PAGE = pageOf({
    identifier: 'page-2',
    title: 'Blog Index',
    url: '/index',
    path: '/blog/index',
    templateId: 'tpl-2',
    state: DotPageBrowserState.CHANGED
});

const CONTACT_PAGE = pageOf({
    identifier: 'page-3',
    title: 'Contact',
    url: '/contact',
    path: '/contact',
    templateId: '',
    state: DotPageBrowserState.DRAFT
});

const PAGES = [HOME_PAGE, BLOG_PAGE, CONTACT_PAGE];

const experimentOn = (pageId: string, status: DotExperimentStatus): DotExperiment => ({
    ...getExperimentMock(0),
    id: `experiment-${pageId}`,
    pageId,
    status
});

/**
 * The Home page hosts a running experiment, so it cannot be picked. The Contact page hosts an
 * archived one, which does not block it — that is the distinction the excluded set encodes.
 */
const EXPERIMENTS = [
    experimentOn(HOME_PAGE.identifier, DotExperimentStatus.RUNNING),
    experimentOn(CONTACT_PAGE.identifier, DotExperimentStatus.ARCHIVED)
];

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.select-page.state.published': 'Published',
    'experiments.configure.select-page.state.changed': 'Changed',
    'experiments.configure.select-page.state.draft': 'Draft',
    'experiments.configure.select-page.template.unknown': 'No template',
    [ROW_DISABLED_TOOLTIP_KEY]: 'This page already has an experiment',
    'experiments.configure.select-page.confirm': 'Select Page',
    'dot.common.cancel': 'Cancel'
});

describe('DotExperimentsSelectPageDialogComponent', () => {
    let spectator: Spectator<DotExperimentsSelectPageDialogComponent>;
    let pagesBrowserMock: {
        getFolderChildren: jest.Mock;
        searchPages: jest.Mock;
    };
    let experimentsServiceMock: { getAllUnfiltered: jest.Mock };
    let dialogRef: DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotExperimentsSelectPageDialogComponent,
        // Both services are declared in the component's own `providers`, so they can only be
        // replaced through `componentProviders`.
        componentProviders: [
            { provide: DotPagesBrowserService, useFactory: () => pagesBrowserMock },
            { provide: DotExperimentsService, useFactory: () => experimentsServiceMock }
        ],
        providers: [
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            { provide: DynamicDialogConfig, useValue: { data: {} } },
            { provide: GlobalStore, useValue: { siteDetails: () => SITE } },
            { provide: DotMessageService, useValue: messageServiceMock },
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() })
        ],
        detectChanges: false
    });

    const render = () => {
        spectator = createComponent();
        dialogRef = spectator.inject(DynamicDialogRef, true);
        spectator.detectChanges();
    };

    const rows = (): HTMLElement[] => spectator.queryAll<HTMLElement>(byTestId('select-page-row'));

    const rowFor = (title: string): HTMLElement =>
        rows().find((row) => row.textContent?.includes(title)) as HTMLElement;

    const treeLabels = (): string[] =>
        spectator
            .queryAll(byTestId('tree-node-label'))
            .map((label) => label.textContent?.trim() ?? '');

    const buttonIn = (testId: string): HTMLButtonElement =>
        spectator.query(byTestId(testId))?.querySelector('button') as HTMLButtonElement;

    const clickRow = (title: string) => {
        spectator.click(rowFor(title));
        spectator.detectChanges();
    };

    /**
     * Picks a folder in the tree. PrimeNG emits `onNodeSelect` from a `setTimeout`, so the
     * selection only reaches the dialog once the macrotask has run.
     */
    const clickFolder = async (name: string) => {
        spectator.click(
            spectator
                .queryAll(byTestId('tree-node-label'))
                .find((label) => label.textContent?.trim() === name) as HTMLElement
        );
        await spectator.fixture.whenStable();
        spectator.detectChanges();
    };

    const searchInput = (): HTMLInputElement =>
        spectator.query(byTestId('select-page-search-input')) as HTMLInputElement;

    beforeEach(() => {
        pagesBrowserMock = {
            getFolderChildren: jest.fn().mockReturnValue(
                of({
                    folders: FOLDERS,
                    totalFolders: FOLDERS.length,
                    page: 1,
                    perPage: DOT_FOLDER_TREE_PAGE_SIZE
                })
            ),
            searchPages: jest.fn().mockReturnValue(of(PAGES))
        };
        experimentsServiceMock = { getAllUnfiltered: jest.fn().mockReturnValue(of(EXPERIMENTS)) };
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('folder tree', () => {
        it('should list the site root children on open', () => {
            render();

            expect(pagesBrowserMock.getFolderChildren).toHaveBeenCalledWith({
                siteId: SITE.identifier,
                hostname: SITE.hostname,
                path: '/',
                page: 1,
                perPage: DOT_FOLDER_TREE_PAGE_SIZE
            });
        });

        it('should render the site as the root node with a node per child folder', () => {
            render();

            expect(treeLabels()).toEqual([SITE.hostname, 'about-us', 'blog']);
        });

        it('should list the pages of the site root before any folder is picked', () => {
            render();

            expect(pagesBrowserMock.searchPages).toHaveBeenCalledWith({
                hostname: SITE.hostname,
                path: '/'
            });
        });

        it('should list the pages of the folder that was clicked', async () => {
            render();
            pagesBrowserMock.searchPages.mockClear();

            await clickFolder('about-us');

            expect(pagesBrowserMock.searchPages).toHaveBeenCalledWith({
                hostname: SITE.hostname,
                path: FOLDERS[0].path
            });
        });

        it('should read the site off the dialog data when the caller supplied one', () => {
            // A caller that already resolved another site must not be sent back to the current one.
            const otherSite = { hostId: 'site-2', hostname: 'other.dotcms.com' };
            spectator = createComponent({
                providers: [{ provide: DynamicDialogConfig, useValue: { data: otherSite } }]
            });
            spectator.detectChanges();

            expect(pagesBrowserMock.getFolderChildren).toHaveBeenCalledWith(
                expect.objectContaining({ siteId: otherSite.hostId, hostname: otherSite.hostname })
            );
        });

        it('should hand a failed folder listing to the shared error manager', () => {
            const error = new Error('boom');
            pagesBrowserMock.getFolderChildren.mockReturnValue(throwError(() => error));

            render();

            expect(spectator.inject(DotHttpErrorManagerService, true).handle).toHaveBeenCalledWith(
                error
            );
        });
    });

    describe('breadcrumb', () => {
        const breadcrumb = () =>
            spectator.query(byTestId('select-page-breadcrumb'))?.textContent?.trim();

        it('should show the bare hostname at the site root', () => {
            render();

            expect(breadcrumb()).toBe(`${SITE.hostname} /`);
        });

        it('should append the folder path once a folder is picked', async () => {
            render();

            await clickFolder('about-us');

            expect(breadcrumb()).toBe(`${SITE.hostname} / about-us`);
        });
    });

    describe('page rows', () => {
        it('should render one row per page of the listed folder', () => {
            render();

            expect(rows()).toHaveLength(PAGES.length);
        });

        it('should render the page title and its path', () => {
            render();

            const row = rowFor(BLOG_PAGE.title);

            expect(row.textContent).toContain(BLOG_PAGE.title);
            expect(row.textContent).toContain(BLOG_PAGE.path);
        });

        it('should shorten a long template identifier and keep the full one as its title', () => {
            render();

            const templateCell = rowFor(HOME_PAGE.title).querySelectorAll('td')[2];

            expect(templateCell.textContent?.trim()).toBe('e4b4f0c2…');
            expect(templateCell.querySelector('span')?.getAttribute('title')).toBe(
                HOME_PAGE.templateId
            );
        });

        it('should say so when the page has no template', () => {
            render();

            expect(rowFor(CONTACT_PAGE.title).querySelectorAll('td')[2].textContent).toContain(
                'No template'
            );
        });

        it('should render the modification date', () => {
            render();

            expect(rowFor(HOME_PAGE.title).querySelectorAll('td')[3].textContent?.trim()).toBe(
                formatDate(MOD_DATE_EPOCH, 'mediumDate', 'en-US')
            );
        });

        it.each([
            [HOME_PAGE.title, 'Published'],
            [BLOG_PAGE.title, 'Changed'],
            [CONTACT_PAGE.title, 'Draft']
        ])('should render the state of %s as %s', (title, expectedLabel) => {
            render();

            expect(
                rowFor(title).querySelector('[data-testid="select-page-row-state"]')?.textContent
            ).toContain(expectedLabel);
        });

        it('should say the folder is empty when it holds no page', () => {
            pagesBrowserMock.searchPages.mockReturnValue(of([]));

            render();

            expect(spectator.query(byTestId('select-page-empty'))).not.toBeNull();
            expect(rows()).toHaveLength(0);
        });

        it('should hand a failed page listing to the shared error manager', () => {
            const error = new Error('boom');
            pagesBrowserMock.searchPages.mockReturnValue(throwError(() => error));

            render();

            expect(spectator.inject(DotHttpErrorManagerService, true).handle).toHaveBeenCalledWith(
                error
            );
        });
    });

    describe('search', () => {
        beforeEach(() => jest.useFakeTimers());
        afterEach(() => jest.useRealTimers());

        const type = (text: string) => {
            spectator.typeInElement(text, searchInput());
            spectator.detectChanges();
        };

        it('should keep every row until the debounce window closes', () => {
            render();

            type('blog');

            expect(rows()).toHaveLength(PAGES.length);
        });

        it('should narrow the rows to the matching page once the term settles', async () => {
            render();

            type('blog');
            await jest.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
            spectator.detectChanges();

            expect(rows()).toHaveLength(1);
            expect(rows()[0].textContent).toContain(BLOG_PAGE.title);
        });

        it('should match on the page path as well as its title', async () => {
            render();

            type('/contact');
            await jest.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
            spectator.detectChanges();

            expect(rows()).toHaveLength(1);
            expect(rows()[0].textContent).toContain(CONTACT_PAGE.title);
        });

        it('should filter without asking the server again', async () => {
            render();
            pagesBrowserMock.searchPages.mockClear();

            type('blog');
            await jest.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
            spectator.detectChanges();

            expect(pagesBrowserMock.searchPages).not.toHaveBeenCalled();
        });

        it('should restore every row when the term is cleared', async () => {
            render();

            type('blog');
            await jest.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
            spectator.detectChanges();

            spectator.click(spectator.query(byTestId('select-page-search-clear')) as HTMLElement);
            spectator.detectChanges();
            await jest.advanceTimersByTimeAsync(SEARCH_DEBOUNCE_MS);
            spectator.detectChanges();

            expect(rows()).toHaveLength(PAGES.length);
        });
    });

    describe('pages that already host an experiment', () => {
        it('should list the page rather than hide it', () => {
            render();

            expect(rowFor(HOME_PAGE.title)).not.toBeUndefined();
        });

        it('should mark the row as disabled', () => {
            render();

            expect(rowFor(HOME_PAGE.title).getAttribute('aria-disabled')).toBe('true');
        });

        it('should explain why the row cannot be picked', () => {
            render();

            const row = spectator.component
                .$rows()
                .find(({ pageId }) => pageId === HOME_PAGE.identifier);

            expect(row?.disabled).toBe(true);
            expect(row?.disabledTooltipKey).toBe(ROW_DISABLED_TOOLTIP_KEY);
        });

        it('should disable the row radio', () => {
            render();

            const radio = rowFor(HOME_PAGE.title).querySelector(
                '[data-testid="select-page-row-radio"] input'
            ) as HTMLInputElement;

            expect(radio.disabled).toBe(true);
        });

        it('should not select the row when it is clicked', () => {
            render();

            clickRow(HOME_PAGE.title);

            expect(spectator.component.$selectedRow()).toBeNull();
            expect(buttonIn('select-page-confirm').disabled).toBe(true);
        });

        it('should leave a page whose only experiment is archived selectable', () => {
            // Archived experiments are out of play, so the page is free again.
            render();

            expect(rowFor(CONTACT_PAGE.title).getAttribute('aria-disabled')).toBeNull();

            clickRow(CONTACT_PAGE.title);

            expect(spectator.component.$selectedRow()?.pageId).toBe(CONTACT_PAGE.identifier);
        });

        it('should leave every row selectable when the experiment lookup fails', () => {
            // The dialog is still usable: the server rejects a duplicate anyway.
            const error = new Error('boom');
            experimentsServiceMock.getAllUnfiltered.mockReturnValue(throwError(() => error));

            render();

            expect(spectator.inject(DotHttpErrorManagerService, true).handle).toHaveBeenCalledWith(
                error
            );
            expect(rowFor(HOME_PAGE.title).getAttribute('aria-disabled')).toBeNull();
        });
    });

    describe('picking a page', () => {
        it('should keep the confirm button disabled while nothing is picked', () => {
            render();

            expect(buttonIn('select-page-confirm').disabled).toBe(true);
        });

        it('should enable the confirm button once a row is picked', () => {
            render();

            clickRow(BLOG_PAGE.title);

            expect(buttonIn('select-page-confirm').disabled).toBe(false);
        });

        it('should name the picked page in the footer', () => {
            render();

            clickRow(BLOG_PAGE.title);

            expect(spectator.query(byTestId('select-page-selected-label'))?.textContent).toContain(
                BLOG_PAGE.path
            );
        });

        it('should select the row through its radio too', () => {
            render();

            spectator.click(
                rowFor(BLOG_PAGE.title).querySelector(
                    '[data-testid="select-page-row-radio"] input'
                ) as HTMLElement
            );
            spectator.detectChanges();

            expect(spectator.component.$selectedRow()?.pageId).toBe(BLOG_PAGE.identifier);
        });

        it('should close with the picked row when confirmed', () => {
            render();

            clickRow(BLOG_PAGE.title);
            spectator.click(buttonIn('select-page-confirm'));

            expect(dialogRef.close).toHaveBeenCalledWith(
                expect.objectContaining({
                    pageId: BLOG_PAGE.identifier,
                    title: BLOG_PAGE.title,
                    url: BLOG_PAGE.path,
                    template: BLOG_PAGE.templateId,
                    disabled: false
                })
            );
        });

        it('should close with nothing when cancelled', () => {
            render();

            clickRow(BLOG_PAGE.title);
            spectator.click(buttonIn('select-page-cancel'));

            expect(dialogRef.close).toHaveBeenCalledWith();
        });
    });
});
