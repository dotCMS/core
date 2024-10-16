import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { moduleMetadata, StoryObj, Meta, applicationConfig } from '@storybook/angular';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotLicenseService, DotMessageService, DotUploadService } from '@dotcms/data-access';
import {
    DotTempFileThumbnailComponent,
    DotDropZoneComponent,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSpinnerModule
} from '@dotcms/ui';

import { DotBinaryFieldPreviewComponent } from './components/dot-binary-field-preview/dot-binary-field-preview.component';
import { DotBinaryFieldUiMessageComponent } from './components/dot-binary-field-ui-message/dot-binary-field-ui-message.component';
import { DotBinaryFieldUrlModeComponent } from './components/dot-binary-field-url-mode/dot-binary-field-url-mode.component';
import { DotEditContentBinaryFieldComponent } from './dot-edit-content-binary-field.component';
import { DotBinaryFieldStore } from './store/binary-field.store';
import { CONTENTLET, CONTENTTYPE_FIELDS_MESSAGE_MOCK, TEMP_FILES_MOCK } from './utils/mock';

import { BINARY_FIELD_MOCK } from '../../utils/mocks';

const meta: Meta<DotEditContentBinaryFieldComponent> = {
    title: 'Library / Edit Content / Binary Field',
    component: DotEditContentBinaryFieldComponent,
    decorators: [
        applicationConfig({
            providers: [provideHttpClient(), DotMessageService]
        }),
        moduleMetadata({
            imports: [
                BrowserAnimationsModule,
                CommonModule,
                ButtonModule,
                DialogModule,
                MonacoEditorModule,
                DotDropZoneComponent,
                DotBinaryFieldUiMessageComponent,
                DotMessagePipe,
                DotSpinnerModule,
                InputTextModule,
                DotBinaryFieldUrlModeComponent,
                DotBinaryFieldPreviewComponent,
                DotFieldValidationMessageComponent,
                DotTempFileThumbnailComponent
            ],
            providers: [
                DotBinaryFieldStore,
                {
                    provide: DotLicenseService,
                    useValue: {
                        isEnterprise: () => of(true)
                    }
                },
                {
                    provide: DotUploadService,
                    useValue: {
                        uploadFile: () => {
                            return new Promise((resolve, _reject) => {
                                setTimeout(() => {
                                    const index = Math.floor(Math.random() * 3);
                                    const TEMP_FILE = TEMP_FILES_MOCK[index];
                                    resolve(TEMP_FILE); // TEMP_FILES_MOCK is imported from utils/mock.ts
                                }, 2000);
                            });
                        }
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: CONTENTTYPE_FIELDS_MESSAGE_MOCK
                }
            ]
        })
    ],
    args: {
        contentlet: CONTENTLET,
        field: BINARY_FIELD_MOCK
    },
    argTypes: {
        contentlet: {
            defaultValue: CONTENTLET,
            control: 'object',
            description: 'Contentlet Object'
        },
        field: {
            defaultValue: BINARY_FIELD_MOCK,
            control: 'object',
            description: 'Content Type Field Object'
        }
    },
    render: (args) => ({
        props: args,
        template: `<dot-edit-content-binary-field [contentlet]="contentlet" [field]="field" />`
    })
};
export default meta;

type Story = StoryObj<DotEditContentBinaryFieldComponent>;

export const Primary: Story = {};
