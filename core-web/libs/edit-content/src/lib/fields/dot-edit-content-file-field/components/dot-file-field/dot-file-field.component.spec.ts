import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { DotAiService, DotMessageService } from '@dotcms/data-access';
import { DotGeneratedAIImage, PromptType } from '@dotcms/dotcms-models';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotFileFieldComponent } from './dot-file-field.component';

import { BINARY_FIELD_MOCK, IMAGE_FIELD_MOCK } from '../../../../utils/mocks';
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
            (dialogService.open as jest.Mock).mockReturnValue({ onClose: of(AI_IMAGE) });

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
