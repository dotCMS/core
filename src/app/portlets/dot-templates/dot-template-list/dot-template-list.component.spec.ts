import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { DotTemplateListComponent } from './dot-template-list.component';
import { of, Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { By } from '@angular/platform-browser';
import {
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    DotPushPublishDialogService,
    LoggerService,
    LoginService,
    StringUtils
} from 'dotcms-js';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { ConfirmationService, SharedModule } from 'primeng/api';
import { dotEventSocketURLFactory } from '@tests/dot-test-bed';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotListingDataTableModule } from '@components/dot-listing-data-table';
import { CommonModule } from '@angular/common';
import { CheckboxModule } from 'primeng/checkbox';
import { Menu, MenuModule } from 'primeng/menu';
import { ButtonModule } from 'primeng/button';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { DotActionMenuButtonComponent } from '@components/_common/dot-action-menu-button/dot-action-menu-button.component';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotAddToBundleComponent } from '@components/_common/dot-add-to-bundle/dot-add-to-bundle.component';
import { ButtonModel } from '@models/action-header';
import { DotTemplate } from '@models/dot-edit-layout-designer';
import { DotActionBulkResult } from '@models/dot-action-bulk-result/dot-action-bulk-result.model';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { DotBulkInformationComponent } from '@components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotContentState } from 'dotcms-models';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

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
        working: true
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
        working: true
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
        working: true
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
        working: false
    }
];

const routeDataMock = {
    dotTemplateListResolverData: [templatesMock, true, true]
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
    'message.template.unlocked': 'Template unlocked',
    'message.template.unpublished': 'Template unpublished',
    'message.template_list.published': 'Templates published',
    'templates.fieldName.description': 'Description',
    'templates.fieldName.lastEdit': 'Last Edit',
    'templates.fieldName.name': 'Name',
    'templates.fieldName.status': 'Status',
    'templates.select.template.title': 'Create a template',
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
    publish: 'Publish',
    unlock: 'Unlock'
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

    const messageServiceMock = new MockDotMessageService(messages);

    const dialogRefClose = new Subject();

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
                        goToEditTemplate: jasmine.createSpy()
                    }
                },
                LoggerService,
                StringUtils,
                DotTemplatesService,
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService,
                LoginService,
                DotcmsEventsService,
                DotEventsSocket,
                DotcmsConfigService,
                DotMessageDisplayService,
                DialogService
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
                DotIconModule,
                BrowserAnimationsModule
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();
    });

    beforeEach(fakeAsync(() => {
        fixture = TestBed.createComponent(DotTemplateListComponent);
        comp = fixture.componentInstance;
        dotTemplatesService = TestBed.inject(DotTemplatesService);
        dotMessageDisplayService = TestBed.inject(DotMessageDisplayService);
        dotPushPublishDialogService = TestBed.inject(DotPushPublishDialogService);
        dotRouterService = TestBed.inject(DotRouterService);
        dialogService = TestBed.inject(DialogService);

        fixture.detectChanges();
        tick(2);
        fixture.detectChanges();
        dotListingDataTable = fixture.debugElement.query(By.css('dot-listing-data-table'))
            .componentInstance;
        spyOn(window, 'confirm').and.callFake(function () {
            return true;
        });
        spyOn(dotPushPublishDialogService, 'open');

        spyOn<any>(dialogService, 'open').and.returnValue({
            onClose: dialogRefClose
        });
    }));

    it('should set attributes of dotListingDataTable', () => {
        expect(dotListingDataTable.columns).toEqual(columnsMock);
        expect(dotListingDataTable.sortField).toEqual('modDate');
        expect(dotListingDataTable.sortOrder).toEqual('DESC');
        expect(dotListingDataTable.url).toEqual('v1/templates');
        expect(dotListingDataTable.actions).toEqual([]);
        expect(dotListingDataTable.checkbox).toEqual(true);
        expect(dotListingDataTable.dataKey).toEqual('inode');
    });

    it('should set Action Header options correctly', () => {
        const model: ButtonModel[] = dotListingDataTable.actionHeaderOptions.primary.model;
        expect(model).toBeUndefined();

        dotListingDataTable.actionHeaderOptions.primary.command();
        expect(dialogService.open).toHaveBeenCalledWith(jasmine.any(Function), {
            header: 'Create a template',
            width: '40rem',
            closeOnEscape: false
        });
    });

    it('should go to create template', () => {
        dotListingDataTable.actionHeaderOptions.primary.command();
        dialogRefClose.next('create');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledOnceWith('/templates/new/create');
    });

    it('should not go to create template', () => {
        dotListingDataTable.actionHeaderOptions.primary.command();
        dialogRefClose.next('');
        expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
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

        lockedTemplate = fixture.debugElement.query(By.css('[data-testid="123Locked"]'))
            .componentInstance;
        const stateIcon = fixture.debugElement.query(By.css('dot-state-icon'));
        const lockIcon = fixture.debugElement.query(
            By.css('p-table dot-icon[name="lock"][size="14"]')
        );

        expect(stateIcon.attributes['size']).toEqual('14px');
        expect(stateIcon.nativeNode.state).toEqual(state);
        expect(stateIcon.nativeNode.labels).toEqual(labels);
        expect(lockIcon).toBeDefined();
    });

    describe('row', () => {
        it('should set actions to publish template', () => {
            publishTemplate = fixture.debugElement.query(By.css('[data-testid="123Published"]'))
                .componentInstance;
            const actions = setBasicOptions();
            actions.push({ menuItem: { label: 'Unpublish', command: jasmine.any(Function) } });
            actions.push({ menuItem: { label: 'Copy', command: jasmine.any(Function) } });

            expect(publishTemplate.actions).toEqual(actions);
        });

        it('should set actions to locked template', () => {
            lockedTemplate = fixture.debugElement.query(By.css('[data-testid="123Locked"]'))
                .componentInstance;
            const actions = setBasicOptions();
            actions.push({ menuItem: { label: 'Unpublish', command: jasmine.any(Function) } });
            actions.push({ menuItem: { label: 'Unlock', command: jasmine.any(Function) } });
            actions.push({ menuItem: { label: 'Copy', command: jasmine.any(Function) } });

            expect(lockedTemplate.actions).toEqual(actions);
        });

        it('should set actions to unPublish template', () => {
            unPublishTemplate = fixture.debugElement.query(By.css('[data-testid="123Unpublish"]'))
                .componentInstance;
            const actions = setBasicOptions();
            actions.push({ menuItem: { label: 'Archive', command: jasmine.any(Function) } });
            actions.push({ menuItem: { label: 'Copy', command: jasmine.any(Function) } });

            expect(unPublishTemplate.actions).toEqual(actions);
        });

        it('should set actions to archived template', () => {
            archivedTemplate = fixture.debugElement.query(By.css('[data-testid="123Archived"]'))
                .componentInstance;
            const actions = [
                { menuItem: { label: 'Unarchive', command: jasmine.any(Function) } },
                { menuItem: { label: 'Delete', command: jasmine.any(Function) } }
            ];
            expect(archivedTemplate.actions).toEqual(actions);
        });

        it('should hide push-publish and Add to Bundle actions', () => {
            let activatedRoute: ActivatedRoute;
            activatedRoute = TestBed.inject(ActivatedRoute);
            spyOnProperty(activatedRoute, 'data', 'get').and.returnValue(
                of({
                    dotTemplateListResolverData: [templatesMock, false, false]
                })
            );
            comp.ngOnInit();
            fixture.detectChanges();
            publishTemplate = fixture.debugElement.query(By.css('[data-testid="123Published"]'))
                .componentInstance;
            const actions = [
                { menuItem: { label: 'Edit', command: jasmine.any(Function) } },
                { menuItem: { label: 'Publish', command: jasmine.any(Function) } },
                { menuItem: { label: 'Unpublish', command: jasmine.any(Function) } },
                { menuItem: { label: 'Copy', command: jasmine.any(Function) } }
            ];

            expect(publishTemplate.actions).toEqual(actions);
        });
    });

    describe('row actions command', () => {
        beforeEach(() => {
            spyOn(dotMessageDisplayService, 'push');
            spyOn(dotListingDataTable, 'loadCurrentPage');
            publishTemplate = fixture.debugElement.query(By.css('[data-testid="123Published"]'))
                .componentInstance;
            lockedTemplate = fixture.debugElement.query(By.css('[data-testid="123Locked"]'))
                .componentInstance;
            unPublishTemplate = fixture.debugElement.query(By.css('[data-testid="123Unpublish"]'))
                .componentInstance;
            archivedTemplate = fixture.debugElement.query(By.css('[data-testid="123Archived"]'))
                .componentInstance;
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
            spyOn(dotTemplatesService, 'unArchive').and.returnValue(of(mockBulkResponseSuccess));
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
            spyOn(dotTemplatesService, 'unPublish').and.returnValue(of(mockBulkResponseSuccess));
            publishTemplate.actions[4].menuItem.command();

            expect(dotTemplatesService.unPublish).toHaveBeenCalledWith(['123Published']);
            checkNotificationAndReLoadOfPage('Template unpublished');
        });
        it('should call unlock api, send notification and reload current page', () => {
            spyOn(dotTemplatesService, 'unlock').and.returnValue(of('template unlocked'));
            lockedTemplate.actions[5].menuItem.command();

            expect(dotTemplatesService.unlock).toHaveBeenCalledWith('123Locked');
            checkNotificationAndReLoadOfPage('Template unlocked');
        });
        it('should call copy api, send notification and reload current page', () => {
            spyOn(dotTemplatesService, 'copy').and.returnValue(of(templatesMock[0]));
            publishTemplate.actions[5].menuItem.command();

            expect(dotTemplatesService.copy).toHaveBeenCalledWith('123Published');
            checkNotificationAndReLoadOfPage('Template copied');
        });
        it('should call delete api, send notification and reload current page', () => {
            spyOn(dotTemplatesService, 'delete').and.returnValue(of(mockBulkResponseSuccess));
            archivedTemplate.actions[1].menuItem.command();
            expect(dotTemplatesService.delete).toHaveBeenCalledWith(['123Archived']);
            checkNotificationAndReLoadOfPage('Template deleted');
        });
    });

    describe('bulk', () => {
        let menu: Menu;

        beforeEach(() => {
            comp.selectedTemplates = [templatesMock[0], templatesMock[1]];
            fixture.detectChanges();
            menu = fixture.debugElement.query(By.css('.template-listing__header-options p-menu'))
                .componentInstance;
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

        it('should execute actions', () => {
            spyOn(dotTemplatesService, 'publish').and.returnValue(of(mockBulkResponseSuccess));
            spyOn(dotTemplatesService, 'unPublish').and.returnValue(of(mockBulkResponseSuccess));
            spyOn(dotTemplatesService, 'archive').and.returnValue(of(mockBulkResponseSuccess));
            spyOn(dotTemplatesService, 'unArchive').and.returnValue(of(mockBulkResponseSuccess));
            spyOn(dotTemplatesService, 'delete').and.returnValue(of(mockBulkResponseSuccess));
            spyOn(dotMessageDisplayService, 'push');
            spyOn(dotListingDataTable, 'loadCurrentPage');
            menu.model[0].command();
            expect(dotTemplatesService.publish).toHaveBeenCalledWith(['123Published', '123Locked']);

            menu.model[1].command();
            expect(dotPushPublishDialogService.open).toHaveBeenCalledWith({
                assetIdentifier: '123Published,123Locked',
                title: 'Push Publish'
            });

            menu.model[2].command();
            expect(dotTemplatesService.publish).toHaveBeenCalledWith(['123Published', '123Locked']);

            menu.model[3].command();
            expect(dotTemplatesService.unPublish).toHaveBeenCalledWith([
                '123Published',
                '123Locked'
            ]);

            menu.model[4].command();
            fixture.detectChanges();
            const addToBundleDialog: DotAddToBundleComponent = fixture.debugElement.query(
                By.css('dot-add-to-bundle')
            ).componentInstance;
            expect(addToBundleDialog.assetIdentifier).toEqual('123Published,123Locked');

            menu.model[5].command();
            expect(dotTemplatesService.unArchive).toHaveBeenCalledWith([
                '123Published',
                '123Locked'
            ]);

            menu.model[6].command();
            expect(dotTemplatesService.delete).toHaveBeenCalledWith(['123Published', '123Locked']);

            expect(dotMessageDisplayService.push).toHaveBeenCalledTimes(5);
            expect(dotListingDataTable.loadCurrentPage).toHaveBeenCalledTimes(5);
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

        fit('error exceptions', () => {
            spyOn(dotTemplatesService, 'publish').and.returnValue(of(mockBulkResponseFail));
            spyOn(dotTemplatesService, 'unPublish').and.returnValue(of(mockBulkResponseFail));
            spyOn(dotTemplatesService, 'archive').and.returnValue(of(mockBulkResponseFail));
            spyOn(dotTemplatesService, 'unArchive').and.returnValue(of(mockBulkResponseFail));
            spyOn(dotTemplatesService, 'delete').and.returnValue(of(mockBulkResponseFail));
            menu.model[0].command();
            menu.model[3].command();
            menu.model[4].command();
            menu.model[5].command();
            menu.model[6].command();
            expect(dialogService.open).toHaveBeenCalledTimes(5);
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
                    action: 'Template deleted'
                }
            });
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
});
