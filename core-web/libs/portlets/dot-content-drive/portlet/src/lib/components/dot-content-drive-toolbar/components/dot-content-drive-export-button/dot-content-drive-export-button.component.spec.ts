import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/vitest';
import { of, Subject, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, Mock, vi } from 'vitest';

vi.mock('@dotcms/utils', async () => ({
    ...(await vi.importActual('@dotcms/utils')),
    getDownloadLink: vi.fn()
}));

import { signal } from '@angular/core';

import { MessageService } from 'primeng/api';
import { Tooltip } from 'primeng/tooltip';

import {
    DotContentDriveExportResult,
    DotContentDriveExportService,
    DotContentTypeService,
    DotMessageService
} from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotContentDriveItem,
    DotContentDriveSearchRequest,
    DotSite
} from '@dotcms/dotcms-models';
import { getDownloadLink } from '@dotcms/utils';
import {
    createFakeContentlet,
    createFakeContentType,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotContentDriveExportButtonComponent } from './dot-content-drive-export-button.component';

import {
    DEFAULT_PAGE,
    ERROR_MESSAGE_LIFE,
    SUCCESS_MESSAGE_LIFE,
    WARNING_MESSAGE_LIFE
} from '../../../../shared/constants';
import {
    DotContentDriveFilters,
    DotContentDrivePage,
    DotContentDriveStatus
} from '../../../../shared/models';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

/** A Form type's variable: fetching it answers with the FORM base type. */
const FORM_VARIABLE = 'ContactForm';

/** The listing request the store would send to `/api/v1/drive/search`. */
const REQUEST = {
    assetPath: '//demo.dotcms.com/',
    browseScope: 'ALL',
    includeSystemHost: true,
    contentTypes: ['Blog'],
    language: ['1'],
    filters: { text: '', filterFolders: true },
    contentCursor: 0,
    folderCursor: 0,
    maxResults: 40,
    sortBy: 'modDate:desc',
    showFolders: false
} as DotContentDriveSearchRequest;

const CONTENTLET = createFakeContentlet({ identifier: 'contentlet-1', inode: 'inode-1' });
const FOLDER = {
    type: 'folder',
    identifier: 'folder-1',
    inode: 'folder-1',
    title: 'blog',
    name: 'blog',
    path: '/blog/'
} as unknown as DotContentDriveItem;

/** The last loaded page, reporting nothing more to load. */
const LAST_PAGE: DotContentDrivePage = { ...DEFAULT_PAGE, hasMoreContent: false };

const FILE_RESULT: DotContentDriveExportResult = {
    kind: 'file',
    blob: new Blob(['Identifier,languageCode,countryCode,title\n']),
    fileName: 'Blog_contents_11_26_2026.csv',
    contentlets: 3,
    files: 1
};

const EMPTY_RESULT: DotContentDriveExportResult = { kind: 'empty' };

/** The message the service errors with once it has read the server's error body. */
const SERVER_MESSAGE = 'Server says no';

/**
 * The most items selection mode exports at once (FR-002a).
 *
 * Declared here until T082 adds `MAX_SELECTION_ITEMS` to `shared/constants.ts`; once it exists,
 * import it instead so the spec and the button cannot drift apart.
 */
const MAX_SELECTION_ITEMS = 60;

/** The site Content Drive is browsing: selection mode sends its identifier as `siteId`. */
const CURRENT_SITE: DotSite = {
    aliases: '',
    archived: false,
    hostname: 'demo.dotcms.com',
    identifier: 'site-001'
};

/** Copy for the limit tooltip, so the test reads the sentence the user sees. */
const MAX_ITEMS_TOOLTIP = 'You can export at most 60 items at once';

/** A selected contentlet, carrying the two values selection mode sends for it. */
const selectedContentlet = (identifier: string, languageId: number): DotContentDriveItem =>
    createFakeContentlet({ identifier, inode: `inode-${identifier}-${languageId}`, languageId });

/** `count` distinct selected contentlets, all in English. */
const selectedContentlets = (count: number): DotContentDriveItem[] =>
    Array.from({ length: count }, (_, index) => selectedContentlet(`contentlet-${index}`, 1));

/** A selection-mode 200: the zip, holding one CSV per content type. */
const ZIP_RESULT: DotContentDriveExportResult = {
    kind: 'file',
    blob: new Blob(['zip']),
    fileName: 'demo.dotcms.com_export_11_26_2026.zip',
    contentlets: 3,
    files: 2
};

// The describes at this level are filter mode (nothing selected); selection mode has its own block
// at the end.
describe('DotContentDriveExportButtonComponent', () => {
    let spectator: Spectator<DotContentDriveExportButtonComponent>;
    let exportService: SpyObject<DotContentDriveExportService>;
    let contentTypeService: SpyObject<DotContentTypeService>;

    // Real signals, so the component's computeds re-run when a test changes them.
    const selectedItemsSignal = signal<DotContentDriveItem[]>([]);
    const filtersSignal = signal<DotContentDriveFilters>({ contentType: ['Blog'] });
    const statusSignal = signal<DotContentDriveStatus>(DotContentDriveStatus.LOADED);
    const itemsSignal = signal<DotContentDriveItem[]>([CONTENTLET]);
    const pagesSignal = signal<DotContentDrivePage[]>([LAST_PAGE]);
    const requestSignal = signal<DotContentDriveSearchRequest>(REQUEST);
    const currentSiteSignal = signal<DotSite>(CURRENT_SITE);

    const messageServiceSpy = { add: vi.fn(), clear: vi.fn() };
    const anchorClick = vi.fn();

    const createComponent = createComponentFactory({
        component: DotContentDriveExportButtonComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                selectedItems: selectedItemsSignal,
                filters: filtersSignal,
                status: statusSignal,
                items: itemsSignal,
                pages: pagesSignal,
                $request: requestSignal,
                currentSite: currentSiteSignal
            }),
            mockProvider(DotContentTypeService, {
                getContentType: vi.fn((variable: string) =>
                    of(
                        createFakeContentType({
                            variable,
                            baseType:
                                variable === FORM_VARIABLE
                                    ? DotCMSBaseTypesContentTypes.FORM
                                    : DotCMSBaseTypesContentTypes.CONTENT
                        })
                    )
                )
            }),
            mockProvider(DotContentDriveExportService, {
                export: vi.fn().mockReturnValue(of(FILE_RESULT))
            }),
            mockProvider(MessageService, messageServiceSpy),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.export.button': 'Export',
                    'content-drive.toast.export-success': 'Export ready',
                    'content-drive.toast.export-success-detail':
                        '{0} items exported in {1} file(s)',
                    'content-drive.toast.export-empty': 'There is nothing to export',
                    'content-drive.toast.export-error': 'Export failed',
                    'content-drive.export.tooltip.max-items': MAX_ITEMS_TOOLTIP
                })
            }
        ],
        detectChanges: false
    });

    /** The PrimeNG host of the Export button. */
    const exportButtonHost = () => spectator.query(byTestId('content-drive-export-button'));

    /** The native button inside it: the element that is disabled, busy, and clicked. */
    const exportButton = () =>
        exportButtonHost()?.querySelector('button') as HTMLButtonElement | null;

    /**
     * Renders, lets the content-type fetch settle, and renders again: whether the button shows
     * depends on the fetched type, which arrives after the first pass.
     */
    const render = async () => {
        spectator.detectChanges();
        await spectator.fixture.whenStable();
        spectator.detectChanges();
    };

    beforeEach(() => {
        (getDownloadLink as Mock).mockReturnValue({ click: anchorClick });
        spectator = createComponent();
        exportService = spectator.inject(DotContentDriveExportService);
        // The mock is shared by every test and clearAllMocks keeps a return value a test set, so
        // reset it here: otherwise the "while running" Subject leaks into the tests after it.
        exportService.export.mockReturnValue(of(FILE_RESULT));
        contentTypeService = spectator.inject(DotContentTypeService);
    });

    afterEach(() => {
        vi.clearAllMocks();
        selectedItemsSignal.set([]);
        filtersSignal.set({ contentType: ['Blog'] });
        statusSignal.set(DotContentDriveStatus.LOADED);
        itemsSignal.set([CONTENTLET]);
        pagesSignal.set([LAST_PAGE]);
        requestSignal.set(REQUEST);
        currentSiteSignal.set(CURRENT_SITE);
    });

    describe('shown', () => {
        it('should show when nothing is selected and the listing is filtered to one non-Form type', async () => {
            await render();

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('Blog');
            expect(exportButtonHost()).toBeTruthy();
            expect(exportButtonHost()?.textContent).toContain('Export');
        });
    });

    // Each case starts from the shown state and changes only the filter, so a button that never
    // renders cannot pass them.
    describe('not shown', () => {
        beforeEach(async () => {
            await render();
            expect(exportButtonHost()).toBeTruthy();
        });

        it('should not show when no content type is filtered', async () => {
            filtersSignal.set({});
            await render();

            expect(exportButtonHost()).toBeNull();
        });

        it('should not show when two content types are filtered', async () => {
            filtersSignal.set({ contentType: ['Blog', 'News'] });
            await render();

            expect(exportButtonHost()).toBeNull();
        });

        it('should not show when the one filtered type is a Form', async () => {
            filtersSignal.set({ contentType: [FORM_VARIABLE] });
            await render();

            expect(contentTypeService.getContentType).toHaveBeenCalledWith(FORM_VARIABLE);
            expect(exportButtonHost()).toBeNull();
        });
    });

    describe('disabled', () => {
        it('should be disabled when the loaded listing is empty and reports no more content', async () => {
            itemsSignal.set([]);
            await render();

            expect(exportButton()).toBeTruthy();
            expect(exportButton()?.disabled).toBe(true);
        });

        it('should be disabled when the loaded listing holds only folders and reports no more content', async () => {
            itemsSignal.set([FOLDER]);
            await render();

            expect(exportButton()).toBeTruthy();
            expect(exportButton()?.disabled).toBe(true);
        });
    });

    describe('enabled', () => {
        it('should be enabled when the loaded listing holds a contentlet', async () => {
            itemsSignal.set([FOLDER, CONTENTLET]);
            await render();

            expect(exportButton()).toBeTruthy();
            expect(exportButton()?.disabled).toBe(false);
        });

        it('should be enabled when only folders are loaded but a page reports more content', async () => {
            itemsSignal.set([FOLDER]);
            pagesSignal.set([{ ...DEFAULT_PAGE, hasMoreContent: true }]);
            await render();

            expect(exportButton()).toBeTruthy();
            expect(exportButton()?.disabled).toBe(false);
        });

        it('should be enabled while the listing is still loading', async () => {
            itemsSignal.set([]);
            statusSignal.set(DotContentDriveStatus.LOADING);
            await render();

            expect(exportButton()).toBeTruthy();
            expect(exportButton()?.disabled).toBe(false);
        });
    });

    describe('click', () => {
        it('should export the listing request the store holds', async () => {
            await render();

            spectator.click(exportButton() as HTMLElement);

            expect(exportService.export).toHaveBeenCalledTimes(1);
            expect(exportService.export).toHaveBeenCalledWith({ query: REQUEST });
        });
    });

    describe('while running', () => {
        it('should show loading, stay disabled, and ignore a second click', async () => {
            // Never answers, so the export stays in flight for the whole test.
            exportService.export.mockReturnValue(new Subject<DotContentDriveExportResult>());
            await render();

            spectator.click(exportButton() as HTMLElement);
            spectator.detectChanges();

            expect(exportButton()).toHaveClass('p-button-loading');
            expect(exportButton()?.disabled).toBe(true);

            spectator.click(exportButton() as HTMLElement);

            expect(exportService.export).toHaveBeenCalledTimes(1);
        });
    });

    describe('on success', () => {
        it('should download the file under the name the server gave', async () => {
            await render();

            spectator.click(exportButton() as HTMLElement);

            expect(getDownloadLink).toHaveBeenCalledWith(FILE_RESULT.blob, FILE_RESULT.fileName);
            expect(anchorClick).toHaveBeenCalledTimes(1);
        });

        it('should show a success toast with the number of exported items', async () => {
            await render();

            spectator.click(exportButton() as HTMLElement);

            expect(messageServiceSpy.add).toHaveBeenCalledWith(
                expect.objectContaining({
                    severity: 'success',
                    summary: 'Export ready',
                    detail: '3 items exported in 1 file(s)',
                    life: SUCCESS_MESSAGE_LIFE
                })
            );
        });
    });

    describe('on empty', () => {
        beforeEach(() => {
            exportService.export.mockReturnValue(of(EMPTY_RESULT));
        });

        it('should show a warning toast that there is nothing to export', async () => {
            await render();

            spectator.click(exportButton() as HTMLElement);

            expect(messageServiceSpy.add).toHaveBeenCalledTimes(1);
            expect(messageServiceSpy.add).toHaveBeenCalledWith(
                expect.objectContaining({
                    severity: 'warn',
                    summary: 'There is nothing to export',
                    life: WARNING_MESSAGE_LIFE
                })
            );
        });

        it('should not download anything', async () => {
            await render();

            spectator.click(exportButton() as HTMLElement);

            expect(getDownloadLink).not.toHaveBeenCalled();
            expect(anchorClick).not.toHaveBeenCalled();
        });
    });

    describe('on error', () => {
        beforeEach(() => {
            exportService.export.mockReturnValue(throwError(() => new Error(SERVER_MESSAGE)));
        });

        it('should show an error toast with the server message', async () => {
            await render();

            spectator.click(exportButton() as HTMLElement);

            expect(messageServiceSpy.add).toHaveBeenCalledTimes(1);
            expect(messageServiceSpy.add).toHaveBeenCalledWith(
                expect.objectContaining({
                    severity: 'error',
                    summary: 'Export failed',
                    detail: SERVER_MESSAGE,
                    life: ERROR_MESSAGE_LIFE
                })
            );
        });

        it('should not download anything', async () => {
            await render();

            spectator.click(exportButton() as HTMLElement);

            expect(getDownloadLink).not.toHaveBeenCalled();
            expect(anchorClick).not.toHaveBeenCalled();
        });
    });

    // Each case holds the answer back until the button has shown it is loading, so the button is
    // seen both loading and cleared: an answer that arrived synchronously could not prove that.
    describe('loading is cleared', () => {
        let answer: Subject<DotContentDriveExportResult>;

        /** Clicks Export and checks the button is loading while the answer is held back. */
        const clickAndExpectLoading = async () => {
            await render();

            spectator.click(exportButton() as HTMLElement);
            spectator.detectChanges();

            expect(exportButton()).toHaveClass('p-button-loading');
            expect(exportButton()?.disabled).toBe(true);
        };

        /** Checks the button is back to its idle state, ready for another export. */
        const expectNotLoading = () => {
            spectator.detectChanges();

            expect(exportButton()).not.toHaveClass('p-button-loading');
            expect(exportButton()?.disabled).toBe(false);
        };

        beforeEach(() => {
            answer = new Subject<DotContentDriveExportResult>();
            exportService.export.mockReturnValue(answer);
        });

        it('should clear loading after a file is downloaded', async () => {
            await clickAndExpectLoading();

            answer.next(FILE_RESULT);
            answer.complete();

            expectNotLoading();
        });

        it('should clear loading when there is nothing to export', async () => {
            await clickAndExpectLoading();

            answer.next(EMPTY_RESULT);
            answer.complete();

            expectNotLoading();
        });

        it('should clear loading when the export fails', async () => {
            await clickAndExpectLoading();

            answer.error(new Error(SERVER_MESSAGE));

            expectNotLoading();
        });
    });

    // Selection mode (#37900, US2): with anything selected, the button exports the selected
    // contentlets as a zip, whatever the listing is filtered to. Folders are Phase 9's (T085).
    describe('selection mode', () => {
        /** The wrapper the limit tooltip hangs off: a disabled button fires no tooltip itself. */
        const exportTooltipWrapper = () =>
            spectator.query(byTestId('content-drive-export-tooltip'));

        /** The tooltip directive on that wrapper, read for the text it would show. */
        // A string selector, not byTestId: Spectator ignores `read` for DOM selectors and would
        // return the span instead of the Tooltip directive.
        const exportTooltip = () =>
            spectator.query('[data-testid="content-drive-export-tooltip"]', { read: Tooltip });

        describe('shown', () => {
            it('should show when something is selected and no content type is filtered', async () => {
                filtersSignal.set({});
                selectedItemsSignal.set([selectedContentlet('blog-1', 1)]);
                await render();

                expect(exportButtonHost()).toBeTruthy();
                expect(exportButtonHost()?.textContent).toContain('Export');
            });

            it('should show when something is selected and several content types are filtered', async () => {
                filtersSignal.set({ contentType: ['Blog', 'News'] });
                selectedItemsSignal.set([selectedContentlet('blog-1', 1)]);
                await render();

                expect(exportButtonHost()).toBeTruthy();
            });

            it('should show when something is selected and the one filtered type is a Form', async () => {
                // The Form rule is filter mode's: the server drops Form contentlets from a
                // selection itself, so the button does not hide over the filter.
                filtersSignal.set({ contentType: [FORM_VARIABLE] });
                selectedItemsSignal.set([selectedContentlet('blog-1', 1)]);
                await render();

                expect(exportButtonHost()).toBeTruthy();
            });
        });

        describe('item limit', () => {
            it(`should be enabled with exactly ${MAX_SELECTION_ITEMS} items selected`, async () => {
                selectedItemsSignal.set(selectedContentlets(MAX_SELECTION_ITEMS));
                await render();

                expect(exportButton()).toBeTruthy();
                expect(exportButton()?.disabled).toBe(false);
                expect(exportTooltip()?.content ?? '').toBe('');
            });

            it(`should be disabled with more than ${MAX_SELECTION_ITEMS} items selected`, async () => {
                selectedItemsSignal.set(selectedContentlets(MAX_SELECTION_ITEMS + 1));
                await render();

                expect(exportButton()).toBeTruthy();
                expect(exportButton()?.disabled).toBe(true);
            });

            it('should say why it is disabled on a wrapper, not on the disabled button', async () => {
                selectedItemsSignal.set(selectedContentlets(MAX_SELECTION_ITEMS + 1));
                await render();

                const wrapper = exportTooltipWrapper();

                expect(wrapper?.tagName).toBe('SPAN');
                expect(wrapper?.contains(exportButtonHost())).toBe(true);
                expect(exportTooltip()?.content).toBe(MAX_ITEMS_TOOLTIP);
            });
        });

        describe('click', () => {
            it('should send the selected contentlets with the current site, and no query', async () => {
                // Two language versions of one Blog and a News: each entry is its own row.
                selectedItemsSignal.set([
                    selectedContentlet('blog-1', 1),
                    selectedContentlet('blog-1', 2),
                    selectedContentlet('news-1', 1)
                ]);
                await render();

                spectator.click(exportButton() as HTMLElement);

                expect(exportService.export).toHaveBeenCalledTimes(1);
                expect(exportService.export).toHaveBeenCalledWith({
                    siteId: CURRENT_SITE.identifier,
                    selection: {
                        folders: [],
                        contentlets: [
                            { identifier: 'blog-1', languageId: 1 },
                            { identifier: 'blog-1', languageId: 2 },
                            { identifier: 'news-1', languageId: 1 }
                        ]
                    }
                });
            });

            it('should not send a selected folder as a contentlet', async () => {
                selectedItemsSignal.set([selectedContentlet('blog-1', 1), FOLDER]);
                await render();

                spectator.click(exportButton() as HTMLElement);

                expect(exportService.export).toHaveBeenCalledTimes(1);
                expect(exportService.export).toHaveBeenCalledWith(
                    expect.objectContaining({
                        siteId: CURRENT_SITE.identifier,
                        selection: expect.objectContaining({
                            contentlets: [{ identifier: 'blog-1', languageId: 1 }]
                        })
                    })
                );
            });
        });

        describe('on success', () => {
            beforeEach(() => {
                exportService.export.mockReturnValue(of(ZIP_RESULT));
                selectedItemsSignal.set([
                    selectedContentlet('blog-1', 1),
                    selectedContentlet('blog-2', 1),
                    selectedContentlet('news-1', 1)
                ]);
            });

            it('should download the zip under the name the server gave', async () => {
                await render();

                spectator.click(exportButton() as HTMLElement);

                expect(getDownloadLink).toHaveBeenCalledWith(ZIP_RESULT.blob, ZIP_RESULT.fileName);
                expect(anchorClick).toHaveBeenCalledTimes(1);
            });

            it('should show a success toast with the exported items and files', async () => {
                await render();

                spectator.click(exportButton() as HTMLElement);

                expect(messageServiceSpy.add).toHaveBeenCalledWith(
                    expect.objectContaining({
                        severity: 'success',
                        summary: 'Export ready',
                        detail: '3 items exported in 2 file(s)',
                        life: SUCCESS_MESSAGE_LIFE
                    })
                );
            });
        });

        describe('on error', () => {
            it('should show an error toast with the server message and download nothing', async () => {
                exportService.export.mockReturnValue(throwError(() => new Error(SERVER_MESSAGE)));
                selectedItemsSignal.set([selectedContentlet('blog-1', 1)]);
                await render();

                spectator.click(exportButton() as HTMLElement);

                expect(messageServiceSpy.add).toHaveBeenCalledWith(
                    expect.objectContaining({
                        severity: 'error',
                        summary: 'Export failed',
                        detail: SERVER_MESSAGE,
                        life: ERROR_MESSAGE_LIFE
                    })
                );
                expect(getDownloadLink).not.toHaveBeenCalled();
            });
        });
    });
});
