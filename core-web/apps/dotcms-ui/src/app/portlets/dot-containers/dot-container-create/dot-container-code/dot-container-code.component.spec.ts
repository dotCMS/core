import { createFakeEvent } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import {
    Component,
    DebugElement,
    EventEmitter,
    forwardRef,
    Input,
    Output,
    inject as inject_1
} from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import {
    ControlValueAccessor,
    FormArray,
    FormBuilder,
    FormGroup,
    FormsModule,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { Menu, MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';
import { TabViewModule } from 'primeng/tabview';

import { DotMessageService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

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

@Component({
    selector: 'dot-host-component',
    template: `
        <dot-container-code [contentTypes]="contentTypes" [fg]="form"></dot-container-code>
    `,
    standalone: false
})
class HostTestComponent {
    private fb = inject_1(FormBuilder);

    form: FormGroup;
    contentTypes = mockContentTypes;

    constructor() {
        this.form = this.fb.group({
            containerStructures: this.fb.array([], [Validators.required, Validators.minLength(1)])
        });
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

const messageServiceMock = new MockDotMessageService({
    'containers.properties.add.variable.title': 'Title',
    'message.containers.empty.content_type_message': 'Content Type Empty',
    'message.containers.empty.content_type_need_help': 'Need help',
    'message.containers.empty.content_type_go_to_documentation': 'Go to documentation',
    Add: 'Add'
});

describe('DotContentEditorComponent', () => {
    let hostFixture: ComponentFixture<HostTestComponent>;
    let hostComponent: HostTestComponent;
    let comp: DotContentEditorComponent;
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
                FormsModule,
                DynamicDialogModule,
                DotAddVariableModule,
                TabViewModule,
                MenuModule,
                ButtonModule,
                DotSafeHtmlPipe,
                DotMessagePipe,
                HttpClientTestingModule,
                BrowserAnimationsModule,
                SkeletonModule
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
        hostComponent = hostFixture.componentInstance;
        de = hostFixture.debugElement;
        coreWebService = TestBed.inject(CoreWebService);
    });

    describe('with data', () => {
        beforeEach(fakeAsync(() => {
            jest.spyOn<CoreWebService>(coreWebService, 'requestView').mockReturnValue(
                of({
                    entity: mockContentTypes
                })
            );
            hostFixture.detectChanges();
            tick();
            de = hostFixture.debugElement;
            hostComponent = hostFixture.componentInstance;
            comp = de.query(By.css('dot-container-code')).componentInstance;
            menu = de.query(By.css('p-menu')).componentInstance;
        }));

        it('should set labels', () => {
            const actions = [
                { label: 'Activity', command: expect.any(Function) },
                { label: 'Activity 2', command: expect.any(Function) }
            ];

            expect(menu.model).toEqual(actions);
        });

        it('shoud have empty content type message', () => {
            comp.removeItem(1);
            hostFixture.detectChanges();
            const icon = de.query(By.css('[data-testId="code"]'));
            const title = de.query(By.css('[data-testId="empty-content-title"]'));
            const subtitle = de.query(By.css('[data-testId="empty-content-subtitle"]'));
            const link = de.query(By.css('[data-testId="empty-content-link"]'));
            expect(icon).toBeDefined();
            expect(title.nativeElement.textContent).toContain('Content Type Empty');
            expect(subtitle.nativeElement.textContent.trim()).toContain('Need help?');
            expect(link.nativeElement.getAttribute('href')).toBeTruthy();
            expect(link.nativeElement.getAttribute('href')).toBe(
                'https://www.dotcms.com/docs/latest/containers'
            );
            expect(hostComponent.form.valid).toEqual(false);
        });

        describe('without default content type', () => {
            beforeEach(() => {
                comp.removeItem(1);
                hostFixture.detectChanges();
            });

            it('should have add content type', fakeAsync(() => {
                menu.model[0].command({ originalEvent: createFakeEvent('click') });
                hostFixture.detectChanges();
                const contentTypes = de.queryAll(By.css('p-tabpanel'));
                const code = de.query(By.css(`[data-testid="${mockContentTypes[0].id}"]`));
                code.triggerEventHandler('monacoInit', {
                    name: menu.model[0].label,
                    editor: {
                        focus: jest.fn()
                    }
                });
                hostFixture.detectChanges();
                tick(100);
                expect(code).not.toBeNull();
                expect(code.attributes.formControlName).toBe('code');
                expect(code.attributes.language).toBe('html');
                expect(code.attributes['ng-reflect-show']).toBe('code');
                expect(contentTypes.length).toEqual(2);
                expect((hostComponent.form.get('containerStructures') as FormArray).length).toEqual(
                    1
                );
                expect(
                    (hostComponent.form.get('containerStructures') as FormArray).controls[0]
                        .get('code')
                        .hasValidator(Validators.required)
                ).toEqual(false);
                expect(hostComponent.form.valid).toEqual(true);
                expect(comp.monacoEditors[mockContentTypes[0].name].focus).toHaveBeenCalled();
            }));

            xit('should have remove content type and focus on another content type', fakeAsync(() => {
                menu.model[0].command({ originalEvent: createFakeEvent('click') });
                menu.model[1].command({ originalEvent: createFakeEvent('click') });
                hostFixture.detectChanges();
                const code = de.query(By.css(`[data-testid="${mockContentTypes[0].id}"]`));
                code.triggerEventHandler('monacoInit', {
                    name: mockContentTypes[0].id,
                    editor: {
                        focus: jest.fn()
                    }
                });
                const code2 = de.query(By.css(`[data-testid="${mockContentTypes[1].id}"]`));
                code2.triggerEventHandler('monacoInit', {
                    name: mockContentTypes[1].id,
                    editor: {
                        focus: jest.fn()
                    }
                });
                hostFixture.detectChanges();
                tick(100);
                const tabCloseBtn = de.queryAll(By.css('.p-tabview-close'));

                tabCloseBtn[1].triggerEventHandler('click');
                hostFixture.detectChanges();
                const contentTypes = de.queryAll(By.css('p-tabpanel'));
                const codeExist = de.query(By.css(`[data-testid="${mockContentTypes[1].id}"]`));

                expect(codeExist).toBeNull();
                expect(contentTypes.length).toEqual(2);
                expect((hostComponent.form.get('containerStructures') as FormArray).length).toEqual(
                    1
                );
                expect(hostComponent.form.valid).toEqual(true);
                expect(comp.monacoEditors[mockContentTypes[0].id].focus).toHaveBeenCalled();
            }));

            it('should have select content type and focus on field', fakeAsync(() => {
                menu.model[0].command({ originalEvent: createFakeEvent('click') });
                menu.model[1].command({ originalEvent: createFakeEvent('click') });
                hostFixture.detectChanges();
                const code = de.query(By.css(`[data-testid="${mockContentTypes[0].id}"]`));
                code.triggerEventHandler('monacoInit', {
                    name: mockContentTypes[0].id,
                    editor: {
                        focus: jest.fn()
                    }
                });
                const code2 = de.query(By.css(`[data-testid="${mockContentTypes[1].id}"]`));
                code2.triggerEventHandler('monacoInit', {
                    name: mockContentTypes[1].id,
                    editor: {
                        focus: jest.fn()
                    }
                });
                hostFixture.detectChanges();
                tick(100);
                const tabLists = de.query(By.css('[role="tablist"]')).children;
                tabLists[1].children[0].triggerEventHandler('click');
                hostFixture.detectChanges();
                const selectedContentType = de.query(By.css('.p-highlight'));
                expect(code).not.toBeNull();
                expect(code.attributes.formControlName).toBe('code');
                expect(code.attributes.language).toBe('html');
                expect(code.attributes['ng-reflect-show']).toBe('code');
                expect(selectedContentType.nativeElement.textContent.toLowerCase()).toBe(
                    mockContentTypes[0].name.toLowerCase()
                );
                expect(hostComponent.form.valid).toEqual(true);
                expect(comp.monacoEditors[mockContentTypes[0].id].focus).toHaveBeenCalled();
            }));
        });

        it('shoud not have required code field on default content type', () => {
            expect(
                (hostComponent.form.get('containerStructures') as FormArray).controls[0]
                    .get('code')
                    .hasValidator(Validators.required)
            ).toEqual(false);
            expect(hostComponent.form.valid).toEqual(true);
        });

        it('shoud have add loader on content types', () => {
            // remove all content types
            comp.contentTypes = [];
            hostFixture.detectChanges();
            const loader = de.query(By.css('p-skeleton'));
            expect(loader).toBeDefined();
        });

        it('should have a menu with max height in 300px and overflow auto', () => {
            expect(menu.style['max-height']).toBe('300px');
            expect(menu.style.overflow).toBe('auto');
        });
    });
});
