import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessageService, DotUploadService } from '@dotcms/data-access';
import {
    DotContentThumbnailComponent,
    DotDropZoneComponent,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSpinnerModule
} from '@dotcms/ui';

import { DotBinaryFieldComponent } from './binary-field.component';
import { DotBinaryFieldPreviewComponent } from './components/dot-binary-field-preview/dot-binary-field-preview.component';
import { DotBinaryFieldUiMessageComponent } from './components/dot-binary-field-ui-message/dot-binary-field-ui-message.component';
import { DotBinaryFieldUrlModeComponent } from './components/dot-binary-field-url-mode/dot-binary-field-url-mode.component';
import { DotBinaryFieldStore } from './store/binary-field.store';

import {
    CONTENTLET,
    CONTENTTYPE_FIELDS_MESSAGE_MOCK,
    FIELD,
    TEMP_FILES_MOCK
} from '../../utils/mock';

export default {
    title: 'Library / Contenttype Fields / DotBinaryFieldComponent',
    component: DotBinaryFieldComponent,
    decorators: [
        moduleMetadata({
            imports: [
                HttpClientModule,
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
                DotContentThumbnailComponent
            ],
            providers: [
                DotBinaryFieldStore,
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
        field: FIELD
    },
    argTypes: {
        contentlet: {
            defaultValue: CONTENTLET,
            control: 'object',
            description: 'Contentlet Object'
        },
        field: {
            defaultValue: FIELD,
            control: 'Object',
            description: 'Content Type Field Object'
        }
    }
} as Meta<DotBinaryFieldComponent>;

const Template: Story<DotBinaryFieldComponent> = (args: DotBinaryFieldComponent) => ({
    props: args,
    template: `<dot-binary-field
        [contentlet]="contentlet"
        [field]="field"
    ></dot-binary-field>`
});

export const Primary = Template.bind({});
