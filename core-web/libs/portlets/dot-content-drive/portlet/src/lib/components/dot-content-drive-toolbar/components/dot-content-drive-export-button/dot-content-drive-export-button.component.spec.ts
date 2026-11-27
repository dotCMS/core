import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/vitest';
import { of, Subject } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, Mock, vi } from 'vitest';

vi.mock('@dotcms/utils', async () => ({
    ...(await vi.importActual('@dotcms/utils')),
    getDownloadLink: vi.fn()
}));

import { signal } from '@angular/core';

import { MessageService } from 'primeng/api';

import {
    DotContentDriveExportResult,
    DotContentDriveExportService,
    DotContentTypeService,
    DotMessageService
} from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotContentDriveItem,
    DotContentDriveSearchRequest
} from '@dotcms/dotcms-models';
import { getDownloadLink } from '@dotcms/utils';
import {
    createFakeContentlet,
    createFakeContentType,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotContentDriveExportButtonComponent } from './dot-content-drive-export-button.component';

import { DEFAULT_PAGE, SUCCESS_MESSAGE_LIFE } from '../../../../shared/constants';
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

describe('DotContentDriveExportButtonComponent (filter mode)', () => {
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
                $request: requestSignal
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
                    'content-drive.toast.export-success-detail': '{0} items exported in {1} file(s)'
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
});
