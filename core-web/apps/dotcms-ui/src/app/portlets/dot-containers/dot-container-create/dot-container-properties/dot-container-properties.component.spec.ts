import { createComponentFactory, Spectator, byTestId } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import {
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    EventEmitter,
    forwardRef,
    Input,
    Output
} from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import {
    ControlValueAccessor,
    FormArray,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';
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
import { DotContainerPropertiesStore } from './store/dot-container-properties.store';

import { DotContainersService } from '../../../../api/services/dot-containers/dot-containers.service';
import { dotEventSocketURLFactory } from '../../../../test/dot-test-bed';
import { DotActionButtonComponent } from '../../../../view/components/_common/dot-action-button/dot-action-button.component';

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
    writeValue(): void {
        /* mock ControlValueAccessor */
    }
    registerOnChange(): void {
        /* mock ControlValueAccessor */
    }
    registerOnTouched(): void {
        /* mock ControlValueAccessor */
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
    @Input() code;
    @Input() height;
    @Input() show;
    @Input() value;
    @Input() width;
    @Output() monacoInit = new EventEmitter();
    @Input() language;
    writeValue(): void {
        /* mock ControlValueAccessor */
    }
    registerOnChange(): void {
        /* mock ControlValueAccessor */
    }
    registerOnTouched(): void {
        /* mock ControlValueAccessor */
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
    let spectator: Spectator<DotContainerPropertiesComponent>;
    let coreWebService: CoreWebService;
    let dotDialogService: DotAlertConfirmService;
    let dotRouterService: DotRouterService;

    const messageServiceMock = new MockDotMessageService(messages);
    const mockRouterService = {
        gotoPortlet: jest.fn(),
        goToEditContainer: jest.fn(),
        goToSiteBrowser: jest.fn(),
        goToURL: jest.fn()
    };

    const createComponent = createComponentFactory({
        component: DotContainerPropertiesComponent,
        imports: [
            CommonModule,
            DotMessagePipe,
            SharedModule,
            CheckboxModule,
            InplaceModule,
            ReactiveFormsModule,
            MenuModule,
            ButtonModule,
            DotContentEditorComponent,
            DotLoopEditorComponent,
            DotTextareaContentMockComponent,
            DotActionButtonComponent,
            DotActionMenuButtonComponent,
            DotAddToBundleComponent,
            HttpClientTestingModule,
            DynamicDialogModule,
            DotAutofocusDirective,
            BrowserAnimationsModule
        ],
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
            { provide: ActivatedRoute, useValue: { data: of(containerMockData) } },
            { provide: DotRouterService, useValue: mockRouterService },
            StringUtils,
            DotHttpErrorManagerService,
            DotContentTypeService,
            DotAlertConfirmService,
            ConfirmationService,
            LoginService,
            DotcmsEventsService,
            DotEventsSocket,
            DotcmsConfigService,
            { provide: DotMessageDisplayService, useClass: DotMessageDisplayServiceMock },
            DialogService,
            DotSiteBrowserService,
            DotContainersService,
            DotGlobalMessageService,
            DotEventsService,
            LoggerService,
            { provide: DotFormatDateService, useClass: DotFormatDateServiceMock }
        ],
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        coreWebService = spectator.inject(CoreWebService);
        dotDialogService = spectator.inject(DotAlertConfirmService);
        dotRouterService = spectator.inject(DotRouterService);
    });

    describe('with data', () => {
        beforeEach(() => {
            (jest.spyOn(coreWebService, 'requestView') as jest.SpyInstance).mockReturnValue(
                of({
                    entity: mockContentTypes,
                    header: (type: string) => (type === 'Link' ? 'test;test=test' : '10')
                })
            );
            spectator.detectChanges();
        });

        it('should focus on title field', () => {
            spectator.detectChanges();
            const title = spectator.query(byTestId('title'));
            expect(title).toBeTruthy();
        });

        it('should setup title', () => {
            const field = spectator.query(byTestId('title'));
            expect(field).toBeTruthy();
        });

        it('should setup description', () => {
            const field = spectator.query(byTestId('description'));
            expect(field).toBeTruthy();
        });

        it('should setup Max Contents', () => {
            const field = spectator.query(byTestId('max-contents'));
            expect(field).toBeTruthy();
        });

        it('should setup code', () => {
            expect(spectator.component.form.get('code')).toBeTruthy();
        });

        it('should render content types when max-content greater then zero', fakeAsync(() => {
            const comp = spectator.component;
            jest.spyOn(comp, 'showContentTypeAndCode');
            comp.form.get('maxContentlets').setValue(0);
            comp.form.get('maxContentlets').valueChanges.subscribe((value) => {
                expect(value).toBe(5);
            });
            expect(comp.form.get('maxContentlets').updateOn).toBe('change');
            comp.form.get('maxContentlets').setValue(5);
            tick(150);
            spectator.detectChanges();
            tick(50);
            spectator.detectChanges();
            expect(spectator.query('dot-loop-editor')).toBeTruthy();
            expect(comp.showContentTypeAndCode).toHaveBeenCalled();
        }));

        it('should clear the field', fakeAsync(() => {
            jest.spyOn(dotDialogService, 'confirm').mockImplementation((conf) => {
                conf.accept();
            });
            const comp = spectator.component;
            jest.spyOn(comp, 'clearContentConfirmationModal');
            comp.form.get('maxContentlets').setValue(0);
            tick(150);
            spectator.detectChanges();
            expect(comp.form.value).toEqual({
                identifier: 'eba434c6-e67a-4a64-9c88-1faffcafb40d',
                title: 'FAQ',
                friendlyName: 'ASD',
                maxContentlets: 0,
                code: '',
                preLoop: '',
                postLoop: '',
                containerStructures: []
            });
            expect(comp.clearContentConfirmationModal).toHaveBeenCalled();
        }));

        it('should clear the field when user click on clear button', () => {
            const comp = spectator.component;
            comp.form.get('maxContentlets').setValue(0);
            comp.form.get('maxContentlets').setValue(5);
            spectator.detectChanges();
            jest.spyOn(comp, 'clearContentConfirmationModal');
            jest.spyOn(dotDialogService, 'confirm').mockImplementation((conf) => {
                conf.accept();
            });
            spectator.click(byTestId('clearContent'));
            expect(comp.form.value).toEqual({
                identifier: 'eba434c6-e67a-4a64-9c88-1faffcafb40d',
                title: 'FAQ',
                friendlyName: 'ASD',
                maxContentlets: 0,
                code: '',
                preLoop: '',
                postLoop: '',
                containerStructures: []
            });
            expect(comp.clearContentConfirmationModal).toHaveBeenCalled();
        });

        it('should save button disable', () => {
            const saveBtn = spectator.query(byTestId('saveBtn'));
            expect(saveBtn).toBeTruthy();
            expect((saveBtn as HTMLButtonElement).disabled).toBe(true);
        });

        it('should save button enable when data change', fakeAsync(() => {
            spectator.component.form.get('title').setValue('Hello');
            tick(150);
            spectator.detectChanges();
            const saveBtn = spectator.query(byTestId('saveBtn')) as HTMLButtonElement;
            expect(saveBtn.disabled).toBe(false);
            spectator.component.form.get('title').setValue('FAQ');
            tick(150);
            spectator.detectChanges();
            expect((spectator.query(byTestId('saveBtn')) as HTMLButtonElement).disabled).toBe(true);
        }));

        it('should save button disable after save', fakeAsync(() => {
            spectator.component.form.get('title').setValue('Hello');
            tick(150); // flush form valueChanges debounce(100) so updateFormStatus already ran
            spectator.detectChanges();
            const store = spectator.debugElement.injector.get(DotContainerPropertiesStore);
            let invalidForm: boolean | undefined;
            const sub = store.vm$.subscribe((s) => (invalidForm = s.invalidForm));
            spectator.click(byTestId('saveBtn'));
            tick(0);
            spectator.detectChanges();
            expect(invalidForm).toBe(true);
            sub.unsubscribe();
        }));

        it('should save button disable but code field is not required', fakeAsync(() => {
            const comp = spectator.component;
            comp.form.get('maxContentlets').setValue(0);
            comp.form.get('maxContentlets').setValue(5);
            tick(200);
            spectator.detectChanges();
            spectator.click(byTestId('saveBtn'));
            spectator.detectChanges();
            expect((spectator.query(byTestId('saveBtn')) as HTMLButtonElement).disabled).toBe(true);
            expect(
                (comp.form.get('containerStructures') as FormArray).controls[0]
                    .get('code')
                    .hasValidator(Validators.required)
            ).toBe(false);
        }));

        it('should redirect to containers list after save', fakeAsync(() => {
            (dotRouterService.goToURL as jest.Mock).mockClear();
            spectator.component.form.get('title').setValue('Hello');
            tick(150);
            spectator.detectChanges();
            spectator.click(byTestId('saveBtn'));
            tick(0);
            spectator.detectChanges();
            expect(dotRouterService.goToURL).toHaveBeenCalledWith('/containers');
            expect(dotRouterService.goToURL).toHaveBeenCalledTimes(1);
        }));
    });
});
