/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable no-console */

import { of, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { Menu, MenuModule } from 'primeng/menu';

import {
    DotAlertConfirmService,
    DotFormatDateService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotSiteBrowserService
} from '@dotcms/data-access';
import {
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    DotPushPublishDialogService,
    LoggerService,
    LoginService,
    SiteService,
    StringUtils
} from '@dotcms/dotcms-js';
import {
    DotActionBulkResult,
    DotContentState,
    DotMessageSeverity,
    DotMessageType,
    DotTemplate
} from '@dotcms/dotcms-models';
import {
    DotActionMenuButtonComponent,
    DotAddToBundleComponent,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';
import {
    CoreWebServiceMock,
    createFakeEvent,
    DotFormatDateServiceMock,
    MockDotMessageService,
    mockSites,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotTemplateListComponent } from './dot-template-list.component';

// Suppress console logs during this test
const originalConsoleInfo = console.info;
const originalConsoleDebug = console.debug;
const originalConsoleWarn = console.warn;
const originalConsoleError = console.error;

beforeAll(() => {
    console.info = jest.fn();
    console.debug = jest.fn();
    console.warn = jest.fn();
    console.error = jest.fn();
});

afterAll(() => {
    console.info = originalConsoleInfo;
    console.debug = originalConsoleDebug;
    console.warn = originalConsoleWarn;
    console.error = originalConsoleError;
});

import { DotTemplatesService } from '../../../api/services/dot-templates/dot-templates.service';
import { ButtonModel } from '../../../shared/models/action-header/button.model';
import { dotEventSocketURLFactory } from '../../../test/dot-test-bed';
import { DotActionButtonComponent } from '../../../view/components/_common/dot-action-button/dot-action-button.component';
import { DotBulkInformationComponent } from '../../../view/components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotListingDataTableComponent } from '../../../view/components/dot-listing-data-table/dot-listing-data-table.component';

const templatesMock: DotTemplate[] = [
    {
        anonymous: false,
        friendlyName: 'Published template',
        identifier: '123Published',
        inode: '1AreSD',
        name: 'Published template',
        type: 'type',
        versionType: 'type',
        deleted: false,
        live: true,
        layout: null,
        canEdit: true,
        canWrite: true,
        canPublish: true,
        hasLiveVersion: true,
        working: true,
        theme: 'test',
        themeInfo: {
            identifier: '123',
            name: 'test',
            title: 'test',
            inode: '123',
            themeThumbnail: 'test',
            hostId: '123',
            host: {
                hostName: 'test',
                inode: '123',
                identifier: '123'
            },
            defaultFileType: 'test',
            filesMasks: 'test',
            modDate: 123,
            path: 'test',
            sortOrder: 123,
            showOnMenu: true,
            type: 'test'
        }
    },
    {
        anonymous: false,
        friendlyName: 'Locked template',
        identifier: '123Locked',
        inode: '1sASD',
        name: 'Locked template',
        type: 'type',
        versionType: 'type',
        deleted: false,
        live: true,
        locked: true,
        layout: null,
        canEdit: true,
        canWrite: true,
        canPublish: true,
        hasLiveVersion: true,
        working: true,
        theme: 'System Theme',
        themeInfo: {
            identifier: '123',
            name: 'System Theme',
            title: 'System Theme',
            inode: 'SYSTEM_THEME',
            themeThumbnail: 'System Theme',
            hostId: '123',
            host: {
                hostName: 'System Theme',
                inode: '123',
                identifier: '123'
            },
            defaultFileType: 'System Theme',
            filesMasks: 'System Theme',
            modDate: 123,
            path: 'System Theme',
            sortOrder: 123,
            showOnMenu: true,
            type: 'System Theme'
        }
    },
    {
        anonymous: false,
        friendlyName: 'Unpublish template',
        identifier: '123Unpublish',
        inode: '1ASgD',
        name: 'Unpublish template',
        type: 'type',
        versionType: 'type',
        deleted: false,
        live: false,
        layout: null,
        canEdit: true,
        canWrite: true,
        canPublish: true,
        hasLiveVersion: false,
        working: true,
        theme: 'test-2',
        themeInfo: {
            identifier: '123',
            name: 'test-2',
            title: 'test-2',
            inode: '123',
            themeThumbnail: 'test-2',
            hostId: '123',
            host: {
                hostName: 'test-2',
                inode: '123',
                identifier: '123'
            },
            defaultFileType: 'test-2',
            filesMasks: 'test-2',
            modDate: 123,
            path: 'test-2',
            sortOrder: 123,
            showOnMenu: true,
            type: 'test-2'
        }
    },
    {
        anonymous: false,
        friendlyName: 'Archived template',
        identifier: '123Archived',
        inode: '1AdsSD',
        name: 'Archived template',
        type: 'type',
        versionType: 'type',
        deleted: true,
        layout: null,
        canEdit: true,
        canWrite: true,
        canPublish: true,
        hasLiveVersion: false,
        working: false,
        theme: 'test-3',
        themeInfo: {
            identifier: '123',
            name: 'test-3',
            title: 'test-3',
            inode: '123',
            themeThumbnail: 'test-3',
            hostId: '123',
            host: {
                hostName: 'test-3',
                inode: '123',
                identifier: '123'
            },
            defaultFileType: 'test-3',
            filesMasks: 'test-3',
            modDate: 123,
            path: 'test-3',
            sortOrder: 123,
            showOnMenu: true,
            type: 'test-3'
        }
    },
    {
        anonymous: false,
        friendlyName: 'Template as a File',
        identifier: '//dir/asFile',
        inode: '1asFile',
        name: 'Template as a File',
        type: 'type',
        versionType: 'type',
        deleted: false,
        live: true,
        layout: null,
        canEdit: true,
        canWrite: true,
        canPublish: true,
        hasLiveVersion: true,
        working: true,
        theme: 'test-4',
        themeInfo: {
            identifier: '123',
            name: 'test-4',
            title: 'test-4',
            inode: '123',
            themeThumbnail: 'test-4',
            hostId: '123',
            host: {
                hostName: 'test-4',
                inode: '123',
                identifier: '123'
            },
            defaultFileType: 'test-4',
            filesMasks: 'test-4',
            modDate: 123,
            path: 'test-4',
            sortOrder: 123,
            showOnMenu: true,
            type: 'test-4'
        }
    },
    {
        anonymous: false,
        friendlyName: 'template without theme',
        identifier: '//dir/asFile',
        inode: '1asFile',
        name: 'template without theme',
        type: 'type',
        versionType: 'type',
        deleted: false,
        live: true,
        layout: null,
        canEdit: true,
        canWrite: true,
        canPublish: true,
        hasLiveVersion: true,
        working: true,
        theme: 'test-4'
    }
];

const routeDataMock = {
    dotTemplateListResolverData: [true, true]
};

class ActivatedRouteMock {
    get data() {
        return of(routeDataMock);
    }
}

const messages = {
    'Add-To-Bundle': 'Add To Bundle',
    'Remote-Publish': 'Push Publish',
    'code-template': 'Advanced Template',
    'contenttypes.content.push_publish': 'Push Publish',
    'design-template': 'Template Designer',
    'message.template.confirm.delete.template':
        'Are you sure you want to delete this Template?  (This operation cannot be undone)',
    'message.template.copy': 'Template copied',
    'message.template.delete': 'Template archived',
    'message.template.full_delete': 'Template deleted',
    'message.template.undelete': 'Template unarchived',
    'message.template.unpublished': 'Template unpublished',
    'message.template_list.published': 'Templates published',
    'templates.fieldName.description': 'Description',
    'templates.fieldName.lastEdit': 'Last Edit',
    'templates.fieldName.name': 'Name',
    'templates.fieldName.theme': 'Theme',
    'templates.fieldName.status': 'Status',
    'Delete-Template': 'Delete Template',
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

const columnsMock = [
    {
        fieldName: 'name',
        header: 'Name',
        sortable: true
    },
    {
        fieldName: 'status',
        header: 'Status',
        width: '8%'
    },
    {
        fieldName: 'theme',
        header: 'Theme'
    },
    {
        fieldName: 'friendlyName',
        header: 'Description'
    },
    {
        fieldName: 'modDate',
        format: 'date',
        header: 'Last Edit',
        sortable: true,
        textAlign: 'left'
    }
];

const mockSingleResponseFail: DotActionBulkResult = {
    skippedCount: 0,
    successCount: 0,
    fails: [
        {
            errorMessage: 'error 1',
            element: '123Published'
        }
    ],
    action: ''
};

const mockBulkResponseFail: DotActionBulkResult = {
    skippedCount: 0,
    successCount: 1,
    fails: [
        {
            errorMessage: 'error 1',
            element: '123Published'
        },
        {
            errorMessage: 'error 2',
            element: '123Locked'
        }
    ],
    action: ''
};

const mockBulkResponseSuccess: DotActionBulkResult = {
    skippedCount: 0,
    successCount: 3,
    fails: []
};

const mockMessageConfig = {
    life: 3000,
    severity: DotMessageSeverity.SUCCESS,
    type: DotMessageType.SIMPLE_MESSAGE
};

describe('DotTemplateListComponent', () => {
    let fixture: ComponentFixture<DotTemplateListComponent>;
    let dotListingDataTable: DotListingDataTableComponent;
    let dotTemplatesService: DotTemplatesService;
    let dotMessageDisplayService: DotMessageDisplayService;
    let dotPushPublishDialogService: DotPushPublishDialogService;
    let dotRouterService: DotRouterService;
    let dialogService: DialogService;

    let comp: DotTemplateListComponent;
    let unPublishTemplate: DotActionMenuButtonComponent;
    let publishTemplate: DotActionMenuButtonComponent;
    let lockedTemplate: DotActionMenuButtonComponent;
    let archivedTemplate: DotActionMenuButtonComponent;
    let dotAlertConfirmService: DotAlertConfirmService;
    let coreWebService: CoreWebService;
    let dotSiteBrowserService: DotSiteBrowserService;
    let mockGoToFolder: jest.SpyInstance;

    const messageServiceMock = new MockDotMessageService(messages);

    const dialogRefClose = new Subject();
    const siteServiceMock = new SiteServiceMock();

    beforeEach(async () => {
        // Create spies for services that will be injected
        const dotTemplatesServiceSpy = {
            archive: jest.fn(),
            unArchive: jest.fn(),
            publish: jest.fn(),
            unPublish: jest.fn(),
            copy: jest.fn(),
            delete: jest.fn()
        };
        const dotSiteBrowserServiceSpy = {
            setSelectedFolder: jest.fn()
        };
        const dotRouterServiceSpy = {
            gotoPortlet: jest.fn(),
            goToEditTemplate: jest.fn(),
            goToSiteBrowser: jest.fn()
        };
        const dialogServiceSpy = {
            open: jest.fn()
        };

        await TestBed.configureTestingModule({
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
                    useValue: dotRouterServiceSpy
                },
                {
                    provide: SiteService,
                    useValue: siteServiceMock
                },
                StringUtils,
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService,
                DotcmsEventsService,
                DotEventsSocket,
                DotcmsConfigService,
                DotMessageDisplayService,
                { provide: DialogService, useValue: dialogServiceSpy },
                LoggerService,
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
                {
                    provide: LoginService,
                    useValue: { currentUserLanguageId: 'en-US' }
                },
                DotPushPublishDialogService
            ],
            imports: [
                DotTemplateListComponent,
                DotListingDataTableComponent,
                CommonModule,
                DotMessagePipe,
                DotRelativeDatePipe,
                SharedModule,
                CheckboxModule,
                MenuModule,
                ButtonModule,
                DotActionButtonComponent,
                DotActionMenuButtonComponent,
                DotAddToBundleComponent,
                HttpClientTestingModule,
                DynamicDialogModule,
                BrowserAnimationsModule
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        })
            .overrideComponent(DotTemplateListComponent, {
                set: {
                    providers: [
                        { provide: DotTemplatesService, useValue: dotTemplatesServiceSpy },
                        { provide: DialogService, useValue: dialogServiceSpy },
                        { provide: DotSiteBrowserService, useValue: dotSiteBrowserServiceSpy }
                    ]
                }
            })
            .compileComponents();
        fixture = TestBed.createComponent(DotTemplateListComponent);
        comp = fixture.componentInstance;
        dotTemplatesService = dotTemplatesServiceSpy;
        dotMessageDisplayService = TestBed.inject(DotMessageDisplayService);
        dotPushPublishDialogService = TestBed.inject(DotPushPublishDialogService);
        dotRouterService = dotRouterServiceSpy;
        dialogService = dialogServiceSpy;
        dotAlertConfirmService = TestBed.inject(DotAlertConfirmService);
        coreWebService = TestBed.inject(CoreWebService);
        dotSiteBrowserService = dotSiteBrowserServiceSpy;
    });

    it('should set archive checkbox as binary', () => {
        const checkbox = fixture.debugElement.query(
            By.css('[data-testId="archiveCheckbox"]')
        ).componentInstance;

        fixture.detectChanges();

        expect(checkbox.binary).toBeTruthy();
    });

    describe('with data', () => {
        beforeEach(fakeAsync(() => {
            jest.spyOn(coreWebService, 'requestView').mockReturnValue(
                of({
                    entity: templatesMock,
                    header: (type) => (type === 'Link' ? 'test;test=test' : '10')
                } as any)
            );
            fixture.detectChanges();
            tick(2);
            fixture.detectChanges();
            dotListingDataTable = fixture.debugElement.query(
                By.css('dot-listing-data-table')
            ).componentInstance;
            jest.spyOn(dotPushPublishDialogService, 'open');

            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose: dialogRefClose
            } as any);

            mockGoToFolder = jest.spyOn(comp, 'goToFolder');
        }));

        // Helper function to load data in the table
        const loadTableData = fakeAsync(() => {
            // Mock the PaginatorService through the dotListingDataTable
            jest.spyOn(dotListingDataTable.paginatorService, 'get').mockReturnValue(
                of(templatesMock)
            );

            // Simulate the lazy load event
            const table = fixture.debugElement.query(By.css('p-table'));
            if (table) {
                table.triggerEventHandler('onLazyLoad', { first: 0, rows: 40 });
            } else {
                // If no table, directly call loadData
                dotListingDataTable.loadData(0);
            }

            // Wait for the setTimeout in setItems
            tick(1);
            fixture.detectChanges();
        });

        it('should reload portlet only when the site change', () => {
            fixture.detectChanges(); // Initialize component and subscriptions
            siteServiceMock.setFakeCurrentSite(mockSites[1]); // switching the site
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('templates');
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        });

        it('should set attributes of dotListingDataTable', () => {
            expect(dotListingDataTable.columns).toEqual(columnsMock);
            expect(dotListingDataTable.sortField).toEqual('modDate');
            expect(dotListingDataTable.sortOrder).toEqual('DESC');
            expect(dotListingDataTable.url).toEqual('v1/templates');
            expect(dotListingDataTable.actions).toEqual([]);
            expect(dotListingDataTable.checkbox).toEqual(true);
            expect(dotListingDataTable.dataKey).toEqual('inode');
        });

        it('should have links for theme folder', () => {
            loadTableData();

            const links = fixture.debugElement.queryAll(
                By.css('[data-testid="theme-folder-link"]')
            );

            const templatesWithoutSystem = templatesMock.filter(
                (template) => template.theme !== 'System Theme'
            );

            expect(links.length).toEqual(4);
            expect(links[0].attributes['target']).toEqual('_self');
            expect(
                links.every(
                    (link, i) =>
                        link.nativeElement.textContent.trim() ===
                        templatesWithoutSystem[i].themeInfo.title
                )
            ).toBe(true);
        });

        it('should trigger goToFolder whem clicking on a theme link', () => {
            loadTableData();

            const link = fixture.debugElement.query(By.css('[data-testid="theme-folder-link"]'));
            expect(link).toBeTruthy();

            link.nativeElement.click();

            expect(mockGoToFolder).toHaveBeenCalledWith(expect.any(Event), 'test');
        });

        it("should render 'System Theme' when the theme is SYSTEM_THEME", () => {
            loadTableData();

            const cells = fixture.debugElement.queryAll(By.css('[data-testid="theme-cell"]'));
            expect(cells.length).toBeGreaterThan(1);

            expect(cells[1].nativeElement.textContent.trim()).toEqual('System Theme');
        });

        it('should not trigger goToFolder when the theme is SYSTEM_THEME', () => {
            loadTableData();

            const cells = fixture.debugElement.queryAll(By.css('[data-testid="theme-cell"]'));
            expect(cells.length).toBeGreaterThan(1);

            cells[1].nativeElement.click();

            expect(mockGoToFolder).not.toHaveBeenCalled();
        });

        it('should render empty when the theme is undefined or null', () => {
            loadTableData();

            const cells = fixture.debugElement.queryAll(By.css('[data-testid="theme-cell"]'));
            expect(cells.length).toBeGreaterThan(0);

            const lastCell = cells.pop();
            expect(lastCell).toBeTruthy();

            expect(lastCell.nativeElement.textContent.trim()).toEqual('');
        });

        it('should not trigger goToFolder when the theme is null or undefined', () => {
            loadTableData();

            const cells = fixture.debugElement.queryAll(By.css('[data-testid="theme-cell"]'));
            expect(cells.length).toBeGreaterThan(0);

            const lastCell = cells.pop();
            expect(lastCell).toBeTruthy();

            lastCell.nativeElement.click();

            expect(mockGoToFolder).not.toHaveBeenCalled();
        });

        it('should set Action Header options correctly', () => {
            const model: ButtonModel[] = dotListingDataTable.actionHeaderOptions.primary.model;
            expect(model).toBeUndefined();

            dotListingDataTable.actionHeaderOptions.primary.command();
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/templates/new');
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        });

        it('should pass data to the status elements', () => {
            loadTableData();

            const state: DotContentState = {
                live: true,
                working: true,
                deleted: false,
                hasLiveVersion: true
            };

            const labels = {
                archived: 'Archived',
                published: 'Published',
                revision: 'Revision',
                draft: 'Draft'
            };

            const lockedTemplateElement = fixture.debugElement.query(
                By.css('[data-testid="123Locked"]')
            );
            expect(lockedTemplateElement).toBeTruthy();
            lockedTemplate = lockedTemplateElement.componentInstance;

            const stateIcon = fixture.debugElement.query(By.css('dot-state-icon'));
            expect(stateIcon).toBeTruthy();

            expect(stateIcon.attributes['size']).toEqual('14px');
            expect(stateIcon.nativeNode.state).toEqual(state);
            expect(stateIcon.nativeNode.labels).toEqual(labels);
        });

        describe('row', () => {
            it('should set actions to publish template', () => {
                loadTableData();

                const publishTemplateElement = fixture.debugElement.query(
                    By.css('[data-testid="123Published"]')
                );
                expect(publishTemplateElement).toBeTruthy();
                publishTemplate = publishTemplateElement.componentInstance;

                const actions = setBasicOptions();
                actions.push({
                    menuItem: { label: 'Unpublish', command: expect.any(Function) }
                });
                actions.push({
                    menuItem: { label: 'Copy', command: expect.any(Function) }
                });

                expect(publishTemplate.actions).toEqual(actions);
            });

            it('should set actions to locked template', () => {
                loadTableData();

                const lockedTemplateElement = fixture.debugElement.query(
                    By.css('[data-testid="123Locked"]')
                );
                expect(lockedTemplateElement).toBeTruthy();
                lockedTemplate = lockedTemplateElement.componentInstance;

                const actions = setBasicOptions();
                actions.push({
                    menuItem: { label: 'Unpublish', command: expect.any(Function) }
                });
                actions.push({
                    menuItem: { label: 'Copy', command: expect.any(Function) }
                });

                expect(lockedTemplate.actions).toEqual(actions);
            });

            it('should set actions to unPublish template', () => {
                loadTableData();

                const unPublishTemplateElement = fixture.debugElement.query(
                    By.css('[data-testid="123Unpublish"]')
                );
                expect(unPublishTemplateElement).toBeTruthy();
                unPublishTemplate = unPublishTemplateElement.componentInstance;

                const actions = setBasicOptions();
                actions.push({
                    menuItem: { label: 'Archive', command: expect.any(Function) }
                });
                actions.push({
                    menuItem: { label: 'Copy', command: expect.any(Function) }
                });

                expect(unPublishTemplate.actions).toEqual(actions);
            });

            it('should set actions to archived template', () => {
                loadTableData();

                const archivedTemplateElement = fixture.debugElement.query(
                    By.css('[data-testid="123Archived"]')
                );
                expect(archivedTemplateElement).toBeTruthy();
                archivedTemplate = archivedTemplateElement.componentInstance;

                const actions = [
                    { menuItem: { label: 'Unarchive', command: expect.any(Function) } },
                    { menuItem: { label: 'Delete', command: expect.any(Function) } }
                ];
                expect(archivedTemplate.actions).toEqual(actions);
            });

            it('should hide push-publish and Add to Bundle actions', () => {
                const activatedRoute: ActivatedRoute = TestBed.inject(ActivatedRoute);
                Object.defineProperty(activatedRoute, 'data', {
                    value: of({
                        dotTemplateListResolverData: [false, false]
                    }),
                    writable: true
                });
                comp.ngOnInit();
                fixture.detectChanges();
                loadTableData();

                const publishTemplateElement = fixture.debugElement.query(
                    By.css('[data-testid="123Published"]')
                );
                expect(publishTemplateElement).toBeTruthy();
                publishTemplate = publishTemplateElement.componentInstance;

                const actions = [
                    { menuItem: { label: 'Edit', command: expect.any(Function) } },
                    { menuItem: { label: 'Publish', command: expect.any(Function) } },
                    { menuItem: { label: 'Unpublish', command: expect.any(Function) } },
                    { menuItem: { label: 'Copy', command: expect.any(Function) } }
                ];

                expect(publishTemplate.actions).toEqual(actions);
            });

            describe('template as a file ', () => {
                it('should go to site Broser when selected', () => {
                    dotSiteBrowserService.setSelectedFolder.mockReturnValue(of(null));
                    loadTableData();

                    const rows: DebugElement[] = fixture.debugElement.queryAll(
                        By.css('.p-selectable-row')
                    );
                    expect(rows.length).toBeGreaterThan(0);

                    rows[rows.length - 1].nativeElement.click();
                    expect(dotSiteBrowserService.setSelectedFolder).toHaveBeenCalledWith(
                        templatesMock[4].identifier
                    );
                    expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
                });

                it('should hide the Action Menu', () => {
                    const menu: DebugElement = fixture.debugElement.query(
                        By.css('[data-testid="//dir/asFile"]')
                    );
                    expect(menu).toBeNull();
                });
            });
        });

        describe('row actions command', () => {
            beforeEach(fakeAsync(() => {
                // Load table data first
                jest.spyOn(dotListingDataTable.paginatorService, 'get').mockReturnValue(
                    of(templatesMock)
                );
                const table = fixture.debugElement.query(By.css('p-table'));
                if (table) {
                    table.triggerEventHandler('onLazyLoad', { first: 0, rows: 40 });
                } else {
                    dotListingDataTable.loadData(0);
                }
                tick(1); // Wait for setItems setTimeout
                fixture.detectChanges();

                jest.spyOn(dotMessageDisplayService, 'push');
                jest.spyOn(dotListingDataTable, 'loadCurrentPage');
                publishTemplate = fixture.debugElement.query(
                    By.css('[data-testid="123Published"]')
                ).componentInstance;
                lockedTemplate = fixture.debugElement.query(
                    By.css('[data-testid="123Locked"]')
                ).componentInstance;
                unPublishTemplate = fixture.debugElement.query(
                    By.css('[data-testid="123Unpublish"]')
                ).componentInstance;
                archivedTemplate = fixture.debugElement.query(
                    By.css('[data-testid="123Archived"]')
                ).componentInstance;
            }));

            it('should open add to bundle dialog', () => {
                publishTemplate.actions[3].menuItem.command();
                fixture.detectChanges();
                const addToBundleDialog: DotAddToBundleComponent = fixture.debugElement.query(
                    By.css('dot-add-to-bundle')
                ).componentInstance;
                expect(addToBundleDialog.assetIdentifier).toEqual('123Published');
            });

            it('should open Push Publish dialog', () => {
                publishTemplate.actions[2].menuItem.command();
                expect(dotPushPublishDialogService.open).toHaveBeenCalledWith({
                    assetIdentifier: '123Published',
                    title: 'Push Publish'
                });
            });
            it('should call archive endpoint, send notification and reload current page', () => {
                dotTemplatesService.archive.mockReturnValue(of(mockBulkResponseSuccess));
                unPublishTemplate.actions[4].menuItem.command();

                expect(dotTemplatesService.archive).toHaveBeenCalledWith(['123Unpublish']);
                expect(dotTemplatesService.archive).toHaveBeenCalledTimes(1);
                checkNotificationAndReLoadOfPage('Template archived');
            });
            it('should call unArchive api, send notification and reload current page', () => {
                dotTemplatesService.unArchive.mockReturnValue(of(mockBulkResponseSuccess));
                archivedTemplate.actions[0].menuItem.command();

                expect(dotTemplatesService.unArchive).toHaveBeenCalledWith(['123Archived']);
                expect(dotTemplatesService.unArchive).toHaveBeenCalledTimes(1);
                checkNotificationAndReLoadOfPage('Template unarchived');
            });
            it('should call publish api, send notification and reload current page', () => {
                dotTemplatesService.publish.mockReturnValue(of(mockBulkResponseSuccess));
                unPublishTemplate.actions[1].menuItem.command();

                expect(dotTemplatesService.publish).toHaveBeenCalledWith(['123Unpublish']);
                expect(dotTemplatesService.publish).toHaveBeenCalledTimes(1);
                checkNotificationAndReLoadOfPage('Templates published');
            });
            it('should call unpublish api, send notification and reload current page', () => {
                dotTemplatesService.unPublish.mockReturnValue(of(mockBulkResponseSuccess));
                publishTemplate.actions[4].menuItem.command();

                expect(dotTemplatesService.unPublish).toHaveBeenCalledWith(['123Published']);
                expect(dotTemplatesService.unPublish).toHaveBeenCalledTimes(1);
                checkNotificationAndReLoadOfPage('Template unpublished');
            });
            it('should call copy api, send notification and reload current page', () => {
                dotTemplatesService.copy.mockReturnValue(of(templatesMock[0]));
                publishTemplate.actions[5].menuItem.command();

                expect(dotTemplatesService.copy).toHaveBeenCalledWith('123Published');
                expect(dotTemplatesService.copy).toHaveBeenCalledTimes(1);
                checkNotificationAndReLoadOfPage('Template copied');
            });
            it('should call delete api, send notification and reload current page', () => {
                dotTemplatesService.delete.mockReturnValue(of(mockBulkResponseSuccess));
                jest.spyOn(dotAlertConfirmService, 'confirm').mockImplementation((conf) => {
                    conf.accept();
                });
                archivedTemplate.actions[1].menuItem.command();
                expect(dotTemplatesService.delete).toHaveBeenCalledWith(['123Archived']);
                expect(dotTemplatesService.delete).toHaveBeenCalledTimes(1);
                checkNotificationAndReLoadOfPage('Template deleted');
            });

            it('should handle error request', () => {
                dotTemplatesService.delete.mockReturnValue(of(mockSingleResponseFail));
                jest.spyOn(dotAlertConfirmService, 'confirm').mockImplementation((conf) => {
                    conf.accept();
                });
                archivedTemplate.actions[1].menuItem.command();

                expect(dialogService.open).toHaveBeenCalledWith(DotBulkInformationComponent, {
                    header: 'Results',
                    width: '40rem',
                    contentStyle: { 'max-height': '500px', overflow: 'auto' },
                    baseZIndex: 10000,
                    data: {
                        ...mockSingleResponseFail,
                        fails: [
                            {
                                errorMessage: 'error 1',
                                element: '123Published',
                                description: 'Published template'
                            }
                        ],
                        action: 'Template deleted'
                    }
                });
            });
        });

        describe('bulk', () => {
            let menu: Menu;

            beforeEach(fakeAsync(() => {
                // Load table data first
                jest.spyOn(dotListingDataTable.paginatorService, 'get').mockReturnValue(
                    of(templatesMock)
                );
                const table = fixture.debugElement.query(By.css('p-table'));
                if (table) {
                    table.triggerEventHandler('onLazyLoad', { first: 0, rows: 40 });
                } else {
                    dotListingDataTable.loadData(0);
                }
                tick(1); // Wait for setItems setTimeout
                fixture.detectChanges();

                comp.selectedTemplates = [templatesMock[0], templatesMock[1]];
                fixture.detectChanges();
                menu = fixture.debugElement.query(
                    By.css('.template-listing__header-options p-menu')
                ).componentInstance;
                jest.spyOn(dotMessageDisplayService, 'push');
                jest.spyOn(dotListingDataTable, 'loadCurrentPage');
            }));

            it('should set labels', () => {
                const actions = [
                    { label: 'Publish', command: expect.any(Function) },
                    { label: 'Push Publish', command: expect.any(Function) },
                    { label: 'Add To Bundle', command: expect.any(Function) },
                    { label: 'Unpublish', command: expect.any(Function) },
                    { label: 'Archive', command: expect.any(Function) },
                    { label: 'Unarchive', command: expect.any(Function) },
                    { label: 'Delete', command: expect.any(Function) }
                ];

                expect(menu.model).toEqual(actions);
            });

            it('should execute Publish action', () => {
                dotTemplatesService.publish.mockReturnValue(of(mockBulkResponseSuccess));
                menu.model[0].command({ originalEvent: createFakeEvent('click') });
                expect(dotTemplatesService.publish).toHaveBeenCalledWith([
                    '123Published',
                    '123Locked'
                ]);
                checkNotificationAndReLoadOfPage('Templates published');
            });
            it('should execute Push Publish action', () => {
                menu.model[1].command({ originalEvent: createFakeEvent('click') });
                expect(dotPushPublishDialogService.open).toHaveBeenCalledWith({
                    assetIdentifier: '123Published,123Locked',
                    title: 'Push Publish'
                });
            });
            it('should execute Add To Bundle action', () => {
                menu.model[2].command({ originalEvent: createFakeEvent('click') });
                fixture.detectChanges();
                const addToBundleDialog: DotAddToBundleComponent = fixture.debugElement.query(
                    By.css('dot-add-to-bundle')
                ).componentInstance;
                expect(addToBundleDialog.assetIdentifier).toEqual('123Published,123Locked');
            });
            it('should execute Unpublish action', () => {
                dotTemplatesService.unPublish.mockReturnValue(of(mockBulkResponseSuccess));
                menu.model[3].command({ originalEvent: createFakeEvent('click') });
                expect(dotTemplatesService.unPublish).toHaveBeenCalledWith([
                    '123Published',
                    '123Locked'
                ]);
                checkNotificationAndReLoadOfPage('Template unpublished');
            });
            it('should execute Archive action', () => {
                dotTemplatesService.archive.mockReturnValue(of(mockBulkResponseSuccess));
                menu.model[4].command({ originalEvent: createFakeEvent('click') });
                expect(dotTemplatesService.archive).toHaveBeenCalledWith([
                    '123Published',
                    '123Locked'
                ]);
                checkNotificationAndReLoadOfPage('Template archived');
            });
            it('should execute UnArchive action', () => {
                dotTemplatesService.unArchive.mockReturnValue(of(mockBulkResponseSuccess));
                menu.model[5].command({ originalEvent: createFakeEvent('click') });
                expect(dotTemplatesService.unArchive).toHaveBeenCalledWith([
                    '123Published',
                    '123Locked'
                ]);
                checkNotificationAndReLoadOfPage('Template unarchived');
            });
            it('should execute Delete action', () => {
                dotTemplatesService.delete.mockReturnValue(of(mockBulkResponseSuccess));
                jest.spyOn(dotAlertConfirmService, 'confirm').mockImplementation((conf) => {
                    conf.accept();
                });
                menu.model[6].command({ originalEvent: createFakeEvent('click') });
                expect(dotTemplatesService.delete).toHaveBeenCalledWith([
                    '123Published',
                    '123Locked'
                ]);
                checkNotificationAndReLoadOfPage('Template deleted');
            });
            it('should disable enable bulk action button based on selection', () => {
                const bulkActionsBtn: HTMLButtonElement = fixture.debugElement.query(
                    By.css('.template-listing__header-options button')
                ).nativeElement;
                expect(bulkActionsBtn.disabled).toEqual(false);
                comp.selectedTemplates = [];
                fixture.detectChanges();
                expect(bulkActionsBtn.disabled).toEqual(true);
            });

            describe('error', () => {
                it('should fire exception on publish', () => {
                    dotTemplatesService.publish.mockReturnValue(of(mockBulkResponseFail));
                    menu.model[0].command({ originalEvent: createFakeEvent('click') });
                    checkOpenOfDialogService('Templates published');
                });
                it('should fire exception on unPublish', () => {
                    dotTemplatesService.unPublish.mockReturnValue(of(mockBulkResponseFail));
                    menu.model[3].command({ originalEvent: createFakeEvent('click') });
                    checkOpenOfDialogService('Template unpublished');
                });
                it('should fire exception on archive', () => {
                    dotTemplatesService.archive.mockReturnValue(of(mockBulkResponseFail));
                    menu.model[4].command({ originalEvent: createFakeEvent('click') });
                    checkOpenOfDialogService('Template archived');
                });
                it('should fire exception on unArchive', () => {
                    dotTemplatesService.unArchive.mockReturnValue(of(mockBulkResponseFail));
                    menu.model[5].command({ originalEvent: createFakeEvent('click') });
                    checkOpenOfDialogService('Template unarchived');
                });
                it('should fire exception on delete', () => {
                    dotTemplatesService.delete.mockReturnValue(of(mockBulkResponseFail));
                    jest.spyOn(dotAlertConfirmService, 'confirm').mockImplementation((conf) => {
                        conf.accept();
                    });
                    menu.model[6].command({ originalEvent: createFakeEvent('click') });
                    checkOpenOfDialogService('Template deleted');
                });
            });
        });
    });

    describe('without data', () => {
        beforeEach(() => {
            jest.spyOn(coreWebService, 'requestView').mockReturnValue(
                of({
                    entity: [],
                    header: (type) => (type === 'Link' ? 'test;test=test' : '10')
                } as any)
            );
            fixture.detectChanges();
        });

        it('should set dot-empty-state if the templates array is empty', () => {
            const emptyState = fixture.debugElement.query(By.css('dot-empty-state'));
            expect(emptyState).toBeDefined();
        });
    });

    function setBasicOptions(): any {
        return [
            { menuItem: { label: 'Edit', command: expect.any(Function) } },
            { menuItem: { label: 'Publish', command: expect.any(Function) } },
            { menuItem: { label: 'Push Publish', command: expect.any(Function) } },
            { menuItem: { label: 'Add To Bundle', command: expect.any(Function) } }
        ];
    }

    function checkNotificationAndReLoadOfPage(messsage: string): void {
        expect(dotMessageDisplayService.push).toHaveBeenCalledWith({
            ...mockMessageConfig,
            message: messsage
        });
        expect(dotListingDataTable.loadCurrentPage).toHaveBeenCalledTimes(1);
    }

    function checkOpenOfDialogService(action: string): void {
        expect(dialogService.open).toHaveBeenCalledWith(DotBulkInformationComponent, {
            header: 'Results',
            width: '40rem',
            contentStyle: { 'max-height': '500px', overflow: 'auto' },
            baseZIndex: 10000,
            data: {
                ...mockBulkResponseFail,
                fails: [
                    {
                        errorMessage: 'error 1',
                        element: '123Published',
                        description: 'Published template'
                    },
                    {
                        errorMessage: 'error 2',
                        element: '123Locked',
                        description: 'Locked template'
                    }
                ],
                action: action
            }
        });
    }
});
