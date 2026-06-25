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
    DotPushPublishFiltersService
} from '@dotcms/data-access';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
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

describe('DotPublishingQueueSelectBundleDialogComponent', () => {
    let spectator: Spectator<DotPublishingQueueSelectBundleDialogComponent>;
    let service: jest.Mocked<DotPublishingQueueService>;
    let confirmationService: jest.Mocked<ConfirmationService>;
    let downloadService: jest.Mocked<DotDownloadBundleDialogService>;
    let dialogRef: jest.Mocked<DynamicDialogRef>;
    let globalMessage: jest.Mocked<DotGlobalMessageService>;

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
                )
            }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest
                    .fn()
                    .mockReturnValue(of({ userId: 'dotcms.org.1', email: 'admin@dotcms.com' }))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotDownloadBundleDialogService, { open: jest.fn() }),
            mockProvider(DotContentTypeService, {
                getContentType: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotGlobalMessageService, { error: jest.fn() }),
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            // The dialog provides DotPushPublishFiltersService at the component level
            // (mirrors the legacy DotPushPublishDialogComponent). The embedded
            // <dot-push-publish-form> calls .get() on it during ngOnInit.
            mockProvider(DotPushPublishFiltersService, {
                get: jest.fn().mockReturnValue(of([]))
            }),
            // The form also calls DotcmsConfigService.getTimeZones() during ngOnInit.
            mockProvider(DotcmsConfigService, {
                getTimeZones: jest.fn().mockReturnValue(of([]))
            }),
            // The form injects DotParseHtmlService (only used when `customCode` is in
            // data; we don't pass that, but the DI lookup happens unconditionally).
            mockProvider(DotParseHtmlService, { parse: jest.fn() }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
    });

    beforeEach(() => {
        spectator = createComponent();
        service = spectator.inject(
            DotPublishingQueueService
        ) as jest.Mocked<DotPublishingQueueService>;
        downloadService = spectator.inject(
            DotDownloadBundleDialogService
        ) as jest.Mocked<DotDownloadBundleDialogService>;
        dialogRef = spectator.inject(DynamicDialogRef) as jest.Mocked<DynamicDialogRef>;
        globalMessage = spectator.inject(
            DotGlobalMessageService
        ) as jest.Mocked<DotGlobalMessageService>;
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

        it('is a no-op when nothing is checked', () => {
            spectator.detectChanges();
            (service.deleteBundles as jest.Mock).mockClear();
            spectator.component.onRemoveBundles();
            expect(service.deleteBundles).not.toHaveBeenCalled();
        });
    });

    describe('download (gated to a single checked bundle)', () => {
        beforeEach(() => spectator.detectChanges());

        it('opens the download dialog for the only checked bundle', () => {
            spectator.component.onCheckedChange([{ id: 'bundle-2', name: 'Blog content sync' }]);
            spectator.component.onDownloadChecked();
            expect(downloadService.open).toHaveBeenCalledWith('bundle-2');
        });

        it('is a no-op when no bundles are checked', () => {
            spectator.component.checkedBundleIds.set([]);
            spectator.component.onDownloadChecked();
            expect(downloadService.open).not.toHaveBeenCalled();
        });

        it('is a no-op when more than one bundle is checked', () => {
            spectator.component.onCheckedChange([
                { id: 'bundle-1', name: 'a' },
                { id: 'bundle-2', name: 'b' }
            ]);
            spectator.component.onDownloadChecked();
            expect(downloadService.open).not.toHaveBeenCalled();
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

        it('Configure → is a no-op when nothing is checked (step stays "select")', () => {
            spectator.component.checkedBundleIds.set([]);
            spectator.component.onOpenConfigureStep();
            expect(spectator.component.step()).toBe('select');
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
