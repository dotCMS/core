import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { CommonModule } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { BinaryFieldComponent } from './binary-field.component';

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
                MonacoEditorModule
            ]
        })
    ]
} as Meta<BinaryFieldComponent>;

const Template: Story<BinaryFieldComponent> = (args: BinaryFieldComponent) => ({
    props: args
});

export const Primary = Template.bind({});
// Primary.args = {
// }
