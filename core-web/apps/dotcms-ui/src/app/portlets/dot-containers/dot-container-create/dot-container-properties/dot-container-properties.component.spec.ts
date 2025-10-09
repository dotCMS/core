import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import {
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    DebugElement,
    EventEmitter,
    forwardRef,
    Input,
    Output
} from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import {
    ControlValueAccessor,
    FormArray,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { InplaceModule } from 'primeng/inplace';
import { MenuModule } from 'primeng/menu';

import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotEventsService,
    DotFormatDateService,
    DotGlobalMessageService,
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
    LoggerService,
    LoginService,
    StringUtils
} from '@dotcms/dotcms-js';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import {
    DotActionMenuButtonComponent,
    DotAddToBundleComponent,
    DotAutofocusDirective,
    DotMessagePipe
} from '@dotcms/ui';
import {
    CoreWebServiceMock,
    DotFormatDateServiceMock,
    DotMessageDisplayServiceMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotContainerPropertiesComponent } from './dot-container-properties.component';

import { DotContainersService } from '../../../../api/services/dot-containers/dot-containers.service';
import { dotEventSocketURLFactory } from '../../../../test/dot-test-bed';
import { DotActionButtonComponent } from '../../../../view/components/_common/dot-action-button/dot-action-button.component';

@Component({
    selector: 'dot-container-code',
    template: '<div></div>',
    standalone: false
})
export class DotContentEditorComponent {}

@Component({
    selector: 'dot-loop-editor',
    template: '<div></div>',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotLoopEditorComponent),
            multi: true
        }
    ],
    standalone: false
})
export class DotLoopEditorComponent {
    writeValue() {
        //
    }

    registerOnChange() {
        //
    }

    registerOnTouched() {
        //
    }
}

@Component({
    selector: 'dot-textarea-content',
    template: '',
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotTextareaContentMockComponent)
        }
    ],
    standalone: false
})
export class DotTextareaContentMockComponent implements ControlValueAccessor {
    @Input()
    code;

    @Input()
    height;

    @Input()
    show;

    @Input()
    value;

    @Input()
    width;

    @Output()
    monacoInit = new EventEmitter();

    @Input()
    language;

    writeValue() {
        //
    }

    registerOnChange() {
        //
    }

    registerOnTouched() {
        //
    }
}

const messages = {
    'message.containers.create.click_to_edit': 'Click to Edit',
    'message.containers.create.description': 'description',
    'message.containers.create.max_contents': 'Max Contents',
    'message.containers.create.clear': 'clear',
    'message.containers.create.content_type_code': 'Code'
};

const containerMockData = {
    container: {
        container: {
            identifier: 'eba434c6-e67a-4a64-9c88-1faffcafb40d',
            title: 'FAQ',
            friendlyName: 'ASD',
            maxContentlets: 20,
            code: 'hello',
            preLoop: '',
            postLoop: ''
        },
        contentTypes: [
            {
                code: '',
                structureId: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                containerId: 'eba434c6-e67a-4a64-9c88-1faffcafb40d',
                containerInode: '',
                contentTypeVar: 'Activity'
            }
        ]
    }
};

const mockContentTypes: DotCMSContentType[] = [
    {
        baseType: 'CONTENT',
        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
        defaultType: false,
        description: 'Activities available at desitnations',
        detailPage: 'e5f131d2-1952-4596-bbbf-28fb28021b68',
        fixed: false,
        folder: 'SYSTEM_FOLDER',
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        iDate: 1567778770000,
        icon: 'paragliding',
        id: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
        modDate: 1663219138000,
        multilingualable: false,
        nEntries: 10,
        name: 'Activity',
        system: false,
        urlMapPattern: '/activities/{urlTitle}',
        variable: 'Activity',
        versionable: true,
        workflows: [],
        fields: [],
        layout: []
    },
    {
        baseType: 'CONTENT',
        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
        defaultType: false,
        description: 'Activities available at desitnations',
        detailPage: 'e5f131d2-1952-4596-bbbf-28fb28021b68',
        fixed: false,
        folder: 'SYSTEM_FOLDER',
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        iDate: 1567778770000,
        icon: 'paragliding',
        id: '12345',
        modDate: 1663219138000,
        multilingualable: false,
        nEntries: 10,
        name: 'Activity 2',
        system: false,
        urlMapPattern: '/activities/{urlTitle}',
        variable: 'Activity2',
        versionable: true,
        workflows: [],
        fields: [],
        layout: []
    }
];

describe('DotContainerPropertiesComponent', () => {
    let fixture: ComponentFixture<DotContainerPropertiesComponent>;
    let comp: DotContainerPropertiesComponent;
    let de: DebugElement;
    let coreWebService: CoreWebService;
    let dotDialogService: DotAlertConfirmService;
    let dotRouterService: DotRouterService;
    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                DotContainerPropertiesComponent,
                DotContentEditorComponent,
                DotLoopEditorComponent,
                DotTextareaContentMockComponent
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: of(containerMockData)
                    }
                },
                {
                    provide: DotRouterService,
                    useValue: {
                        gotoPortlet: jest.fn(),
                        goToEditContainer: jest.fn(),
                        goToSiteBrowser: jest.fn(),
                        goToURL: jest.fn()
                    }
                },
                StringUtils,
                DotHttpErrorManagerService,
                DotContentTypeService,
                DotAlertConfirmService,
                ConfirmationService,
                LoginService,
                DotcmsEventsService,
                DotEventsSocket,
                DotcmsConfigService,
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                DialogService,
                DotSiteBrowserService,
                DotContainersService,
                DotGlobalMessageService,
                DotEventsService,
                LoggerService,
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock }
            ],
            imports: [
                CommonModule,
                DotMessagePipe,
                SharedModule,
                CheckboxModule,
                InplaceModule,
                ReactiveFormsModule,
                MenuModule,
                ButtonModule,
                DotActionButtonComponent,
                DotActionMenuButtonComponent,
                DotAddToBundleComponent,
                HttpClientTestingModule,
                DynamicDialogModule,
                DotAutofocusDirective,
                BrowserAnimationsModule
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();
        fixture = TestBed.createComponent(DotContainerPropertiesComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        coreWebService = TestBed.inject(CoreWebService);
        dotDialogService = TestBed.inject(DotAlertConfirmService);
        dotRouterService = TestBed.inject(DotRouterService);
    });

    describe('with data', () => {
        beforeEach(() => {
            jest.spyOn<CoreWebService>(coreWebService, 'requestView').mockReturnValue(
                of({
                    entity: mockContentTypes,
                    header: (type) => (type === 'Link' ? 'test;test=test' : '10')
                })
            );
            fixture.detectChanges();
        });

        it('should focus on title field', async () => {
            fixture.detectChanges();
            const title = de.query(By.css('[data-testId="title"]'));
            expect(title.attributes.dotAutofocus).toBeDefined();
        });

        it('should setup title', () => {
            const field = de.query(By.css('[data-testId="title"]'));
            expect(field.attributes.pInputText).toBeDefined();
        });

        it('should setup description', () => {
            const field = de.query(By.css('[data-testId="description"]'));
            expect(field).toBeDefined();
        });

        it('should setup Max Contents', () => {
            const field = de.query(By.css('[data-testId="max-contents"]'));
            expect(field).toBeDefined();
        });

        it('should setup code', () => {
            const field = de.query(By.css('[data-testId="code"]'));
            expect(field).toBeDefined();
        });

        it('should render content types when max-content greater then zero', fakeAsync(() => {
            jest.spyOn(fixture.componentInstance, 'showContentTypeAndCode');
            fixture.componentInstance.form.get('maxContentlets').setValue(0);
            fixture.componentInstance.form.get('maxContentlets').valueChanges.subscribe((value) => {
                expect(value).toBe(5);
            });
            expect(fixture.componentInstance.form.get('maxContentlets').updateOn).toBe('change');
            fixture.componentInstance.form.get('maxContentlets').setValue(5);
            tick();
            fixture.detectChanges();
            const preLoopComponent = de.query(By.css('dot-loop-editor'));
            const codeEditorComponent = de.query(By.css('dot-container-code'));
            expect(preLoopComponent).toBeDefined();
            expect(codeEditorComponent).toBeDefined();
            expect(fixture.componentInstance.showContentTypeAndCode).toHaveBeenCalled();
        }));

        it('should clear the field', fakeAsync(() => {
            jest.spyOn(dotDialogService, 'confirm').mockImplementation((conf) => {
                conf.accept();
            });
            jest.spyOn(comp, 'clearContentConfirmationModal');
            comp.form.get('maxContentlets').setValue(0);
            tick();
            fixture.detectChanges();
            expect(comp.form.value).toEqual({
                identifier: 'eba434c6-e67a-4a64-9c88-1faffcafb40d',
                title: 'FAQ',
                friendlyName: 'ASD',
                maxContentlets: 0,
                code: '',
                preLoop: null,
                postLoop: null,
                containerStructures: []
            });

            expect(comp.clearContentConfirmationModal).toHaveBeenCalled();
        }));

        it('should clear the field when user click on clear button', () => {
            comp.form.get('maxContentlets').setValue(0);
            comp.form.get('maxContentlets').setValue(5);
            fixture.detectChanges();
            jest.spyOn(comp, 'clearContentConfirmationModal');
            jest.spyOn(dotDialogService, 'confirm').mockImplementation((conf) => {
                conf.accept();
            });
            const clearBtn = de.query(By.css('[data-testId="clearContent"]'));
            clearBtn.triggerEventHandler('click');
            expect(comp.form.value).toEqual({
                identifier: 'eba434c6-e67a-4a64-9c88-1faffcafb40d',
                title: 'FAQ',
                friendlyName: 'ASD',
                maxContentlets: 0,
                code: '',
                preLoop: null,
                postLoop: null,
                containerStructures: []
            });
            expect(comp.clearContentConfirmationModal).toHaveBeenCalled();
        });

        it('should save button disable', () => {
            const saveBtn = de.query(By.css('[data-testId="saveBtn"]'));
            expect(saveBtn.attributes.disabled).toBeDefined();
        });

        it('should save button enable when data change', () => {
            comp.form.get('title').setValue('Hello');
            fixture.detectChanges();
            const saveBtn = de.query(By.css('[data-testId="saveBtn"]'));
            expect(saveBtn.attributes.disabled).not.toBeDefined();
            comp.form.get('title').setValue('FAQ');

            fixture.detectChanges();
            expect(de.query(By.css('[data-testId="saveBtn"]')).attributes.disabled).toBeDefined();
        });

        it('should save button disable after save', fakeAsync(() => {
            comp.form.get('title').setValue('Hello');
            fixture.detectChanges();
            const saveBtn = de.query(By.css('[data-testId="saveBtn"]'));
            saveBtn.triggerEventHandler('click');
            tick(200);
            fixture.detectChanges();
            expect(de.query(By.css('[data-testId="saveBtn"]')).attributes.disabled).toBeDefined();
        }));

        it('should save button disable but code field is not required', fakeAsync(() => {
            fixture.componentInstance.form.get('maxContentlets').setValue(0);
            fixture.componentInstance.form.get('maxContentlets').setValue(5);
            tick(200);
            fixture.detectChanges();
            const saveBtn = de.query(By.css('[data-testId="saveBtn"]'));
            saveBtn.triggerEventHandler('click');
            fixture.detectChanges();
            expect(de.query(By.css('[data-testId="saveBtn"]')).attributes.disabled).toBeDefined();
            expect(
                (fixture.componentInstance.form.get('containerStructures') as FormArray).controls[0]
                    .get('code')
                    .hasValidator(Validators.required)
            ).toEqual(false);
        }));

        it('should redirect to containers list after save', () => {
            comp.form.get('title').setValue('Hello');
            fixture.detectChanges();
            const saveBtn = de.query(By.css('[data-testId="saveBtn"]'));
            saveBtn.triggerEventHandler('click');
            fixture.detectChanges();
            expect(dotRouterService.goToURL).toHaveBeenCalledWith('/containers');
            expect(dotRouterService.goToURL).toHaveBeenCalledTimes(1);
        });
    });
});
