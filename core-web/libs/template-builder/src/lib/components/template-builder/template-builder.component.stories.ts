import { moduleMetadata, Story, Meta } from '@storybook/angular';
import { of } from 'rxjs';

import { NgFor, NgIf, AsyncPipe, NgClass } from '@angular/common';
import { HttpClientModule, HttpClient } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { DynamicDialogModule, DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ToolbarModule } from 'primeng/toolbar';

import {
    DotMessageService,
    DotContainersService,
    DotEventsService,
    PaginatorService
} from '@dotcms/data-access';
import { CoreWebService, SiteService } from '@dotcms/dotcms-js';
import { DotMessagePipeModule } from '@dotcms/ui';
import {
    DotContainersServiceMock,
    CoreWebServiceMock,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotAddStyleClassesDialogStore } from './components/add-style-classes-dialog/store/add-style-classes-dialog.store';
import { TemplateBuilderComponentsModule } from './components/template-builder-components.module';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
import {
    DOT_MESSAGE_SERVICE_TB_MOCK,
    FULL_DATA_MOCK,
    MOCK_STYLE_CLASSES_FILE
} from './utils/mocks';

export default {
    title: 'Library/Template Builder',
    component: TemplateBuilderComponent,
    decorators: [
        moduleMetadata({
            imports: [
                NgFor,
                NgIf,
                AsyncPipe,
                NgClass,
                TemplateBuilderComponentsModule,
                DotMessagePipeModule,
                BrowserAnimationsModule,
                DynamicDialogModule,
                HttpClientModule,
                ButtonModule,
                ToolbarModule,
                DividerModule,
                DropdownModule
            ],
            providers: [
                DotTemplateBuilderStore,
                DialogService,
                DynamicDialogRef,
                DotAddStyleClassesDialogStore,
                {
                    provide: DotMessageService,
                    useValue: DOT_MESSAGE_SERVICE_TB_MOCK
                },
                {
                    provide: DotContainersService,
                    useValue: new DotContainersServiceMock()
                },
                {
                    provide: HttpClient,
                    useValue: {
                        get: (_: string) => of(MOCK_STYLE_CLASSES_FILE),
                        request: () => of({})
                    }
                },
                {
                    provide: PaginatorService
                },
                {
                    provide: SiteService,
                    useValue: new SiteServiceMock()
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                {
                    provide: DotEventsService
                }
            ]
        })
    ]
} as Meta<TemplateBuilderComponent>;

const Template: Story<TemplateBuilderComponent> = (args: TemplateBuilderComponent) => ({
    props: args,
    template: `
        <dotcms-template-builder
            [layout]="layout"
            [themeId]="themeId"
            [title]="title"
        >
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
    layout: {
        body: FULL_DATA_MOCK,
        header: true,
        footer: false,
        sidebar: {
            location: 'left',
            width: 'medium',
            containers: []
        }
    },
    themeId: '123',
    title: null
};
