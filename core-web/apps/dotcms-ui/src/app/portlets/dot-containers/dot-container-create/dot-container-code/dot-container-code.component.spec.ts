import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, forwardRef, Input, Output } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import {
    ControlValueAccessor,
    FormArray,
    FormBuilder,
    FormControl,
    FormGroup,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule
} from '@angular/forms';
import { By } from '@angular/platform-browser';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@dotcms/app/test/dot-message-service.mock';
import { DotPipesModule } from '@dotcms/app/view/pipes/dot-pipes.module';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { Menu, MenuModule } from 'primeng/menu';
import { TabViewModule } from 'primeng/tabview';
import { of } from 'rxjs';
import { DotAddVariableModule } from './dot-add-variable/dot-add-variable.module';
import { DotContentEditorComponent } from './dot-container-code.component';

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
    }
];

@Component({
    selector: 'dot-host-component',
    template: `
        <dot-container-code [contentTypes]="contentTypes" [fg]="form"> </dot-container-code>
    `
})
class HostTestComponent {
    form: FormGroup;
    contentTypes = mockContentTypes;
    constructor(private fb: FormBuilder) {
        this.form = this.fb.group({
            containerStructures: this.fb.array([])
        });

        (this.form.get('containerStructures') as FormArray).push(
            this.fb.group({
                structureId: new FormControl('12345'),
                code: new FormControl('')
            })
        );
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

const messageServiceMock = new MockDotMessageService({
    'containers.properties.add.variable.title': 'Title',
    Add: 'Add'
});

describe('DotContentEditorComponent', () => {
    let hostFixture: ComponentFixture<HostTestComponent>;
    let de: DebugElement;
    let coreWebService: CoreWebService;
    let menu: Menu;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                HostTestComponent,
                DotContentEditorComponent,
                DotTextareaContentMockComponent
            ],
            imports: [
                ReactiveFormsModule,
                DynamicDialogModule,
                DotAddVariableModule,
                TabViewModule,
                MenuModule,
                ButtonModule,
                DotPipesModule,
                HttpClientTestingModule
            ],

            providers: [
                DialogService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock }
            ]
        });

        hostFixture = TestBed.createComponent(HostTestComponent);
        de = hostFixture.debugElement;
        coreWebService = TestBed.inject(CoreWebService);
    });

    describe('dot-add-variable-dialog', () => {
        beforeEach(fakeAsync(() => {
            spyOn<CoreWebService>(coreWebService, 'requestView').and.returnValue(
                of({
                    entity: mockContentTypes
                })
            );
            hostFixture.detectChanges();
            tick();
            hostFixture.detectChanges();
            de = hostFixture.debugElement;
        }));

        it('should set labels', () => {
            menu = de.query(By.css('p-menu')).componentInstance;
            const actions = [{ label: 'Activity', command: jasmine.any(Function) }];

            expect(menu.model).toEqual(actions);
        });

        it('should have content types', () => {
            const contentTypes = de.queryAll(By.css('p-tabpanel'));
            expect(contentTypes.length).toEqual(2);
        });
    });
});
