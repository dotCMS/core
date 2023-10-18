import { Meta, moduleMetadata, Story } from '@storybook/angular';
import { of } from 'rxjs';

import { AsyncPipe, NgClass, NgFor, NgIf } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ToolbarModule } from 'primeng/toolbar';

import {
    DotContainersService,
    DotEventsService,
    DotMessageService,
    PaginatorService
} from '@dotcms/data-access';
import { CoreWebService, SiteService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import {
    CoreWebServiceMock,
    DotContainersServiceMock,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { JsonClassesService } from './components/add-style-classes-dialog/services/json-classes.service';
import { TemplateBuilderComponentsModule } from './components/template-builder-components.module';
import { DotTemplateBuilderStore } from './store/template-builder.store';
import { TemplateBuilderComponent } from './template-builder.component';
import {
    CONTAINER_MAP_MOCK,
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
                DotMessagePipe,
                BrowserAnimationsModule,
                DynamicDialogModule,
                HttpClientModule,
                ButtonModule,
                ToolbarModule,
                DividerModule,
                DropdownModule,
                FormsModule
            ],
            providers: [
                DotTemplateBuilderStore,
                DialogService,
                DynamicDialogRef,
                JsonClassesService,
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
        <dotcms-template-builder-lib
            [layout]="layout"
            [themeId]="themeId"
            [containerMap]="containerMap"
        >
            <button
                [label]="'Publish'"
                toolbar-actions-right
                type="button"
                pButton
            ></button>
        </dotcms-template-builder-lib>
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
    containerMap: CONTAINER_MAP_MOCK
};
