import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';

import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { of } from 'rxjs';
import { ContainerListComponent } from './container-list.component';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import { CoreWebServiceMock, DotPushPublishDialogService } from '@dotcms/dotcms-js';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import {
    DotAlertConfirmService,
    DotMessageService,
    DotSiteBrowserService
} from '@dotcms/data-access';
import { ActivatedRoute } from '@angular/router';
import { DotEventsSocketURL } from '@dotcms/dotcms-js';
import { dotEventSocketURLFactory } from '@tests/dot-test-bed';
import { StringUtils } from '@dotcms/dotcms-js';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { ConfirmationService, SelectItem, SharedModule } from 'primeng/api';
import { LoginService } from '@dotcms/dotcms-js';
import { DotcmsEventsService } from '@dotcms/dotcms-js';
import { DotEventsSocket } from '@dotcms/dotcms-js';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { CommonModule } from '@angular/common';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { CheckboxModule } from 'primeng/checkbox';
import { Menu, MenuModule } from 'primeng/menu';
import { ButtonModule } from 'primeng/button';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Component, CUSTOM_ELEMENTS_SCHEMA, EventEmitter, Input, Output } from '@angular/core';
import { By } from '@angular/platform-browser';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';
import { DotActionMenuButtonComponent } from '@components/_common/dot-action-menu-button/dot-action-menu-button.component';
import { DotContainer, CONTAINER_SOURCE, DotActionBulkResult } from '@dotcms/dotcms-models';
import { MockDotMessageService, DotFormatDateServiceMock } from '@dotcms/utils-testing';

const containersMock: DotContainer[] = [
    {
        archived: false,
        categoryId: '6e07301c-e6d2-4c1f-9e8e-fcc4a31947d3',
        deleted: false,
        friendlyName: '',
        identifier: '123Published',
        live: true,
        name: 'movie',
        parentPermissionable: {
            hostname: 'default'
        },
        path: null,
        source: CONTAINER_SOURCE.DB,
        title: 'movie',
        type: 'containers',
        working: true
    },
    {
        archived: false,
        categoryId: 'a443d26e-0e92-4a9e-a2ab-90a44fd1eb8d',
        deleted: false,
        friendlyName: '',
        identifier: '123Unpublish',
        live: false,
        name: 'test',
        parentPermissionable: {
            hostname: 'default'
        },
        path: null,
        source: CONTAINER_SOURCE.DB,
        title: 'test',
        type: 'containers',
        working: true
    },
    {
        archived: true,
        categoryId: 'a443d26e-0e92-4a9e-a2ab-90a44fd1eb8d',
        deleted: true,
        friendlyName: '',
        identifier: '123Archived',
        live: false,
        name: 'test',
        parentPermissionable: {
            hostname: 'default'
        },
        path: null,
        source: CONTAINER_SOURCE.DB,
        title: 'test',
        type: 'containers',
        working: true
    },
    {
        archived: true,
        categoryId: 'a443d26e-0e92-4a9e-a2ab-90a44fd1eb8d',
        deleted: true,
        friendlyName: '',
        identifier: 'SYSTEM_CONTAINER',
        live: false,
        name: 'test',
        parentPermissionable: {
            hostname: 'default'
        },
        path: null,
        source: CONTAINER_SOURCE.DB,
        title: 'test',
        type: 'containers',
        working: true
    }
];

const columnsMock = [
    {
        fieldName: 'title',
        header: 'Name',
        sortable: true
    },
    {
        fieldName: 'status',
        header: 'Status',
        width: '8%'
    },
    {
        fieldName: 'friendlyName',
        header: 'Description'
    },
    {
        fieldName: 'modDate',
        format: 'date',
        header: 'Last Edit',
        sortable: true
    }
];

const messages = {
    'Add-To-Bundle': 'Add To Bundle',
    'Remote-Publish': 'Push Publish',
    'code-container': 'Advanced Container',
    'contenttypes.content.push_publish': 'Push Publish',
    'design-container': 'Container Designer',
    'message.container.confirm.delete.container':
        'Are you sure you want to delete this Container?  (This operation cannot be undone)',
    'message.container.copy': 'Container copied',
    'message.container.delete': 'Container archived',
    'message.container.full_delete': 'Container deleted',
    'message.container.undelete': 'Container unarchived',
    'message.container.unpublished': 'Container unpublished',
    'message.container_list.published': 'Containers published',
    'message.containers.fieldName.description': 'Description',
    'message.containers.fieldName.lastEdit': 'Last Edit',
    'message.containers.fieldName.name': 'Name',
    'message.containers.fieldName.status': 'Status',
    'Delete-Container': 'Delete Container',
    Archive: 'Archive',
    Archived: 'Archived',
    Copy: 'Copy',
    Delete: 'Delete',
    Draft: 'Draft',
    Publish: 'Publish',
    Published: 'Published',
    Results: 'Results',
    Revision: 'Revision',
    Unarchive: 'Unarchive',
    Unpublish: 'Unpublish',
    edit: 'Edit',
    publish: 'Publish'
};

const routeDataMock = {
    dotContainerListResolverData: [true, true]
};

const mockBulkResponseSuccess: DotActionBulkResult = {
    skippedCount: 0,
    successCount: 3,
    fails: []
};

class ActivatedRouteMock {
    get data() {
        return of(routeDataMock);
    }
}

@Component({
    selector: 'dot-base-type-selector',
    template: ''
})
class MockDotBaseTypeSelectorComponent {
    @Input() value: SelectItem;
    @Output() selected = new EventEmitter<string>();
}

describe('ContainerListComponent', () => {
    let fixture: ComponentFixture<ContainerListComponent>;
    let comp: ContainerListComponent;
    let dotListingDataTable: DotListingDataTableComponent;
    let dotPushPublishDialogService: DotPushPublishDialogService;
    let coreWebService: CoreWebService;
    let dotRouterService: DotRouterService;

    let unPublishContainer: DotActionMenuButtonComponent;
    let publishContainer: DotActionMenuButtonComponent;
    let archivedContainer: DotActionMenuButtonComponent;
    let baseTypesSelector: MockDotBaseTypeSelectorComponent;
    let dotContainersService: DotContainersService;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [ContainerListComponent, MockDotBaseTypeSelectorComponent],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                {
                    provide: DotRouterService,
                    useValue: {
                        gotoPortlet: jasmine.createSpy(),
                        goToEditContainer: jasmine.createSpy(),
                        goToSiteBrowser: jasmine.createSpy()
                    }
                },
                StringUtils,
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService,
                LoginService,
                DotcmsEventsService,
                DotEventsSocket,
                DotcmsConfigService,
                DotMessageDisplayService,
                DialogService,
                DotSiteBrowserService,
                DotContainersService,
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock }
            ],
            imports: [
                DotListingDataTableModule,
                CommonModule,
                DotMessagePipeModule,
                SharedModule,
                CheckboxModule,
                MenuModule,
                ButtonModule,
                DotActionButtonModule,
                DotActionMenuButtonModule,
                DotAddToBundleModule,
                HttpClientTestingModule,
                DynamicDialogModule,
                BrowserAnimationsModule
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();
        fixture = TestBed.createComponent(ContainerListComponent);
        comp = fixture.componentInstance;
        dotPushPublishDialogService = TestBed.inject(DotPushPublishDialogService);
        coreWebService = TestBed.inject(CoreWebService);
        dotRouterService = TestBed.inject(DotRouterService);
        dotContainersService = TestBed.inject(DotContainersService);
    });

    describe('with data', () => {
        beforeEach(fakeAsync(() => {
            spyOn<CoreWebService>(coreWebService, 'requestView').and.returnValue(
                of({
                    entity: containersMock,
                    header: (type) => (type === 'Link' ? 'test;test=test' : '10')
                })
            );
            fixture.detectChanges();
            tick(2);
            fixture.detectChanges();
            dotListingDataTable = fixture.debugElement.query(
                By.css('dot-listing-data-table')
            ).componentInstance;
            spyOn(dotPushPublishDialogService, 'open');
        }));

        it('should set attributes of dotListingDataTable', () => {
            expect(dotListingDataTable.columns).toEqual(columnsMock);
            expect(dotListingDataTable.url).toEqual('v1/containers?system=true');
            expect(dotListingDataTable.actions).toEqual([]);
            expect(dotListingDataTable.checkbox).toEqual(true);
            expect(dotListingDataTable.dataKey).toEqual('inode');
        });

        it('should clicked on row and emit dotRouterService', () => {
            comp.listing.dataTable.tableViewChild.nativeElement.rows[1].click();
            expect(dotRouterService.goToEditContainer).toHaveBeenCalledTimes(1);
            expect(dotRouterService.goToEditContainer).toHaveBeenCalledWith(
                containersMock[0].identifier
            );
        });

        it('should set actions to publish template', () => {
            publishContainer = fixture.debugElement.query(
                By.css('[data-testid="123Published"]')
            ).componentInstance;
            const actions = setBasicOptions();
            actions.push({ menuItem: { label: 'Unpublish', command: jasmine.any(Function) } });
            actions.push({ menuItem: { label: 'Duplicate', command: jasmine.any(Function) } });

            expect(publishContainer.actions).toEqual(actions);
        });

        it('should set actions to unPublish template', () => {
            unPublishContainer = fixture.debugElement.query(
                By.css('[data-testid="123Unpublish"]')
            ).componentInstance;
            const actions = setBasicOptions();
            actions.push({ menuItem: { label: 'Archive', command: jasmine.any(Function) } });
            actions.push({ menuItem: { label: 'Duplicate', command: jasmine.any(Function) } });

            expect(unPublishContainer.actions).toEqual(actions);
        });

        it('should set actions to archived template', () => {
            archivedContainer = fixture.debugElement.query(
                By.css('[data-testid="123Archived"]')
            ).componentInstance;

            const actions = [
                { menuItem: { label: 'Unarchive', command: jasmine.any(Function) } },
                { menuItem: { label: 'Delete', command: jasmine.any(Function) } }
            ];
            expect(archivedContainer.actions).toEqual(actions);
        });

        it('should select all except system container', () => {
            const menu: Menu = fixture.debugElement.query(
                By.css('.container-listing__header-options p-menu')
            ).componentInstance;
            spyOn(dotContainersService, 'publish').and.returnValue(of(mockBulkResponseSuccess));
            comp.updateSelectedContainers(containersMock);
            menu.model[0].command();
            expect(dotContainersService.publish).toHaveBeenCalledWith([
                '123Published',
                '123Unpublish',
                '123Archived'
            ]);
        });
    });

    it('should emit changes in base types selector', () => {
        fixture.detectChanges();
        baseTypesSelector = fixture.debugElement.query(
            By.css('dot-base-type-selector')
        ).componentInstance;
        spyOn(comp.listing.paginatorService, 'setExtraParams');
        spyOn(comp.listing, 'loadFirstPage');
        baseTypesSelector.selected.emit('test');

        expect(comp.listing.paginatorService.setExtraParams).toHaveBeenCalledWith('type', 'test');
        expect(comp.listing.loadFirstPage).toHaveBeenCalledWith();
    });

    function setBasicOptions() {
        return [
            { menuItem: { label: 'Edit', command: jasmine.any(Function) } },
            { menuItem: { label: 'Publish', command: jasmine.any(Function) } },
            { menuItem: { label: 'Push Publish', command: jasmine.any(Function) } },
            { menuItem: { label: 'Add To Bundle', command: jasmine.any(Function) } }
        ];
    }
});
