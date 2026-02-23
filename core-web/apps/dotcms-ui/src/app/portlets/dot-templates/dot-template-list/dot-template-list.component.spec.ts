/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable no-console */

import { of, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, flush, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, MenuItem, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';

import {
    DotAlertConfirmService,
    DotFormatDateService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotSiteBrowserService,
    PushPublishService
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
    DotContentletStatusChipComponent,
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

// Mock window.matchMedia (required by PrimeNG ContextMenu)
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: jest.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(),
        removeListener: jest.fn(),
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn()
    }))
});

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
import { dotEventSocketURLFactory } from '../../../test/dot-test-bed';
import { DotActionButtonComponent } from '../../../view/components/_common/dot-action-button/dot-action-button.component';
import { DotBulkInformationComponent } from '../../../view/components/_common/dot-bulk-information/dot-bulk-information.component';

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

type DotTemplatesServiceSpy = {
    [K in keyof Pick<
        DotTemplatesService,
        'archive' | 'unArchive' | 'publish' | 'unPublish' | 'copy' | 'delete' | 'getFiltered'
    >]: jest.Mock;
};

describe('DotTemplateListComponent', () => {
    let fixture: ComponentFixture<DotTemplateListComponent>;
    let dotTemplatesService: DotTemplatesServiceSpy;
    let dotMessageDisplayService: DotMessageDisplayService;
    let dotPushPublishDialogService: DotPushPublishDialogService;
    let dotRouterService: DotRouterService;
    let dialogService: DialogService;

    let comp: DotTemplateListComponent;
    let dotAlertConfirmService: DotAlertConfirmService;
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
            delete: jest.fn(),
            getFiltered: jest
                .fn()
                .mockReturnValue(
                    of({ templates: templatesMock, totalRecords: templatesMock.length })
                )
        };
        const dotSiteBrowserServiceSpy = {
            setSelectedFolder: jest.fn().mockReturnValue(of(null))
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
                DotPushPublishDialogService,
                {
                    provide: PushPublishService,
                    useValue: { getEnvironments: jest.fn().mockReturnValue(of([])) }
                }
            ],
            imports: [
                DotTemplateListComponent,
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
                DotContentletStatusChipComponent,
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
        // Avoid ExpressionChangedAfterItHasBeenCheckedError when getFiltered updates state during change detection
        const originalDetectChanges = fixture.detectChanges.bind(fixture);
        fixture.detectChanges = (_checkNoChanges?: boolean) => originalDetectChanges(false);
        dotTemplatesService = dotTemplatesServiceSpy as unknown as DotTemplatesServiceSpy;
        dotMessageDisplayService = TestBed.inject(DotMessageDisplayService);
        dotPushPublishDialogService = TestBed.inject(DotPushPublishDialogService);
        dotRouterService = dotRouterServiceSpy;
        dialogService = dialogServiceSpy;
        dotAlertConfirmService = TestBed.inject(DotAlertConfirmService);
        void TestBed.inject(CoreWebService);
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
            fixture.detectChanges();
            tick(1);
            fixture.detectChanges();
            jest.spyOn(dotPushPublishDialogService, 'open');

            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose: dialogRefClose
            } as any);

            mockGoToFolder = jest.spyOn(comp, 'goToFolder');
        }));

        // Helper: ensure getFiltered and loadEnvironments have completed; table has data. Call only from within fakeAsync().
        const loadTableData = () => {
            fixture.detectChanges();
            flush();
            fixture.detectChanges();
        };

        const openRowContextMenu = (testId: string) => {
            const btn = fixture.debugElement.query(By.css(`[data-testid="${testId}"]`));
            if (btn?.nativeElement) {
                btn.nativeElement.click();
                fixture.detectChanges();
            }
        };

        it('should reload portlet only when the site change', () => {
            fixture.detectChanges(); // Initialize component and subscriptions
            siteServiceMock.setFakeCurrentSite(mockSites[1]); // switching the site
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('templates');
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        });

        it('should set table state (columns, sortField, sortOrder)', () => {
            const state = comp.$state() as unknown as {
                tableColumns: typeof columnsMock;
                sortField: string;
                sortOrder: number;
            };
            expect(state.tableColumns.length).toBe(columnsMock.length);
            expect(state.sortField).toEqual('modDate');
            expect(state.sortOrder).toEqual(-1);
        });

        it('should have links for theme folder', fakeAsync(() => {
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
        }));

        it('should trigger goToFolder whem clicking on a theme link', fakeAsync(() => {
            loadTableData();

            const link = fixture.debugElement.query(By.css('[data-testid="theme-folder-link"]'));
            expect(link).toBeTruthy();

            link.nativeElement.click();

            expect(mockGoToFolder).toHaveBeenCalledWith(expect.any(Event), 'test');
        }));

        it("should render 'System Theme' when the theme is SYSTEM_THEME", fakeAsync(() => {
            loadTableData();

            const cells = fixture.debugElement.queryAll(By.css('[data-testid="theme-cell"]'));
            expect(cells.length).toBeGreaterThan(1);

            expect(cells[1].nativeElement.textContent.trim()).toEqual('System Theme');
        }));

        it('should not trigger goToFolder when the theme is SYSTEM_THEME', fakeAsync(() => {
            loadTableData();

            const cells = fixture.debugElement.queryAll(By.css('[data-testid="theme-cell"]'));
            expect(cells.length).toBeGreaterThan(1);

            cells[1].nativeElement.click();

            expect(mockGoToFolder).not.toHaveBeenCalled();
        }));

        it('should render empty when the theme is undefined or null', fakeAsync(() => {
            loadTableData();

            const cells = fixture.debugElement.queryAll(By.css('[data-testid="theme-cell"]'));
            expect(cells.length).toBeGreaterThan(0);

            const lastCell = cells.pop();
            expect(lastCell).toBeTruthy();

            expect(lastCell.nativeElement.textContent.trim()).toEqual('');
        }));

        it('should not trigger goToFolder when the theme is null or undefined', fakeAsync(() => {
            loadTableData();

            const cells = fixture.debugElement.queryAll(By.css('[data-testid="theme-cell"]'));
            expect(cells.length).toBeGreaterThan(0);

            const lastCell = cells.pop();
            expect(lastCell).toBeTruthy();

            lastCell.nativeElement.click();

            expect(mockGoToFolder).not.toHaveBeenCalled();
        }));

        it('should set Action Header options correctly', () => {
            const addBtn = fixture.debugElement.query(By.css('[data-testid="addTemplate"]'));
            expect(addBtn).toBeTruthy();
            addBtn.nativeElement.click();
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/templates/new');
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        });

        it('should pass data to the status chip component', fakeAsync(() => {
            loadTableData();

            const state: DotContentState = {
                live: true,
                working: true,
                deleted: false,
                hasLiveVersion: true
            };

            const statusChips = fixture.debugElement.queryAll(By.css('dot-contentlet-status-chip'));
            expect(statusChips.length).toBeGreaterThan(0);
            const chipForLocked = statusChips[1];
            expect(chipForLocked).toBeTruthy();

            const chipComponent =
                chipForLocked.componentInstance as DotContentletStatusChipComponent;
            expect(chipComponent.state()).toEqual(state);
        }));

        describe('row', () => {
            it('should set actions to publish template', fakeAsync(() => {
                loadTableData();
                openRowContextMenu('123Published');

                expect(comp.contextMenuItems.length).toBeGreaterThan(0);
                const labels = comp.contextMenuItems.map((m) => m.label);
                expect(labels).toContain('Edit');
                expect(labels).toContain('Unpublish');
                expect(labels).toContain('Copy');
            }));

            it('should set actions to locked template', fakeAsync(() => {
                loadTableData();
                openRowContextMenu('123Locked');

                expect(comp.contextMenuItems.length).toBeGreaterThan(0);
                const labels = comp.contextMenuItems.map((m) => m.label);
                expect(labels).toContain('Edit');
                expect(labels).toContain('Unpublish');
                expect(labels).toContain('Copy');
            }));

            it('should set actions to unPublish template', fakeAsync(() => {
                loadTableData();
                openRowContextMenu('123Unpublish');

                expect(comp.contextMenuItems.length).toBeGreaterThan(0);
                const labels = comp.contextMenuItems.map((m) => m.label);
                expect(labels).toContain('Archive');
                expect(labels).toContain('Copy');
            }));

            it('should set actions to archived template', fakeAsync(() => {
                loadTableData();
                openRowContextMenu('123Archived');

                expect(comp.contextMenuItems.length).toBeGreaterThan(0);
                const labels = comp.contextMenuItems.map((m) => m.label);
                expect(labels).toContain('Unarchive');
                expect(labels).toContain('Delete');
            }));

            it('should set actions to archived template (full list)', fakeAsync(() => {
                loadTableData();
                openRowContextMenu('123Archived');

                const labels = comp.contextMenuItems.map((m) => m.label);
                expect(labels).toContain('Unarchive');
                expect(labels).toContain('Delete');
            }));

            it('should hide push-publish and Add to Bundle actions', fakeAsync(() => {
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
                openRowContextMenu('123Published');

                const labels = comp.contextMenuItems.map((m) => m.label);
                expect(labels).not.toContain('Push Publish');
                expect(labels).toContain('Edit');
                expect(labels).toContain('Unpublish');
            }));

            describe('template as a file ', () => {
                it('should go to site Broser when selected', fakeAsync(() => {
                    dotSiteBrowserService.setSelectedFolder.mockReturnValue(of(null));
                    loadTableData();

                    const rows: DebugElement[] = fixture.debugElement.queryAll(
                        By.css('[data-testid="item-row"]')
                    );
                    expect(rows.length).toBeGreaterThan(0);

                    // Find the row for "Template as a File" and click the name span to trigger onRowClick -> editTemplate -> setSelectedFolder
                    const fileTemplateRow = rows.find(
                        (r) => r.nativeElement.textContent?.includes('Template as a File') ?? false
                    );
                    expect(fileTemplateRow).toBeTruthy();
                    const nameCell = fileTemplateRow!.query(By.css('td span'));
                    nameCell.nativeElement.click();
                    expect(dotSiteBrowserService.setSelectedFolder).toHaveBeenCalledWith(
                        templatesMock[4].identifier
                    );
                    expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
                }));

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
                loadTableData();
                jest.spyOn(dotMessageDisplayService, 'push');
                jest.spyOn(comp, 'loadCurrentPage');
            }));

            const getActionIndex = (labels: string[], label: string) =>
                labels.findIndex((l) => l === label);

            it('should open add to bundle dialog', () => {
                openRowContextMenu('123Published');
                const addToBundleIdx = getActionIndex(
                    comp.contextMenuItems.map((m) => m.label),
                    'Add To Bundle'
                );
                comp.contextMenuItems[addToBundleIdx].command!({
                    originalEvent: createFakeEvent('click')
                } as any);
                fixture.detectChanges();
                const addToBundleDialog: DotAddToBundleComponent = fixture.debugElement.query(
                    By.css('dot-add-to-bundle')
                ).componentInstance;
                expect(addToBundleDialog.assetIdentifier).toEqual('123Published');
            });

            it('should open Push Publish dialog', () => {
                openRowContextMenu('123Published');
                const labels = comp.contextMenuItems.map((m) => m.label);
                const pushPublishIdx = getActionIndex(labels, 'Push Publish');
                if (pushPublishIdx >= 0) {
                    comp.contextMenuItems[pushPublishIdx].command!({
                        originalEvent: createFakeEvent('click')
                    } as any);
                    expect(dotPushPublishDialogService.open).toHaveBeenCalledWith({
                        assetIdentifier: '123Published',
                        title: 'Push Publish'
                    });
                }
            });
            it('should call archive endpoint, send notification and reload current page', () => {
                dotTemplatesService.archive.mockReturnValue(of(mockBulkResponseSuccess));
                openRowContextMenu('123Unpublish');
                const archiveIdx = getActionIndex(
                    comp.contextMenuItems.map((m) => m.label),
                    'Archive'
                );
                comp.contextMenuItems[archiveIdx].command!({
                    originalEvent: createFakeEvent('click')
                } as any);

                expect(dotTemplatesService.archive).toHaveBeenCalledWith(['123Unpublish']);
                expect(dotTemplatesService.archive).toHaveBeenCalledTimes(1);
                checkNotificationAndReLoadOfPage('Template archived');
            });
            it('should call unArchive api, send notification and reload current page', () => {
                dotTemplatesService.unArchive.mockReturnValue(of(mockBulkResponseSuccess));
                openRowContextMenu('123Archived');
                const unarchiveIdx = getActionIndex(
                    comp.contextMenuItems.map((m) => m.label),
                    'Unarchive'
                );
                comp.contextMenuItems[unarchiveIdx].command!({
                    originalEvent: createFakeEvent('click')
                } as any);

                expect(dotTemplatesService.unArchive).toHaveBeenCalledWith(['123Archived']);
                expect(dotTemplatesService.unArchive).toHaveBeenCalledTimes(1);
                checkNotificationAndReLoadOfPage('Template unarchived');
            });
            it('should call publish api, send notification and reload current page', () => {
                dotTemplatesService.publish.mockReturnValue(of(mockBulkResponseSuccess));
                openRowContextMenu('123Unpublish');
                const publishIdx = getActionIndex(
                    comp.contextMenuItems.map((m) => m.label),
                    'Publish'
                );
                comp.contextMenuItems[publishIdx].command!({
                    originalEvent: createFakeEvent('click')
                } as any);

                expect(dotTemplatesService.publish).toHaveBeenCalledWith(['123Unpublish']);
                expect(dotTemplatesService.publish).toHaveBeenCalledTimes(1);
                checkNotificationAndReLoadOfPage('Templates published');
            });
            it('should call unpublish api, send notification and reload current page', () => {
                dotTemplatesService.unPublish.mockReturnValue(of(mockBulkResponseSuccess));
                openRowContextMenu('123Published');
                const unpublishIdx = getActionIndex(
                    comp.contextMenuItems.map((m) => m.label),
                    'Unpublish'
                );
                comp.contextMenuItems[unpublishIdx].command!({
                    originalEvent: createFakeEvent('click')
                } as any);

                expect(dotTemplatesService.unPublish).toHaveBeenCalledWith(['123Published']);
                expect(dotTemplatesService.unPublish).toHaveBeenCalledTimes(1);
                checkNotificationAndReLoadOfPage('Template unpublished');
            });
            it('should call copy api, send notification and reload current page', () => {
                dotTemplatesService.copy.mockReturnValue(of(templatesMock[0]));
                openRowContextMenu('123Published');
                const copyIdx = getActionIndex(
                    comp.contextMenuItems.map((m) => m.label),
                    'Copy'
                );
                comp.contextMenuItems[copyIdx].command!({
                    originalEvent: createFakeEvent('click')
                } as any);

                expect(dotTemplatesService.copy).toHaveBeenCalledWith('123Published');
                expect(dotTemplatesService.copy).toHaveBeenCalledTimes(1);
                checkNotificationAndReLoadOfPage('Template copied');
            });
            it('should call delete api, send notification and reload current page', () => {
                dotTemplatesService.delete.mockReturnValue(of(mockBulkResponseSuccess));
                jest.spyOn(dotAlertConfirmService, 'confirm').mockImplementation((conf) => {
                    conf.accept();
                });
                openRowContextMenu('123Archived');
                const deleteIdx = getActionIndex(
                    comp.contextMenuItems.map((m) => m.label),
                    'Delete'
                );
                comp.contextMenuItems[deleteIdx].command!({
                    originalEvent: createFakeEvent('click')
                } as any);
                expect(dotTemplatesService.delete).toHaveBeenCalledWith(['123Archived']);
                expect(dotTemplatesService.delete).toHaveBeenCalledTimes(1);
                checkNotificationAndReLoadOfPage('Template deleted');
            });

            it('should handle error request', () => {
                dotTemplatesService.delete.mockReturnValue(of(mockSingleResponseFail));
                jest.spyOn(dotAlertConfirmService, 'confirm').mockImplementation((conf) => {
                    conf.accept();
                });
                openRowContextMenu('123Archived');
                const deleteIdx = getActionIndex(
                    comp.contextMenuItems.map((m) => m.label),
                    'Delete'
                );
                comp.contextMenuItems[deleteIdx].command!({
                    originalEvent: createFakeEvent('click')
                } as any);

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
            const getBulkActions = (): MenuItem[] =>
                (comp.$state() as unknown as { templateBulkActions: MenuItem[] })
                    .templateBulkActions;

            beforeEach(fakeAsync(() => {
                loadTableData();
                comp.selectedTemplates = [templatesMock[0], templatesMock[1]];
                comp.onSelectionChange();
                fixture.detectChanges();
                jest.spyOn(dotMessageDisplayService, 'push');
                jest.spyOn(comp, 'loadCurrentPage');
            }));

            const bulkActionIndex = (label: string) =>
                getBulkActions().findIndex((m) => m.label === label);

            it('should set labels', () => {
                const labels = getBulkActions().map((m) => m.label);
                expect(labels).toContain('Publish');
                expect(labels).toContain('Add To Bundle');
                expect(labels).toContain('Unpublish');
                expect(labels).toContain('Archive');
                expect(labels).toContain('Unarchive');
                expect(labels).toContain('Delete');
            });

            it('should execute Publish action', () => {
                dotTemplatesService.publish.mockReturnValue(of(mockBulkResponseSuccess));
                getBulkActions()[bulkActionIndex('Publish')].command!({
                    originalEvent: createFakeEvent('click')
                });
                expect(dotTemplatesService.publish).toHaveBeenCalledWith([
                    '123Published',
                    '123Locked'
                ]);
                checkNotificationAndReLoadOfPage('Templates published');
            });
            it('should execute Push Publish action when environments exist', () => {
                const actions = getBulkActions();
                const idx = bulkActionIndex('Push Publish');
                if (idx >= 0) {
                    actions[idx].command!({ originalEvent: createFakeEvent('click') });
                    expect(dotPushPublishDialogService.open).toHaveBeenCalledWith({
                        assetIdentifier: '123Published,123Locked',
                        title: 'Push Publish'
                    });
                }
            });
            it('should execute Add To Bundle action', () => {
                getBulkActions()[bulkActionIndex('Add To Bundle')].command!({
                    originalEvent: createFakeEvent('click')
                });
                fixture.detectChanges();
                const addToBundleEl = fixture.debugElement.query(By.css('dot-add-to-bundle'));
                const assetId =
                    addToBundleEl?.componentInstance?.assetIdentifier ??
                    (comp.$state() as { addToBundleIdentifier: string | null })
                        .addToBundleIdentifier;
                expect(assetId).toEqual('123Published,123Locked');
            });
            it('should execute Unpublish action', () => {
                dotTemplatesService.unPublish.mockReturnValue(of(mockBulkResponseSuccess));
                getBulkActions()[bulkActionIndex('Unpublish')].command!({
                    originalEvent: createFakeEvent('click')
                });
                expect(dotTemplatesService.unPublish).toHaveBeenCalledWith([
                    '123Published',
                    '123Locked'
                ]);
                checkNotificationAndReLoadOfPage('Template unpublished');
            });
            it('should execute Archive action', () => {
                dotTemplatesService.archive.mockReturnValue(of(mockBulkResponseSuccess));
                getBulkActions()[bulkActionIndex('Archive')].command!({
                    originalEvent: createFakeEvent('click')
                });
                expect(dotTemplatesService.archive).toHaveBeenCalledWith([
                    '123Published',
                    '123Locked'
                ]);
                checkNotificationAndReLoadOfPage('Template archived');
            });
            it('should execute UnArchive action', () => {
                dotTemplatesService.unArchive.mockReturnValue(of(mockBulkResponseSuccess));
                getBulkActions()[bulkActionIndex('Unarchive')].command!({
                    originalEvent: createFakeEvent('click')
                });
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
                getBulkActions()[bulkActionIndex('Delete')].command!({
                    originalEvent: createFakeEvent('click')
                });
                expect(dotTemplatesService.delete).toHaveBeenCalledWith([
                    '123Published',
                    '123Locked'
                ]);
                checkNotificationAndReLoadOfPage('Template deleted');
            });
            it('should disable enable bulk action button based on selection', () => {
                const bulkActionsHost = fixture.debugElement.query(
                    By.css('[data-testid="bulkActions"]')
                ).nativeElement as HTMLElement;
                const bulkActionsBtn = bulkActionsHost.querySelector?.('button') ?? bulkActionsHost;
                expect((bulkActionsBtn as HTMLButtonElement).disabled).toEqual(false);
                comp.selectedTemplates = [];
                comp.onSelectionChange();
                fixture.detectChanges();
                const btnAfter = (bulkActionsHost.querySelector?.('button') ??
                    bulkActionsHost) as HTMLButtonElement;
                expect(btnAfter.disabled).toEqual(true);
            });

            describe('error', () => {
                it('should fire exception on publish', () => {
                    dotTemplatesService.publish.mockReturnValue(of(mockBulkResponseFail));
                    getBulkActions()[bulkActionIndex('Publish')].command!({
                        originalEvent: createFakeEvent('click')
                    });
                    checkOpenOfDialogService('Templates published');
                });
                it('should fire exception on unPublish', () => {
                    dotTemplatesService.unPublish.mockReturnValue(of(mockBulkResponseFail));
                    getBulkActions()[bulkActionIndex('Unpublish')].command!({
                        originalEvent: createFakeEvent('click')
                    });
                    checkOpenOfDialogService('Template unpublished');
                });
                it('should fire exception on archive', () => {
                    dotTemplatesService.archive.mockReturnValue(of(mockBulkResponseFail));
                    getBulkActions()[bulkActionIndex('Archive')].command!({
                        originalEvent: createFakeEvent('click')
                    });
                    checkOpenOfDialogService('Template archived');
                });
                it('should fire exception on unArchive', () => {
                    dotTemplatesService.unArchive.mockReturnValue(of(mockBulkResponseFail));
                    getBulkActions()[bulkActionIndex('Unarchive')].command!({
                        originalEvent: createFakeEvent('click')
                    });
                    checkOpenOfDialogService('Template unarchived');
                });
                it('should fire exception on delete', () => {
                    dotTemplatesService.delete.mockReturnValue(of(mockBulkResponseFail));
                    jest.spyOn(dotAlertConfirmService, 'confirm').mockImplementation((conf) => {
                        conf.accept();
                    });
                    getBulkActions()[bulkActionIndex('Delete')].command!({
                        originalEvent: createFakeEvent('click')
                    });
                    checkOpenOfDialogService('Template deleted');
                });
            });
        });
    });

    describe('without data', () => {
        beforeEach(fakeAsync(() => {
            dotTemplatesService.getFiltered.mockReturnValue(of({ templates: [], totalRecords: 0 }));
            fixture = TestBed.createComponent(DotTemplateListComponent);
            comp = fixture.componentInstance;
            fixture.detectChanges();
            tick(1);
            fixture.detectChanges();
        }));

        it('should set dot-empty-state if the templates array is empty', () => {
            const emptyState = fixture.debugElement.query(By.css('dot-empty-state'));
            expect(emptyState).toBeDefined();
        });
    });

    function checkNotificationAndReLoadOfPage(messsage: string): void {
        expect(dotMessageDisplayService.push).toHaveBeenCalledWith({
            ...mockMessageConfig,
            message: messsage
        });
        expect(comp.loadCurrentPage).toHaveBeenCalledTimes(1);
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
