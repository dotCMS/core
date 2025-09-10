/* eslint-disable @typescript-eslint/no-empty-function */

import { mockProvider } from '@ngneat/spectator/jest';
import { Observable, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, forwardRef, Injectable, Input } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {
    AbstractControl,
    ControlValueAccessor,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule
} from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { TabViewModule } from 'primeng/tabview';

import {
    DotAlertConfirmService,
    DotContentTypesInfoService,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageDisplayService,
    DotMessageService,
    DotWorkflowService
} from '@dotcms/data-access';
import { CoreWebService, DotcmsConfigService, LoginService, SiteService } from '@dotcms/dotcms-js';
import {
    DotCMSContentTypeLayoutRow,
    DotCMSSystemActionType,
    FeaturedFlags,
    DotCMSClazzes
} from '@dotcms/dotcms-models';
import { DotFieldValidationMessageComponent, DotIconModule, DotMessagePipe } from '@dotcms/ui';
import {
    CoreWebServiceMock,
    dotcmsContentTypeBasicMock,
    dotcmsContentTypeFieldBasicMock,
    DotMessageDisplayServiceMock,
    DotWorkflowServiceMock,
    LoginServiceMock,
    MockDotMessageService,
    mockWorkflows,
    mockWorkflowsActions,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { ContentTypesFormComponent } from './content-types-form.component';

import { DotDirectivesModule } from '../../../../../shared/dot-directives.module';
import { DotMdIconSelectorModule } from '../../../../../view/components/_common/dot-md-icon-selector/dot-md-icon-selector.module';
import { DotPageSelectorModule } from '../../../../../view/components/_common/dot-page-selector/dot-page-selector.module';
import { DotWorkflowsActionsSelectorFieldModule } from '../../../../../view/components/_common/dot-workflows-actions-selector-field/dot-workflows-actions-selector-field.module';
import { DotWorkflowsSelectorFieldModule } from '../../../../../view/components/_common/dot-workflows-selector-field/dot-workflows-selector-field.module';
import { DotFieldHelperModule } from '../../../../../view/components/dot-field-helper/dot-field-helper.module';

@Component({
    selector: 'dot-site-selector-field',
    template: '',
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotSiteSelectorComponent)
        }
    ],
    standalone: false
})
class DotSiteSelectorComponent implements ControlValueAccessor {
    @Input() system;

    writeValue() {}

    registerOnChange() {}

    registerOnTouched() {}
}

@Injectable()
class MockDotLicenseService {
    isEnterprise(): Observable<boolean> {
        return of(false);
    }
}

const mockActivatedRoute = {
    snapshot: {
        data: {
            featuredFlags: {
                [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true
            }
        }
    }
};

describe('ContentTypesFormComponent', () => {
    let comp: ContentTypesFormComponent;
    let fixture: ComponentFixture<ContentTypesFormComponent>;
    let de: DebugElement;
    let dotLicenseService: DotLicenseService;
    const layout: DotCMSContentTypeLayoutRow[] = [
        {
            divider: {
                ...dotcmsContentTypeFieldBasicMock,
                clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                name: 'row_field'
            },
            columns: [
                {
                    columnDivider: {
                        ...dotcmsContentTypeFieldBasicMock,
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                        name: 'column_field'
                    },
                    fields: [
                        {
                            ...dotcmsContentTypeFieldBasicMock,
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
                            id: '123',
                            indexed: true,
                            name: 'Date 1'
                        },
                        {
                            ...dotcmsContentTypeFieldBasicMock,
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
                            id: '456',
                            indexed: true,
                            name: 'Date 2'
                        }
                    ]
                }
            ]
        }
    ];
    let activatedRoute: ActivatedRoute;

    beforeEach(waitForAsync(() => {
        const messageServiceMock = new MockDotMessageService({
            'contenttypes.form.field.detail.page': 'Detail Page',
            'contenttypes.form.field.expire.date.field': 'Expire Date Field',
            'contenttypes.form.field.host_folder.label': 'Host or Folder',
            'contenttypes.form.identifier': 'Identifier',
            'contenttypes.form.label.publish.date.field': 'Publish Date Field',
            'contenttypes.hint.URL.map.pattern.hint1': 'Hello World',
            'contenttypes.form.label.URL.pattern': 'URL Pattern',
            'contenttypes.content.variable': 'Variable',
            'contenttypes.form.label.workflow': 'Workflow',
            'contenttypes.action.cancel': 'Cancel',
            'contenttypes.form.label.description': 'Description',
            'contenttypes.form.name': 'Name',
            'contenttypes.action.save': 'Save',
            'contenttypes.action.update': 'Update',
            'contenttypes.action.create': 'Create',
            'contenttypes.action.edit': 'Edit',
            'contenttypes.action.delete': 'Delete',
            'contenttypes.form.name.error.required': 'Error is wrong',
            'contenttypes.action.form.cancel': 'Cancel',
            'contenttypes.content.contenttype': 'content type',
            'contenttypes.content.fileasset': 'fileasset',
            'contenttypes.content.content': 'Content',
            'contenttypes.content.form': 'Form',
            'contenttypes.content.persona': 'Persona',
            'contenttypes.content.widget': 'Widget',
            'contenttypes.content.htmlpage': 'Page',
            'contenttypes.content.key_value': 'Key Value',
            'contenttypes.content.vanity_url:': 'Vanity Url'
        });

        const siteServiceMock = new SiteServiceMock();

        TestBed.configureTestingModule({
            declarations: [ContentTypesFormComponent, DotSiteSelectorComponent],
            imports: [
                RouterTestingModule.withRoutes([
                    { component: ContentTypesFormComponent, path: 'test' }
                ]),
                BrowserAnimationsModule,
                ButtonModule,
                DotDirectivesModule,
                DotFieldHelperModule,
                DotFieldValidationMessageComponent,
                DotIconModule,
                DotPageSelectorModule,
                DotWorkflowsActionsSelectorFieldModule,
                DotWorkflowsSelectorFieldModule,
                DropdownModule,
                InputTextModule,
                CheckboxModule,
                OverlayPanelModule,
                ReactiveFormsModule,
                RouterTestingModule,
                TabViewModule,
                HttpClientTestingModule,
                DotMdIconSelectorModule,
                DotMessagePipe
            ],
            providers: [
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: SiteService, useValue: siteServiceMock },
                { provide: DotWorkflowService, useClass: DotWorkflowServiceMock },
                { provide: DotLicenseService, useClass: MockDotLicenseService },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotcmsConfigService,
                DotContentTypesInfoService,
                mockProvider(DotHttpErrorManagerService),
                mockProvider(DotAlertConfirmService),
                mockProvider(ConfirmationService),
                {
                    provide: ActivatedRoute,
                    useValue: mockActivatedRoute
                }
            ]
        });

        fixture = TestBed.createComponent(ContentTypesFormComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        dotLicenseService = de.injector.get(DotLicenseService);
        activatedRoute = de.injector.get(ActivatedRoute);
    }));

    it('should be invalid by default', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT'
        };
        fixture.detectChanges();
        expect(comp.form.valid).toBe(false);
    });

    it('should be valid when name field have value', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        comp.form.get('name').setValue('content type name');
        expect(comp.form.valid).toBe(true);
    });

    it('should have name focus by default on create mode', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT'
        };
        fixture.detectChanges();
        expect(comp.name.nativeElement).toBe(document.activeElement);
    });

    it('should have canSave property false by default (form is invalid)', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        expect(comp.canSave).toBe(false);
    });

    it('should set canSave property true form is valid', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            name: 'hello',
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        // Form is only valid when "name" property is set
        comp.form.get('description').setValue('hello world');
        fixture.detectChanges();

        expect(comp.canSave).toBe(true);
    });

    it('should set canSave property false when form is invalid in edit mode', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        };
        fixture.detectChanges();

        comp.form.get('name').setValue(null);
        fixture.detectChanges();

        expect(comp.canSave).toBe(false);
    });

    it('should set canSave property true when form is valid and model updated in edit mode', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        };
        fixture.detectChanges();

        comp.form.get('description').setValue('some desc');
        fixture.detectChanges();

        expect(comp.canSave).toBe(true);
    });

    it('should set canSave property false when form is invalid and model updated in edit mode', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        };
        fixture.detectChanges();

        comp.form.get('name').setValue(null);
        comp.form.get('description').setValue('some desc');
        fixture.detectChanges();

        expect(comp.canSave).toBe(false);
    });

    // tslint:disable-next-line:max-line-length
    it('should set canSave property false when the form value is updated and then gets back to the original content (no community license)', () => {
        jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));

        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        };
        fixture.detectChanges();
        expect(comp.canSave).toBe(false, 'by default is false');

        comp.form.get('name').setValue('A new  name');
        fixture.detectChanges();
        expect(comp.canSave).toBe(true, 'name updated set it to true');

        comp.form.get('name').setValue('Hello World');
        fixture.detectChanges();
        expect(comp.canSave).toBe(false, 'revert the change button disabled set it to false');
    });

    // eslint-disable-next-line max-len
    it('should set canSave property false when the form value is updated and then gets back to the original content (community license)', () => {
        jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));

        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        };
        fixture.detectChanges();
        expect(comp.canSave).toBe(false, 'by default is false');

        comp.form.get('name').setValue('A new  name');
        fixture.detectChanges();
        expect(comp.canSave).toBe(true, 'name updated set it to true');

        comp.form.get('name').setValue('Hello World');
        fixture.detectChanges();
        expect(comp.canSave).toBe(false, 'revert the change button disabled set it to false');
    });

    it('should set canSave property false when edit a content with fields', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        };
        comp.layout = layout;

        fixture.detectChanges();
        expect(comp.canSave).toBe(false, 'by default is false');
    });

    it('should set canSave property false on edit mode', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123'
        };
        fixture.detectChanges();

        expect(comp.canSave).toBe(false);
    });

    it('should have basic form controls for non-content base types', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'WIDGET'
        };
        fixture.detectChanges();

        expect(Object.keys(comp.form.controls).length).toBe(14);
        expect(comp.form.get('icon')).not.toBeNull();
        expect(comp.form.get('clazz')).not.toBeNull();
        expect(comp.form.get('name')).not.toBeNull();
        expect(comp.form.get('host')).not.toBeNull();
        expect(comp.form.get('description')).not.toBeNull();
        expect(comp.form.get('workflows')).not.toBeNull();
        expect(comp.form.get('publishDateVar')).not.toBeNull();
        expect(comp.form.get('expireDateVar')).not.toBeNull();
        expect(comp.form.get('defaultType')).not.toBeNull();
        expect(comp.form.get('fixed')).not.toBeNull();
        expect(comp.form.get('system')).not.toBeNull();
        expect(comp.form.get('folder')).not.toBeNull();
        const workflowAction = comp.form.get('systemActionMappings');
        expect(workflowAction.get(DotCMSSystemActionType.NEW)).not.toBeNull();

        expect(comp.form.get('detailPage')).toBeNull();
        expect(comp.form.get('urlMapPattern')).toBeNull();
        expect(comp.form.get('newEditContent')).not.toBeNull();
    });

    it('should render basic fields for non-content base types', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'WIDGET'
        };
        fixture.detectChanges();

        const fields = [
            '#content-type-form-description',
            '#content-type-form-host',
            '#content-type-form-name',
            '#content-type-form-workflow',
            '#content-type-form-publish-date-field',
            '#content-type-form-expire-date-field',
            '#content-type-form-icon'
        ];

        fields.forEach((field) => {
            expect(fixture.debugElement.query(By.css(field))).not.toBeNull();
        });
    });

    it('should have basic form controls for content base type', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        expect(Object.keys(comp.form.controls).length).toBe(16);
        expect(comp.form.get('clazz')).not.toBeNull();
        expect(comp.form.get('name')).not.toBeNull();
        expect(comp.form.get('icon')).not.toBeNull();
        expect(comp.form.get('host')).not.toBeNull();
        expect(comp.form.get('description')).not.toBeNull();
        expect(comp.form.get('workflows')).not.toBeNull();
        expect(comp.form.get('publishDateVar')).not.toBeNull();
        expect(comp.form.get('expireDateVar')).not.toBeNull();
        expect(comp.form.get('detailPage')).not.toBeNull();
        expect(comp.form.get('urlMapPattern')).not.toBeNull();
        expect(comp.form.get('defaultType')).not.toBeNull();
        expect(comp.form.get('fixed')).not.toBeNull();
        expect(comp.form.get('system')).not.toBeNull();
        expect(comp.form.get('folder')).not.toBeNull();
        expect(comp.form.get('newEditContent')).not.toBeNull();

        const workflowAction = comp.form.get('systemActionMappings');
        expect(workflowAction.get(DotCMSSystemActionType.NEW)).not.toBeNull();
    });

    it('should set value to the form', () => {
        jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));

        const base = {
            icon: null,
            clazz: DotCMSClazzes.TEXT,
            defaultType: false,
            description: 'description',
            expireDateVar: 'expireDateVar',
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: 'host-id',
            name: 'name',
            publishDateVar: 'publishDateVar',
            system: false,
            detailPage: 'detail-page',
            urlMapPattern: '/url/map'
        };

        comp.data = {
            ...dotcmsContentTypeBasicMock,
            ...base,
            baseType: 'CONTENT'
        };
        comp.layout = layout;

        fixture.detectChanges();

        expect(comp.form.value).toEqual({
            ...base,
            systemActionMappings: {
                NEW: ''
            },
            workflows: [
                {
                    ...mockWorkflows[2],
                    creationDate: expect.any(Date),
                    modDate: expect.any(Date)
                }
            ],
            newEditContent: false
        });
    });

    describe('systemActionMappings', () => {
        beforeEach(() => {
            jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));
        });

        it('should set value to the form with systemActionMappings', () => {
            comp.data = {
                ...dotcmsContentTypeBasicMock,
                baseType: 'CONTENT',
                systemActionMappings: {
                    NEW: {
                        identifier: '',
                        systemAction: '',
                        workflowAction: mockWorkflowsActions[0],
                        owner: null,
                        ownerContentType: false,
                        ownerScheme: false
                    },
                    PUBLISH: {
                        identifier: '123',
                        systemAction: '',
                        workflowAction: null,
                        owner: null,
                        ownerContentType: false,
                        ownerScheme: false
                    }
                }
            };

            fixture.detectChanges();

            expect(comp.form.get('systemActionMappings').value).toEqual({
                NEW: '44d4d4cd-c812-49db-adb1-1030be73e69a'
            });
        });

        it('should set value to the form with systemActionMappings with empty object', () => {
            comp.data = {
                ...dotcmsContentTypeBasicMock,
                baseType: 'CONTENT',
                systemActionMappings: {}
            };

            fixture.detectChanges();

            expect(comp.form.get('systemActionMappings').value).toEqual({
                NEW: ''
            });
        });
    });

    it('should render extra fields for content types', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        const fields = [
            '#content-type-form-description',
            '#content-type-form-detail-page',
            '#content-type-form-host',
            '#content-type-form-name',
            '#content-type-form-url-map-pattern',
            '#content-type-form-workflow',
            '#content-type-form-detail-page',
            '#content-type-form-url-map-pattern'
        ];

        fields.forEach((field) => {
            expect(fixture.debugElement.query(By.css(field))).not.toBeNull();
        });
    });

    it('should render disabled dates fields and hint when date fields are not passed', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123'
        };
        fixture.detectChanges();

        const dateFieldMsg = de.query(By.css('#field-dates-hint'));

        expect(dateFieldMsg).toBeTruthy();
        expect(comp.form.get('publishDateVar').disabled).toBe(true);
        expect(comp.form.get('expireDateVar').disabled).toBe(true);
    });

    it('should render the new content banner when the feature flag is enabled', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123'
        };

        activatedRoute.snapshot.data.featuredFlags[
            FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
        ] = true;

        fixture.detectChanges();

        const newContentBanner = de.query(
            By.css('[data-test-id="content-type__new-content-banner"]')
        );
        expect(newContentBanner).not.toBeNull();
    });

    it('should hide the new content banner when the feature flag is disabled', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123'
        };

        activatedRoute.snapshot.data.featuredFlags[
            FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
        ] = false;

        fixture.detectChanges();

        const newContentBanner = de.query(
            By.css('[data-test-id="content-type__new-content-banner"]')
        );
        expect(newContentBanner).toBeNull();
    });

    describe('fields dates enabled', () => {
        beforeEach(() => {
            comp.data = {
                ...dotcmsContentTypeBasicMock,
                baseType: 'CONTENT'
            };
            comp.layout = layout;
            fixture.detectChanges();
        });

        it('should render enabled dates fields when date fields are passed', () => {
            expect(comp.form.get('publishDateVar').disabled).toBe(false);
            expect(comp.form.get('expireDateVar').disabled).toBe(false);
        });

        it('should patch publishDateVar', () => {
            const field: AbstractControl = comp.form.get('publishDateVar');
            field.setValue('123');

            const expireDateVarField = de.query(By.css('#content-type-form-expire-date-field'));
            expireDateVarField.triggerEventHandler('onChange', { value: '123' });

            expect(field.value).toBe('');
        });

        it('should patch expireDateVar', () => {
            const field: AbstractControl = comp.form.get('expireDateVar');

            field.setValue('123');

            const expireDateVarField = de.query(By.css('#content-type-form-publish-date-field'));
            expireDateVarField.triggerEventHandler('onChange', { value: '123' });

            expect(field.value).toBe('');
        });
    });

    it('should not submit form with invalid form', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        let data = null;
        jest.spyOn(comp, 'submitForm');

        comp.send.subscribe((res) => (data = res));
        comp.submitForm();

        expect(data).toBeNull();
    });

    it('should not submit a valid form without changes and in Edit mode', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123'
        };
        comp.layout = layout;
        fixture.detectChanges();
        jest.spyOn(comp, 'submitForm');
        jest.spyOn(comp.send, 'emit');

        comp.submitForm();

        expect(comp.send.emit).not.toHaveBeenCalled();
    });

    it('should have dot-page-selector component and right attrs', () => {
        comp.data = {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            host: '123'
        };
        fixture.detectChanges();

        const pageSelector: DebugElement = de.query(By.css('dot-page-selector'));
        expect(pageSelector !== null).toBe(true);
    });

    describe('send data with valid form', () => {
        let data;

        beforeEach(() => {
            jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));
            comp.data = {
                ...dotcmsContentTypeBasicMock,
                baseType: 'CONTENT'
            };
            fixture.detectChanges();
            data = null;
            jest.spyOn(comp, 'submitForm');
            comp.send.subscribe((res) => (data = res));
            comp.form.controls.name.setValue('A content type name');
            fixture.detectChanges();
        });

        it('should submit form correctly', () => {
            const metadata = {};
            metadata[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] = false;
            comp.submitForm();

            expect(data).toEqual({
                icon: null,
                clazz: '',
                description: '',
                host: '',
                defaultType: false,
                fixed: false,
                folder: '',
                system: false,
                name: 'A content type name',
                workflows: [
                    {
                        id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
                        creationDate: expect.any(Date),
                        name: 'System Workflow',
                        description: '',
                        archived: false,
                        mandatory: false,
                        defaultScheme: false,
                        modDate: expect.any(Date),
                        entryActionId: null,
                        system: true
                    }
                ],
                systemActionMappings: { NEW: '' },
                detailPage: '',
                urlMapPattern: '',
                metadata
            });
        });
    });

    describe('workflow field', () => {
        describe('create', () => {
            beforeEach(() => {
                comp.data = {
                    ...dotcmsContentTypeBasicMock,
                    baseType: 'CONTENT'
                };
            });

            describe('community license true', () => {
                beforeEach(() => {
                    jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));
                    fixture.detectChanges();
                });

                it('should show workflow disabled and with message if the license community its true', () => {
                    const workflowMsg = de.query(By.css('#field-workflow-hint'));
                    expect(workflowMsg).toBeDefined();
                    expect(comp.form.get('workflows').disabled).toBe(true);
                    expect(
                        comp.form.get('systemActionMappings').get(DotCMSSystemActionType.NEW)
                            .disabled
                    ).toBe(true);
                });
            });

            describe('community license true', () => {
                beforeEach(() => {
                    jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));
                    fixture.detectChanges();
                });

                it('should show workflow enable and no message if the license community its false', () => {
                    const workflowMsg = de.query(By.css('#field-workflow-hint'));
                    expect(workflowMsg).toBeDefined();
                    expect(comp.form.get('workflows').disabled).toBe(false);
                    expect(
                        comp.form.get('systemActionMappings').get(DotCMSSystemActionType.NEW)
                            .disabled
                    ).toBe(false);
                });
            });
        });

        describe('edit', () => {
            it('should set values from the server', () => {
                comp.data = {
                    ...dotcmsContentTypeBasicMock,
                    baseType: 'CONTENT',
                    id: '123',
                    workflows: [
                        {
                            ...mockWorkflows[0],
                            id: '123',
                            name: 'Workflow 1'
                        },
                        {
                            ...mockWorkflows[1],
                            id: '456',
                            name: 'Workflow 2'
                        }
                    ]
                };
                jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));
                fixture.detectChanges();
                expect(comp.form.get('workflows').value).toEqual([
                    {
                        ...mockWorkflows[0],
                        id: '123',
                        name: 'Workflow 1'
                    },
                    {
                        ...mockWorkflows[1],
                        id: '456',
                        name: 'Workflow 2'
                    }
                ]);
            });

            it('should set empty value', () => {
                comp.data = {
                    ...dotcmsContentTypeBasicMock,
                    baseType: 'CONTENT',
                    id: '123'
                };
                jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));
                fixture.detectChanges();
                expect(comp.form.get('workflows').value).toEqual([]);
            });
            it('should initialize workflowsSelected$ with the value from workflows field', async () => {
                comp.data = {
                    ...dotcmsContentTypeBasicMock,
                    baseType: 'CONTENT',
                    id: '123',
                    workflows: [
                        {
                            ...mockWorkflows[0],
                            id: '123',
                            name: 'Workflow 1'
                        }
                    ]
                };
                fixture.detectChanges();
                await fixture.whenStable();
                comp.workflowsSelected$.subscribe((value) => {
                    expect(value).toEqual([
                        {
                            ...mockWorkflows[0],
                            id: '123',
                            name: 'Workflow 1'
                        }
                    ]);
                });
            });
        });
    });
});
