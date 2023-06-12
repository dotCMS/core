import { moduleMetadata, Story, Meta } from '@storybook/angular';
import { of } from 'rxjs';

import { NgFor, AsyncPipe } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DividerModule } from 'primeng/divider';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessageService, DotStyleClassesService } from '@dotcms/data-access';
import { DotMessagePipeModule } from '@dotcms/ui';

import { AddStyleClassesDialogComponent } from './components/add-style-classes-dialog/add-style-classes-dialog.component';
import { AddWidgetComponent } from './components/add-widget/add-widget.component';
import { RemoveConfirmDialogComponent } from './components/remove-confirm-dialog/remove-confirm-dialog.component';
import { TemplateBuilderActionsComponent } from './components/template-builder-actions/template-builder-actions.component';
import { TemplateBuilderBackgroundColumnsComponent } from './components/template-builder-background-columns/template-builder-background-columns.component';
import { TemplateBuilderBoxComponent } from './components/template-builder-box/template-builder-box.component';
import { TemplateBuilderRowComponent } from './components/template-builder-row/template-builder-row.component';
import { TemplateBuilderSectionComponent } from './components/template-builder-section/template-builder-section.component';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
import {
    DOT_MESSAGE_SERVICE_TB_MOCK,
    FULL_DATA_MOCK,
    MOCK_STYLE_CLASSES_FILE
} from './utils/mocks';

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
                AddStyleClassesDialogComponent,
                DynamicDialogModule,
                HttpClientModule,
                TemplateBuilderActionsComponent,
                ToolbarModule,
                DividerModule
            ],
            providers: [
                DotTemplateBuilderStore,
                DialogService,
                DynamicDialogRef,
                DotStyleClassesService,
                {
                    provide: DotMessageService,
                    useValue: DOT_MESSAGE_SERVICE_TB_MOCK
                },
                {
                    provide: HttpClient,
                    useValue: {
                        get: (_: string) => of(MOCK_STYLE_CLASSES_FILE)
                    }
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
