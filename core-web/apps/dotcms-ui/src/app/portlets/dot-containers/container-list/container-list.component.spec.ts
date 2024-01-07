import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, CUSTOM_ELEMENTS_SCHEMA, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { Menu, MenuModule } from 'primeng/menu';
import { Table, TableModule } from 'primeng/table';

import { DotActionMenuButtonComponent } from '@components/_common/dot-action-menu-button/dot-action-menu-button.component';
import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import { DotEmptyStateModule } from '@components/_common/dot-empty-state/dot-empty-state.module';
import { ActionHeaderModule } from '@components/dot-listing-data-table/action-header/action-header.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import {
    DotAlertConfirmService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotSiteBrowserService,
    PaginatorService
} from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    DotPushPublishDialogService,
    LoggerService,
    LoginService,
    mockSites,
    SiteService,
    StringUtils
} from '@dotcms/dotcms-js';
import { CONTAINER_SOURCE, DotActionBulkResult, DotContainer } from '@dotcms/dotcms-models';
import { DotFormatDateService, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';
import {
    DotcmsConfigServiceMock,
    DotFormatDateServiceMock,
    DotMessageDisplayServiceMock,
    MockDotMessageService,
    SiteServiceMock
} from '@dotcms/utils-testing';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';
import { dotEventSocketURLFactory } from '@tests/dot-test-bed';

import { ContainerListRoutingModule } from './container-list-routing.module';
import { ContainerListComponent } from './container-list.component';
import { DotContainerListStore } from './store/dot-container-list.store';

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
    },
    {
        archived: true,
        categoryId: 'a443d26e-0e92-4a9e-a2ab-90a44fd1eb8d',
        deleted: true,
        friendlyName: '',
        identifier: 'FILE_CONTAINER',
        live: false,
        name: 'test',
        parentPermissionable: {
            hostname: 'default'
        },
        path: '//demo.dotcms.com/application/containers/default/',
        source: CONTAINER_SOURCE.FILE,
        title: 'test',
        type: 'containers',
        working: true
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
    selector: 'dot-content-type-selector',
    template: ''
})
class MockDotContentTypeSelectorComponent {
    @Input() value: SelectItem;
    @Output() selected = new EventEmitter<string>();
}

describe('ContainerListComponent', () => {
    let fixture: ComponentFixture<ContainerListComponent>;
    let table: Table;
    let comp: ContainerListComponent;
    let dotPushPublishDialogService: DotPushPublishDialogService;

    let dotRouterService: DotRouterService;

    let unPublishContainer: DotActionMenuButtonComponent;
    let publishContainer: DotActionMenuButtonComponent;
    let archivedContainer: DotActionMenuButtonComponent;
    let contentTypesSelector: MockDotContentTypeSelectorComponent;
    let dotContainersService: DotContainersService;
    let dotSiteBrowserService: DotSiteBrowserService;
    let siteService: SiteServiceMock;
    let store: DotContainerListStore;
    let paginatorService: PaginatorService;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [ContainerListComponent, MockDotContentTypeSelectorComponent],
            providers: [
                ConfirmationService,
                DialogService,
                DotAlertConfirmService,
                DotcmsConfigService,
                DotcmsEventsService,
                DotContainerListStore,
                DotContainersService,
                DotEventsSocket,
                DotHttpErrorManagerService,
                DotSiteBrowserService,
                HttpClient,
                LoggerService,
                LoginService,
                PaginatorService,
                StringUtils,
                {
                    provide: SiteService,
                    useClass: SiteServiceMock
                },
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                },
                {
                    provide: DotcmsConfigService,
                    useClass: DotcmsConfigServiceMock
                },
                {
                    provide: DotRouterService,
                    useValue: {
                        gotoPortlet: jasmine.createSpy(),
                        goToEditContainer: jasmine.createSpy(),
                        goToSiteBrowser: jasmine.createSpy()
                    }
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                }
            ],
            imports: [
                ActionHeaderModule,
                ButtonModule,
                CheckboxModule,
                CommonModule,
                ContainerListRoutingModule,
                DotActionMenuButtonModule,
                DotAddToBundleModule,
                DotEmptyStateModule,
                DotMessagePipe,
                DotPortletBaseModule,
                DotRelativeDatePipe,
                HttpClientTestingModule,
                InputTextModule,
                MenuModule,
                TableModule
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();

        dotPushPublishDialogService = TestBed.inject(DotPushPublishDialogService);
        dotRouterService = TestBed.inject(DotRouterService);
        dotContainersService = TestBed.inject(DotContainersService);
        dotSiteBrowserService = TestBed.inject(DotSiteBrowserService);
        siteService = TestBed.inject(SiteService) as unknown as SiteServiceMock;
        paginatorService = TestBed.inject(PaginatorService);
        spyOn(paginatorService, 'get').and.returnValue(of(containersMock));

        fixture = TestBed.createComponent(ContainerListComponent);
        comp = fixture.componentInstance;
        store = fixture.debugElement.injector.get(DotContainerListStore); // To get store instance from the isolated provider
    });

    describe('with data', () => {
        beforeEach(fakeAsync(() => {
            siteService.setFakeCurrentSite();
            fixture.detectChanges();
            tick(2);
            fixture.detectChanges();

            spyOn(dotPushPublishDialogService, 'open');
            table = fixture.debugElement.query(
                By.css('[data-testId="container-list-table"]')
            ).componentInstance;
        }));

        it('should clicked on row and emit dotRouterService', () => {
            fixture.detectChanges();
            comp.tableRows.get(0).nativeElement.click();
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
            actions.push({
                menuItem: { label: 'Unpublish', command: jasmine.any(Function) }
            });
            actions.push({
                menuItem: { label: 'Duplicate', command: jasmine.any(Function) }
            });

            expect(publishContainer.actions).toEqual(actions);
        });

        it('should set actions to unPublish template', () => {
            unPublishContainer = fixture.debugElement.query(
                By.css('[data-testid="123Unpublish"]')
            ).componentInstance;
            const actions = setBasicOptions();
            actions.push({
                menuItem: { label: 'Archive', command: jasmine.any(Function) }
            });
            actions.push({
                menuItem: { label: 'Duplicate', command: jasmine.any(Function) }
            });

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

        it('should select all except system and file container', () => {
            const menu: Menu = fixture.debugElement.query(
                By.css('.container-listing__header-options p-menu')
            ).componentInstance;
            spyOn(dotContainersService, 'publish').and.returnValue(of(mockBulkResponseSuccess));

            comp.selectedContainers = containersMock;

            fixture.detectChanges();

            comp.handleActionMenuOpen({} as MouseEvent);

            menu.model[0].command();
            expect(dotContainersService.publish).toHaveBeenCalledWith([
                '123Published',
                '123Unpublish',
                '123Archived'
            ]);
        });

        it('should hide action of file or system container', () => {
            const systemContainerActions = fixture.debugElement
                .query(By.css('[data-testrowid="SYSTEM_CONTAINER"]'))
                .query(By.css('dot-content-type-selector'));

            const fileContainerAction = fixture.debugElement
                .query(By.css('[data-testrowid="FILE_CONTAINER"]'))
                .query(By.css('dot-content-type-selector'));

            expect(systemContainerActions).toBe(null);
            expect(fileContainerAction).toBe(null);
        });

        it('should click on file container and move on Browser Screen', () => {
            spyOn(dotSiteBrowserService, 'setSelectedFolder').and.returnValue(of(null));
            fixture.debugElement
                .query(By.css('[data-testrowid="FILE_CONTAINER"]'))
                .triggerEventHandler('click', null);

            fixture.detectChanges();
            const path = new URL(`http:${containersMock[4].path}`).pathname;
            expect(dotSiteBrowserService.setSelectedFolder).toHaveBeenCalledWith(path);
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
        });

        it('should fetch containers when content types selector changes', () => {
            spyOn(store, 'getContainersByContentType');
            fixture.detectChanges();

            contentTypesSelector = fixture.debugElement.query(
                By.css('dot-content-type-selector')
            ).componentInstance;

            contentTypesSelector.selected.emit('test');

            expect(store.getContainersByContentType).toHaveBeenCalledWith('test');
        });

        it('should fetch containers when archive state change', () => {
            spyOn(store, 'getContainersByArchiveState');

            const headerCheckbox = fixture.debugElement.query(
                By.css('[data-testId="archiveCheckbox"]')
            ).componentInstance;

            headerCheckbox.onChange.emit({ checked: true });

            expect(store.getContainersByArchiveState).toHaveBeenCalledWith(true);
        });

        it('should fetch containers when query change', () => {
            spyOn(store, 'getContainersByQuery');

            const queryInput = fixture.debugElement.query(
                By.css('[data-testId="query-input"]')
            ).nativeElement;

            queryInput.value = 'test';
            queryInput.dispatchEvent(new Event('input'));

            fixture.detectChanges();

            expect(store.getContainersByQuery).toHaveBeenCalledWith('test');
        });

        it('should fetch containers with offset when table emits onPage', () => {
            spyOn(store, 'getContainersWithOffset');

            table.onPage.emit({ first: 10 });

            expect(store.getContainersWithOffset).toHaveBeenCalledWith(10);
        });

        it('should update selectedContainers in store when actions button is clicked', () => {
            spyOn(store, 'updateSelectedContainers');
            comp.selectedContainers = [containersMock[0]];
            fixture.detectChanges();

            const bulkButton = fixture.debugElement.query(
                By.css('[data-testId="bulkActions"]')
            ).nativeElement;

            bulkButton.click();

            expect(store.updateSelectedContainers).toHaveBeenCalledWith([containersMock[0]]);
        });

        it('should focus first row when you press arrow down in query input', () => {
            spyOn(comp, 'focusFirstRow');
            const queryInput = fixture.debugElement.query(
                By.css('[data-testId="query-input"]')
            ).nativeElement;

            queryInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown' }));

            fixture.detectChanges();

            expect(comp.focusFirstRow).toHaveBeenCalled();
        });

        it("should fetch containers when site is changed and it's not the first time", () => {
            spyOn(paginatorService, 'setExtraParams').and.callThrough();

            siteService.setFakeCurrentSite(mockSites[1]);

            fixture.detectChanges();

            expect(paginatorService.setExtraParams).toHaveBeenCalledWith(
                'host',
                mockSites[1].identifier
            );
            expect(paginatorService.get).toHaveBeenCalled();
        });
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
