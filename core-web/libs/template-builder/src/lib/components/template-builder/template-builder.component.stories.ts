import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { NgFor, AsyncPipe } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AddWidgetComponent } from './components/add-widget/add-widget.component';
import { RemoveConfirmDialogComponent } from './components/remove-confirm-dialog/remove-confirm-dialog.component';
import { TemplateBuilderBoxComponent } from './components/template-builder-box/template-builder-box.component';
import { TemplateBuilderRowComponent } from './components/template-builder-row/template-builder-row.component';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
import { FULL_DATA_MOCK } from './utils/mocks';

export default {
    title: 'TemplateBuilderComponent',
    component: TemplateBuilderComponent,
    decorators: [
        moduleMetadata({
            imports: [
                NgFor,
                AsyncPipe,
                TemplateBuilderRowComponent,
                AddWidgetComponent,
                TemplateBuilderBoxComponent,
                RemoveConfirmDialogComponent,
                BrowserAnimationsModule
            ],
            providers: [DotTemplateBuilderStore]
        })
    ]
} as Meta<TemplateBuilderComponent>;

const Template: Story<TemplateBuilderComponent> = (args: TemplateBuilderComponent) => ({
    props: args
});

export const Primary = Template.bind({});

Primary.args = {
    templateLayout: { body: FULL_DATA_MOCK }
};
