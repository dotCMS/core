import { of, Subject } from 'rxjs';
import { vi } from 'vitest';

import { CommonModule } from '@angular/common';
import { HttpClient, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import {
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    EventEmitter,
    Input,
    NO_ERRORS_SCHEMA,
    Output
} from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { Menu, MenuModule } from 'primeng/menu';
import { Table, TableModule } from 'primeng/table';

import {
    DotAlertConfirmService,
    DotFormatDateService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotSiteBrowserService,
    PaginatorService
} from '@dotcms/data-access';
import {
    DotcmsConfigService,
    DotPushPublishDialogService,
    LoggerService,
    LoginService,
    mockSites,
    SiteService,
    StringUtils
} from '@dotcms/dotcms-js';
import {
    CONTAINER_SOURCE,
    DotActionBulkResult,
    DotBulkFailItem,
    DotCMSResponse,
    DotContainer,
    DotContainerDependencies,
    DotSite
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import {
    DotAddToBundleComponent,
    DotContentletStatusBadgeComponent,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';
import {
    createFakeEvent,
    DotcmsConfigServiceMock,
    DotFormatDateServiceMock,
    DotMessageDisplayServiceMock,
    MockDotMessageService,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { ContainerListComponent } from './container-list.component';
import { DotContainerListStore } from './store/dot-container-list.store';

import { DotContainersService } from '../../../api/services/dot-containers/dot-containers.service';
import { DotBulkInformationComponent } from '../../../view/components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotEmptyStateComponent } from '../../../view/components/_common/dot-empty-state/dot-empty-state.component';
import { DotContentTypeSelectorComponent } from '../../../view/components/dot-content-type-selector/dot-content-type-selector.component';
import { ActionHeaderComponent } from '../../../view/components/dot-listing-data-table/action-header/action-header.component';
import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';

// Mock window.matchMedia (required by PrimeNG ContextMenu)
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn()
    }))
});

const containersMock: DotContainer[] = [
    {
        archived: false,
        categoryId: '6e07301c-e6d2-4c1f-9e8e-fcc4a31947d3',
        deleted: false,
        friendlyName: '',
        identifier: '123Published',
        live: true,
        name: 'movie',
        hostName: 'default',
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
        hostName: 'default',
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
        hostName: 'default',
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
        hostName: 'default',
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
        hostName: 'default',
        path: '//demo.dotcms.com/application/containers/default/',
        pathName: '//demo.dotcms.com/application/containers/default/',
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
    'message.containers.in_use.header': 'Container in use',
    'container-used-templates': 'This Container is used by Templates.',
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

    let contentTypesSelector: MockDotContentTypeSelectorComponent;
    let siteService: SiteServiceMock;
    let store: DotContainerListStore;
    let paginatorService: PaginatorService;
    let switchSiteSubject: Subject<DotSite>;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async () => {
        switchSiteSubject = new Subject<DotSite>();

        await TestBed.configureTestingModule({
            declarations: [],
            imports: [
                ContainerListComponent,
                MockDotContentTypeSelectorComponent,
                ActionHeaderComponent,
                ButtonModule,
                CheckboxModule,
                CommonModule,
                DotAddToBundleComponent,
                DotContentletStatusBadgeComponent,
                DotEmptyStateComponent,
                DotMessagePipe,
                DotPortletBaseComponent,
                DotRelativeDatePipe,
                InputTextModule,
                MenuModule,
                TableModule
            ],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                provideNoopAnimations(),
                ConfirmationService,
                DialogService,
                DotAlertConfirmService,
                DotcmsConfigService,
                DotContainerListStore,
                DotContainersService,
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
                        gotoPortlet: vi.fn(),
                        goToEditContainer: vi.fn(),
                        goToSiteBrowser: vi.fn()
                    }
                },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                {
                    provide: GlobalStore,
                    useValue: {
                        switchSiteEvent$: () => switchSiteSubject.asObservable()
                    }
                }
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
        })
            .overrideComponent(ContainerListComponent, {
                remove: { imports: [DotContentTypeSelectorComponent] },
                add: { imports: [MockDotContentTypeSelectorComponent], schemas: [NO_ERRORS_SCHEMA] }
            })
            .compileComponents();

        dotPushPublishDialogService = TestBed.inject(DotPushPublishDialogService);
        dotRouterService = TestBed.inject(DotRouterService);
        siteService = TestBed.inject(SiteService) as unknown as SiteServiceMock;
        paginatorService = TestBed.inject(PaginatorService);
        vi.spyOn(paginatorService, 'get').mockReturnValue(of(containersMock));

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

            vi.spyOn(dotPushPublishDialogService, 'open');
            table = fixture.debugElement.query(
                By.css('[data-testId="container-list-table"]')
            ).componentInstance;
        }));

        it('should clicked on row and emit dotRouterService', () => {
            fixture.detectChanges();
            comp.tableRows()[0].nativeElement.click();
            expect(dotRouterService.goToEditContainer).toHaveBeenCalledTimes(1);
            expect(dotRouterService.goToEditContainer).toHaveBeenCalledWith(
                containersMock[0].identifier
            );
        });

        it('should set actions to publish template', () => {
            const publishedContainer = containersMock.find((c) => c.identifier === '123Published');
            const actions = setBasicOptions();
            actions.push({
                menuItem: { label: 'Unpublish', command: expect.any(Function) }
            });
            actions.push({
                menuItem: { label: 'Duplicate', command: expect.any(Function) }
            });

            expect(comp.setContainerActions(publishedContainer)).toEqual(actions);
        });

        it('should set actions to unPublish template', () => {
            const unpublishedContainer = containersMock.find(
                (c) => c.identifier === '123Unpublish'
            );
            const actions = setBasicOptions();
            actions.push({
                menuItem: { label: 'Archive', command: expect.any(Function) }
            });
            actions.push({
                menuItem: { label: 'Duplicate', command: expect.any(Function) }
            });

            expect(comp.setContainerActions(unpublishedContainer)).toEqual(actions);
        });

        it('should set actions to archived template', () => {
            const archivedContainer = containersMock.find((c) => c.identifier === '123Archived');

            const actions = [
                { menuItem: { label: 'Unarchive', command: expect.any(Function) } },
                { menuItem: { label: 'Delete', command: expect.any(Function) } }
            ];
            expect(comp.setContainerActions(archivedContainer)).toEqual(actions);
        });

        describe('delete container', () => {
            const dependencies = (
                templates: DotContainerDependencies['templates'],
                identifier = '123Archived'
            ) =>
                ({
                    inode: `inode-${identifier}`,
                    identifier,
                    title: 'Archived container',
                    templates
                }) as DotContainerDependencies;

            const usedBy = [
                {
                    inode: 'tpl-inode',
                    identifier: 'tpl-id',
                    title: '<b>Home</b> & more',
                    siteName: 'demo.dotcms.com'
                }
            ];

            let containersService: DotContainersService;
            let alertConfirmService: DotAlertConfirmService;
            let dialogService: DialogService;

            const clickDelete = () => {
                const archivedContainer = containersMock.find(
                    (c) => c.identifier === '123Archived'
                ) as DotContainer;

                const deleteAction = comp
                    .setContainerActions(archivedContainer)
                    .find(({ menuItem }) => menuItem['label'] === 'Delete');

                expect(deleteAction).toBeDefined();
                deleteAction?.menuItem.command?.({});
            };

            beforeEach(() => {
                containersService = fixture.debugElement.injector.get(DotContainersService);
                alertConfirmService = TestBed.inject(DotAlertConfirmService);
                dialogService = fixture.debugElement.injector.get(DialogService);

                vi.spyOn(alertConfirmService, 'confirm').mockImplementation(() => undefined);
                vi.spyOn(dialogService, 'open').mockReturnValue(null);
                vi.spyOn(containersService, 'delete').mockReturnValue(of(mockBulkResponseSuccess));
            });

            it('should list the templates and not delete when the container is in use', () => {
                vi.spyOn(containersService, 'getDependencies').mockReturnValue(
                    of([dependencies(usedBy)])
                );

                clickDelete();

                expect(containersService.getDependencies).toHaveBeenCalledWith(['123Archived']);
                expect(alertConfirmService.confirm).not.toHaveBeenCalled();
                expect(containersService.delete).not.toHaveBeenCalled();

                const [component, config] = (dialogService.open as ReturnType<typeof vi.fn>).mock
                    .calls[0];

                expect(component).toBe(DotBulkInformationComponent);
                expect(config.data.fails[0].errorMessage).toContain(
                    'This Container is used by Templates.'
                );
                // titles are kept as typed: they are rendered as text by the dialog
                expect(config.data.fails[0].errorMessage).toContain(
                    '<b>Home</b> & more (tpl-id) - demo.dotcms.com'
                );
            });

            it('should name the dialog after the container being in use, not after a delete', () => {
                vi.spyOn(containersService, 'getDependencies').mockReturnValue(
                    of([dependencies(usedBy)])
                );

                clickDelete();

                const [, config] = (dialogService.open as ReturnType<typeof vi.fn>).mock.calls[0];

                expect(config.data.action).toBe('Container in use');
            });

            it('should ask for confirmation and delete when no template uses the container', () => {
                vi.spyOn(containersService, 'getDependencies').mockReturnValue(
                    of([dependencies([])])
                );

                clickDelete();

                expect(alertConfirmService.confirm).toHaveBeenCalledTimes(1);
                expect(containersService.delete).not.toHaveBeenCalled();

                (alertConfirmService.confirm as ReturnType<typeof vi.fn>).mock.calls[0][0].accept();

                expect(containersService.delete).toHaveBeenCalledWith(['123Archived']);
                expect(dialogService.open).not.toHaveBeenCalled();
            });

            it('should check a bulk selection with one request and delete nothing when one is in use', () => {
                vi.spyOn(containersService, 'getDependencies').mockReturnValue(
                    of([dependencies([], 'free'), dependencies(usedBy, 'in-use')])
                );

                store['deleteContainer'](['free', 'in-use']);

                expect(containersService.getDependencies).toHaveBeenCalledTimes(1);
                expect(containersService.getDependencies).toHaveBeenCalledWith(['free', 'in-use']);
                expect(alertConfirmService.confirm).not.toHaveBeenCalled();
                expect(containersService.delete).not.toHaveBeenCalled();
                expect(dialogService.open).toHaveBeenCalledTimes(1);

                const [, config] = (dialogService.open as ReturnType<typeof vi.fn>).mock.calls[0];

                expect(config.data.fails.map((fail: DotBulkFailItem) => fail.element)).toEqual([
                    'in-use'
                ]);
            });

            it('should not delete when the dependencies lookup fails', () => {
                vi.spyOn(containersService, 'getDependencies').mockReturnValue(of(null));

                clickDelete();

                expect(alertConfirmService.confirm).not.toHaveBeenCalled();
                expect(containersService.delete).not.toHaveBeenCalled();
                expect(dialogService.open).not.toHaveBeenCalled();
            });

            it('should not notify when the delete request fails', () => {
                vi.spyOn(containersService, 'getDependencies').mockReturnValue(
                    of([dependencies([])])
                );
                vi.spyOn(containersService, 'delete').mockReturnValue(of(null));

                clickDelete();
                (alertConfirmService.confirm as ReturnType<typeof vi.fn>).mock.calls[0][0].accept();

                expect(dialogService.open).not.toHaveBeenCalled();
            });
        });

        it('should select all except system and file container', () => {
            const menu: Menu = fixture.debugElement.query(By.directive(Menu)).componentInstance;
            // Spy on the store's dotContainersService since it's now using component-level providers
            vi.spyOn(store['dotContainersService'], 'publish').mockReturnValue(
                of(mockBulkResponseSuccess)
            );

            fixture.detectChanges();

            comp.selectedContainers = containersMock;

            comp.handleActionMenuOpen({} as MouseEvent);

            menu.model[0].command({
                originalEvent: createFakeEvent('click')
            });
            expect(store['dotContainersService'].publish).toHaveBeenCalledWith([
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
            const fileContainer = containersMock.find((c) => c.identifier === 'FILE_CONTAINER');
            // Spy on the store's methods since it's now using component-level providers
            vi.spyOn(store['dotSiteBrowserService'], 'setSelectedFolder').mockReturnValue(
                of({} as DotCMSResponse<Record<string, unknown>>)
            );
            vi.spyOn(store['dotRouterService'], 'goToSiteBrowser');

            // Call the method directly instead of triggering the event
            comp.handleRowClick(fileContainer);

            fixture.detectChanges();
            expect(store['dotSiteBrowserService'].setSelectedFolder).toHaveBeenCalledWith(
                fileContainer.pathName
            );
            expect(store['dotSiteBrowserService'].setSelectedFolder).toHaveBeenCalledTimes(1);
            expect(store['dotRouterService'].goToSiteBrowser).toHaveBeenCalledWith(
                fileContainer?.pathName
            );
            expect(store['dotRouterService'].goToSiteBrowser).toHaveBeenCalledTimes(1);
        });

        it('should fetch containers when content types selector changes', () => {
            vi.spyOn(store, 'getContainersByContentType');
            fixture.detectChanges();

            contentTypesSelector = fixture.debugElement.query(
                By.css('dot-content-type-selector')
            ).componentInstance;

            contentTypesSelector.selected.emit('test');

            expect(store.getContainersByContentType).toHaveBeenCalledWith('test');
            expect(store.getContainersByContentType).toHaveBeenCalledTimes(1);
        });

        it('should fetch containers when archive state change', () => {
            vi.spyOn(store, 'getContainersByArchiveState');

            const headerCheckbox = fixture.debugElement.query(
                By.css('[data-testId="archiveCheckbox"]')
            ).componentInstance;

            headerCheckbox.onChange.emit({ checked: true });

            expect(store.getContainersByArchiveState).toHaveBeenCalledWith(true);
            expect(store.getContainersByArchiveState).toHaveBeenCalledTimes(1);
        });

        it('should fetch containers when query change', () => {
            vi.spyOn(store, 'getContainersByQuery');

            const queryInput = fixture.debugElement.query(
                By.css('[data-testId="query-input"]')
            ).nativeElement;

            queryInput.value = 'test';
            queryInput.dispatchEvent(new Event('input'));

            fixture.detectChanges();

            expect(store.getContainersByQuery).toHaveBeenCalledWith('test');
            expect(store.getContainersByQuery).toHaveBeenCalledTimes(1);
        });

        it('should fetch containers with offset when table emits onPage', () => {
            vi.spyOn(store, 'getContainersWithOffset');

            table.onPage.emit({ first: 10, rows: 10 });

            expect(store.getContainersWithOffset).toHaveBeenCalledWith(10);
            expect(store.getContainersWithOffset).toHaveBeenCalledTimes(1);
        });

        it('should update selectedContainers in store when actions button is clicked', () => {
            vi.spyOn(store, 'updateSelectedContainers');

            fixture.detectChanges();

            comp.selectedContainers = [containersMock[0]];

            comp.handleActionMenuOpen({} as MouseEvent);

            expect(store.updateSelectedContainers).toHaveBeenCalledWith([containersMock[0]]);
            expect(store.updateSelectedContainers).toHaveBeenCalledTimes(1);
        });

        it('should focus first row when you press arrow down in query input', () => {
            vi.spyOn(comp, 'focusFirstRow');
            const queryInput = fixture.debugElement.query(
                By.css('[data-testId="query-input"]')
            ).nativeElement;

            queryInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown' }));

            fixture.detectChanges();

            expect(comp.focusFirstRow).toHaveBeenCalled();
        });

        it("should fetch containers when site is changed and it's not the first time", () => {
            vi.spyOn(paginatorService, 'setExtraParams');
            vi.spyOn(paginatorService, 'getFirstPage').mockReturnValue(of(containersMock));

            switchSiteSubject.next(mockSites[1] as unknown as DotSite);

            fixture.detectChanges();

            expect(paginatorService.setExtraParams).toHaveBeenCalledWith(
                'host',
                mockSites[1].identifier
            );
            expect(paginatorService.getFirstPage).toHaveBeenCalled();
        });
    });

    function setBasicOptions() {
        return [
            { menuItem: { label: 'Edit', command: expect.any(Function) } },
            { menuItem: { label: 'Publish', command: expect.any(Function) } },
            { menuItem: { label: 'Push Publish', command: expect.any(Function) } },
            { menuItem: { label: 'Add To Bundle', command: expect.any(Function) } }
        ];
    }
});
