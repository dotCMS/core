import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { NgFor, AsyncPipe } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipeModule } from '@dotcms/ui';

import { AddWidgetComponent } from './components/add-widget/add-widget.component';
import { RemoveConfirmDialogComponent } from './components/remove-confirm-dialog/remove-confirm-dialog.component';
import { TemplateBuilderActionsComponent } from './components/template-builder-actions/template-builder-actions.component';
import { TemplateBuilderBackgroundColumnsComponent } from './components/template-builder-background-columns/template-builder-background-columns.component';
import { TemplateBuilderBoxComponent } from './components/template-builder-box/template-builder-box.component';
import { TemplateBuilderRowComponent } from './components/template-builder-row/template-builder-row.component';
import { TemplateBuilderSectionComponent } from './components/template-builder-section/template-builder-section.component';
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
                TemplateBuilderBackgroundColumnsComponent,
                TemplateBuilderSectionComponent,
                ButtonModule,
                TemplateBuilderActionsComponent,
                ToolbarModule,
                DividerModule
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
    props: args,
    template: `
        <dotcms-template-builder [templateLayout]="templateLayout">
            <button
                [label]="'Publish'"
                toolbar-actions-right
                type="button"
                pButton
            ></button>
        </dotcms-template-builder>
    `
});

export const Base = Template.bind({});

Base.args = {
    templateLayout: {
        body: FULL_DATA_MOCK,
        header: true,
        footer: false,
        sidebar: {
            location: 'left',
            width: 'small',
            containers: []
        }
    }
};
