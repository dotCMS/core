/* eslint-disable @typescript-eslint/no-explicit-any */

import { createFakeEvent } from '@ngneat/spectator';
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
    DotFormatDateServiceMock,
    MockDotMessageService,
    mockSites,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotTemplateListComponent } from './dot-template-list.component';

import { DotTemplatesService } from '../../../api/services/dot-templates/dot-templates.service';
import { ButtonModel } from '../../../shared/models/action-header/button.model';
import { dotEventSocketURLFactory } from '../../../test/dot-test-bed';
import { DotActionButtonModule } from '../../../view/components/_common/dot-action-button/dot-action-button.module';
import { DotBulkInformationComponent } from '../../../view/components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotListingDataTableComponent } from '../../../view/components/dot-listing-data-table/dot-listing-data-table.component';
import { DotListingDataTableModule } from '../../../view/components/dot-listing-data-table/dot-listing-data-table.module';

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
    let mockGoToFolder: jasmine.Spy;

    const messageServiceMock = new MockDotMessageService(messages);

    const dialogRefClose = new Subject();
    const siteServiceMock = new SiteServiceMock();

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotTemplateListComponent],
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
                        goToEditTemplate: jasmine.createSpy(),
                        goToSiteBrowser: jasmine.createSpy()
                    }
                },
                {
                    provide: SiteService,
                    useValue: siteServiceMock
                },
                StringUtils,
                DotTemplatesService,
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService,
                DotcmsEventsService,
                DotEventsSocket,
                DotcmsConfigService,
                DotMessageDisplayService,
                DialogService,
                DotSiteBrowserService,
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
                {
                    provide: LoginService,
                    useValue: { currentUserLanguageId: 'en-US' }
                }
            ],
            imports: [
                DotListingDataTableModule,
                CommonModule,
                DotMessagePipe,
                DotRelativeDatePipe,
                SharedModule,
                CheckboxModule,
                MenuModule,
                ButtonModule,
                DotActionButtonModule,
                DotActionMenuButtonComponent,
                DotAddToBundleComponent,
                HttpClientTestingModule,
                DynamicDialogModule,
                BrowserAnimationsModule
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();
        fixture = TestBed.createComponent(DotTemplateListComponent);
        comp = fixture.componentInstance;
        dotTemplatesService = TestBed.inject(DotTemplatesService);
        dotMessageDisplayService = TestBed.inject(DotMessageDisplayService);
        dotPushPublishDialogService = TestBed.inject(DotPushPublishDialogService);
        dotRouterService = TestBed.inject(DotRouterService);
        dialogService = TestBed.inject(DialogService);
        dotAlertConfirmService = TestBed.inject(DotAlertConfirmService);
        coreWebService = TestBed.inject(CoreWebService);
        dotSiteBrowserService = TestBed.inject(DotSiteBrowserService);
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
            spyOn<any>(coreWebService, 'requestView').and.returnValue(
                of({
                    entity: templatesMock,
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

            spyOn<any>(dialogService, 'open').and.returnValue({
                onClose: dialogRefClose
            });

            mockGoToFolder = spyOn(comp, 'goToFolder');
        }));

        it('should reload portlet only when the site change', () => {
            fixture.detectChanges(); // Initialize component and subscriptions
            siteServiceMock.setFakeCurrentSite(mockSites[1]); // switching the site
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('templates');
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
            const link = fixture.debugElement.query(By.css('[data-testid="theme-folder-link"]'));

            link.nativeElement.click();

            expect(mockGoToFolder).toHaveBeenCalledWith(new PointerEvent('click'), 'test');
        });

        it("should render 'System Theme' when the theme is SYSTEM_THEME", () => {
            const cells = fixture.debugElement.queryAll(By.css('[data-testid="theme-cell"]'));

            expect(cells[1].nativeElement.textContent.trim()).toEqual('System Theme');
        });

        it('should not trigger goToFolder when the theme is SYSTEM_THEME', () => {
            const cells = fixture.debugElement.queryAll(By.css('[data-testid="theme-cell"]'));

            cells[1].nativeElement.click();

            expect(mockGoToFolder).not.toHaveBeenCalled();
        });

        it('should render empty when the theme is undefined or null', () => {
            const cells = fixture.debugElement.queryAll(By.css('[data-testid="theme-cell"]'));

            const lastCell = cells.pop();

            expect(lastCell.nativeElement.textContent.trim()).toEqual('');
        });

        it('should not trigger goToFolder when the theme is null or undefined', () => {
            const cells = fixture.debugElement.queryAll(By.css('[data-testid="theme-cell"]'));

            const lastCell = cells.pop();

            lastCell.nativeElement.click();

            expect(mockGoToFolder).not.toHaveBeenCalled();
        });

        it('should set Action Header options correctly', () => {
            const model: ButtonModel[] = dotListingDataTable.actionHeaderOptions.primary.model;
            expect(model).toBeUndefined();

            dotListingDataTable.actionHeaderOptions.primary.command();
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/templates/new');
        });

        it('should pass data to the status elements', () => {
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
            lockedTemplate = fixture.debugElement.query(
                By.css('[data-testid="123Locked"]')
            ).componentInstance;
            const stateIcon = fixture.debugElement.query(By.css('dot-state-icon'));

            expect(stateIcon.attributes['size']).toEqual('14px');
            expect(stateIcon.nativeNode.state).toEqual(state);
            expect(stateIcon.nativeNode.labels).toEqual(labels);
        });

        describe('row', () => {
            it('should set actions to publish template', () => {
                publishTemplate = fixture.debugElement.query(
                    By.css('[data-testid="123Published"]')
                ).componentInstance;
                const actions = setBasicOptions();
                actions.push({
                    menuItem: { label: 'Unpublish', command: jasmine.any(Function) }
                });
                actions.push({
                    menuItem: { label: 'Copy', command: jasmine.any(Function) }
                });

                expect(publishTemplate.actions).toEqual(actions);
            });

            it('should set actions to locked template', () => {
                lockedTemplate = fixture.debugElement.query(
                    By.css('[data-testid="123Locked"]')
                ).componentInstance;
                const actions = setBasicOptions();
                actions.push({
                    menuItem: { label: 'Unpublish', command: jasmine.any(Function) }
                });
                actions.push({
                    menuItem: { label: 'Copy', command: jasmine.any(Function) }
                });

                expect(lockedTemplate.actions).toEqual(actions);
            });

            it('should set actions to unPublish template', () => {
                unPublishTemplate = fixture.debugElement.query(
                    By.css('[data-testid="123Unpublish"]')
                ).componentInstance;
                const actions = setBasicOptions();
                actions.push({
                    menuItem: { label: 'Archive', command: jasmine.any(Function) }
                });
                actions.push({
                    menuItem: { label: 'Copy', command: jasmine.any(Function) }
                });

                expect(unPublishTemplate.actions).toEqual(actions);
            });

            it('should set actions to archived template', () => {
                archivedTemplate = fixture.debugElement.query(
                    By.css('[data-testid="123Archived"]')
                ).componentInstance;
                const actions = [
                    { menuItem: { label: 'Unarchive', command: jasmine.any(Function) } },
                    { menuItem: { label: 'Delete', command: jasmine.any(Function) } }
                ];
                expect(archivedTemplate.actions).toEqual(actions);
            });

            it('should hide push-publish and Add to Bundle actions', () => {
                const activatedRoute: ActivatedRoute = TestBed.inject(ActivatedRoute);
                spyOnProperty(activatedRoute, 'data', 'get').and.returnValue(
                    of({
                        dotTemplateListResolverData: [false, false]
                    })
                );
                comp.ngOnInit();
                fixture.detectChanges();
                publishTemplate = fixture.debugElement.query(
                    By.css('[data-testid="123Published"]')
                ).componentInstance;
                const actions = [
                    { menuItem: { label: 'Edit', command: jasmine.any(Function) } },
                    { menuItem: { label: 'Publish', command: jasmine.any(Function) } },
                    { menuItem: { label: 'Unpublish', command: jasmine.any(Function) } },
                    { menuItem: { label: 'Copy', command: jasmine.any(Function) } }
                ];

                expect(publishTemplate.actions).toEqual(actions);
            });

            describe('template as a file ', () => {
                it('should go to site Broser when selected', () => {
                    spyOn(dotSiteBrowserService, 'setSelectedFolder').and.returnValue(of(null));
                    const rows: DebugElement[] = fixture.debugElement.queryAll(
                        By.css('.p-selectable-row')
                    );
                    fixture.detectChanges();
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
            beforeEach(() => {
                spyOn(dotMessageDisplayService, 'push');
                spyOn(dotListingDataTable, 'loadCurrentPage');
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
            });

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
                spyOn(dotTemplatesService, 'archive').and.returnValue(of(mockBulkResponseSuccess));
                unPublishTemplate.actions[4].menuItem.command();

                expect(dotTemplatesService.archive).toHaveBeenCalledWith(['123Unpublish']);
                checkNotificationAndReLoadOfPage('Template archived');
            });
            it('should call unArchive api, send notification and reload current page', () => {
                spyOn(dotTemplatesService, 'unArchive').and.returnValue(
                    of(mockBulkResponseSuccess)
                );
                archivedTemplate.actions[0].menuItem.command();

                expect(dotTemplatesService.unArchive).toHaveBeenCalledWith(['123Archived']);
                checkNotificationAndReLoadOfPage('Template unarchived');
            });
            it('should call publish api, send notification and reload current page', () => {
                spyOn(dotTemplatesService, 'publish').and.returnValue(of(mockBulkResponseSuccess));
                unPublishTemplate.actions[1].menuItem.command();

                expect(dotTemplatesService.publish).toHaveBeenCalledWith(['123Unpublish']);
                checkNotificationAndReLoadOfPage('Templates published');
            });
            it('should call unpublish api, send notification and reload current page', () => {
                spyOn(dotTemplatesService, 'unPublish').and.returnValue(
                    of(mockBulkResponseSuccess)
                );
                publishTemplate.actions[4].menuItem.command();

                expect(dotTemplatesService.unPublish).toHaveBeenCalledWith(['123Published']);
                checkNotificationAndReLoadOfPage('Template unpublished');
            });
            it('should call copy api, send notification and reload current page', () => {
                spyOn(dotTemplatesService, 'copy').and.returnValue(of(templatesMock[0]));
                publishTemplate.actions[5].menuItem.command();

                expect(dotTemplatesService.copy).toHaveBeenCalledWith('123Published');
                checkNotificationAndReLoadOfPage('Template copied');
            });
            it('should call delete api, send notification and reload current page', () => {
                spyOn(dotTemplatesService, 'delete').and.returnValue(of(mockBulkResponseSuccess));
                spyOn(dotAlertConfirmService, 'confirm').and.callFake((conf) => {
                    conf.accept();
                });
                archivedTemplate.actions[1].menuItem.command();
                expect(dotTemplatesService.delete).toHaveBeenCalledWith(['123Archived']);
                checkNotificationAndReLoadOfPage('Template deleted');
            });

            it('should handle error request', () => {
                spyOn(dotTemplatesService, 'delete').and.returnValue(of(mockSingleResponseFail));
                spyOn(dotAlertConfirmService, 'confirm').and.callFake((conf) => {
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

            beforeEach(() => {
                comp.selectedTemplates = [templatesMock[0], templatesMock[1]];
                fixture.detectChanges();
                menu = fixture.debugElement.query(
                    By.css('.template-listing__header-options p-menu')
                ).componentInstance;
                spyOn(dotMessageDisplayService, 'push');
                spyOn(dotListingDataTable, 'loadCurrentPage');
            });

            it('should set labels', () => {
                const actions = [
                    { label: 'Publish', command: jasmine.any(Function) },
                    { label: 'Push Publish', command: jasmine.any(Function) },
                    { label: 'Add To Bundle', command: jasmine.any(Function) },
                    { label: 'Unpublish', command: jasmine.any(Function) },
                    { label: 'Archive', command: jasmine.any(Function) },
                    { label: 'Unarchive', command: jasmine.any(Function) },
                    { label: 'Delete', command: jasmine.any(Function) }
                ];

                expect(menu.model).toEqual(actions);
            });

            it('should execute Publish action', () => {
                spyOn(dotTemplatesService, 'publish').and.returnValue(of(mockBulkResponseSuccess));
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
                spyOn(dotTemplatesService, 'unPublish').and.returnValue(
                    of(mockBulkResponseSuccess)
                );
                menu.model[3].command({ originalEvent: createFakeEvent('click') });
                expect(dotTemplatesService.unPublish).toHaveBeenCalledWith([
                    '123Published',
                    '123Locked'
                ]);
                checkNotificationAndReLoadOfPage('Template unpublished');
            });
            it('should execute Archive action', () => {
                spyOn(dotTemplatesService, 'archive').and.returnValue(of(mockBulkResponseSuccess));
                menu.model[4].command({ originalEvent: createFakeEvent('click') });
                expect(dotTemplatesService.archive).toHaveBeenCalledWith([
                    '123Published',
                    '123Locked'
                ]);
                checkNotificationAndReLoadOfPage('Template archived');
            });
            it('should execute UnArchive action', () => {
                spyOn(dotTemplatesService, 'unArchive').and.returnValue(
                    of(mockBulkResponseSuccess)
                );
                menu.model[5].command({ originalEvent: createFakeEvent('click') });
                expect(dotTemplatesService.unArchive).toHaveBeenCalledWith([
                    '123Published',
                    '123Locked'
                ]);
                checkNotificationAndReLoadOfPage('Template unarchived');
            });
            it('should execute Delete action', () => {
                spyOn(dotTemplatesService, 'delete').and.returnValue(of(mockBulkResponseSuccess));
                spyOn(dotAlertConfirmService, 'confirm').and.callFake((conf) => {
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
                    spyOn(dotTemplatesService, 'publish').and.returnValue(of(mockBulkResponseFail));
                    menu.model[0].command({ originalEvent: createFakeEvent('click') });
                    checkOpenOfDialogService('Templates published');
                });
                it('should fire exception on unPublish', () => {
                    spyOn(dotTemplatesService, 'unPublish').and.returnValue(
                        of(mockBulkResponseFail)
                    );
                    menu.model[3].command({ originalEvent: createFakeEvent('click') });
                    checkOpenOfDialogService('Template unpublished');
                });
                it('should fire exception on archive', () => {
                    spyOn(dotTemplatesService, 'archive').and.returnValue(of(mockBulkResponseFail));
                    menu.model[4].command({ originalEvent: createFakeEvent('click') });
                    checkOpenOfDialogService('Template archived');
                });
                it('should fire exception on unArchive', () => {
                    spyOn(dotTemplatesService, 'unArchive').and.returnValue(
                        of(mockBulkResponseFail)
                    );
                    menu.model[5].command({ originalEvent: createFakeEvent('click') });
                    checkOpenOfDialogService('Template unarchived');
                });
                it('should fire exception on delete', () => {
                    spyOn(dotTemplatesService, 'delete').and.returnValue(of(mockBulkResponseFail));
                    spyOn(dotAlertConfirmService, 'confirm').and.callFake((conf) => {
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

    function setBasicOptions(): any {
        return [
            { menuItem: { label: 'Edit', command: jasmine.any(Function) } },
            { menuItem: { label: 'Publish', command: jasmine.any(Function) } },
            { menuItem: { label: 'Push Publish', command: jasmine.any(Function) } },
            { menuItem: { label: 'Add To Bundle', command: jasmine.any(Function) } }
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
