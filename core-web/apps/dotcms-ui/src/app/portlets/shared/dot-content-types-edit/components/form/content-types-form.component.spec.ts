/* eslint-disable @typescript-eslint/no-empty-function */

import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Observable, of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import {
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageService,
    DotSiteService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import {
    DotCMSClazzes,
    DotCMSContentTypeLayoutRow,
    DotCMSSystemActionType,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { DotSiteComponent } from '@dotcms/ui';
import {
    CoreWebServiceMock,
    dotcmsContentTypeBasicMock,
    dotcmsContentTypeFieldBasicMock,
    DotWorkflowServiceMock,
    MockDotMessageService,
    mockWorkflows,
    mockWorkflowsActions
} from '@dotcms/utils-testing';

import { ContentTypesFormComponent } from './content-types-form.component';

import { DotWorkflowsActionsSelectorFieldService } from '../../../../../view/components/_common/dot-workflows-actions-selector-field/services/dot-workflows-actions-selector-field.service';

@Injectable()
class MockDotLicenseService {
    isEnterprise(): Observable<boolean> {
        return of(false);
    }
}

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

const mockActivatedRoute = {
    snapshot: {
        data: {
            featuredFlags: {
                [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true
            }
        }
    }
};

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

describe('ContentTypesFormComponent', () => {
    let spectator: Spectator<ContentTypesFormComponent>;
    let dotLicenseService: DotLicenseService;
    let activatedRoute: ActivatedRoute;

    const createComponent = createComponentFactory({
        component: ContentTypesFormComponent,
        componentProviders: [DotSiteComponent],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            { provide: DotMessageService, useValue: messageServiceMock },
            {
                provide: DotSiteService,
                useValue: {
                    getSites: jest.fn().mockReturnValue(
                        of({
                            sites: [
                                {
                                    hostname: 'demo.dotcms.com',
                                    identifier: '123-xyz-567-xxl',
                                    archived: false,
                                    aliases: null
                                }
                            ],
                            pagination: { currentPage: 1, perPage: 40, totalEntries: 1 }
                        })
                    ),
                    getSiteById: jest.fn().mockReturnValue(
                        of({
                            hostname: 'demo.dotcms.com',
                            identifier: '123-xyz-567-xxl',
                            archived: false,
                            aliases: null
                        })
                    )
                }
            },
            { provide: DotWorkflowService, useClass: DotWorkflowServiceMock },
            { provide: DotLicenseService, useClass: MockDotLicenseService },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            { provide: ActivatedRoute, useValue: mockActivatedRoute },
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotWorkflowsActionsService),
            {
                provide: DotWorkflowsActionsSelectorFieldService,
                useValue: {
                    get: () => of([]),
                    load: jest.fn()
                }
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        dotLicenseService = spectator.inject(DotLicenseService);
        activatedRoute = spectator.inject(ActivatedRoute);
    });

    it('should be invalid by default', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT'
        });
        spectator.detectChanges();
        expect(spectator.component.form.valid).toBe(false);
    });

    it('should be valid when name field have value', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT'
        });
        spectator.detectChanges();

        spectator.component.form.get('name').setValue('content type name');
        expect(spectator.component.form.valid).toBe(true);
    });

    it('should have name focus by default on create mode', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT'
        });
        spectator.detectChanges();
        expect(spectator.component.$inputName().nativeElement).toBe(document.activeElement);
    });

    it('should have canSave property false by default (form is invalid)', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT'
        });
        spectator.detectChanges();

        expect(spectator.component.canSave).toBe(false);
    });

    it('should set canSave property true form is valid', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            name: 'hello',
            baseType: 'CONTENT'
        });
        spectator.detectChanges();

        // Form is only valid when "name" property is set
        spectator.component.form.get('description').setValue('hello world');
        spectator.detectChanges();

        expect(spectator.component.canSave).toBe(true);
    });

    it('should set canSave property false when form is invalid in edit mode', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        });
        spectator.detectChanges();

        spectator.component.form.get('name').setValue(null);
        spectator.detectChanges();

        expect(spectator.component.canSave).toBe(false);
    });

    it('should set canSave property true when form is valid and model updated in edit mode', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        });
        spectator.detectChanges();

        spectator.component.form.get('description').setValue('some desc');
        spectator.detectChanges();

        expect(spectator.component.canSave).toBe(true);
    });

    it('should set canSave property false when form is invalid and model updated in edit mode', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        });
        spectator.detectChanges();

        spectator.component.form.get('name').setValue(null);
        spectator.component.form.get('description').setValue('some desc');
        spectator.detectChanges();

        expect(spectator.component.canSave).toBe(false);
    });

    // tslint:disable-next-line:max-line-length
    it('should set canSave property false when the form value is updated and then gets back to the original content (no community license)', async () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World',
            host: '123-xyz-567-xxl' // Match the mock site
        });
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        // The form is valid in edit mode with a name, so canSave starts as false (no changes)
        expect(spectator.component.canSave).toBe(false); // by default is false

        spectator.component.form.get('name').setValue('A new  name');
        spectator.detectChanges();
        expect(spectator.component.canSave).toBe(true); // name updated set it to true

        spectator.component.form.get('name').setValue('Hello World');
        spectator.detectChanges();
        expect(spectator.component.canSave).toBe(false); // revert the change button disabled set it to false
    });

    // eslint-disable-next-line max-len
    it('should set canSave property false when the form value is updated and then gets back to the original content (community license)', async () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World',
            host: '123-xyz-567-xxl' // Match the mock site
        });
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        // The form is valid in edit mode with a name, so canSave starts as false (no changes)
        expect(spectator.component.canSave).toBe(false); // by default is false

        spectator.component.form.get('name').setValue('A new  name');
        spectator.detectChanges();
        expect(spectator.component.canSave).toBe(true); // name updated set it to true

        spectator.component.form.get('name').setValue('Hello World');
        spectator.detectChanges();
        expect(spectator.component.canSave).toBe(false); // revert the change button disabled set it to false
    });

    it('should set canSave property false when edit a content with fields', async () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World',
            host: '123-xyz-567-xxl', // Match the mock site
            layout: layout
        });
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        expect(spectator.component.canSave).toBe(false); // by default is false
    });

    it('should set canSave property false on edit mode', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123'
        });
        spectator.detectChanges();

        expect(spectator.component.canSave).toBe(false);
    });

    it('should have basic form controls for non-content base types', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'WIDGET'
        });
        spectator.detectChanges();

        expect(Object.keys(spectator.component.form.controls).length).toBe(14);
        expect(spectator.component.form.get('icon')).not.toBeNull();
        expect(spectator.component.form.get('clazz')).not.toBeNull();
        expect(spectator.component.form.get('name')).not.toBeNull();
        expect(spectator.component.form.get('host')).not.toBeNull();
        expect(spectator.component.form.get('description')).not.toBeNull();
        expect(spectator.component.form.get('workflows')).not.toBeNull();
        expect(spectator.component.form.get('publishDateVar')).not.toBeNull();
        expect(spectator.component.form.get('expireDateVar')).not.toBeNull();
        expect(spectator.component.form.get('defaultType')).not.toBeNull();
        expect(spectator.component.form.get('fixed')).not.toBeNull();
        expect(spectator.component.form.get('system')).not.toBeNull();
        expect(spectator.component.form.get('folder')).not.toBeNull();
        const workflowAction = spectator.component.form.get('systemActionMappings');
        expect(workflowAction.get(DotCMSSystemActionType.NEW)).not.toBeNull();

        expect(spectator.component.form.get('detailPage')).toBeNull();
        expect(spectator.component.form.get('urlMapPattern')).toBeNull();
        expect(spectator.component.form.get('newEditContent')).not.toBeNull();
    });

    it('should render basic fields for non-content base types', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'WIDGET'
        });
        spectator.detectChanges();

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
            expect(spectator.query(field)).not.toBeNull();
        });
    });

    it('should have basic form controls for content base type', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT'
        });
        spectator.detectChanges();

        expect(Object.keys(spectator.component.form.controls).length).toBe(16);
        expect(spectator.component.form.get('clazz')).not.toBeNull();
        expect(spectator.component.form.get('name')).not.toBeNull();
        expect(spectator.component.form.get('icon')).not.toBeNull();
        expect(spectator.component.form.get('host')).not.toBeNull();
        expect(spectator.component.form.get('description')).not.toBeNull();
        expect(spectator.component.form.get('workflows')).not.toBeNull();
        expect(spectator.component.form.get('publishDateVar')).not.toBeNull();
        expect(spectator.component.form.get('expireDateVar')).not.toBeNull();
        expect(spectator.component.form.get('detailPage')).not.toBeNull();
        expect(spectator.component.form.get('urlMapPattern')).not.toBeNull();
        expect(spectator.component.form.get('defaultType')).not.toBeNull();
        expect(spectator.component.form.get('fixed')).not.toBeNull();
        expect(spectator.component.form.get('system')).not.toBeNull();
        expect(spectator.component.form.get('folder')).not.toBeNull();
        expect(spectator.component.form.get('newEditContent')).not.toBeNull();

        const workflowAction = spectator.component.form.get('systemActionMappings');
        expect(workflowAction.get(DotCMSSystemActionType.NEW)).not.toBeNull();
    });

    it('should set value to the form', async () => {
        jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));

        const base = {
            icon: null,
            clazz: DotCMSClazzes.TEXT,
            defaultType: false,
            description: 'description',
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: 'host-id',
            name: 'name',
            system: false,
            detailPage: 'detail-page',
            urlMapPattern: '/url/map'
        };

        // Need to create a new spectator with enterprise license before initialization
        // Use fixture.componentRef.setInput before detectChanges for Angular 21 required signal inputs
        spectator.fixture.componentRef.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            ...base,
            baseType: 'CONTENT',
            expireDateVar: 'expireDateVar',
            publishDateVar: 'publishDateVar',
            layout: layout
        });
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        // Manually call setDateVarFieldsState since ngOnChanges doesn't exist
        spectator.component['setDateVarFieldsState']();
        spectator.detectChanges();

        expect(spectator.component.form.value).toEqual({
            ...base,
            expireDateVar: 'expireDateVar',
            publishDateVar: 'publishDateVar',
            systemActionMappings: {
                NEW: ''
            },
            workflows: [
                {
                    ...mockWorkflows[2],
                    creationDate: '2018-04-05T14:21:33.321Z',
                    modDate: '2018-04-03T22:35:58.958Z'
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
            spectator.setInput('contentType', {
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
            });

            spectator.detectChanges();

            expect(spectator.component.form.get('systemActionMappings').value).toEqual({
                NEW: '44d4d4cd-c812-49db-adb1-1030be73e69a'
            });
        });

        it('should set value to the form with systemActionMappings with empty object', () => {
            spectator.setInput('contentType', {
                ...dotcmsContentTypeBasicMock,
                baseType: 'CONTENT',
                systemActionMappings: {}
            });

            spectator.detectChanges();

            expect(spectator.component.form.get('systemActionMappings').value).toEqual({
                NEW: ''
            });
        });
    });

    it('should render extra fields for content types', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT'
        });
        spectator.detectChanges();

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
            expect(spectator.query(field)).not.toBeNull();
        });
    });

    it('should render disabled dates fields and hint when date fields are not passed', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123'
        });
        spectator.detectChanges();

        const dateFieldMsg = spectator.query('#field-dates-hint');

        expect(dateFieldMsg).toBeTruthy();
        expect(spectator.component.form.get('publishDateVar').disabled).toBe(true);
        expect(spectator.component.form.get('expireDateVar').disabled).toBe(true);
    });

    it('should render the new content banner when the feature flag is enabled', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123'
        });

        activatedRoute.snapshot.data.featuredFlags[
            FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
        ] = true;

        spectator.detectChanges();

        const newContentBanner = spectator.query(
            '[data-test-id="content-type__new-content-banner"]'
        );
        expect(newContentBanner).not.toBeNull();
    });

    it('should hide the new content banner when the feature flag is disabled', () => {
        // Need to update the flag before component initialization
        activatedRoute.snapshot.data.featuredFlags[
            FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
        ] = false;

        // Use main spectator but update the input value directly
        // Set input using fixture's componentRef before any detectChanges
        spectator.fixture.componentRef.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123'
        });
        spectator.detectChanges();

        const newContentBanner = spectator.query(
            '[data-test-id="content-type__new-content-banner"]'
        );
        expect(newContentBanner).toBeNull();

        // Reset flag for other tests
        activatedRoute.snapshot.data.featuredFlags[
            FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
        ] = true;
    });

    describe('fields dates enabled', () => {
        beforeEach(async () => {
            spectator.setInput('contentType', {
                ...dotcmsContentTypeBasicMock,
                baseType: 'CONTENT',
                layout: layout
            });
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            // Manually call setDateVarFieldsState since ngOnChanges doesn't exist
            spectator.component['setDateVarFieldsState']();
            spectator.detectChanges();
        });

        it('should render enabled dates fields when date fields are passed', () => {
            expect(spectator.component.form.get('publishDateVar').disabled).toBe(false);
            expect(spectator.component.form.get('expireDateVar').disabled).toBe(false);
        });

        it('should patch publishDateVar', () => {
            const field: AbstractControl = spectator.component.form.get('publishDateVar');
            field.setValue('123');

            spectator.component.handleDateVarChange({ value: '123' }, 'expireDateVar');

            expect(field.value).toBe('');
        });

        it('should patch expireDateVar', () => {
            const field: AbstractControl = spectator.component.form.get('expireDateVar');

            field.setValue('123');

            spectator.component.handleDateVarChange({ value: '123' }, 'publishDateVar');

            expect(field.value).toBe('');
        });
    });

    it('should not submit form with invalid form', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT'
        });
        spectator.detectChanges();

        let data = null;
        jest.spyOn(spectator.component, 'submitForm');

        spectator.component.$send.subscribe((res) => (data = res));
        spectator.component.submitForm();

        expect(data).toBeNull();
    });

    it('should not submit a valid form without changes and in Edit mode', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            id: '123',
            layout: layout
        });
        spectator.detectChanges();
        jest.spyOn(spectator.component, 'submitForm');
        jest.spyOn(spectator.component.$send, 'emit');

        spectator.component.submitForm();

        expect(spectator.component.$send.emit).not.toHaveBeenCalled();
    });

    it('should have dot-page-selector component and right attrs', () => {
        spectator.setInput('contentType', {
            ...dotcmsContentTypeBasicMock,
            baseType: 'CONTENT',
            host: '123'
        });
        spectator.detectChanges();

        const pageSelector = spectator.query('dot-page-selector');
        expect(pageSelector !== null).toBe(true);
    });

    describe('send data with valid form', () => {
        let data;

        beforeEach(() => {
            jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));
            spectator.setInput('contentType', {
                ...dotcmsContentTypeBasicMock,
                baseType: 'CONTENT'
            });
            spectator.detectChanges();
            data = null;
            jest.spyOn(spectator.component, 'submitForm');
            spectator.component.$send.subscribe((res) => (data = res));
            spectator.component.form.controls.name.setValue('A content type name');
            // Set host to match SiteServiceMock currentSite identifier
            spectator.component.form.controls.host.setValue('123-xyz-567-xxl');
            spectator.detectChanges();
        });

        it('should submit form correctly', () => {
            const metadata = {};
            metadata[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] = false;
            spectator.component.submitForm();

            expect(data).toEqual({
                icon: null,
                clazz: '',
                description: '',
                host: '123-xyz-567-xxl', // from SiteServiceMock currentSite
                defaultType: false,
                fixed: false,
                folder: '',
                system: false,
                name: 'A content type name',
                workflows: [
                    {
                        id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
                        creationDate: '2018-04-05T14:21:33.321Z',
                        name: 'System Workflow',
                        description: '',
                        archived: false,
                        mandatory: false,
                        defaultScheme: false,
                        modDate: '2018-04-03T22:35:58.958Z',
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
                spectator.setInput('contentType', {
                    ...dotcmsContentTypeBasicMock,
                    baseType: 'CONTENT'
                });
            });

            describe('community license true', () => {
                beforeEach(() => {
                    jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));
                    spectator.detectChanges();
                });

                it('should show workflow disabled and with message if the license community its true', () => {
                    const workflowMsg = spectator.query('#field-workflow-hint');
                    expect(workflowMsg).toBeDefined();
                    expect(spectator.component.form.get('workflows').disabled).toBe(true);
                    expect(
                        spectator.component.form
                            .get('systemActionMappings')
                            .get(DotCMSSystemActionType.NEW).disabled
                    ).toBe(true);
                });
            });

            describe('community license true', () => {
                it('should show workflow enable and no message if the license community its false', () => {
                    // Mock before creating the component
                    jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));

                    // Create new component with enterprise license
                    const enterpriseSpectator = createComponent();
                    enterpriseSpectator.setInput('contentType', {
                        ...dotcmsContentTypeBasicMock,
                        baseType: 'CONTENT'
                    });
                    enterpriseSpectator.detectChanges();

                    const workflowMsg = enterpriseSpectator.query('#field-workflow-hint');
                    expect(workflowMsg).toBeDefined();
                    expect(enterpriseSpectator.component.form.get('workflows').disabled).toBe(
                        false
                    );
                    expect(
                        enterpriseSpectator.component.form
                            .get('systemActionMappings')
                            .get(DotCMSSystemActionType.NEW).disabled
                    ).toBe(false);
                });
            });
        });

        describe('edit', () => {
            it('should set values from the server', () => {
                spectator.setInput('contentType', {
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
                });
                jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));
                spectator.detectChanges();
                expect(spectator.component.form.get('workflows').value).toEqual([
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
                spectator.setInput('contentType', {
                    ...dotcmsContentTypeBasicMock,
                    baseType: 'CONTENT',
                    id: '123'
                });
                jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));
                spectator.detectChanges();
                expect(spectator.component.form.get('workflows').value).toEqual([]);
            });
            it('should initialize workflowsSelected$ with the value from workflows field', async () => {
                spectator.setInput('contentType', {
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
                });
                spectator.detectChanges();
                await spectator.fixture.whenStable();
                spectator.component.workflowsSelected$.subscribe((value) => {
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
