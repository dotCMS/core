import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import {
    DotAiService,
    DotMessageService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotGeneratedAIImage, PromptType } from '@dotcms/dotcms-models';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotFileFieldComponent } from './dot-file-field.component';

import { BINARY_FIELD_MOCK, FILE_FIELD_MOCK, IMAGE_FIELD_MOCK } from '../../../../utils/mocks';
import { IMAGE_EDITOR_LAUNCHER } from '../../../shared/image-editor-launcher';
import {
    LegacyDialogImageEditorLauncher,
    LegacyDojoImageEditorLauncher
} from '../../services/image-editor';
import { DotFileFieldUploadService } from '../../services/upload-file/upload-file.service';
import { FileFieldStore } from '../../store/file-field.store';
import { DotFileFieldPreviewComponent } from '../dot-file-field-preview/dot-file-field-preview.component';
import { DotFileFieldUiMessageComponent } from '../dot-file-field-ui-message/dot-file-field-ui-message.component';

describe('DotFileFieldComponent', () => {
    let spectator: Spectator<DotFileFieldComponent>;

    // Shared mock for the new Angular image editor launcher. Configured per-test
    // (provided at factory level so it survives component re-creation without
    // triggering a post-instantiation `overrideProvider`).
    const mockImageEditorLauncher = {
        isAvailable: jest.fn().mockReturnValue(false),
        open: jest.fn().mockReturnValue(of(null))
    };

    const createComponent = createComponentFactory({
        component: DotFileFieldComponent,
        imports: [ReactiveFormsModule],
        componentMocks: [DotFileFieldPreviewComponent, DotFileFieldUiMessageComponent],
        providers: [
            FileFieldStore,
            mockProvider(DotFileFieldUploadService),
            mockProvider(DialogService),
            LegacyDialogImageEditorLauncher,
            LegacyDojoImageEditorLauncher,
            mockProvider(DotWorkflowActionsFireService),
            { provide: IMAGE_EDITOR_LAUNCHER, useValue: mockImageEditorLauncher },
            mockProvider(DotMessageService, {
                get: jest.fn().mockReturnValue('Test Message')
            }),
            mockProvider(DotAiService, {
                checkPluginInstallation: jest.fn().mockReturnValue(of(false))
            }),
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        mockImageEditorLauncher.isAvailable.mockReset().mockReturnValue(false);
        mockImageEditorLauncher.open.mockReset().mockReturnValue(of(null));

        spectator = createComponent({
            props: {
                field: IMAGE_FIELD_MOCK,
                contentlet: createFakeContentlet({ [IMAGE_FIELD_MOCK.variable]: null }),
                hasError: false
            } as never
        });
    });

    describe('AI image generation', () => {
        const AI_CONTENTLET = createFakeContentlet({
            identifier: 'ai-contentlet-identifier',
            inode: 'ai-contentlet-inode',
            folder: 'SYSTEM_FOLDER',
            asset: '/dA/ai-contentlet-inode/asset/generated.png',
            assetMetaData: {
                contentType: 'image/png',
                isImage: true,
                length: 12345,
                name: 'generated.png'
            }
        });

        const AI_IMAGE: DotGeneratedAIImage = {
            request: { text: 'a cat', type: PromptType.INPUT, size: '1024x1024' },
            response: {
                originalPrompt: 'a cat',
                response: 'temp_ai_file_id',
                revised_prompt: 'a cat revised',
                tempFileName: 'generated.png',
                url: '/dA/ai-contentlet-inode/asset/generated.png',
                contentlet: AI_CONTENTLET
            }
        };

        const setupWithField = (field: typeof BINARY_FIELD_MOCK | typeof IMAGE_FIELD_MOCK) => {
            spectator = createComponent({
                props: {
                    field,
                    contentlet: createFakeContentlet({ [field.variable]: null }),
                    hasError: false
                } as never
            });

            const dialogService = spectator.inject(DialogService);
            (dialogService.open as jest.Mock).mockReturnValue({
                onClose: of(AI_IMAGE),
                close: jest.fn()
            });

            spectator.detectChanges();

            return dialogService;
        };

        it('should store the AI image as a temp file for Binary fields', () => {
            setupWithField(BINARY_FIELD_MOCK);

            const setPreviewFileSpy = jest.spyOn(spectator.component.store, 'setPreviewFile');

            spectator.component.showAIImagePromptDialog();

            expect(setPreviewFileSpy).toHaveBeenCalledWith({
                source: 'temp',
                file: expect.objectContaining({
                    id: 'temp_ai_file_id',
                    fileName: 'generated.png',
                    referenceUrl: '/dA/ai-contentlet-inode/asset/generated.png'
                })
            });
            // The binary field value must be the temp id, not the contentlet identifier.
            expect(spectator.component.store.value()).toBe('temp_ai_file_id');
        });

        it('should store the AI image as a contentlet for Image fields', () => {
            setupWithField(IMAGE_FIELD_MOCK);

            const setPreviewFileSpy = jest.spyOn(spectator.component.store, 'setPreviewFile');

            spectator.component.showAIImagePromptDialog();

            expect(setPreviewFileSpy).toHaveBeenCalledWith({
                source: 'contentlet',
                file: AI_CONTENTLET
            });
            expect(spectator.component.store.value()).toBe('ai-contentlet-identifier');
        });
    });

    describe('image editor', () => {
        const EDITED_TEMP_FILE = { id: 'edited-temp-id', fileName: 'edited.png' };

        // Puts an image temp file in preview so `$canEditImage()` is true (Binary + isImage).
        const setupBinaryWithImage = () => {
            spectator = createComponent({
                props: {
                    field: BINARY_FIELD_MOCK,
                    contentlet: createFakeContentlet({ [BINARY_FIELD_MOCK.variable]: null }),
                    hasError: false
                } as never
            });
            spectator.detectChanges();

            spectator.component.store.setPreviewFile({
                source: 'temp',
                file: {
                    id: 'temp-1',
                    fileName: 'img.png',
                    metadata: { isImage: true, contentType: 'image/png', name: 'img.png' }
                }
            } as never);
            spectator.detectChanges();
        };

        it('should open the new Angular editor when its launcher is available', () => {
            mockImageEditorLauncher.isAvailable.mockReturnValue(true);
            mockImageEditorLauncher.open.mockReturnValue(of(EDITED_TEMP_FILE));
            setupBinaryWithImage();

            const applySpy = jest
                .spyOn(spectator.component.store, 'applyEditedImage')
                .mockImplementation();

            spectator.component.onEditImage();

            expect(mockImageEditorLauncher.open).toHaveBeenCalledWith(
                expect.objectContaining({
                    tempId: 'temp-1',
                    variable: BINARY_FIELD_MOCK.variable,
                    fieldName: BINARY_FIELD_MOCK.variable,
                    mimeType: 'image/png'
                })
            );
            expect(applySpy).toHaveBeenCalled();
        });

        it('should fall back to the legacy editor when the new launcher is unavailable', () => {
            mockImageEditorLauncher.isAvailable.mockReturnValue(false);
            setupBinaryWithImage();

            const legacy = spectator.fixture.debugElement.injector.get(
                LegacyDialogImageEditorLauncher
            );
            const legacyOpenSpy = jest
                .spyOn(legacy, 'open')
                .mockReturnValue(of(EDITED_TEMP_FILE) as never);
            const applySpy = jest
                .spyOn(spectator.component.store, 'applyEditedImage')
                .mockImplementation();

            spectator.component.onEditImage();

            expect(mockImageEditorLauncher.open).not.toHaveBeenCalled();
            expect(legacyOpenSpy).toHaveBeenCalled();
            expect(applySpy).toHaveBeenCalled();
        });
    });

    describe('edit image availability', () => {
        const IMAGE_ASSET_META = { isImage: true, contentType: 'image/png', name: 'beach.png' };
        const NON_IMAGE_ASSET_META = {
            isImage: false,
            contentType: 'application/pdf',
            name: 'doc.pdf'
        };

        // Hydrates the preview from a referenced dotAsset contentlet (Image/File
        // fields), whose metadata lives in `assetMetaData`.
        const setReferencedAsset = (
            field: typeof IMAGE_FIELD_MOCK,
            assetMetaData: Record<string, unknown>
        ) => {
            spectator = createComponent({
                props: {
                    field,
                    contentlet: createFakeContentlet({ [field.variable]: 'ref-identifier' }),
                    hasError: false
                } as never
            });
            spectator.detectChanges();

            spectator.component.store.setPreviewFile({
                source: 'contentlet',
                file: { identifier: 'ref-identifier', inode: 'ref-inode', assetMetaData } as never
            });
            spectator.detectChanges();
        };

        it('shows the editor for an Image field pointing at an image asset', () => {
            setReferencedAsset(IMAGE_FIELD_MOCK, IMAGE_ASSET_META);
            expect(spectator.component.$canEditImage()).toBe(true);
        });

        it('shows the editor for a File field when the referenced asset is an image', () => {
            setReferencedAsset(FILE_FIELD_MOCK, IMAGE_ASSET_META);
            expect(spectator.component.$canEditImage()).toBe(true);
        });

        it('hides the editor for a File field when the referenced asset is not an image', () => {
            setReferencedAsset(FILE_FIELD_MOCK, NON_IMAGE_ASSET_META);
            expect(spectator.component.$canEditImage()).toBe(false);
        });

        it('hides the editor for an empty Image field', () => {
            spectator = createComponent({
                props: {
                    field: IMAGE_FIELD_MOCK,
                    contentlet: createFakeContentlet({ [IMAGE_FIELD_MOCK.variable]: null }),
                    hasError: false
                } as never
            });
            spectator.detectChanges();
            expect(spectator.component.$canEditImage()).toBe(false);
        });

        it('keeps Binary hidden for a non-image file', () => {
            spectator = createComponent({
                props: {
                    field: BINARY_FIELD_MOCK,
                    contentlet: createFakeContentlet({ [BINARY_FIELD_MOCK.variable]: null }),
                    hasError: false
                } as never
            });
            spectator.detectChanges();
            spectator.component.store.setPreviewFile({
                source: 'temp',
                file: {
                    id: 'temp-1',
                    fileName: 'doc.pdf',
                    metadata: { isImage: false, contentType: 'application/pdf', name: 'doc.pdf' }
                }
            } as never);
            spectator.detectChanges();
            expect(spectator.component.$canEditImage()).toBe(false);
        });
    });

    describe('vertical layout', () => {
        it('should use horizontal layout by default', () => {
            spectator.detectChanges();
            const container = spectator.query('[data-testid="file-field-container"]');
            expect(container).toHaveClass('dot-file-field__container');
            expect(container).not.toHaveClass('dot-file-field__container--vertical');
        });

        it('should use vertical layout when vertical input is true', () => {
            spectator.setInput('vertical' as never, true);
            spectator.detectChanges();
            const container = spectator.query('[data-testid="file-field-container"]');
            expect(container).toHaveClass('dot-file-field__container--vertical');
        });
    });
});
