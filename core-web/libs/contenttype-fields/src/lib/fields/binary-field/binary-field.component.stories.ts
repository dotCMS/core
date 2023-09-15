import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotMessageService, DotUploadService } from '@dotcms/data-access';
import { DotDropZoneComponent, DotDropZoneMessageComponent, DotMessagePipe } from '@dotcms/ui';

import { DotBinaryFieldComponent } from './binary-field.component';
import { DotBinaryFieldStore } from './store/binary-field.store';

import { CONTENTTYPE_FIELDS_MESSAGE_MOCK } from '../../utils/mock';

export default {
    title: 'Library / Contenttype Fields / Fields / DotBinaryFieldComponent',
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
                DotDropZoneMessageComponent,
                DotMessagePipe
            ],
            providers: [
                DotBinaryFieldStore,
                {
                    provide: DotUploadService,
                    useValue: {
                        uploadFile: () => {
                            return new Promise((resolve) => {
                                resolve({
                                    fileName: 'fileName',
                                    folder: 'folder',
                                    id: 'tempFileId',
                                    image: true,
                                    length: 10000,
                                    mimeType: 'mimeType',
                                    referenceUrl: 'referenceUrl',
                                    thumbnailUrl: 'thumbnailUrl'
                                });
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
        accept: ['image/*'],
        maxFileSize: 1000000,
        helperText: 'This field accepts only images with a maximum size of 1MB.'
    },
    argTypes: {
        accept: {
            defaultValue: ['image/*'],
            control: 'object',
            description: 'Array of accepted file types'
        },
        maxFileSize: {
            defaultValue: 1000000,
            control: 'number',
            description: 'Maximum file size in bytes'
        },
        helperText: {
            defaultValue: 'This field accepts only images with a maximum size of 1MB.',
            control: 'text',
            description: 'Helper label to be displayed below the field'
        }
    }
} as Meta<DotBinaryFieldComponent>;

const Template: Story<DotBinaryFieldComponent> = (args: DotBinaryFieldComponent) => ({
    props: args,
    template: `<dotcms-binary-field
        [accept]="accept"
        [maxFileSize]="maxFileSize"
        [helperText]="helperText"
    ></dotcms-binary-field>`
});

export const Primary = Template.bind({});
