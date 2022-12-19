import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { of } from 'rxjs';
import { DotContainerPropertiesComponent } from './dot-container-properties.component';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { LoggerService } from '@dotcms/dotcms-js';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotFormatDateServiceMock, MockDotMessageService } from '@dotcms/utils-testing';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotEventsService,
    DotMessageService,
    DotSiteBrowserService
} from '@dotcms/data-access';
import { ActivatedRoute } from '@angular/router';
import { CoreWebServiceMock } from '@dotcms/utils-testing';
import { DotEventsSocketURL } from '@dotcms/dotcms-js';
import { dotEventSocketURLFactory } from '@tests/dot-test-bed';
import { StringUtils } from '@dotcms/dotcms-js';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { ConfirmationService, SharedModule } from 'primeng/api';
import { LoginService } from '@dotcms/dotcms-js';
import { DotcmsEventsService } from '@dotcms/dotcms-js';
import { DotEventsSocket } from '@dotcms/dotcms-js';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { CommonModule } from '@angular/common';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { CheckboxModule } from 'primeng/checkbox';
import { MenuModule } from 'primeng/menu';
import { ButtonModule } from 'primeng/button';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    DebugElement,
    EventEmitter,
    forwardRef,
    Input,
    Output
} from '@angular/core';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { InplaceModule } from 'primeng/inplace';
import {
    ControlValueAccessor,
    FormArray,
    FormControl,
    FormGroup,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule
} from '@angular/forms';
import { By } from '@angular/platform-browser';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';

@Component({
    selector: 'dot-container-code',
    template: '<div></div>'
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
    ]
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
    ]
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
        contentTypes: []
    }
};

describe('DotContainerPropertiesComponent', () => {
    let fixture: ComponentFixture<DotContainerPropertiesComponent>;
    let comp: DotContainerPropertiesComponent;
    let de: DebugElement;
    let coreWebService: CoreWebService;
    let dotDialogService: DotAlertConfirmService;
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
                        gotoPortlet: jasmine.createSpy(),
                        goToEditContainer: jasmine.createSpy(),
                        goToSiteBrowser: jasmine.createSpy()
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
                DotMessageDisplayService,
                DialogService,
                DotSiteBrowserService,
                DotContainersService,
                DotGlobalMessageService,
                DotEventsService,
                DotHttpErrorManagerService,
                LoggerService,
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock }
            ],
            imports: [
                CommonModule,
                DotMessagePipeModule,
                SharedModule,
                CheckboxModule,
                InplaceModule,
                ReactiveFormsModule,
                MenuModule,
                ButtonModule,
                DotActionButtonModule,
                DotActionMenuButtonModule,
                DotAddToBundleModule,
                HttpClientTestingModule,
                DynamicDialogModule,
                DotAutofocusModule,
                BrowserAnimationsModule
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();
        fixture = TestBed.createComponent(DotContainerPropertiesComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        coreWebService = TestBed.inject(CoreWebService);
        dotDialogService = TestBed.inject(DotAlertConfirmService);
    });

    describe('with data', () => {
        beforeEach(() => {
            spyOn<CoreWebService>(coreWebService, 'requestView').and.returnValue(
                of({
                    entity: [],
                    header: (type) => (type === 'Link' ? 'test;test=test' : '10')
                })
            );
            fixture.detectChanges();
        });

        it('should focus on title field', async () => {
            const inplace = de.query(By.css('[data-testId="inplace"]'));
            inplace.componentInstance.activate();
            fixture.detectChanges();
            const title = de.query(By.css('[data-testId="title"]'));

            expect(inplace.componentInstance.active).toBe(true);
            expect(title.attributes.dotAutofocus).toBeDefined();
        });

        it('should setup title', () => {
            const inplace = de.query(By.css('[data-testId="inplace"]'));
            inplace.componentInstance.activate();
            fixture.detectChanges();
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

        it('should button enable when max content greater then zero', fakeAsync(() => {
            fixture.componentInstance.form.get('maxContentlets').setValue(2);
            const contentTypeButton = de.query(By.css('[data-testId=showContentTypeAndCode]'));
            spyOn(fixture.componentInstance, 'showContentTypeAndCode');
            contentTypeButton.triggerEventHandler('click');
            tick();
            fixture.detectChanges();
            const preLoopComponent = de.query(By.css('dot-loop-editor'));
            const codeEditoromponent = de.query(By.css('dot-container-code'));
            expect(contentTypeButton.attributes.disable).not.toBeDefined();
            expect(preLoopComponent).toBeDefined();
            expect(codeEditoromponent).toBeDefined();
            expect(fixture.componentInstance.showContentTypeAndCode).toHaveBeenCalled();
        }));

        it('should clear the field', () => {
            comp.form.setValue({
                title: 'Title 1',
                friendlyName: 'friendlyName',
                maxContentlets: 23,
                code: 'code',
                preLoop: 'preloop',
                postLoop: 'postloop',
                identifier: '',
                containerStructures: []
            });
            (comp.form.get('containerStructures') as FormArray).push(
                new FormGroup({
                    code: new FormControl(''),
                    structureId: new FormControl('structureId')
                })
            );
            comp.showContentTypeAndCode();
            fixture.detectChanges();
            const clearBtn = de.query(By.css('[data-testId="clearContent"]'));
            spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
                conf.accept();
            });
            clearBtn.triggerEventHandler('click');

            expect(comp.form.value).toEqual({
                title: 'Title 1',
                friendlyName: 'friendlyName',
                maxContentlets: 0,
                code: null,
                preLoop: null,
                postLoop: null,
                identifier: '',
                containerStructures: []
            });
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
    });
});
