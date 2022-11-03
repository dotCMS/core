/* eslint-disable @typescript-eslint/no-explicit-any */

import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { DotCategoriesListComponent } from './dot-categories-list.component';
import { of } from 'rxjs';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { By } from '@angular/platform-browser';
import { CoreWebService, StringUtils } from '@dotcms/dotcms-js';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { CommonModule } from '@angular/common';
import { InputNumberModule } from 'primeng/inputnumber';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { MenuModule } from 'primeng/menu';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { DotCategory } from '@models/categories/dot-categories.model';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotSiteBrowserService } from '@services/dot-site-browser/dot-site-browser.service';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { DotFormatDateServiceMock } from '@dotcms/app/test/format-date-service.mock';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';

const categoriesMock: DotCategory[] = [
    {
        categoryId: '123',
        categoryName: 'Test 1',
        identifier: '332',
        inode: '123',
        name: 'string',
        type: 'string'
    },
    {
        categoryId: '456',
        categoryName: 'Test 2',
        identifier: '13223',
        inode: '456',
        name: 'ASD',
        type: 'DSA'
    },
    {
        categoryId: '789',
        categoryName: 'Test 3',
        identifier: '312312',
        inode: '789',
        name: 'WER',
        type: 'WER'
    },
    {
        categoryId: '101',
        categoryName: 'Test 4',
        identifier: '233132',
        inode: '101',
        name: 'QWE',
        type: 'QWE'
    },
    {
        categoryId: '102',
        categoryName: 'Test 5',
        identifier: '31233',
        inode: '123',
        name: 'ASDFG',
        type: 'ASDFG'
    }
];

const messages = {
    'message.categories.fieldName.Name': 'Name',
    'message.categories.fieldName.Key': 'Key',
    'message.categories.fieldName.CategoryVelocityVarName': 'CategoryVelocityVarName',
    'message.categories.fieldName.SortOrder': 'SortOrder'
};

const columnsMock = [
    {
        fieldName: 'name',
        header: 'Name',
        width: '50%',
        sortable: true
    },
    {
        fieldName: 'key',
        header: 'Key',
        width: '20%',
        sortable: true
    },
    {
        fieldName: 'categoryVelocityVarName',
        header: 'CategoryVelocityVarName',
        width: '20%',
        sortable: true
    },
    {
        fieldName: 'sortOrder',
        width: '5%',
        header: 'SortOrder',
        sortable: true
    }
];

describe('DotCategoriesListComponent', () => {
    let fixture: ComponentFixture<DotCategoriesListComponent>;
    let dotListingDataTable: DotListingDataTableComponent;

    let coreWebService: CoreWebService;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotCategoriesListComponent],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                StringUtils,
                DotSiteBrowserService,
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
                DotAlertConfirmService,
                ConfirmationService
            ],
            imports: [
                DotListingDataTableModule,
                CommonModule,
                DotMessagePipeModule,
                MenuModule,
                ButtonModule,
                DotActionButtonModule,
                DotActionMenuButtonModule,
                HttpClientTestingModule,
                BrowserAnimationsModule,
                BreadcrumbModule,
                InputNumberModule
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();
        fixture = TestBed.createComponent(DotCategoriesListComponent);
        comp = fixture.componentInstance;
        coreWebService = TestBed.inject(CoreWebService);
    });

    describe('with data', () => {
        beforeEach(fakeAsync(() => {
            spyOn<any>(coreWebService, 'requestView').and.returnValue(
                of({
                    entity: categoriesMock,
                    header: (type) => (type === 'Link' ? 'test;test=test' : '10')
                })
            );
            fixture.detectChanges();
            tick(2);
            fixture.detectChanges();
            dotListingDataTable = fixture.debugElement.query(
                By.css('dot-listing-data-table')
            ).componentInstance;
        }));

        it('should set attributes of dotListingDataTable', () => {
            expect(dotListingDataTable.columns).toEqual(columnsMock);
            expect(dotListingDataTable.url).toEqual('v1/categories');
            expect(dotListingDataTable.actions).toEqual([]);
            expect(dotListingDataTable.dataKey).toEqual('inode');
        });
    });

    describe('without data', () => {
        beforeEach(() => {
            spyOn<any>(coreWebService, 'requestView').and.returnValue(
                of({
                    entity: [],
                    header: (type) => (type === 'Link' ? 'test;test=test' : '10')
                })
            );
            fixture.detectChanges();
        });

        it('should set dot-empty-state if the templates array is empty', () => {
            const emptyState = fixture.debugElement.query(By.css('dot-empty-state'));
            expect(emptyState).toBeDefined();
        });
    });
});
