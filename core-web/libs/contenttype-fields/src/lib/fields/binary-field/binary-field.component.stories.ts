import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotDropZoneComponent, DotMessagePipe } from '@dotcms/ui';

import { BinaryFieldComponent } from './binary-field.component';

import { CONTENTTYPE_FIELDS_MESSAGE_MOCK } from '../../utils/mock';

export default {
    title: 'Library / Contenttype Fields / Fields / BinaryFieldComponent',
    component: BinaryFieldComponent,
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
                DotMessagePipe
            ],
            providers: [
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
        helperLabel: 'This field accepts only images with a maximum size of 1MB.'
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
        helperLabel: {
            defaultValue: 'This field accepts only images with a maximum size of 1MB.',
            control: 'text',
            description: 'Helper label to be displayed below the field'
        }
    }
} as Meta<BinaryFieldComponent>;

const Template: Story<BinaryFieldComponent> = (args: BinaryFieldComponent) => ({
    props: args,
    template: `<dotcms-binary-field
        [accept]="accept"
        [maxFileSize]="maxFileSize"
        [helperLabel]="helperLabel"
    ></dotcms-binary-field>`
});

export const Primary = Template.bind({});
