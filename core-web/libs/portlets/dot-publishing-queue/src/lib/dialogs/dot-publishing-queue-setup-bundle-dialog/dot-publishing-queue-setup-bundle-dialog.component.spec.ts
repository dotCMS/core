import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { MockComponent } from 'ng-mocks';
import { Subject, of, throwError } from 'rxjs';
import { Mock, MockInstance, Mocked, vi } from 'vitest';

import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

/* eslint-disable @nx/enforce-module-boundaries */

import { DotPushPublishFormComponent } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.component';
import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPublishingQueueService,
    DotPushPublishFiltersService,
    PushPublishService
} from '@dotcms/data-access';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotParseHtmlService } from '@services/dot-parse-html/dot-parse-html.service';

import { DotPublishingQueueSetupBundleDialogComponent } from './dot-publishing-queue-setup-bundle-dialog.component';

const MOCK_ASSETS = [
    { asset: 'a1', title: 'Spring Sale Landing', type: 'contentlet' },
    { asset: 'a2', title: 'hero-spring.jpg', type: 'contentlet' }
];

const PUSH_RESULT = {
    bundleId: 'bundle-1',
    operation: 'publish',
    publishDate: null,
    expireDate: null,
    environments: ['env-1'],
    filterKey: 'default.yml'
};

// Mock the blob-download helper so the click side-effect is observable in tests.
const mockAnchorClick = vi.fn();
vi.mock('@dotcms/utils', async () => {
    const actual = await vi.importActual('@dotcms/utils');
    return {
        ...actual,
        getDownloadLink: vi.fn(() => ({ click: mockAnchorClick }) as unknown as HTMLAnchorElement)
    };
});

/** The PrimeNG buttons rendered with the given `data-testid`. */
const buttonsByTestId = (
    spectator: Spectator<DotPublishingQueueSetupBundleDialogComponent>,
    testId: string
): Button[] =>
    spectator
        .queryAll(Button)
        .filter((button) => button.el.nativeElement.dataset.testid === testId);

/** Clicks the native `<button>` inside a `<p-button>` host — `(onClick)` is bound there. */
const clickButton = (
    spectator: Spectator<DotPublishingQueueSetupBundleDialogComponent>,
    testId: string
): void => {
    const host = spectator.query(byTestId(testId));
    spectator.click((host?.querySelector('button') ?? host) as HTMLElement);
};

const DEFAULT_DIALOG_DATA = { bundleId: 'bundle-1', bundleName: 'Spring campaign refresh' };
let dialogData = DEFAULT_DIALOG_DATA;

describe('DotPublishingQueueSetupBundleDialogComponent', () => {
    let spectator: Spectator<DotPublishingQueueSetupBundleDialogComponent>;
    let service: Mocked<DotPublishingQueueService>;
    let confirmationService: Mocked<ConfirmationService>;
    let dialogRef: Mocked<DynamicDialogRef>;
    let httpErrorManager: Mocked<DotHttpErrorManagerService>;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueSetupBundleDialogComponent,
        providers: [
            mockProvider(DotPublishingQueueService, {
                getBundleAssets: vi.fn().mockReturnValue(of(MOCK_ASSETS)),
                removeAssetsFromBundle: vi
                    .fn()
                    .mockReturnValue(of([{ assetId: 'a1', success: true, message: 'ok' }])),
                deleteBundle: vi.fn().mockReturnValue(of({ message: 'ok' })),
                pushBundle: vi.fn().mockReturnValue(of(PUSH_RESULT)),
                generateBundle: vi
                    .fn()
                    .mockReturnValue(of({ blob: new Blob(['x']), filename: 'bundle.tar.gz' }))
            }),
            {
                provide: DynamicDialogConfig,
                // Getter so a test can swap the data before creating its own component.
                useValue: {
                    get data() {
                        return dialogData;
                    }
                }
            },
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() }),
            mockProvider(DotContentTypeService, {
                getContentType: vi.fn().mockReturnValue(of({}))
            }),
            mockProvider(DynamicDialogRef, { close: vi.fn() }),
            // The dialog provides DotPushPublishFiltersService at the component
            // level (mirrors the legacy DotPushPublishDialogComponent). Both the
            // embedded <dot-push-publish-form> AND the inline Download menu call
            // .get() on it during ngOnInit.
            mockProvider(DotPushPublishFiltersService, {
                get: vi.fn().mockReturnValue(
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
                getTimeZones: vi.fn().mockReturnValue(of([]))
            }),
            // The form injects DotParseHtmlService (only used when `customCode` is in
            // data; we don't pass that, but the DI lookup happens unconditionally).
            mockProvider(DotParseHtmlService, { parse: vi.fn() }),
            // PushPublishEnvSelectorComponent (rendered inside the embedded form)
            // injects PushPublishService for the "remember last push" env list.
            mockProvider(PushPublishService, {
                getEnvironments: vi.fn().mockReturnValue(of([])),
                pushPublishContent: vi.fn().mockReturnValue(of({}))
            }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ],
        // Swap the embedded push-publish form for a minimal stub. The real
        // component lives in apps/dotcms-ui and drives an internal reactive form
        // we don't exercise here — mocking it lets a misspelled selector, wrong
        // data-testid or missing import actually fail a test.
        overrideComponents: [
            [
                DotPublishingQueueSetupBundleDialogComponent,
                {
                    remove: { imports: [DotPushPublishFormComponent] },
                    add: { imports: [MockComponent(DotPushPublishFormComponent)] }
                }
            ]
        ]
    });

    beforeEach(() => {
        dialogData = DEFAULT_DIALOG_DATA;
        mockAnchorClick.mockClear();
        spectator = createComponent();
        service = spectator.inject(DotPublishingQueueService) as Mocked<DotPublishingQueueService>;
        dialogRef = spectator.inject(DynamicDialogRef) as Mocked<DynamicDialogRef>;
        httpErrorManager = spectator.inject(
            DotHttpErrorManagerService
        ) as Mocked<DotHttpErrorManagerService>;
        confirmationService = spectator.inject(
            ConfirmationService,
            true
        ) as Mocked<ConfirmationService>;
        vi.spyOn(confirmationService, 'confirm').mockImplementation((cfg) => {
            cfg.accept?.();
            return confirmationService;
        });
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('init', () => {
        it("loads the bundle's items and starts on the items step", () => {
            expect(service.getBundleAssets).toHaveBeenCalledWith('bundle-1');
            expect(spectator.component.$step()).toBe('items');
            expect(spectator.queryAll(byTestId('pq-setup-bundle-asset-row')).length).toBe(2);
        });

        it('shows the bundle name, item count and id in the header', () => {
            expect(spectator.query(byTestId('pq-setup-bundle-title'))).toHaveText(
                'Spring campaign refresh'
            );
            expect(spectator.query(byTestId('pq-setup-bundle-count'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-setup-bundle-id'))).toHaveText('bundle-1');
        });

        it('falls back to the bundle id when the bundle has no name', () => {
            dialogData = { bundleId: 'bundle-9', bundleName: '' };
            spectator = createComponent();
            expect(spectator.component.bundleName).toBe('bundle-9');
        });

        it('renders the empty state when the bundle has no items', () => {
            (service.getBundleAssets as Mock).mockReturnValueOnce(of([]));
            spectator = createComponent();
            expect(spectator.query(byTestId('pq-setup-bundle-asset-empty'))).toBeTruthy();
        });

        it('hands a failed item load to the error manager', () => {
            const error = new Error('boom');
            (service.getBundleAssets as Mock).mockReturnValueOnce(throwError(() => error));
            spectator = createComponent();
            httpErrorManager = spectator.inject(
                DotHttpErrorManagerService
            ) as Mocked<DotHttpErrorManagerService>;
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(spectator.component.$assetsStatus()).toBe('error');
        });
    });

    describe('type icon', () => {
        it('maps known asset types to Material Symbol names', () => {
            expect(spectator.component.typeIcon('contentlet')).toBe('description');
            expect(spectator.component.typeIcon('template')).toBe('open_in_new');
        });

        it('falls back to a generic icon for unknown types', () => {
            expect(spectator.component.typeIcon('weird-type')).toBe('description');
        });
    });

    describe('asset row click → opens content editor', () => {
        let openSpy: MockInstance;

        beforeEach(() => {
            openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);
        });

        afterEach(() => openSpy.mockRestore());

        it('opens the resolved edit URL in a new tab when the row has one', () => {
            spectator.component.$assetEditUrls.set(new Map([['a1', '/edit/contentlet/a1']]));
            spectator.component.onSelectAssetRow(MOCK_ASSETS[0]);
            expect(openSpy).toHaveBeenCalledWith('/edit/contentlet/a1', '_blank', 'noopener');
        });

        it('is a no-op when the asset has no resolved edit URL', () => {
            spectator.component.$assetEditUrls.set(new Map());
            spectator.component.onSelectAssetRow({ asset: 'a1', title: 'x', type: 'template' });
            expect(openSpy).not.toHaveBeenCalled();
        });
    });

    describe('remove asset', () => {
        it('renders the per-row delete icon with the neutral secondary severity, not danger', () => {
            const buttons = buttonsByTestId(spectator, 'pq-setup-bundle-asset-delete');
            expect(buttons.length).toBe(2);
            buttons.forEach((button) => expect(button.severity).toBe('secondary'));
        });

        it('confirms then calls removeAssetsFromBundle and reloads the items', () => {
            (service.getBundleAssets as Mock).mockClear();

            spectator.component.onRemoveAsset(MOCK_ASSETS[0]);

            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(service.removeAssetsFromBundle).toHaveBeenCalledWith('bundle-1', ['a1']);
            expect(service.getBundleAssets).toHaveBeenCalledWith('bundle-1');
        });

        it('on service error: hands off to httpErrorManager', () => {
            const error = new Error('boom');
            (service.removeAssetsFromBundle as Mock).mockReturnValueOnce(throwError(() => error));
            spectator.component.onRemoveAsset(MOCK_ASSETS[0]);
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
        });
    });

    describe('remove bundle', () => {
        it('confirms, deletes the bundle and closes the dialog', () => {
            clickButton(spectator, 'pq-setup-bundle-remove-btn');

            expect(confirmationService.confirm).toHaveBeenCalled();
            expect(service.deleteBundle).toHaveBeenCalledWith('bundle-1');
            expect(dialogRef.close).toHaveBeenCalled();
        });

        it('keeps the dialog open and reports the error when the delete fails', () => {
            const error = new Error('boom');
            (service.deleteBundle as Mock).mockReturnValueOnce(throwError(() => error));

            spectator.component.onRemoveBundle();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(dialogRef.close).not.toHaveBeenCalled();
        });
    });

    describe('download step', () => {
        const validForm = {
            bundleId: 'bundle-1',
            operation: '0' as const,
            filterKey: 'ForcePush.yml'
        };

        function primeDownloadableState() {
            spectator.component.onOpenDownloadStep();
            spectator.component.onDownloadFormValue(validForm);
            spectator.component.onDownloadFormValid(true);
        }

        it('Download → moves to the download step', () => {
            clickButton(spectator, 'pq-setup-bundle-download-btn');
            expect(spectator.component.$step()).toBe('download');
        });

        it('onDownload calls generateBundle for this bundle and clicks the download link', () => {
            primeDownloadableState();
            spectator.component.onDownload();
            expect(service.generateBundle).toHaveBeenCalledWith('bundle-1', '0', 'ForcePush.yml');
            expect(mockAnchorClick).toHaveBeenCalled();
        });

        it('onDownload returns to the items step on success', () => {
            primeDownloadableState();
            spectator.component.onDownload();
            expect(spectator.component.$step()).toBe('items');
        });

        it('toggles isDownloading around the network call', () => {
            const subject = new Subject<{ blob: Blob; filename: string }>();
            (service.generateBundle as Mock).mockReturnValueOnce(subject.asObservable());
            primeDownloadableState();

            spectator.component.onDownload();
            expect(spectator.component.$isDownloading()).toBe(true);
            subject.next({ blob: new Blob(['x']), filename: 'bundle-1.tar.gz' });
            subject.complete();
            expect(spectator.component.$isDownloading()).toBe(false);
        });

        it('hands errors off to DotHttpErrorManagerService and releases isDownloading', () => {
            const error = new Error('boom');
            (service.generateBundle as Mock).mockReturnValueOnce(throwError(() => error));
            primeDownloadableState();
            spectator.component.onDownload();
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(spectator.component.$isDownloading()).toBe(false);
            expect(mockAnchorClick).not.toHaveBeenCalled();
        });

        it('is a no-op when the form is invalid', () => {
            spectator.component.onOpenDownloadStep();
            spectator.component.onDownload();
            expect(service.generateBundle).not.toHaveBeenCalled();
        });

        it('does not re-fire while a download is already in flight', () => {
            const pending = new Subject<{ blob: Blob; filename: string }>();
            (service.generateBundle as Mock).mockReturnValueOnce(pending.asObservable());
            primeDownloadableState();

            spectator.component.onDownload();
            (service.generateBundle as Mock).mockClear();
            spectator.component.onDownload();
            expect(service.generateBundle).not.toHaveBeenCalled();
        });
    });

    describe('configure step', () => {
        const validForm = {
            pushActionSelected: 'publish',
            publishDate: new Date('2026-07-01T10:00:00Z').toString(),
            expireDate: new Date('2026-08-01T10:00:00Z').toString(),
            environment: ['env-1'],
            filterKey: 'default.yml',
            timezoneId: 'UTC'
        };

        function primeSendableState() {
            spectator.component.onOpenConfigureStep();
            spectator.component.onConfigureFormValue(validForm as never);
            spectator.component.onConfigureFormValid(true);
        }

        it('Configure → moves to the configure step and renders the form, title and footer', () => {
            clickButton(spectator, 'pq-setup-bundle-configure-btn');
            spectator.detectChanges();

            expect(spectator.component.$step()).toBe('configure');
            expect(spectator.query(byTestId('pq-setup-bundle-configure-title'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-setup-bundle-configure-body'))).toBeTruthy();
            expect(spectator.query(byTestId('pq-setup-bundle-configure-footer'))).toBeTruthy();
        });

        it('feeds the push-publish form this bundle as a bundle push', () => {
            expect(spectator.component.configureFormData).toEqual({
                assetIdentifier: 'bundle-1',
                title: 'Spring campaign refresh',
                isBundle: true
            });
        });

        it('Back to items → returns to the items step', () => {
            spectator.component.onOpenConfigureStep();
            spectator.component.onBackToItems();
            expect(spectator.component.$step()).toBe('items');
        });

        it('Send with an incomplete form surfaces a warning instead of pushing', () => {
            spectator.component.onOpenConfigureStep();
            spectator.detectChanges();

            spectator.component.onSend();
            spectator.detectChanges();

            expect(service.pushBundle).not.toHaveBeenCalled();
            expect(spectator.query(byTestId('pq-setup-bundle-configure-warning'))).toBeTruthy();
        });

        it('clears the form-invalid warning once the form reports valid', () => {
            spectator.component.onOpenConfigureStep();
            spectator.component.onSend();
            expect(spectator.component.$validationWarningKey()).toBe(
                'publishing-queue.setup-bundle.warning.form-invalid'
            );
            spectator.component.onConfigureFormValid(true);
            expect(spectator.component.$validationWarningKey()).toBeNull();
        });

        it('pushes this bundle with the form values and closes the dialog on success', () => {
            primeSendableState();

            spectator.component.onSend();

            expect(service.pushBundle).toHaveBeenCalledTimes(1);
            expect(service.pushBundle).toHaveBeenCalledWith(
                'bundle-1',
                expect.objectContaining({
                    operation: 'publish',
                    environments: ['env-1'],
                    filterKey: 'default.yml'
                })
            );
            expect(dialogRef.close).toHaveBeenCalled();
        });

        it('keeps the dialog open and reports the error when the push fails', () => {
            const error = new Error('boom');
            (service.pushBundle as Mock).mockReturnValueOnce(throwError(() => error));
            primeSendableState();

            spectator.component.onSend();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(dialogRef.close).not.toHaveBeenCalled();
            expect(spectator.component.$isSending()).toBe(false);
        });

        it('toggles isSending around the network call', () => {
            const subject = new Subject<unknown>();
            (service.pushBundle as Mock).mockReturnValueOnce(subject.asObservable());
            primeSendableState();

            spectator.component.onSend();
            expect(spectator.component.$isSending()).toBe(true);
            subject.next(PUSH_RESULT);
            subject.complete();
            expect(spectator.component.$isSending()).toBe(false);
        });
    });

    it('the header X closes the dialog', () => {
        clickButton(spectator, 'pq-setup-bundle-close-btn');
        expect(dialogRef.close).toHaveBeenCalled();
    });
});
