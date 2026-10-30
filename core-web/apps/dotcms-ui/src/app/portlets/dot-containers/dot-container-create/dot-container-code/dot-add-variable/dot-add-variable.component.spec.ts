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

import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotSiteBrowserService,
    DotGlobalMessageService
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
    DotMessageDisplayServiceMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotAddVariableComponent } from './dot-add-variable.component';
import { FilteredFieldTypes } from './dot-add-variable.models';
import { DOT_CONTENT_MAP, DotFieldsService } from './services/dot-fields.service';

import { dotEventSocketURLFactory } from '../../../../../test/dot-test-bed';

@Component({
    selector: 'dot-form-dialog',
    template: '<ng-content></ng-content>',
    styleUrls: [],
    standalone: false
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
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            contentTypeId: 'ce930143870e11569f93f8a9fff5da19',
            dataType: 'SYSTEM',
            fieldType: 'Text',
            fieldTypeLabel: 'Text',
            fieldVariables: [],
            fixed: false,
            iDate: 1667904275000,
            id: 'fd98a0871c994d0ff1a8407a391487da',
            indexed: false,
            listed: false,
            modDate: 1667904276000,
            name: 'Sub Title',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 0,
            unique: false,
            variable: 'subTitle'
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
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableImageField',
            contentTypeId: '60b5bce270de216d36eb1a07674c773b',
            dataType: 'TEXT',
            fieldType: 'Image',
            fieldTypeLabel: 'Image',
            fieldVariables: [],
            fixed: false,
            iDate: 1696260470000,
            id: '27b59164438e70f11ead3a7318884af8',
            indexed: false,
            listed: false,
            modDate: 1696260470000,
            name: 'Test Image',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 2,
            unique: false,
            variable: 'testImage'
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
    Add: 'Add',
    'Content-Identifier-value': 'Content Identifier Value',
    'Content-Identifier': 'Content Identifier',
    Image: 'Image',
    'Image-Identifier': 'Image Identifier',
    'Image-Title': 'Image Title',
    'Image-Width': 'Image Width',
    'Image-Extension': 'Image Extension',
    'Image-Height': 'Image Height'
});

describe('DotAddVariableComponent', () => {
    let fixture: ComponentFixture<DotAddVariableComponent>;
    let de: DebugElement;
    let dialogConfig: DynamicDialogConfig;
    let dialogRef: DynamicDialogRef;

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
                DotFieldsService,
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
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                DialogService,
                DotSiteBrowserService,
                {
                    provide: DotContentTypeService,
                    useValue: {
                        getContentType: jasmine.createSpy().and.returnValue(of(mockContentTypes))
                    }
                },
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
    });

    describe('dot-add-variable-dialog', () => {
        beforeEach(fakeAsync(() => {
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

        it('should be a field list without FielteredTypes', () => {
            const fieldTypes = fixture.nativeElement.querySelectorAll('small');
            fieldTypes.forEach((field) => {
                const content = field.textContent.trim();
                expect(content).not.toEqual(FilteredFieldTypes.Column);
                expect(content).not.toEqual(FilteredFieldTypes.Row);
            });
        });

        it('should contain a field with the text "Content Identifier Value"', () => {
            const contentIdentifier = de.query(By.css(`[data-testId="h3ContentIdentifier"]`));

            expect(contentIdentifier.nativeElement.textContent.trim()).toEqual(
                'Content Identifier Value'
            );
        });

        it('should contain 6 fields with the text label as "Image"', () => {
            const fieldTypes = Array.from(fixture.nativeElement.querySelectorAll('small')).filter(
                (fieldElement: HTMLElement) => {
                    return fieldElement.textContent.trim() === 'Image';
                }
            );

            expect(fieldTypes.length).toEqual(6);
        });

        it("should call dialog config on save with the custom codeTemplate when click on 'Add' button for Image field", () => {
            const dialog = de.query(
                By.css(`[data-testId="${mockContentTypes.fields[2].variable}.image"]`)
            );

            dialog.triggerEventHandler('click');

            expect(dialogConfig.data.onSave).toHaveBeenCalledWith(
                `#if ($!{${DOT_CONTENT_MAP}.${mockContentTypes.fields[2].variable}.rawUri})\n    <img src="$!{${DOT_CONTENT_MAP}.${mockContentTypes.fields[2].variable}.rawUri}" alt="$!{${DOT_CONTENT_MAP}.${mockContentTypes.fields[2].variable}.title}" />\n#elseif($!{${DOT_CONTENT_MAP}.${mockContentTypes.fields[2].variable}.identifier})\n    <img src="/dA/\${${DOT_CONTENT_MAP}.${mockContentTypes.fields[2].variable}.identifier}" alt="$!{${DOT_CONTENT_MAP}.${mockContentTypes.fields[2].variable}.title}"/>\n#end`
            );
            expect(dialogRef.close).toHaveBeenCalled();
        });
    });
});
