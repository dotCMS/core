import { createServiceFactory, SpectatorService } from '@openng/spectator';
import { MockComponent } from 'ng-mocks';
import { Observable, of, Subject, throwError } from 'rxjs';
import { expect, vi } from 'vitest';

import { signal } from '@angular/core';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotMessageService,
    DotPropertiesService,
    DotSiteService,
    DotUploadFileService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotSite } from '@dotcms/dotcms-models';
import {
    ASSET_PICKER_LAUNCHER,
    AngularAssetPickerLauncher,
    DotAssetPickerComponent
} from '@dotcms/ui';
import { EMPTY_CONTENTLET } from '@dotcms/utils-testing';

import { DotWysiwygPluginService } from './dot-wysiwyg-plugin.service';
import { formatDotImageNode } from './utils/editor.utils';

import { DotEditContentStore } from '../../../store/edit-content.store';

/**
 * This Mock is used to check we are sending the correct configuration to the editor
 * No need to mock all the methods and properties of the Editor
 * Some methods are customized to check the configuration
 */
class MockEditor {
    private customButtons = {};
    private events = {};

    ui = {
        registry: {
            getAll: () => {
                return {
                    buttons: this.customButtons
                };
            },
            addButton: (name, config) => {
                this.customButtons[name] = config;
            }
        }
    };

    on = (name, fn) => {
        if (!this.events[name]) {
            this.events[name] = [fn];

            return;
        }

        this.events[name].push(fn);
    };

    fakeOnCall = (name, event) => {
        this.events[name].forEach((fn) => fn(event));
    };

    insertContent = vi.fn();

    focus = vi.fn();
}

const MOCK_IMAGE_URL_PATTERN = '/dA/{shortyId}/{name}?language_id={languageId}';

const SITE: DotSite = {
    identifier: 'site-1',
    hostname: 'dotcms.com',
    aliases: null,
    archived: false
};

const LOCALE_ID = 2;

/**
 * Swapped per test so the site lookup can be resolved, deferred or failed. Read lazily by the mock
 * because `currentSite$` defers the call until the first picker opens.
 */
let siteSource: Observable<DotSite>;

describe('DotWysiwygPluginService', () => {
    let spectator: SpectatorService<DotWysiwygPluginService>;
    let dialogService: DialogService;
    let dotUploadFileService: DotUploadFileService;
    let dotPropertiesService: DotPropertiesService;
    /**
     * `any` is used here because the Editor is a complex object that we don't need to mock all the methods and properties
     * This mock also contains some custom methods to check the configuration
     * We are using this mock to check the configuration of the editor
     */
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let editor: any;

    const createService = createServiceFactory({
        service: DotWysiwygPluginService,
        declarations: [MockComponent(DotAssetPickerComponent)],
        providers: [
            DialogService,
            {
                provide: DotPropertiesService,
                useValue: {
                    getKey: vi.fn().mockReturnValue(of(MOCK_IMAGE_URL_PATTERN))
                }
            },
            {
                provide: DotUploadFileService,
                useValue: {
                    publishContent: vi.fn()
                }
            },
            {
                provide: DotSiteService,
                useValue: { getCurrentSite: vi.fn(() => siteSource) }
            },
            {
                provide: DotMessageService,
                useValue: { get: vi.fn((key: string) => key) }
            },
            {
                provide: DotEditContentStore,
                useValue: {
                    hasAttemptedSubmit: signal(false),
                    currentLocale: signal({ id: LOCALE_ID })
                }
            },
            // Angular Edit Content host: the launcher is what makes the new picker the picker.
            // Its legacy counterpart lives in `dot-wysiwyg-plugin.service.legacy-host.spec.ts`.
            { provide: ASSET_PICKER_LAUNCHER, useClass: AngularAssetPickerLauncher }
        ]
    });

    beforeEach(() => {
        // The `useValue` mocks are one object shared by every `createService()` in this file, so call
        // counts accumulate across tests unless they are cleared first. Before the service is built,
        // since its constructor is itself a call worth counting.
        vi.clearAllMocks();
        siteSource = of(SITE);
        spectator = createService();
        dialogService = spectator.inject(DialogService);
        dotUploadFileService = spectator.inject(DotUploadFileService);
        dotPropertiesService = spectator.inject(DotPropertiesService);
        editor = new MockEditor();
    });

    /** Clicks the toolbar button the plugin registers. */
    const clickAddImage = () => {
        spectator.service.initializePlugins(editor);
        editor.ui.registry.getAll().buttons['dotAddImage'].onAction();
    };

    it('should request the image URL pattern', () => {
        expect(dotPropertiesService.getKey).toHaveBeenCalledWith('WYSIWYG_IMAGE_URL_PATTERN');
    });

    describe('dotImagePlugin', () => {
        it('should configure the dotAddImage button', () => {
            const spyButton = vi.spyOn(editor.ui.registry, 'addButton');
            const spyOn = vi.spyOn(editor, 'on');

            spectator.service.initializePlugins(editor);

            expect(spyOn).toHaveBeenCalledWith('drop', expect.any(Function));
            expect(spyButton).toHaveBeenCalledWith('dotAddImage', {
                icon: 'image',
                // TinyMCE turns `tooltip` into the button's `aria-label` and `title`. It is the only
                // accessible name an icon-only button gets, so it is part of the contract.
                tooltip: 'insert-image',
                onAction: expect.any(Function)
            });
        });

        it('should open the shared asset picker when the button is clicked', () => {
            const spyDialog = vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose: of(EMPTY_CONTENTLET)
            } as DynamicDialogRef);

            const spyEditorInserContent = vi.spyOn(editor, 'insertContent');

            clickAddImage();

            expect(spyDialog).toHaveBeenCalledWith(
                DotAssetPickerComponent,
                // The dialog flags are the picker's own contract, asserted in its spec. What matters
                // here is the payload this field is responsible for.
                expect.objectContaining({
                    showHeader: false,
                    data: expect.objectContaining({
                        site: SITE,
                        mimeTypes: ['image/*'],
                        languageId: String(LOCALE_ID),
                        title: 'dot.asset.picker.header.image'
                    })
                })
            );
            expect(spyEditorInserContent).toHaveBeenCalledWith(
                formatDotImageNode(MOCK_IMAGE_URL_PATTERN, EMPTY_CONTENTLET)
            );
            // Focus returns to the editor after inserting an image
            expect(editor.focus).toHaveBeenCalled();
        });

        it('should NOT insert content when the dialog is closed without selecting an image', () => {
            const spyDialog = vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose: of(undefined)
            } as DynamicDialogRef);

            const spyEditorInserContent = vi.spyOn(editor, 'insertContent');

            clickAddImage();

            expect(spyDialog).toHaveBeenCalled();
            expect(spyEditorInserContent).not.toHaveBeenCalled();
            // AC5: focus still returns to the editor when the dialog is dismissed
            // without selecting an image (X, Esc or overlay mask)
            expect(editor.focus).toHaveBeenCalled();
        });

        it('should not stack a second picker while the site lookup is still in flight', () => {
            // The site lookup is what makes opening asynchronous, so "is a dialog open" is not a
            // sufficient guard on its own — the ref does not exist yet while it runs.
            const site$ = new Subject<DotSite>();
            siteSource = site$.asObservable();
            spectator = createService();
            const spyDialog = vi
                .spyOn(spectator.inject(DialogService), 'open')
                .mockReturnValue({ onClose: of(undefined) } as DynamicDialogRef);

            spectator.service.initializePlugins(editor);
            const button = editor.ui.registry.getAll().buttons['dotAddImage'];
            button.onAction();
            button.onAction();
            site$.next(SITE);

            expect(spyDialog).toHaveBeenCalledTimes(1);
        });

        it('should open again after the picker closed', () => {
            const spyDialog = vi
                .spyOn(dialogService, 'open')
                .mockReturnValue({ onClose: of(undefined) } as DynamicDialogRef);

            clickAddImage();
            editor.ui.registry.getAll().buttons['dotAddImage'].onAction();

            expect(spyDialog).toHaveBeenCalledTimes(2);
        });

        it('should not open a picker that has nothing to browse', () => {
            siteSource = throwError(() => new Error('no site'));
            spectator = createService();
            const spyDialog = vi.spyOn(spectator.inject(DialogService), 'open');

            spectator.service.initializePlugins(editor);
            editor.ui.registry.getAll().buttons['dotAddImage'].onAction();

            expect(spyDialog).not.toHaveBeenCalled();
        });

        it('should retry the site lookup after it failed', () => {
            siteSource = throwError(() => new Error('no site'));
            spectator = createService();
            const siteService = spectator.inject(DotSiteService);

            spectator.service.initializePlugins(editor);
            const button = editor.ui.registry.getAll().buttons['dotAddImage'];
            button.onAction();
            button.onAction();

            // The busy flag has to be released on error, or the button is dead for the session.
            expect(siteService.getCurrentSite).toHaveBeenCalledTimes(2);
        });

        it('should not request a site until an image is actually inserted', () => {
            // Most editing sessions never insert one; the lookup is deferred until the first click.
            spectator.service.initializePlugins(editor);

            expect(spectator.inject(DotSiteService).getCurrentSite).not.toHaveBeenCalled();
        });

        it('should upload the image when dropped', () => {
            const uploadRespMock: unknown = [{ '1234': EMPTY_CONTENTLET }];
            const spyUpload = vi
                .spyOn(dotUploadFileService, 'publishContent')
                .mockReturnValue(of(uploadRespMock as DotCMSContentlet[]));
            const spyEditorInserContent = vi.spyOn(editor, 'insertContent');

            spectator.service.initializePlugins(editor);

            const dropEvent = {
                dataTransfer: {
                    files: [
                        {
                            type: 'image/png'
                        }
                    ]
                },
                preventDefault: vi.fn(),
                stopImmediatePropagation: vi.fn(),
                stopPropagation: vi.fn()
            };

            editor.fakeOnCall('drop', dropEvent);

            expect(spyUpload).toHaveBeenCalledWith({
                data: dropEvent.dataTransfer.files[0]
            });
            expect(spyEditorInserContent).toHaveBeenCalledWith(
                formatDotImageNode(MOCK_IMAGE_URL_PATTERN, EMPTY_CONTENTLET)
            );

            expect(dropEvent.preventDefault).toHaveBeenCalled();
            expect(dropEvent.stopImmediatePropagation).toHaveBeenCalled();
            expect(dropEvent.stopPropagation).toHaveBeenCalled();
        });

        it('should not upload the image when dropped', () => {
            const uploadRespMock: unknown = [{ '1234': EMPTY_CONTENTLET }];
            const spyUpload = vi
                .spyOn(dotUploadFileService, 'publishContent')
                .mockReturnValue(of(uploadRespMock as DotCMSContentlet[]));
            const spyEditorInserContent = vi.spyOn(editor, 'insertContent');

            spectator.service.initializePlugins(editor);

            const dropEvent = {
                dataTransfer: {
                    files: [
                        {
                            type: 'video/mp4'
                        }
                    ]
                },
                preventDefault: vi.fn(),
                stopImmediatePropagation: vi.fn(),
                stopPropagation: vi.fn()
            };

            editor.fakeOnCall('drop', dropEvent);

            expect(spyUpload).not.toHaveBeenCalledWith({
                data: dropEvent.dataTransfer.files[0]
            });
            expect(spyEditorInserContent).not.toHaveBeenCalledWith(
                formatDotImageNode(MOCK_IMAGE_URL_PATTERN, EMPTY_CONTENTLET)
            );
        });
    });

    describe('filePickerOptions (browse buttons in the image and link dialogs)', () => {
        const FILE_ASSET = {
            ...EMPTY_CONTENTLET,
            baseType: 'FILEASSET',
            path: '/docs/report.pdf',
            title: 'Annual report'
        } as DotCMSContentlet;

        const DOT_ASSET = {
            ...EMPTY_CONTENTLET,
            baseType: 'DOTASSET',
            identifier: 'abcdef1234567890',
            fileName: 'brochure.pdf',
            title: 'Brochure'
        } as DotCMSContentlet;

        /** Clicks the browse button of the TinyMCE dialog that asks for `filetype`. */
        const browse = (filetype: 'image' | 'file', callback = vi.fn()) => {
            spectator.service
                .filePickerOptions()
                .file_picker_callback(callback, '', { filetype, fieldname: 'src' });

            return callback;
        };

        it('should enable the browse button for images and files', () => {
            const options = spectator.service.filePickerOptions();

            expect(options.file_picker_types).toBe('file image');
            expect(options.file_picker_callback).toEqual(expect.any(Function));
        });

        it('should open the picker in image mode from the image dialog, above TinyMCE', () => {
            const spyDialog = vi
                .spyOn(dialogService, 'open')
                .mockReturnValue({ onClose: of(undefined) } as DynamicDialogRef);

            browse('image');

            expect(spyDialog).toHaveBeenCalledWith(
                DotAssetPickerComponent,
                expect.objectContaining({
                    // TinyMCE's dialog layer is at 1300; anything lower opens behind it.
                    baseZIndex: 1300,
                    data: expect.objectContaining({
                        site: SITE,
                        mimeTypes: ['image/*'],
                        languageId: String(LOCALE_ID),
                        title: 'dot.asset.picker.header.image'
                    })
                })
            );
        });

        it('should open the picker in file mode from the link dialog', () => {
            const spyDialog = vi
                .spyOn(dialogService, 'open')
                .mockReturnValue({ onClose: of(undefined) } as DynamicDialogRef);

            browse('file');

            const config = spyDialog.mock.calls[0][1];
            expect(config.baseZIndex).toBe(1300);
            expect(config.data).toEqual(
                expect.objectContaining({ site: SITE, title: 'dot.asset.picker.header.file' })
            );
            // File mode lists any asset, not just images.
            expect(config.data.mimeTypes ?? []).not.toContain('image/*');
        });

        it('should hand the image URL and alt text back to the dialog instead of inserting', () => {
            vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose: of(EMPTY_CONTENTLET)
            } as DynamicDialogRef);

            const callback = browse('image');

            expect(callback).toHaveBeenCalledWith(
                formatDotImageNode(MOCK_IMAGE_URL_PATTERN, EMPTY_CONTENTLET).match(
                    /src="([^"]*)"/
                )[1],
                { alt: EMPTY_CONTENTLET.title }
            );
            expect(editor.insertContent).not.toHaveBeenCalled();
        });

        it('should link a file asset by its path', () => {
            vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose: of(FILE_ASSET)
            } as DynamicDialogRef);

            expect(browse('file')).toHaveBeenCalledWith('/docs/report.pdf', {
                text: 'Annual report'
            });
        });

        it('should link a dotAsset by its /dA/ URL', () => {
            vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose: of(DOT_ASSET)
            } as DynamicDialogRef);

            expect(browse('file')).toHaveBeenCalledWith('/dA/abcdef1234/brochure.pdf', {
                text: 'Brochure'
            });
        });

        it('should leave the dialog untouched when the picker is dismissed', () => {
            vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose: of(undefined)
            } as DynamicDialogRef);

            expect(browse('file')).not.toHaveBeenCalled();
        });

        it('should not stack a second picker while one is open', () => {
            const onClose = new Subject<DotCMSContentlet>();
            const spyDialog = vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose: onClose.asObservable(),
                close: vi.fn()
            } as unknown as DynamicDialogRef);

            browse('file');
            browse('image');

            expect(spyDialog).toHaveBeenCalledTimes(1);

            onClose.next(undefined);
            browse('image');

            expect(spyDialog).toHaveBeenCalledTimes(2);
        });
    });
});
