/* eslint-disable @typescript-eslint/no-explicit-any */

import { BehaviorSubject, Observable, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, Input, Output, forwardRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
    ControlValueAccessor,
    NG_VALUE_ACCESSOR,
    FormsModule,
    ReactiveFormsModule
} from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';

import {
    DotCrudService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRouterService,
    DotSystemConfigService,
    DotTempFileUploadService,
    DotThemesService,
    DotWorkflowActionsFireService,
    PaginatorService
} from '@dotcms/data-access';
import { CoreWebService, SiteService } from '@dotcms/dotcms-js';
import { DotSystemConfig } from '@dotcms/dotcms-models';
import { DotFormDialogComponent, DotMessagePipe, DotApiLinkComponent } from '@dotcms/ui';
import {
    CoreWebServiceMock,
    MockDotMessageService,
    MockDotRouterService,
    mockDotThemes,
    mockSites,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotTemplateBuilderComponent } from './dot-template-builder/dot-template-builder.component';
import { DotTemplateCreateEditComponent } from './dot-template-create-edit.component';
import { DotTemplatePropsComponent } from './dot-template-props/dot-template-props.component';
import { DotTemplateThumbnailFieldComponent } from './dot-template-props/dot-template-thumbnail-field/dot-template-thumbnail-field.component';
import {
    DotTemplateItem,
    DotTemplateStore,
    EMPTY_TEMPLATE_ADVANCED,
    EMPTY_TEMPLATE_DESIGN
} from './store/dot-template.store';

import { DotPortletToolbarComponent } from '../../../view/components/dot-portlet-base/components/dot-portlet-toolbar/dot-portlet-toolbar.component';
import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';

@Component({
    selector: 'dot-api-link',
    template: ''
})
export class DotApiLinkMockComponent {
    @Input() href;
}

@Component({
    selector: 'dot-template-builder',
    template: ''
})
export class DotTemplateBuilderMockComponent {
    @Input() item;
    @Input() didTemplateChanged;
    @Output() save = new EventEmitter();
    @Output() cancel = new EventEmitter();
    @Output() custom: EventEmitter<CustomEvent> = new EventEmitter();
}

@Component({
    selector: 'dot-portlet-base',
    template: '<ng-content></ng-content>'
})
export class DotPortletBaseMockComponent {
    @Input() boxed;
}

@Component({
    selector: 'dot-portlet-toolbar',
    template:
        '<div><div class="left"><ng-content select="[left]"></ng-content></div><ng-content></ng-content></div>'
})
export class DotPortletToolbarMockComponent {
    @Input() title;
}

@Component({
    selector: 'dot-template-thumbnail-field',
    template: '<input type="text" [value]="value" (input)="onInput($event)" />',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotTemplateThumbnailFieldMockComponent),
            multi: true
        }
    ]
})
export class DotTemplateThumbnailFieldMockComponent implements ControlValueAccessor {
    @Input() value = '';

    private onChange = (_value: any) => undefined;
    private onTouched = () => undefined;

    onInput(event: any) {
        this.value = event.target.value;
        this.onChange(this.value);
    }

    writeValue(value: any): void {
        this.value = value || '';
    }

    registerOnChange(fn: any): void {
        this.onChange = fn;
    }

    registerOnTouched(fn: any): void {
        this.onTouched = fn;
    }
}

const messageServiceMock = new MockDotMessageService({
    'templates.create.title': 'Create new template',
    'templates.properties.title': 'Template Properties',
    'templates.edit': 'Edit'
});

interface TemplateStoreValueType {
    [key: string]: jest.SpyInstance;
}

const mockSystemConfig: DotSystemConfig = {
    logos: { loginScreen: '', navBar: '' },
    colors: { primary: '#54428e', secondary: '#3a3847', background: '#BB30E1' },
    releaseInfo: { buildDate: 'June 24, 2019', version: '5.0.0' },
    systemTimezone: { id: 'America/Costa_Rica', label: 'Costa Rica', offset: 360 },
    languages: [],
    license: {
        level: 100,
        displayServerId: '19fc0e44',
        levelName: 'COMMUNITY EDITION',
        isCommunity: true
    },
    cluster: { clusterId: 'test-cluster', companyKeyDigest: 'test-digest' }
};

class MockDotSystemConfigService {
    getSystemConfig(): Observable<DotSystemConfig> {
        return of(mockSystemConfig);
    }
}

async function makeFormValid(fixture) {
    // can't use debugElement because the dialogs opens outside the component
    const title: HTMLInputElement = document.querySelector(
        '[data-testid="templatePropsTitleField"]'
    );

    title.value = 'Hello World';

    const event = new Event('input', {
        bubbles: true,
        cancelable: true
    });

    title.dispatchEvent(event);

    const themeButton: any = document.querySelector(
        '[data-testid="templatePropsThemeField"] button'
    );

    await fixture.whenRenderingDone();
    themeButton.click();
    fixture.detectChanges();
    await fixture.whenRenderingDone();
    const item: HTMLElement = document.querySelector('.theme-selector__data-list-item');
    item.click();
}

describe('DotTemplateCreateEditComponent', () => {
    let fixture: ComponentFixture<DotTemplateCreateEditComponent>;
    let de: DebugElement;
    let component: DotTemplateCreateEditComponent;
    let dialogService: DialogService;
    let store: DotTemplateStore;
    let templateStoreValue: TemplateStoreValueType;
    const siteServiceMock = new SiteServiceMock();

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                DotTemplateCreateEditComponent,
                DotMessagePipe,
                FormsModule,
                ReactiveFormsModule,
                BrowserAnimationsModule,
                DotFormDialogComponent,
                DotTemplatePropsComponent,
                ButtonModule,
                HttpClientTestingModule
            ],
            providers: [
                DotHttpErrorManagerService,
                DialogService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                {
                    provide: DotEventsService,
                    useValue: {
                        listen() {
                            return of([]);
                        }
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                /*
            DotTempFileUploadService, DotWorkflowActionsFireService and DotCrudService:
            This three are from DotTemplateThumbnailFieldComponent and because
            I had to import DotTemplatePropsComponent so I can click the real dialog that
            gets append to the body.
        */
                {
                    provide: DotTempFileUploadService,
                    useValue: {
                        upload: jest.fn().mockReturnValue(
                            of([
                                {
                                    assetVersion: '',
                                    name: '',
                                    identifier: ''
                                }
                            ])
                        )
                    }
                },
                {
                    provide: DotWorkflowActionsFireService,
                    useValue: {
                        publishContentletAndWaitForIndex: jest.fn().mockReturnValue(
                            of({
                                identifier: ''
                            })
                        )
                    }
                },
                {
                    provide: DotCrudService,
                    useValue: {
                        getDataById: jest.fn().mockReturnValue(
                            of([
                                {
                                    identifier: ''
                                }
                            ])
                        )
                    }
                },
                /*
            PaginatorService, SiteService and DotThemesService:
            This three are from DotThemeSelectorDropdownComponent and because
            I had to import DotTemplatePropsComponent so I can click the real dialog that
            gets append to the body.
        */
                {
                    provide: PaginatorService,
                    useValue: {
                        url: '',
                        paginationPerPage: '',
                        totalRecords: mockDotThemes.length,
                        get: jest.fn().mockReturnValue(of([...mockDotThemes])),
                        setExtraParams() {
                            //
                        },
                        getWithOffset() {
                            return of([...mockDotThemes]);
                        }
                    }
                },
                {
                    provide: SiteService,
                    useValue: siteServiceMock
                },
                {
                    provide: DotThemesService,
                    useValue: {
                        get: jest.fn().mockReturnValue(of(mockDotThemes[1]))
                    }
                },
                { provide: DotSystemConfigService, useClass: MockDotSystemConfigService },
                { provide: DotRouterService, useClass: MockDotRouterService }
            ]
        })
            .overrideComponent(DotTemplateCreateEditComponent, {
                remove: {
                    imports: [
                        DotApiLinkComponent,
                        DotPortletBaseComponent,
                        DotPortletToolbarComponent,
                        DotTemplateBuilderComponent
                    ]
                },
                add: {
                    imports: [
                        DotApiLinkMockComponent,
                        DotPortletBaseMockComponent,
                        DotPortletToolbarMockComponent,
                        DotTemplateBuilderMockComponent
                    ]
                }
            })
            .overrideComponent(DotTemplatePropsComponent, {
                remove: {
                    imports: [DotTemplateThumbnailFieldComponent]
                },
                add: {
                    imports: [DotTemplateThumbnailFieldMockComponent]
                }
            })
            .compileComponents();

        templateStoreValue = {
            createTemplate: jest.fn(),
            goToEditTemplate: jest.fn(),
            goToTemplateList: jest.fn(),
            saveTemplate: jest.fn(),
            saveWorkingTemplate: jest.fn(),
            saveAndPublishTemplate: jest.fn(),
            updateTemplate: jest.fn(),
            updateWorkingTemplate: jest.fn()
        };
    });

    afterEach(() => {
        const dialog = document.querySelector('p-dynamicdialog');
        if (dialog) {
            dialog.remove();
        }
    });

    describe('Create', () => {
        describe('Design', () => {
            beforeEach(() => {
                const storeMock = {
                    ...templateStoreValue,
                    vm$: of({
                        working: EMPTY_TEMPLATE_DESIGN,
                        original: EMPTY_TEMPLATE_DESIGN
                    })
                };

                // TestBed.

                TestBed.overrideProvider(DotTemplateStore, { useValue: storeMock });
                fixture = TestBed.createComponent(DotTemplateCreateEditComponent);
                de = fixture.debugElement;

                dialogService = fixture.debugElement.injector.get(DialogService);
                store = fixture.debugElement.injector.get(DotTemplateStore);
                jest.spyOn(dialogService, 'open');

                fixture.detectChanges();
            });

            it('should not show extra components', () => {
                const portlet = de.query(By.css('dot-portlet-base'));
                const toolbar = de.query(By.css('dot-portlet-toolbar'));
                const builder = de.query(By.css('dot-template-builder'));
                const apiLink = de.query(By.css('dot-api-link'));

                expect(portlet).toBeNull();
                expect(toolbar).toBeNull();
                expect(builder).toBeNull();
                expect(apiLink).toBeNull();
            });

            it('should open create dialog', async () => {
                expect(dialogService.open).toHaveBeenCalledWith(expect.any(Function), {
                    header: 'Create new template',
                    width: '40rem',
                    closable: false,
                    closeOnEscape: false,
                    data: {
                        template: {
                            type: 'design',
                            title: '',
                            layout: {
                                header: true,
                                footer: true,
                                body: { rows: [] },
                                sidebar: null,
                                title: '',
                                width: null
                            },
                            identifier: '',
                            friendlyName: '',
                            theme: '',
                            image: ''
                        },
                        onSave: expect.any(Function)
                    }
                });
            });

            it('should go to template list when cancel dialog button is clicked', () => {
                // can't use debugElement because the dialogs opens outside the component
                const button: HTMLButtonElement = document.querySelector(
                    '[data-testid="dotFormDialogCancel"]'
                );
                button.click();

                expect(store.goToTemplateList).toHaveBeenCalledTimes(1);
            });

            xit('should save template when save dialog button is clicked', async () => {
                await makeFormValid(fixture);

                const button: HTMLButtonElement = document.querySelector(
                    '[data-testid="dotFormDialogSave"]'
                );

                button.click();

                await fixture.whenStable();

                expect(store.createTemplate).toHaveBeenCalledWith({
                    type: 'design',
                    title: 'Hello World',
                    layout: {
                        header: true,
                        footer: true,
                        body: { rows: [] },
                        sidebar: null,
                        title: '',
                        width: null
                    },
                    identifier: '',
                    friendlyName: '',
                    theme: 'test',
                    image: ''
                });
            });
        });

        describe('Advanced', () => {
            beforeEach(() => {
                const storeMock = {
                    ...templateStoreValue,
                    vm$: of({
                        working: EMPTY_TEMPLATE_ADVANCED,
                        original: EMPTY_TEMPLATE_ADVANCED
                    })
                };

                TestBed.overrideProvider(DotTemplateStore, { useValue: storeMock });
                fixture = TestBed.createComponent(DotTemplateCreateEditComponent);

                dialogService = fixture.debugElement.injector.get(DialogService);
                store = fixture.debugElement.injector.get(DotTemplateStore);
                jest.spyOn(dialogService, 'open');

                fixture.detectChanges();
            });

            it('should open create dialog', async () => {
                expect(dialogService.open).toHaveBeenCalledWith(expect.any(Function), {
                    header: 'Create new template',
                    width: '40rem',
                    closable: false,
                    closeOnEscape: false,
                    data: {
                        template: {
                            type: 'advanced',
                            title: '',
                            body: '',
                            identifier: '',
                            friendlyName: '',
                            image: ''
                        },
                        onSave: expect.any(Function)
                    }
                });
            });

            it('should save template when save dialog button is clicked', async () => {
                // can't use debugElement because the dialogs opens outside the component
                const title: HTMLInputElement = document.querySelector(
                    '[data-testid="templatePropsTitleField"]'
                );

                title.value = 'Hello World';

                const event = new Event('input', {
                    bubbles: true,
                    cancelable: true
                });

                title.dispatchEvent(event);

                const button: HTMLButtonElement = document.querySelector(
                    '[data-testid="dotFormDialogSave"]'
                );
                button.click();

                await fixture.whenStable();

                expect(store.createTemplate).toHaveBeenCalledWith({
                    type: 'advanced',
                    title: 'Hello World',
                    body: '',
                    identifier: '',
                    friendlyName: '',
                    image: ''
                });
            });
        });
    });

    describe('Edit', () => {
        describe('Design', () => {
            beforeEach(() => {
                const template: DotTemplateItem = {
                    ...EMPTY_TEMPLATE_DESIGN,
                    identifier: '123',
                    title: 'Some template'
                };
                const storeMock = {
                    ...templateStoreValue,
                    saveTemplate: jest.fn(),
                    saveAndPublishTemplate: jest.fn(),
                    goToTemplateList: jest.fn(),
                    goToEditTemplate: jest.fn(),
                    vm$: of({
                        working: template,
                        original: template,
                        apiLink: '/api/link'
                    })
                };

                TestBed.overrideProvider(DotTemplateStore, { useValue: storeMock });
                fixture = TestBed.createComponent(DotTemplateCreateEditComponent);
                de = fixture.debugElement;

                dialogService = fixture.debugElement.injector.get(DialogService);
                store = fixture.debugElement.injector.get(DotTemplateStore);
                jest.spyOn(dialogService, 'open');

                fixture.detectChanges();
            });

            it('should load edit mode', () => {
                const portlet = de.query(By.css('dot-portlet-base')).componentInstance;
                const builder = de.query(By.css('dot-template-builder')).componentInstance;
                const apiLink = de.query(By.css('dot-api-link')).componentInstance;

                expect(portlet.boxed).toBe(false);
                expect(builder.item).toEqual({
                    ...EMPTY_TEMPLATE_DESIGN,
                    identifier: '123',
                    title: 'Some template'
                });
                expect(apiLink.href).toBe('/api/link');

                expect(dialogService.open).not.toHaveBeenCalled();
            });

            describe('edit layout', () => {
                it('should save', () => {
                    const builder = de.query(By.css('dot-template-builder'));
                    builder.triggerEventHandler('save', {
                        layout: {
                            title: '',
                            width: '',
                            footer: true,
                            header: false,
                            sidebar: {},
                            body: {
                                rows: []
                            }
                        },
                        themeId: '123'
                    });
                    expect(store.saveTemplate).toHaveBeenCalledWith({
                        type: 'design',
                        title: 'Some template',
                        layout: {
                            title: '',
                            width: '',
                            footer: true,
                            header: false,
                            sidebar: {},
                            body: {
                                rows: []
                            }
                        },
                        identifier: '123',
                        friendlyName: '',
                        theme: '123',
                        image: ''
                    });
                });

                it('should call updateWorkingTemplate from store when updateTemplate', () => {
                    const builder = de.query(By.css('dot-template-builder'));
                    builder.triggerEventHandler('updateTemplate', {
                        layout: {
                            title: '',
                            width: '',
                            footer: true,
                            header: false,
                            sidebar: {},
                            body: {
                                rows: []
                            }
                        },
                        themeId: '123'
                    });

                    const template: DotTemplateItem = {
                        type: 'design',
                        title: 'Some template',
                        layout: {
                            title: '',
                            width: '',
                            footer: true,
                            header: false,
                            sidebar: {},
                            body: {
                                rows: []
                            }
                        },
                        identifier: '123',
                        friendlyName: '',
                        theme: '123',
                        image: ''
                    };

                    expect(store.updateWorkingTemplate).toHaveBeenCalledWith(template);
                    expect(store.updateWorkingTemplate).toHaveBeenCalledTimes(1);
                });

                it('should saveAndPublishTemplate', () => {
                    const builder = de.query(By.css('dot-template-builder'));
                    builder.triggerEventHandler('saveAndPublish', {
                        layout: {
                            title: '',
                            width: '',
                            footer: true,
                            header: false,
                            sidebar: {},
                            body: {
                                rows: []
                            }
                        },
                        themeId: '123'
                    });

                    expect(store.saveAndPublishTemplate).toHaveBeenCalledWith({
                        title: 'Some template',
                        type: 'design',
                        layout: {
                            title: '',
                            width: '',
                            footer: true,
                            header: false,
                            sidebar: {},
                            body: {
                                rows: []
                            }
                        },
                        identifier: '123',
                        friendlyName: '',
                        theme: '123',
                        image: ''
                    });
                });

                it('should cancel', () => {
                    const builder = de.query(By.css('dot-template-builder'));
                    builder.triggerEventHandler('cancel', {});

                    expect(store.goToTemplateList).toHaveBeenCalledTimes(1);
                });

                it('should go to edit template page', () => {
                    const builder = de.query(By.css('dot-template-builder'));
                    builder.triggerEventHandler('custom', {
                        detail: { data: { id: '1', inode: '2' } }
                    });

                    expect(store.goToEditTemplate).toHaveBeenCalledWith('1', '2');
                    expect(store.goToEditTemplate).toHaveBeenCalledTimes(1);
                });

                it('should go to listing if page site changes', () => {
                    fixture.detectChanges(); // Initialize component and subscriptions
                    siteServiceMock.setFakeCurrentSite(mockSites[1]); // switching the site
                    expect(store.goToTemplateList).toHaveBeenCalledTimes(1);
                });
            });

            describe('edit properties', () => {
                it('should have edit button', () => {
                    const button = de.query(By.css('.left [data-testId="editTemplateButton"]'));
                    // In Angular 20, ng-reflect-* attributes are not available
                    // Verify the label by checking the button text content
                    const buttonElement = button.nativeElement;
                    expect(buttonElement.textContent?.trim()).toBe('Edit');
                    // Verify icon by checking the PrimeNG Button component instance
                    // PrimeNG Button component has an 'icon' property
                    const buttonComponent = button.componentInstance;
                    if (buttonComponent && buttonComponent.icon) {
                        expect(buttonComponent.icon).toBe('pi pi-pencil');
                    } else {
                        // Fallback: verify icon exists in DOM (PrimeNG may render it differently)
                        const iconInDom =
                            buttonElement.querySelector('.pi-pencil') ||
                            buttonElement.querySelector('[class*="pi-pencil"]');
                        expect(iconInDom).toBeTruthy();
                    }
                    // Verify class
                    expect(buttonElement.classList.contains('p-button-text')).toBe(true);
                    // Verify pButton directive is applied (button should have PrimeNG button classes)
                    expect(buttonElement.classList.contains('p-button')).toBe(true);
                });

                it('should open edit props form', () => {
                    const button = de.query(By.css('[data-testId="editTemplateButton"]'));
                    button.nativeElement.click();

                    expect(dialogService.open).toHaveBeenCalledWith(expect.any(Function), {
                        header: 'Template Properties',
                        width: '30rem',

                        data: {
                            template: {
                                type: 'design',
                                title: 'Some template',
                                layout: {
                                    header: true,
                                    footer: true,
                                    body: { rows: [] },
                                    sidebar: null,
                                    title: '',
                                    width: null
                                },
                                identifier: '123',
                                friendlyName: '',
                                theme: '',
                                image: ''
                            },
                            onSave: expect.any(Function)
                        }
                    });
                });
            });
        });

        describe('Advanced', () => {
            beforeEach(() => {
                const template: DotTemplateItem = {
                    ...EMPTY_TEMPLATE_ADVANCED,
                    identifier: '123',
                    title: 'Some template'
                };
                const storeMock = {
                    ...templateStoreValue,
                    vm$: of({
                        working: template,
                        original: template,
                        apiLink: '/api/link'
                    })
                };

                TestBed.overrideProvider(DotTemplateStore, { useValue: storeMock });
                fixture = TestBed.createComponent(DotTemplateCreateEditComponent);
                de = fixture.debugElement;

                dialogService = fixture.debugElement.injector.get(DialogService);
                store = fixture.debugElement.injector.get(DotTemplateStore);
                jest.spyOn(dialogService, 'open');

                fixture.detectChanges();
            });

            describe('edit layout', () => {
                it('should save and publish', () => {
                    const builder = de.query(By.css('dot-template-builder'));
                    builder.triggerEventHandler('saveAndPublish', {
                        body: `<h1>##Container and stuff</h1>`
                    });

                    expect(store.saveAndPublishTemplate).toHaveBeenCalledWith({
                        type: 'advanced',
                        title: 'Some template',
                        body: '<h1>##Container and stuff</h1>',
                        identifier: '123',
                        friendlyName: '',
                        image: ''
                    });
                });

                it('should call updateWorkingTemplate from store when updateTemplate', () => {
                    const builder = de.query(By.css('dot-template-builder'));
                    builder.triggerEventHandler('updateTemplate', {
                        body: `<h1>##Container and stuff</h1>`
                    });

                    expect(store.updateWorkingTemplate).toHaveBeenCalledWith({
                        type: 'advanced',
                        title: 'Some template',
                        body: '<h1>##Container and stuff</h1>',
                        identifier: '123',
                        friendlyName: '',
                        image: ''
                    });
                });
            });
        });
    });

    describe('Forms', () => {
        const subject = new BehaviorSubject<any>(null);

        beforeEach(() => {
            const storeMock = {
                ...templateStoreValue,
                vm$: subject
            };

            TestBed.overrideProvider(DotTemplateStore, { useValue: storeMock });
            fixture = TestBed.createComponent(DotTemplateCreateEditComponent);
            component = fixture.componentInstance;

            dialogService = fixture.debugElement.injector.get(DialogService);
            store = fixture.debugElement.injector.get(DotTemplateStore);
            jest.spyOn(dialogService, 'open');

            subject.next({
                working: EMPTY_TEMPLATE_ADVANCED,
                original: EMPTY_TEMPLATE_ADVANCED
            });

            fixture.detectChanges();
        });

        it('should have basic value', () => {
            expect(component.form.value).toEqual({
                type: 'advanced',
                title: '',
                body: '',
                identifier: '',
                friendlyName: '',
                image: ''
            });
        });

        it('should update the form when state updates', () => {
            expect(component.form.value).toEqual({
                type: 'advanced',
                title: '',
                body: '',
                identifier: '',
                friendlyName: '',
                image: ''
            });

            const nextTemplate = {
                type: 'advanced',
                friendlyName: 'Not batman',
                identifier: '123',
                title: 'Hello World',
                body: '<h1>I am Batman</h1>',
                image: ''
            };

            subject.next({
                working: nextTemplate,
                original: nextTemplate
            });

            fixture.detectChanges();

            expect(component.form.value).toEqual({
                type: 'advanced',
                title: 'Hello World',
                body: '<h1>I am Batman</h1>',
                identifier: '123',
                friendlyName: 'Not batman',
                image: ''
            });
        });
    });
});
