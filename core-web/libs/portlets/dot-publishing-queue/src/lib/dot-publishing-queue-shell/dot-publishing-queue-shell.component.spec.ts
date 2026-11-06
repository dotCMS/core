import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { Subject, of } from 'rxjs';
import { Mock, Mocked, vi } from 'vitest';

import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Toast } from 'primeng/toast';

/* eslint-disable @nx/enforce-module-boundaries */

import {
    DotCurrentUserService,
    DotFormatDateService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotPublishingQueueService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { PublishAuditStatus, PublishingJobView } from '@dotcms/dotcms-models';
import {
    DotEditContentOpenerService,
    DotEditContentOpenOptions,
    DotPanelUrlStateService
} from '@dotcms/edit-content';
import { DotSidePanelRef } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';

import { DotPublishingQueueShellComponent } from './dot-publishing-queue-shell.component';

import { DotPublishingQueueStore } from '../store/dot-publishing-queue.store';

/** What the shell, its store and the table it renders need, mocked. */
const shellProviders = () => [
    mockProvider(DotPublishingQueueService, {
        listPublishingJobs: vi.fn().mockReturnValue(
            of({
                entity: [],
                pagination: { currentPage: 1, perPage: 10, totalEntries: 0 }
            })
        ),
        getBundleAssets: vi.fn().mockReturnValue(of([])),
        getPublishingJobDetails: vi.fn().mockReturnValue(of({})),
        probeBundleDownload: vi.fn().mockReturnValue(of(true)),
        probeBundleManifest: vi.fn().mockReturnValue(of(true)),
        getUnsendBundles: vi
            .fn()
            .mockReturnValue(of({ identifier: 'id', label: 'name', items: [], numRows: 0 }))
    }),
    mockProvider(DotCurrentUserService, {
        getCurrentUser: vi.fn().mockReturnValue(of({ userId: 'user-1' }))
    }),
    mockProvider(DotHttpErrorManagerService),
    mockProvider(DotFormatDateService),
    mockProvider(DotGlobalMessageService, { error: vi.fn() }),
    mockProvider(DotMessageDisplayService, { push: vi.fn() }),
    mockProvider(DotPushPublishDialogService, { open: vi.fn() }),
    mockProvider(DotDownloadBundleDialogService, { open: vi.fn() }),
    { provide: DotMessageService, useValue: new MockDotMessageService({}) },
    mockProvider(DotEditContentOpenerService, {
        open: vi.fn(() => new DotSidePanelRef(() => undefined))
    }),
    mockProvider(DotPanelUrlStateService, { clear: vi.fn(), refuse: vi.fn(), register: vi.fn() })
];

describe('DotPublishingQueueShellComponent', () => {
    let spectator: Spectator<DotPublishingQueueShellComponent>;
    let dialogService: Mocked<DialogService>;
    let confirmationService: Mocked<ConfirmationService>;
    let store: InstanceType<typeof DotPublishingQueueStore>;

    let onCloseSubject = new Subject<unknown>();
    const dialogRef = {
        close: vi.fn(),
        get onClose() {
            return onCloseSubject;
        }
    } as unknown as DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueShellComponent,
        componentProviders: [
            DotPublishingQueueStore,
            ConfirmationService,
            // The shell's own, for a panel reopened from the URL; its describe below checks it.
            MessageService,
            mockProvider(DialogService, { open: vi.fn().mockReturnValue(dialogRef) })
        ],
        providers: [
            ...shellProviders(),
            // A URL that names no panel (#38303); the load-time cases have their own describe.
            { provide: ActivatedRoute, useValue: { snapshot: { queryParams: {} } } }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
    });

    beforeEach(() => {
        vi.clearAllMocks();
        onCloseSubject = new Subject<unknown>();
        spectator = createComponent();
        dialogService = spectator.inject(DialogService, true) as Mocked<DialogService>;
        confirmationService = spectator.inject(
            ConfirmationService,
            true
        ) as Mocked<ConfirmationService>;
        vi.spyOn(confirmationService, 'confirm');
        store = spectator.inject(DotPublishingQueueStore, true);
    });

    it('renders the toolbar', () => {
        expect(spectator.query('dot-publishing-queue-toolbar')).toBeTruthy();
    });

    it('renders the single bundles table (no tabs)', () => {
        expect(spectator.query('dot-publishing-queue-table')).toBeTruthy();
        expect(spectator.query('p-tabs')).toBeFalsy();
    });

    describe('asset list dialog sync', () => {
        it('opens dialog when selectedBundleId becomes set', () => {
            store.openAssetList('B-1');
            spectator.detectChanges();
            expect(dialogService.open).toHaveBeenCalled();
        });

        it('calls store.closeAssetList when dialog closes', () => {
            store.openAssetList('B-1');
            spectator.detectChanges();
            onCloseSubject.next(undefined);
            expect(store.selectedBundleId()).toBeNull();
        });

        describe('allowRemove per bundle status', () => {
            function openAssetListFor(status: PublishAuditStatus | null): void {
                const row: PublishingJobView = {
                    bundleId: 'B-1',
                    bundleName: 'Bundle 1',
                    status,
                    filterName: null,
                    filterKey: null,
                    assetCount: 2,
                    assetPreview: [],
                    environmentCount: 1,
                    createDate: '2026-01-01T00:00:00Z',
                    statusUpdated: null,
                    numTries: 0
                };
                const service = spectator.inject(DotPublishingQueueService);
                vi.mocked(service.listPublishingJobs).mockReturnValue(
                    of({
                        entity: [row],
                        pagination: { currentPage: 1, perPage: 10, totalEntries: 1 }
                    })
                );
                store.loadBundles();
                store.openAssetList('B-1');
                spectator.detectChanges();
            }

            function allowRemoveOfLastOpen(): boolean | undefined {
                const config = dialogService.open.mock.calls.at(-1)?.[1] as
                    | DynamicDialogConfig<{ allowRemove: boolean }>
                    | undefined;

                return config?.data?.allowRemove;
            }

            it.each([
                PublishAuditStatus.PUBLISHING_BUNDLE,
                PublishAuditStatus.BUNDLING,
                PublishAuditStatus.SENDING_TO_ENDPOINTS
            ])('opens the asset list read-only for a %s bundle', (status) => {
                openAssetListFor(status);
                expect(allowRemoveOfLastOpen()).toBe(false);
            });

            it.each([
                PublishAuditStatus.BUNDLE_REQUESTED,
                PublishAuditStatus.WAITING_FOR_PUBLISHING,
                PublishAuditStatus.SCHEDULED,
                null
            ])('allows removing assets for a %s bundle', (status) => {
                openAssetListFor(status);
                expect(allowRemoveOfLastOpen()).toBe(true);
            });
        });
    });

    /**
     * #38303 (R12) — a contentlet row in either dialog opens the side panel over the dialog. A
     * PrimeNG dialog traps focus by default, which would keep the panel's editor from taking
     * keystrokes, so the two dialogs that open content turn the trap off.
     */
    describe('dialogs that open content in the side panel', () => {
        /** The config of the last dialog the shell opened. */
        const lastConfig = (): DynamicDialogConfig | undefined =>
            dialogService.open.mock.calls.at(-1)?.[1] as DynamicDialogConfig | undefined;

        it('opens the asset list without a focus trap', () => {
            store.openAssetList('B-1');
            spectator.detectChanges();

            expect(lastConfig()?.focusTrap).toBe(false);
        });

        it('opens the select bundle dialog without a focus trap', () => {
            spectator.component.openSelectBundle();

            expect(lastConfig()?.focusTrap).toBe(false);
        });
    });

    describe('detail dialog sync', () => {
        it('opens dialog when detailBundleId becomes set', () => {
            store.openDetail('B-2');
            spectator.detectChanges();
            expect(dialogService.open).toHaveBeenCalled();
        });
    });

    describe('upload', () => {
        it('opens dialog when openUpload is called', () => {
            spectator.component.openUpload();
            expect(dialogService.open).toHaveBeenCalled();
        });
    });

    describe('confirmDeleteBundles', () => {
        it('does nothing when there is no selection (defensive guard)', () => {
            vi.spyOn(store, 'deleteBundlesBulk');
            spectator.component.confirmDeleteBundles();
            expect(confirmationService.confirm).not.toHaveBeenCalled();
            expect(store.deleteBundlesBulk).not.toHaveBeenCalled();
        });

        it('opens a ConfirmDialog when there is a selection', () => {
            store.setBundlesSelection(['b1', 'b2']);
            spectator.component.confirmDeleteBundles();
            expect(confirmationService.confirm).toHaveBeenCalled();
        });

        it('calls store.deleteBundlesBulk with the selected ids on accept', () => {
            store.setBundlesSelection(['b1', 'b2']);
            const spy = vi.spyOn(store, 'deleteBundlesBulk').mockReturnValue(undefined);
            confirmationService.confirm.mockImplementation((cfg) => {
                cfg.accept?.();
                return confirmationService;
            });
            spectator.component.confirmDeleteBundles();
            expect(spy).toHaveBeenCalledWith(['b1', 'b2']);
        });

        it('does NOT delete on reject', () => {
            store.setBundlesSelection(['b1']);
            const spy = vi.spyOn(store, 'deleteBundlesBulk').mockReturnValue(undefined);
            confirmationService.confirm.mockImplementation((cfg) => {
                cfg.reject?.();
                return confirmationService;
            });
            spectator.component.confirmDeleteBundles();
            expect(spy).not.toHaveBeenCalled();
        });
    });
});

/**
 * #38303 (FR-049) — a row opened from the asset list or the bundle wizard writes its panel to the
 * portlet's URL. On load the shell reopens that panel over the list, under its own injector, without
 * the dialog it came from; saves and the close reload the list.
 */
describe('DotPublishingQueueShellComponent — the panel named in the URL', () => {
    const IDENTIFIER = '3f0c8e5a-7b2d-4c1e-9a6f-0d4b8e2c1a57';

    let spectator: Spectator<DotPublishingQueueShellComponent>;
    let dialogOpen: Mock;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueShellComponent,
        // No `componentProviders`: the shell's own providers apply, its MessageService among them.
        providers: shellProviders(),
        schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA],
        detectChanges: false
    });

    /** Builds the portlet on a URL with these query params. */
    const mount = (queryParams: Params): void => {
        dialogOpen = vi.fn();
        spectator = createComponent({
            providers: [
                { provide: ActivatedRoute, useValue: { snapshot: { queryParams } } },
                mockProvider(DialogService, { open: dialogOpen })
            ]
        });
        spectator.detectChanges();
    };

    const opened = () => spectator.inject(DotEditContentOpenerService).open as Mock;

    /** The options the shell handed the opener, on its only `open`. */
    const openOptions = (): DotEditContentOpenOptions => {
        expect(opened().mock.calls.length, 'the shell opens one panel').toBe(1);

        return opened().mock.calls[0][1] as DotEditContentOpenOptions;
    };

    beforeEach(() => vi.clearAllMocks());

    it('reopens the version the URL names over the list, by identifier and language', () => {
        mount({ editContent: IDENTIFIER, editContentLang: '2' });

        expect(opened().mock.calls.length, 'the shell opens one panel').toBe(1);
        expect(opened().mock.calls[0][0]).toEqual({
            mode: 'edit',
            identifier: IDENTIFIER,
            languageId: 2,
            portletId: 'publishing-queue'
        });
        expect(openOptions().url).toEqual({});
        // Over the list: the dialog the row was in is not reopened.
        expect(dialogOpen).not.toHaveBeenCalled();
    });

    it("opens it under the shell's injector", () => {
        mount({ editContent: IDENTIFIER });

        // Compared by identity: printing an injector runs the worker out of memory.
        expect(
            openOptions().injector.get(DotPublishingQueueShellComponent, null) ===
                spectator.component
        ).toBe(true);
    });

    // A create is never written here, but a link may name one; it opens like anywhere else.
    it('opens a create of the content type the URL names', () => {
        mount({ createContent: 'Blog' });

        expect(opened().mock.calls[0][0]).toEqual({
            mode: 'new',
            contentType: 'Blog',
            portletId: 'publishing-queue'
        });
    });

    it('reloads the list on every save and when the panel closes', () => {
        mount({ editContent: IDENTIFIER, editContentLang: '2' });
        const refresh = vi.spyOn(spectator.inject(DotPublishingQueueStore, true), 'refresh');

        openOptions().saved?.({ inode: 'saved' } as never);
        openOptions().closed?.();

        expect(refresh).toHaveBeenCalledTimes(2);
    });

    it('opens nothing for a URL that names no panel', () => {
        mount({});

        expect(opened()).not.toHaveBeenCalled();
        expect(spectator.inject(DotPanelUrlStateService).clear).not.toHaveBeenCalled();
        expect(spectator.inject(DotPanelUrlStateService).refuse).not.toHaveBeenCalled();
    });

    // A link the author cannot have meant opens nothing, tells them, and loses its params (T172).
    it.each([
        ['names two panels', { editContent: IDENTIFIER, createContent: 'Blog' }],
        ['names content by a malformed identifier', { editContent: 'wrong-identifier' }]
    ])('refuses a URL that %s', (_label, queryParams) => {
        mount(queryParams);

        expect(opened()).not.toHaveBeenCalled();
        expect(spectator.inject(DotPanelUrlStateService).refuse).toHaveBeenCalledTimes(1);
    });

    /**
     * The dialogs bring their own MessageService and toast for the panels they open; a panel opened
     * over the list toasts through the shell's. The admin root provides none (one there would hide
     * the page editor's).
     */
    it('provides a MessageService of its own and renders its toast', () => {
        mount({ editContent: IDENTIFIER });
        const own = spectator.inject(MessageService, true);

        expect(own).toBeTruthy();
        expect(openOptions().injector.get(MessageService, null) === own).toBe(true);
        expect(spectator.query(Toast)).not.toBeNull();
    });
});
