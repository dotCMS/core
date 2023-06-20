import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { DropdownModule } from 'primeng/dropdown';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotEventsService, DotMessageService, PaginatorService } from '@dotcms/data-access';
import { CoreWebService, SiteService } from '@dotcms/dotcms-js';
import { DotMessagePipeModule, DotSiteSelectorDirective } from '@dotcms/ui';
import { CoreWebServiceMock, SiteServiceMock } from '@dotcms/utils-testing';

import { TemplateBuilderThemeSelectorComponent } from './template-builder-theme-selector.component';

import { DOT_MESSAGE_SERVICE_TB_MOCK } from '../../utils/mocks';

describe('TemplateBuilderThemeSelectorComponent', () => {
    let spectator: Spectator<TemplateBuilderThemeSelectorComponent>;
    const siteServiceMock = new SiteServiceMock();
    const createComponent = createComponentFactory({
        component: TemplateBuilderThemeSelectorComponent,
        imports: [
            CommonModule,
            ButtonModule,
            DropdownModule,
            DataViewModule,
            DotMessagePipeModule,
            DotSiteSelectorDirective,
            HttpClientTestingModule
        ],
        providers: [
            PaginatorService,
            DialogService,
            DynamicDialogRef,
            MessageService,
            {
                provide: DynamicDialogConfig
            },
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            },
            {
                provide: CoreWebService,
                useClass: CoreWebServiceMock
            },
            {
                provide: PaginatorService
            },

            { provide: SiteService, useValue: siteServiceMock },
            {
                provide: DotEventsService
            }
        ],
        mocks: [PaginatorService, DialogService, MessageService],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();
    });
});
