import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Subject, of, throwError } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

/* eslint-disable @nx/enforce-module-boundaries */

import {
    DotContentTypeService,
    DotCurrentUserService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPublishingQueueService,
    DotPushPublishFiltersService,
    PushPublishService
} from '@dotcms/data-access';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotParseHtmlService } from '@services/dot-parse-html/dot-parse-html.service';

import { DotPublishingQueueSelectBundleDialogComponent } from './dot-publishing-queue-select-bundle-dialog.component';

const UNSENT_RESPONSE = {
    identifier: 'id',
    label: 'name',
    items: [
        { id: 'bundle-1', name: 'Spring campaign refresh' },
        { id: 'bundle-2', name: 'Blog content sync' }
    ],
    numRows: 2
};

const MOCK_ASSETS = [
    { asset: 'a1', title: 'Spring Sale Landing', type: 'contentlet' },
    { asset: 'a2', title: 'hero-spring.jpg', type: 'contentlet' }
];

// Mock the blob-download helper so the click side-effect is observable in tests.
const mockAnchorClick = jest.fn();
jest.mock('@dotcms/utils', () => {
    const actual = jest.requireActual('@dotcms/utils');
    return {
        ...actual,
        getDownloadLink: jest.fn(() => ({ click: mockAnchorClick }) as unknown as HTMLAnchorElement)
    };
});

describe('DotPublishingQueueSelectBundleDialogComponent', () => {
    let spectator: Spectator<DotPublishingQueueSelectBundleDialogComponent>;
    let service: jest.Mocked<DotPublishingQueueService>;
    let confirmationService: jest.Mocked<ConfirmationService>;
    let dialogRef: jest.Mocked<DynamicDialogRef>;
    let globalMessage: jest.Mocked<DotGlobalMessageService>;
    let httpErrorManager: jest.Mocked<DotHttpErrorManagerService>;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueSelectBundleDialogComponent,
        providers: [
            mockProvider(DotPublishingQueueService, {
                getUnsendBundles: jest.fn().mockReturnValue(of(UNSENT_RESPONSE)),
                getBundleAssets: jest.fn().mockReturnValue(of(MOCK_ASSETS)),
                removeAssetsFromBundle: jest
                    .fn()
                    .mockReturnValue(of([{ assetId: 'a1', success: true, message: 'ok' }])),
                deleteBundles: jest.fn().mockReturnValue(of({ entity: 'ok' })),
                pushBundle: jest.fn().mockReturnValue(
                    of({
                        bundleId: 'bundle-1',
                        operation: 'publish',
                        publishDate: null,
                        expireDate: null,
                        environments: ['env-1'],
                        filterKey: 'default.yml'
                    })
                ),
                generateBundle: jest
                    .fn()
                    .mockReturnValue(of({ blob: new Blob(['x']), filename: 'bundle.tar.gz' }))
            }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest
                    .fn()
                    .mockReturnValue(of({ userId: 'dotcms.org.1', email: 'admin@dotcms.com' }))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() }),
            mockProvider(DotContentTypeService, {
                getContentType: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotGlobalMessageService, { error: jest.fn() }),
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            // The dialog provides DotPushPublishFiltersService at the component
            // level (mirrors the legacy DotPushPublishDialogComponent). Both the
            // embedded <dot-push-publish-form> AND the inline Download menu call
            // .get() on it during ngOnInit.
            mockProvider(DotPushPublishFiltersService, {
                get: jest.fn().mockReturnValue(
                    of([
                        {
                            key: 'ForcePush.yml',
                            title: 'Force Push Everything',
                            defaultFilter: false
                        },
                        {
                            key: 'OnlySelected.yml',
                            title: 'Only Selected Items',
                            defaultFilter: false
                        },
                        {
                            key: 'ContentDeps.yml',
                            title: 'Content, Assets and Pages',
                            defaultFilter: true
                        }
                    ])
                )
            }),
            // The form also calls DotcmsConfigService.getTimeZones() during ngOnInit.
            mockProvider(DotcmsConfigService, {
                getTimeZones: jest.fn().mockReturnValue(of([]))
            }),
            // The form injects DotParseHtmlService (only used when `customCode` is in
            // data; we don't pass that, but the DI lookup happens unconditionally).
            mockProvider(DotParseHtmlService, { parse: jest.fn() }),
            // PushPublishEnvSelectorComponent (rendered inside the embedded form)
            // injects PushPublishService for the "remember last push" env list.
            mockProvider(PushPublishService, {
                getEnvironments: jest.fn().mockReturnValue(of([])),
                pushPublishContent: jest.fn().mockReturnValue(of({}))
            }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
    });

    beforeEach(() => {
        mockAnchorClick.mockClear();
        spectator = createComponent();
        service = spectator.inject(
            DotPublishingQueueService
        ) as jest.Mocked<DotPublishingQueueService>;
        dialogRef = spectator.inject(DynamicDialogRef) as jest.Mocked<DynamicDialogRef>;
        globalMessage = spectator.inject(
            DotGlobalMessageService
        ) as jest.Mocked<DotGlobalMessageService>;
        httpErrorManager = spectator.inject(
            DotHttpErrorManagerService
        ) as jest.Mocked<DotHttpErrorManagerService>;
        confirmationService = spectator.inject(
            ConfirmationService,
            true
        ) as jest.Mocked<ConfirmationService>;
        jest.spyOn(confirmationService, 'confirm').mockImplementation((cfg) => {
            cfg.accept?.();
            return confirmationService;
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('init', () => {
        it('fetches drafts via getUnsendBundles and renders both bundle rows', () => {
            spectator.detectChanges();
            expect(service.getUnsendBundles).toHaveBeenCalledWith(
                'dotcms.org.1',
                '*',
                0,
                expect.any(Number)
            );
            expect(spectator.component.bundles().length).toBe(2);
        });

        it('auto-selects the first bundle and loads its assets', () => {
            spectator.detectChanges();
            expect(spectator.component.activeBundleId()).toBe('bundle-1');
            expect(service.getBundleAssets).toHaveBeenCalledWith('bundle-1');
            expect(spectator.component.assets().length).toBe(2);
        });

        it('starts in the select step', () => {
            spectator.detectChanges();
            expect(spectator.component.step()).toBe('select');
        });
    });

    describe('select bundle', () => {
        it('clicking a different bundle loads its assets', () => {
            spectator.detectChanges();
            (service.getBundleAssets as jest.Mock).mockClear();

            spectator.component.onSelectBundle({ id: 'bundle-2', name: 'Blog content sync' });
            expect(spectator.component.activeBundleId()).toBe('bundle-2');
            expect(service.getBundleAssets).toHaveBeenCalledWith('bundle-2');
        });

        it('clicking the already-active bundle is a no-op (no extra fetch)', () => {
            spectator.detectChanges();
            (service.getBundleAssets as jest.Mock).mockClear();

            spectator.component.onSelectBundle({ id: 'bundle-1', name: 'Spring campaign refresh' });
            expect(service.getBundleAssets).not.toHaveBeenCalled();
        });
    });

    describe('type icon', () => {
        it('maps known asset types to icons', () => {
            expect(spectator.component.typeIcon('contentlet')).toBe('pi pi-file');
            expect(spectator.component.typeIcon('template')).toBe('pi pi-window-maximize');
        });

        it('falls back to a generic icon for unknown types', () => {
            expect(spectator.component.typeIcon('weird-type')).toBe('pi pi-file');
        });
    });

    describe('asset row click → opens content editor', () => {
        let openSpy: jest.SpyInstance;

        beforeEach(() => {
            openSpy = jest.spyOn(window, 'open').mockImplementation(() => null);
            spectator.detectChanges();
        });

        afterEach(() => openSpy.mockRestore());

        it('opens the resolved edit URL in a new tab when the row has one', () => {
            // Prime the resolved URL map so editUrlFor() returns a non-null URL.
            spectator.component.assetEditUrls.set(
                new Map([['a1', '/edit/contentlet/a1']])
            );
            spectator.component.onSelectAssetRow({
                asset: 'a1',
                title: 'Spring Sale Landing',
                type: 'contentlet'
            });
            expect(openSpy).toHaveBeenCalledWith('/edit/contentlet/a1', '_blank', 'noopener');
        });

        it('is a no-op when the asset has no resolved edit URL', () => {
            spectator.component.assetEditUrls.set(new Map());
            spectator.component.onSelectAssetRow({
                asset: 'a1',
                title: 'x',
                type: 'template'
            });
            expect(openSpy).not.toHaveBeenCalled();
        });

        it('renders the asset name as plain text, not as an anchor', () => {
            spectator.component.assetEditUrls.set(
                new Map([['a1', '/edit/contentlet/a1']])
            );
            spectator.detectChanges();
            const name = spectator.query(byTestId('pq-select-bundle-asset-name'));
            // The element tag must not be <a> — the row, not the name, is the
            // clickable surface now.
            expect(name?.tagName.toLowerCase()).not.toBe('a');
        });
    });

    describe('remove asset', () => {
        it('confirms then calls removeAssetsFromBundle and refetches', () => {
            spectator.detectChanges();
            (service.getBundleAssets as jest.Mock).mockClear();

            spectator.component.onRemoveAsset({
                asset: 'a1',
                title: 'Spring Sale Landing',
                type: 'contentlet'
            });

            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(service.removeAssetsFromBundle).toHaveBeenCalledWith('bundle-1', ['a1']);
            expect(service.getBundleAssets).toHaveBeenCalledWith('bundle-1');
        });

        it('is a no-op when no active bundle', () => {
            spectator.detectChanges();
            spectator.component.activeBundleId.set(null);
            (service.removeAssetsFromBundle as jest.Mock).mockClear();
            spectator.component.onRemoveAsset({
                asset: 'a1',
                title: 'x',
                type: 'contentlet'
            });
            expect(service.removeAssetsFromBundle).not.toHaveBeenCalled();
        });

        it('on service error: hands off to httpErrorManager', () => {
            spectator.detectChanges();
            const error = new Error('boom');
            (service.removeAssetsFromBundle as jest.Mock).mockReturnValueOnce(
                throwError(() => error)
            );
            const handler = spectator.inject(
                DotHttpErrorManagerService
            ) as jest.Mocked<DotHttpErrorManagerService>;
            spectator.component.onRemoveAsset({
                asset: 'a1',
                title: 'x',
                type: 'contentlet'
            });
            expect(handler.handle).toHaveBeenCalledWith(error);
        });
    });

    describe('remove bundles (bulk)', () => {
        it('confirms then calls deleteBundles with the checked ids; auto-selects next bundle if active was deleted', () => {
            spectator.detectChanges();
            spectator.component.onCheckedChange([
                { id: 'bundle-1', name: 'Spring campaign refresh' }
            ]);
            // After delete, the next list call returns only the remaining bundle.
            (service.getUnsendBundles as jest.Mock).mockReturnValueOnce(
                of({
                    identifier: 'id',
                    label: 'name',
                    items: [{ id: 'bundle-2', name: 'Blog content sync' }],
                    numRows: 1
                })
            );

            spectator.component.onRemoveBundles();

            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(service.deleteBundles).toHaveBeenCalledWith(['bundle-1']);
            // Active flips off the deleted bundle and re-selects the next remaining one.
            expect(spectator.component.activeBundleId()).toBe('bundle-2');
        });

        it('does not delete and surfaces the "select one" warning when nothing is checked', () => {
            spectator.detectChanges();
            (service.deleteBundles as jest.Mock).mockClear();
            spectator.component.onRemoveBundles();
            expect(service.deleteBundles).not.toHaveBeenCalled();
            expect(spectator.component.validationWarningKey()).toBe(
                'publishing-queue.select-bundle.warning.select-one'
            );
        });
    });

    describe('inline download menu', () => {
        beforeEach(() => {
            spectator.detectChanges();
            spectator.component.onCheckedChange([{ id: 'bundle-2', name: 'Blog content sync' }]);
        });

        it('builds a 2-level menu: To Publish (with filters) + To Unpublish (leaf)', () => {
            const items = spectator.component.downloadMenuItems();
            expect(items.length).toBe(2);
            // To Publish parent has the 3 fixture filters as children.
            expect(items[0].items?.length).toBe(3);
            expect(items[0].items?.map((i) => i.label)).toEqual([
                'Force Push Everything',
                'Only Selected Items',
                'Content, Assets and Pages'
            ]);
            // To Unpublish is a leaf (no children, direct command).
            expect(items[1].items).toBeUndefined();
            expect(items[1].command).toBeDefined();
        });

        it('picking a filter leaf calls generateBundle(bundleId, "0", filterKey)', () => {
            const items = spectator.component.downloadMenuItems();
            const forcePush = items[0].items?.find((i) => i.label === 'Force Push Everything');
            forcePush?.command?.({} as never);
            expect(service.generateBundle).toHaveBeenCalledWith('bundle-2', '0', 'ForcePush.yml');
            expect(mockAnchorClick).toHaveBeenCalled();
        });

        it('picking To Unpublish calls generateBundle(bundleId, "1", "")', () => {
            const items = spectator.component.downloadMenuItems();
            items[1].command?.({} as never);
            expect(service.generateBundle).toHaveBeenCalledWith('bundle-2', '1', '');
            expect(mockAnchorClick).toHaveBeenCalled();
        });

        it('toggles isDownloading around the network call', () => {
            const subject = new Subject<{ blob: Blob; filename: string }>();
            (service.generateBundle as jest.Mock).mockReturnValueOnce(subject.asObservable());

            expect(spectator.component.isDownloading()).toBe(false);
            spectator.component.onDownloadOption('1', '');
            expect(spectator.component.isDownloading()).toBe(true);
            subject.next({ blob: new Blob(['x']), filename: 'bundle-2.tar.gz' });
            subject.complete();
            expect(spectator.component.isDownloading()).toBe(false);
        });

        it('hands errors off to DotHttpErrorManagerService and releases isDownloading', () => {
            const error = new Error('boom');
            (service.generateBundle as jest.Mock).mockReturnValueOnce(throwError(() => error));
            spectator.component.onDownloadOption('0', 'ForcePush.yml');
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(spectator.component.isDownloading()).toBe(false);
            expect(mockAnchorClick).not.toHaveBeenCalled();
        });

        it('is a no-op when no bundles are checked', () => {
            spectator.component.checkedBundleIds.set([]);
            spectator.component.onDownloadOption('1', '');
            expect(service.generateBundle).not.toHaveBeenCalled();
        });

        it('is a no-op when more than one bundle is checked', () => {
            spectator.component.onCheckedChange([
                { id: 'bundle-1', name: 'a' },
                { id: 'bundle-2', name: 'b' }
            ]);
            spectator.component.onDownloadOption('1', '');
            expect(service.generateBundle).not.toHaveBeenCalled();
        });

        it('does not re-fire while a download is already in flight', () => {
            spectator.component.isDownloading.set(true);
            spectator.component.onDownloadOption('1', '');
            expect(service.generateBundle).not.toHaveBeenCalled();
        });

        it('disables the To Publish item when no filters loaded yet', () => {
            // Fresh component instance with the filters service returning nothing
            // would yield an empty filter list. Here we simulate that state
            // directly on the signal to keep the rest of the suite stable.
            spectator.component.downloadFilters.set([]);
            const items = spectator.component.downloadMenuItems();
            expect(items[0].disabled).toBe(true);
            // To Unpublish stays usable.
            expect(items[1].command).toBeDefined();
        });
    });

    describe('configure step transition', () => {
        beforeEach(() => spectator.detectChanges());

        it('Configure → transitions step from "select" to "configure" when bundles are checked', () => {
            spectator.component.onCheckedChange([
                { id: 'bundle-1', name: 'Spring campaign refresh' }
            ]);
            spectator.component.onOpenConfigureStep();
            expect(spectator.component.step()).toBe('configure');
        });

        it('Configure → does not transition and sets the "select one" warning when nothing is checked', () => {
            spectator.component.checkedBundleIds.set([]);
            spectator.component.onOpenConfigureStep();
            expect(spectator.component.step()).toBe('select');
            expect(spectator.component.validationWarningKey()).toBe(
                'publishing-queue.select-bundle.warning.select-one'
            );
        });

        it('clicking Download with nothing checked sets the "select one" warning and does not open the menu', () => {
            spectator.component.checkedBundleIds.set([]);
            const toggleSpy = jest.fn();
            // Stub the menu's toggle so we can assert it was NOT called.
            (
                spectator.component as unknown as {
                    downloadMenuRef: () => { toggle: jest.Mock };
                }
            ).downloadMenuRef = () => ({ toggle: toggleSpy });

            spectator.component.onDownloadButtonClick({
                currentTarget: document.createElement('button')
            } as unknown as MouseEvent);

            expect(toggleSpy).not.toHaveBeenCalled();
            expect(spectator.component.validationWarningKey()).toBe(
                'publishing-queue.select-bundle.warning.select-one'
            );
        });

        it('clicking Download with multiple checked sets the "single only" warning and does not open the menu', () => {
            spectator.component.onCheckedChange([
                { id: 'bundle-1', name: 'a' },
                { id: 'bundle-2', name: 'b' }
            ]);
            const toggleSpy = jest.fn();
            (
                spectator.component as unknown as {
                    downloadMenuRef: () => { toggle: jest.Mock };
                }
            ).downloadMenuRef = () => ({ toggle: toggleSpy });

            spectator.component.onDownloadButtonClick({
                currentTarget: document.createElement('button')
            } as unknown as MouseEvent);

            expect(toggleSpy).not.toHaveBeenCalled();
            expect(spectator.component.validationWarningKey()).toBe(
                'publishing-queue.select-bundle.download.single-only'
            );
        });

        it('changing the selection clears any active warning', () => {
            spectator.component.validationWarningKey.set(
                'publishing-queue.select-bundle.warning.select-one'
            );
            spectator.component.onCheckedChange([{ id: 'bundle-1', name: 'a' }]);
            expect(spectator.component.validationWarningKey()).toBeNull();
        });

        it('renders the warning element in the footer when validationWarningKey is set', () => {
            spectator.detectChanges();
            spectator.component.validationWarningKey.set(
                'publishing-queue.select-bundle.warning.select-one'
            );
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-select-bundle-warning'))).toBeTruthy();
        });

        it('Back to list → reverts step to "select"', () => {
            spectator.component.onCheckedChange([
                { id: 'bundle-1', name: 'Spring campaign refresh' }
            ]);
            spectator.component.onOpenConfigureStep();
            expect(spectator.component.step()).toBe('configure');
            spectator.component.onBackToList();
            expect(spectator.component.step()).toBe('select');
        });

        it('configureFormData carries the first checked bundle id and a count title for N>1', () => {
            spectator.component.onCheckedChange([
                { id: 'bundle-1', name: 'Spring campaign refresh' },
                { id: 'bundle-2', name: 'Blog content sync' }
            ]);
            const data = spectator.component.configureFormData();
            expect(data.assetIdentifier).toBe('bundle-1');
            expect(data.title).toBe('2 bundles');
            expect(data.isBundle).toBe(true);
        });

        it('configureFormData uses the bundle name as the title for a single checked bundle', () => {
            spectator.component.onCheckedChange([{ id: 'bundle-2', name: 'Blog content sync' }]);
            const data = spectator.component.configureFormData();
            expect(data.assetIdentifier).toBe('bundle-2');
            expect(data.title).toBe('Blog content sync');
        });
    });

    describe('send (fan-out push)', () => {
        const validForm = {
            pushActionSelected: 'publish',
            publishDate: new Date('2026-07-01T10:00:00Z').toString(),
            expireDate: new Date('2026-08-01T10:00:00Z').toString(),
            environment: ['env-1'],
            filterKey: 'default.yml',
            timezoneId: 'UTC'
        };

        function primeSendableState(ids: string[]) {
            spectator.detectChanges();
            spectator.component.onCheckedChange(ids.map((id) => ({ id, name: id })));
            spectator.component.onOpenConfigureStep();
            spectator.component.onConfigureFormValue(validForm as never);
            spectator.component.onConfigureFormValid(true);
        }

        it('Send is disabled until the embedded form reports valid', () => {
            spectator.detectChanges();
            spectator.component.onCheckedChange([{ id: 'bundle-1', name: 'a' }]);
            spectator.component.onOpenConfigureStep();
            // form has not emitted (value=null, valid=false) → canSend = false
            expect(spectator.component.canSend()).toBe(false);
        });

        it('fans out one pushBundle call per checked bundle and closes the dialog on success', () => {
            primeSendableState(['bundle-1', 'bundle-2']);

            spectator.component.onSend();

            expect(service.pushBundle).toHaveBeenCalledTimes(2);
            expect(service.pushBundle).toHaveBeenCalledWith(
                'bundle-1',
                expect.objectContaining({ operation: 'publish' })
            );
            expect(service.pushBundle).toHaveBeenCalledWith(
                'bundle-2',
                expect.objectContaining({ operation: 'publish' })
            );
            expect(dialogRef.close).toHaveBeenCalled();
        });

        it('surfaces a partial-failure toast and keeps the dialog open if any push fails', () => {
            primeSendableState(['bundle-1', 'bundle-2']);
            (service.pushBundle as jest.Mock).mockImplementationOnce(() =>
                of({
                    bundleId: 'bundle-1',
                    operation: 'publish',
                    publishDate: null,
                    expireDate: null,
                    environments: ['env-1'],
                    filterKey: 'default.yml'
                })
            );
            (service.pushBundle as jest.Mock).mockImplementationOnce(() =>
                throwError(() => new Error('boom'))
            );

            spectator.component.onSend();

            expect(globalMessage.error).toHaveBeenCalled();
            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('is a no-op when nothing is checked OR the form is invalid', () => {
            spectator.detectChanges();
            spectator.component.checkedBundleIds.set([]);
            spectator.component.onConfigureFormValid(true);
            spectator.component.onSend();
            expect(service.pushBundle).not.toHaveBeenCalled();

            spectator.component.onCheckedChange([{ id: 'bundle-1', name: 'a' }]);
            spectator.component.onConfigureFormValid(false);
            spectator.component.onSend();
            expect(service.pushBundle).not.toHaveBeenCalled();
        });

        it('toggles isSending around the network calls', () => {
            const subject = new Subject<unknown>();
            (service.pushBundle as jest.Mock).mockReturnValueOnce(subject.asObservable());
            primeSendableState(['bundle-1']);

            expect(spectator.component.isSending()).toBe(false);
            spectator.component.onSend();
            expect(spectator.component.isSending()).toBe(true);
            subject.next({
                bundleId: 'bundle-1',
                operation: 'publish',
                publishDate: null,
                expireDate: null,
                environments: ['env-1'],
                filterKey: 'default.yml'
            });
            subject.complete();
            expect(spectator.component.isSending()).toBe(false);
        });
    });

    describe('layout', () => {
        it('renders the two panes + action bar in the select step', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-select-bundle-left'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-select-bundle-right'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-select-bundle-actions'))).toBeTruthy();
        });

        it('renders bundle rows', () => {
            spectator.detectChanges();
            expect(spectator.queryAll(byTestId('pq-select-bundle-row')).length).toBe(2);
        });

        it('renders the configure header, body, and footer in the configure step', () => {
            spectator.detectChanges();
            spectator.component.onCheckedChange([
                { id: 'bundle-1', name: 'Spring campaign refresh' }
            ]);
            spectator.component.onOpenConfigureStep();
            spectator.detectChanges();
            expect(spectator.query(byTestId('pq-select-bundle-configure-header'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-select-bundle-configure-body'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-select-bundle-configure-footer'))).toBeTruthy();
            // Select-step content is no longer in the DOM.
            expect(spectator.query(byTestId('pq-select-bundle-actions'))).toBeFalsy();
        });
    });
});
