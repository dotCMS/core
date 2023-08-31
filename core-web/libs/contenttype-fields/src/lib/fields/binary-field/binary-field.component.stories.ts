import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { CommonModule } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { BinaryFieldComponent } from './binary-field.component';

export const MESSAGES_MOCK = {
    'contenttypes.content.edit.write.code': 'Write Code'
};

export default {
    title: 'Library / Contenttype Fields / Fields / BinaryFieldComponent',
    component: BinaryFieldComponent,
    decorators: [
        moduleMetadata({
            imports: [
                BrowserAnimationsModule,
                CommonModule,
                ButtonModule,
                DialogModule,
                MonacoEditorModule,
                DotMessagePipe
            ],
            providers: [
                DotMessagePipe,
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService(MESSAGES_MOCK)
                }
            ]
        })
    ]
} as Meta<BinaryFieldComponent>;

const Template: Story<BinaryFieldComponent> = (args: BinaryFieldComponent) => ({
    props: args
});

export const Primary = Template.bind({});
