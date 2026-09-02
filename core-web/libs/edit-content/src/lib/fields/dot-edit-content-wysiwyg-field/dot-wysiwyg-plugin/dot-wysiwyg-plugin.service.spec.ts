import { expect } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@openng/spectator';
import { MockComponent } from 'ng-mocks';
import { Observable, of, Subject, throwError } from 'rxjs';

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

    insertContent = jest.fn();

    focus = jest.fn();
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
                    getKey: jest.fn().mockReturnValue(of(MOCK_IMAGE_URL_PATTERN))
                }
            },
            {
                provide: DotUploadFileService,
                useValue: {
                    publishContent: jest.fn()
                }
            },
            {
                provide: DotSiteService,
                useValue: { getCurrentSite: jest.fn(() => siteSource) }
            },
            {
                provide: DotMessageService,
                useValue: { get: jest.fn((key: string) => key) }
            },
            {
                provide: DotEditContentStore,
                useValue: { currentLocale: signal({ id: LOCALE_ID }) }
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
        jest.clearAllMocks();
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
            const spyButton = jest.spyOn(editor.ui.registry, 'addButton');
            const spyOn = jest.spyOn(editor, 'on');

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
            const spyDialog = jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose: of(EMPTY_CONTENTLET)
            } as DynamicDialogRef);

            const spyEditorInserContent = jest.spyOn(editor, 'insertContent');

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
            const spyDialog = jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose: of(undefined)
            } as DynamicDialogRef);

            const spyEditorInserContent = jest.spyOn(editor, 'insertContent');

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
            const spyDialog = jest
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
            const spyDialog = jest
                .spyOn(dialogService, 'open')
                .mockReturnValue({ onClose: of(undefined) } as DynamicDialogRef);

            clickAddImage();
            editor.ui.registry.getAll().buttons['dotAddImage'].onAction();

            expect(spyDialog).toHaveBeenCalledTimes(2);
        });

        it('should not open a picker that has nothing to browse', () => {
            siteSource = throwError(() => new Error('no site'));
            spectator = createService();
            const spyDialog = jest.spyOn(spectator.inject(DialogService), 'open');

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
            const spyUpload = jest
                .spyOn(dotUploadFileService, 'publishContent')
                .mockReturnValue(of(uploadRespMock as DotCMSContentlet[]));
            const spyEditorInserContent = jest.spyOn(editor, 'insertContent');

            spectator.service.initializePlugins(editor);

            const dropEvent = {
                dataTransfer: {
                    files: [
                        {
                            type: 'image/png'
                        }
                    ]
                },
                preventDefault: jest.fn(),
                stopImmediatePropagation: jest.fn(),
                stopPropagation: jest.fn()
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
            const spyUpload = jest
                .spyOn(dotUploadFileService, 'publishContent')
                .mockReturnValue(of(uploadRespMock as DotCMSContentlet[]));
            const spyEditorInserContent = jest.spyOn(editor, 'insertContent');

            spectator.service.initializePlugins(editor);

            const dropEvent = {
                dataTransfer: {
                    files: [
                        {
                            type: 'video/mp4'
                        }
                    ]
                },
                preventDefault: jest.fn(),
                stopImmediatePropagation: jest.fn(),
                stopPropagation: jest.fn()
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
});
