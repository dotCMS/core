import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { NgFor, AsyncPipe } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipeModule } from '@dotcms/ui';

import { AddWidgetComponent } from './components/add-widget/add-widget.component';
import { RemoveConfirmDialogComponent } from './components/remove-confirm-dialog/remove-confirm-dialog.component';
import { TemplateBuilderBackgroundColumnsComponent } from './components/template-builder-background-columns/template-builder-background-columns.component';
import { TemplateBuilderBoxComponent } from './components/template-builder-box/template-builder-box.component';
import { TemplateBuilderRowComponent } from './components/template-builder-row/template-builder-row.component';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
import { DOT_MESSAGE_SERVICE_TB_MOCK, FULL_DATA_MOCK } from './utils/mocks';

export default {
    title: 'Template Builder',
    component: TemplateBuilderComponent,
    decorators: [
        moduleMetadata({
            imports: [
                NgFor,
                AsyncPipe,
                TemplateBuilderRowComponent,
                AddWidgetComponent,
                TemplateBuilderBoxComponent,
                DotMessagePipeModule,
                RemoveConfirmDialogComponent,
                BrowserAnimationsModule,
                TemplateBuilderBackgroundColumnsComponent
            ],
            providers: [
                DotTemplateBuilderStore,
                {
                    provide: DotMessageService,
                    useValue: DOT_MESSAGE_SERVICE_TB_MOCK
                }
            ]
        })
    ]
} as Meta<TemplateBuilderComponent>;

const Template: Story<TemplateBuilderComponent> = (args: TemplateBuilderComponent) => ({
    props: args
});

export const Base = Template.bind({});

Base.args = {
    templateLayout: { body: FULL_DATA_MOCK }
};
