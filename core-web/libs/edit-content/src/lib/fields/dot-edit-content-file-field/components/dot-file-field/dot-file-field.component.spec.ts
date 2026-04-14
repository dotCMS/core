import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { DotAiService, DotMessageService } from '@dotcms/data-access';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotFileFieldComponent } from './dot-file-field.component';

import { IMAGE_FIELD_MOCK } from '../../../../utils/mocks';
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

    describe('vertical layout', () => {
        it('should use horizontal layout by default', () => {
            spectator.detectChanges();
            const container =
                spectator.query('[data-testid="file-field-container"]') ??
                spectator.element.querySelector('div');
            expect(container?.className).toContain('min-[306px]:flex-row');
        });

        it('should use vertical layout when vertical input is true', () => {
            spectator.setInput('vertical' as never, true);
            spectator.detectChanges();
            const container = spectator.element.querySelector('div');
            expect(container?.className).not.toContain('min-[306px]:flex-row');
        });
    });
});
