/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import {
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    DebugElement,
    EventEmitter,
    Output
} from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotMessageDisplayServiceMock } from '@components/dot-message-display/dot-message-display.component.spec';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { dotEventSocketURLFactory } from '@dotcms/app/test/dot-test-bed';
import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotEventsService,
    DotMessageService,
    DotSiteBrowserService
} from '@dotcms/data-access';
import {
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils
} from '@dotcms/dotcms-js';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import {
    ActivatedRouteMock,
    CoreWebServiceMock,
    MockDotMessageService
} from '@dotcms/utils-testing';
import { DotRouterService } from '@services/dot-router/dot-router.service';

import { DotAddVariableComponent } from './dot-add-variable.component';

@Component({
    selector: 'dot-form-dialog',
    template: '<ng-content></ng-content>',
    styleUrls: []
})
export class DotFormDialogMockComponent {
    @Output() save = new EventEmitter();
}

const mockContentTypes: DotCMSContentType = {
    baseType: 'CONTENT',
    nEntries: 23,
    clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
    defaultType: false,
    fields: [
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableBinaryField',
            contentTypeId: 'ce930143870e11569f93f8a9fff5da19',
            dataType: 'SYSTEM',
            fieldType: 'Binary',
            fieldTypeLabel: 'Binary',
            fieldVariables: [],
            fixed: false,
            iDate: 1667904275000,
            id: 'fd98a0871c994d0ff1a8407a391487da',
            indexed: false,
            listed: false,
            modDate: 1667904276000,
            name: 'Screenshot',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 0,
            unique: false,
            variable: 'screenshot'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            contentTypeId: 'ce930143870e11569f93f8a9fff5da19',
            dataType: 'TEXT',
            fieldType: 'Text',
            fieldTypeLabel: 'Text',
            fieldVariables: [],
            fixed: false,
            iDate: 1667904275000,
            id: 'd0c2a52795fd310b21b4778d3e9b17e5',
            indexed: false,
            listed: false,
            modDate: 1667904276000,
            name: 'title',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 1,
            unique: false,
            variable: 'title'
        }
    ],
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: 'SYSTEM_HOST',
    iDate: 1667904275000,
    icon: 'event_note',
    id: 'ce930143870e11569f93f8a9fff5da19',
    layout: [
        {
            divider: {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                contentTypeId: 'ce930143870e11569f93f8a9fff5da19',
                dataType: 'SYSTEM',
                fieldType: 'Row',
                fieldTypeLabel: 'Row',
                fieldVariables: [],
                fixed: false,
                iDate: 1668073859000,
                indexed: false,
                listed: false,
                modDate: 1668073859000,
                name: 'Row Field',
                readOnly: false,
                required: false,
                searchable: false,
                sortOrder: 0,
                unique: false,
                id: '123',
                variable: 'ffff'
            }
        }
    ],
    modDate: 1667904276000,
    multilingualable: false,
    name: 'Dot Favorite Page',
    system: false,
    systemActionMappings: {},
    variable: 'dotFavoritePage',
    versionable: true,
    workflows: []
};

const messageServiceMock = new MockDotMessageService({
    'containers.properties.add.variable.title': 'Title',
    Add: 'Add'
});

describe('DotAddVariableComponent', () => {
    let fixture: ComponentFixture<DotAddVariableComponent>;
    let de: DebugElement;
    let dialogConfig: DynamicDialogConfig;
    let dialogRef: DynamicDialogRef;
    let coreWebService: CoreWebService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotAddVariableComponent, DotFormDialogMockComponent],
            imports: [
                ButtonModule,
                DataViewModule,
                HttpClientTestingModule,
                SharedModule,
                DotMessagePipe
            ],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                {
                    provide: DynamicDialogRef,
                    useValue: {
                        close: jasmine.createSpy()
                    }
                },
                {
                    provide: DynamicDialogConfig,
                    useValue: {
                        data: {
                            contentTypeVariable: 'contentType',
                            onSave: jasmine.createSpy()
                        }
                    }
                },
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                StringUtils,
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService,
                DotcmsEventsService,
                DotEventsSocket,
                DotcmsConfigService,
                { provide: DotMessageDisplayService, useClass: DotMessageDisplayServiceMock },
                DialogService,
                DotSiteBrowserService,
                DotContentTypeService,
                DotAlertConfirmService,
                ConfirmationService,
                DotGlobalMessageService,
                DotEventsService,
                LoggerService,
                LoginService,
                DotRouterService,
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                }
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAddVariableComponent);
        de = fixture.debugElement;
        dialogConfig = TestBed.inject(DynamicDialogConfig);
        coreWebService = TestBed.inject(CoreWebService);
    });

    describe('dot-add-variable-dialog', () => {
        beforeEach(fakeAsync(() => {
            spyOn<CoreWebService>(coreWebService, 'requestView').and.returnValue(
                of({
                    entity: mockContentTypes
                })
            );
            fixture.detectChanges();
            tick();
            fixture.detectChanges();
            de = fixture.debugElement;
            dialogRef = TestBed.inject(DynamicDialogRef);
        }));

        it('should call add from list and apply mask in variable', () => {
            const dialog = de.query(
                By.css(`[data-testId="${mockContentTypes.fields[0].variable}"]`)
            );
            dialog.triggerEventHandler('click');
            expect(dialogConfig.data.onSave).toHaveBeenCalledTimes(1);
            expect(dialogConfig.data.onSave).toHaveBeenCalledWith(
                `$!{dotContentMap.${mockContentTypes.fields[0].variable}}`
            );
            expect(dialogRef.close).toHaveBeenCalled();
        });
    });
});
