import { expect } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotPropertiesService, DotUploadFileService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotAssetSearchDialogComponent } from '@dotcms/ui';
import { EMPTY_CONTENTLET } from '@dotcms/utils-testing';

import { DotWysiwygPluginService } from './dot-wysiwyg-plugin.service';
import { formatDotImageNode } from './utils/editor.utils';

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
}

const MOCK_IMAGE_URL_PATTERN = '/dA/{shortyId}/{name}?language_id={languageId}';

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
        declarations: [MockComponent(DotAssetSearchDialogComponent)],
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
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        dialogService = spectator.inject(DialogService);
        dotUploadFileService = spectator.inject(DotUploadFileService);
        dotPropertiesService = spectator.inject(DotPropertiesService);
        editor = new MockEditor();
    });

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
                onAction: expect.any(Function)
            });
        });

        it('should open the dialog when the button is clicked', () => {
            const spyDialog = jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose: of(EMPTY_CONTENTLET)
            } as DynamicDialogRef);

            const spyEditorInserContent = jest.spyOn(editor, 'insertContent');

            spectator.service.initializePlugins(editor);

            const button = editor.ui.registry.getAll().buttons['dotAddImage'];
            const dialogConfig = {
                header: 'Insert Image',
                width: '800px',
                height: '500px',
                contentStyle: { padding: 0 },
                data: {
                    assetType: 'image'
                }
            };

            // Simulate the button click
            button.onAction();

            expect(spyDialog).toHaveBeenCalledWith(DotAssetSearchDialogComponent, dialogConfig);
            expect(spyEditorInserContent).toHaveBeenCalledWith(
                formatDotImageNode(MOCK_IMAGE_URL_PATTERN, EMPTY_CONTENTLET)
            );
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
