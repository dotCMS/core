import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { of } from 'rxjs';

import { DialogService } from 'primeng/dynamicdialog';
import { ButtonModule } from 'primeng/button';

import {
    DotTemplateStore,
    EMPTY_TEMPLATE_ADVANCED,
    EMPTY_TEMPLATE_DESIGN
} from './store/dot-template.store';
import { DotTemplateCreateEditComponent } from './dot-template-create-edit.component';
import { DotFormDialogModule } from '@components/dot-form-dialog/dot-form-dialog.module';
import { DotTemplatePropsModule } from './dot-template-props/dot-template-props.module';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessagePipe } from '@pipes/dot-message/dot-message.pipe';

@Component({
    selector: 'dot-api-link',
    template: ''
})
export class DotApiLinkMockComponent {
    @Input() href;

    constructor() {}
}

@Component({
    selector: 'dot-template-builder',
    template: ''
})
export class DotTemplateBuilderMockComponent {
    @Input() item;
    @Output() save = new EventEmitter();
    @Output() cancel = new EventEmitter();

    constructor() {}
}

@Component({
    selector: 'dot-portlet-base',
    template: '<ng-content></ng-content>'
})
export class DotPortletBaseMockComponent {
    @Input() boxed;

    constructor() {}
}

@Component({
    selector: 'dot-portlet-toolbar',
    template: '<ng-content></ng-content>'
})
export class DotPortletToolbarMockComponent {
    @Input() title;

    constructor() {}
}

const messageServiceMock = new MockDotMessageService({
    'templates.create.title': 'Create new template',
    'templates.properties.title': 'Template Properties',
    'templates.edit': 'Edit'
});

describe('DotTemplateCreateEditComponent', () => {
    let fixture: ComponentFixture<DotTemplateCreateEditComponent>;
    let de: DebugElement;
    let dialogService: DialogService;
    let store: DotTemplateStore;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                DotPortletBaseMockComponent,
                DotPortletToolbarMockComponent,
                DotTemplateBuilderMockComponent,
                DotTemplateCreateEditComponent,
                DotApiLinkMockComponent,
                DotMessagePipe
            ],
            imports: [
                FormsModule,
                ReactiveFormsModule,
                BrowserAnimationsModule,
                DotFormDialogModule,
                DotTemplatePropsModule,
                ButtonModule
            ],
            providers: [
                DialogService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        });
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
                const storeMock = jasmine.createSpyObj(
                    'DotTemplateStore',
                    ['createTemplate', 'goToTemplateList'],
                    {
                        vm$: of({
                            original: EMPTY_TEMPLATE_DESIGN
                        })
                    }
                );

                TestBed.overrideProvider(DotTemplateStore, { useValue: storeMock });
                fixture = TestBed.createComponent(DotTemplateCreateEditComponent);
                de = fixture.debugElement;

                dialogService = fixture.debugElement.injector.get(DialogService);
                store = fixture.debugElement.injector.get(DotTemplateStore);
                spyOn(dialogService, 'open').and.callThrough();

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
                expect(dialogService.open).toHaveBeenCalledWith(jasmine.any(Function), {
                    header: 'Create new template',
                    width: '30rem',
                    closable: false,
                    closeOnEscape: false,
                    data: {
                        template: {
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
                            theme: 'd7b0ebc2-37ca-4a5a-b769-e8a3ff187661'
                        },
                        onSave: jasmine.any(Function),
                        onCancel: jasmine.any(Function)
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

            it('should save template when save dialog button is clicked', () => {
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

                expect(store.createTemplate).toHaveBeenCalledWith({
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
                    theme: 'd7b0ebc2-37ca-4a5a-b769-e8a3ff187661'
                });
            });
        });

        describe('Advanced', () => {
            beforeEach(() => {
                const storeMock = jasmine.createSpyObj(
                    'DotTemplateStore',
                    ['createTemplate', 'goToTemplateList'],
                    {
                        vm$: of({
                            original: EMPTY_TEMPLATE_ADVANCED
                        })
                    }
                );

                TestBed.overrideProvider(DotTemplateStore, { useValue: storeMock });
                fixture = TestBed.createComponent(DotTemplateCreateEditComponent);

                dialogService = fixture.debugElement.injector.get(DialogService);
                store = fixture.debugElement.injector.get(DotTemplateStore);
                spyOn(dialogService, 'open').and.callThrough();

                fixture.detectChanges();
            });

            it('should open create dialog', async () => {
                expect(dialogService.open).toHaveBeenCalledWith(jasmine.any(Function), {
                    header: 'Create new template',
                    width: '30rem',
                    closable: false,
                    closeOnEscape: false,
                    data: {
                        template: { title: '', body: '', identifier: '', friendlyName: '' },
                        onSave: jasmine.any(Function),
                        onCancel: jasmine.any(Function)
                    }
                });
            });

            it('should save template when save dialog button is clicked', () => {
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

                expect(store.createTemplate).toHaveBeenCalledWith({
                    title: 'Hello World',
                    body: '',
                    identifier: '',
                    friendlyName: ''
                });
            });
        });
    });

    describe('Edit', () => {
        describe('Design', () => {
            beforeEach(() => {
                const storeMock = jasmine.createSpyObj(
                    'DotTemplateStore',
                    ['saveTemplate', 'goToTemplateList'],
                    {
                        vm$: of({
                            original: {
                                ...EMPTY_TEMPLATE_DESIGN,
                                identifier: '123',
                                title: 'Some template'
                            },
                            apiLink: '/api/link'
                        })
                    }
                );

                TestBed.overrideProvider(DotTemplateStore, { useValue: storeMock });
                fixture = TestBed.createComponent(DotTemplateCreateEditComponent);
                de = fixture.debugElement;

                dialogService = fixture.debugElement.injector.get(DialogService);
                store = fixture.debugElement.injector.get(DotTemplateStore);
                spyOn(dialogService, 'open').and.callThrough();

                fixture.detectChanges();
            });

            it('should load edit mode', () => {
                const portlet = de.query(By.css('dot-portlet-base')).componentInstance;
                const toolbar = de.query(By.css('dot-portlet-toolbar')).componentInstance;
                const builder = de.query(By.css('dot-template-builder')).componentInstance;
                const apiLink = de.query(By.css('dot-api-link')).componentInstance;

                expect(portlet.boxed).toBe(false);
                expect(toolbar.title).toBe('Some template');
                expect(builder.item).toEqual({
                    ...EMPTY_TEMPLATE_DESIGN,
                    identifier: '123',
                    title: 'Some template'
                });
                expect(apiLink.href).toBe('/api/link');

                expect(dialogService.open).not.toHaveBeenCalled();
            });

            describe('edit body', () => {
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
                        }
                    });

                    expect(store.saveTemplate).toHaveBeenCalledWith({
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
                        theme: 'd7b0ebc2-37ca-4a5a-b769-e8a3ff187661'
                    });
                });

                it('should cancel', () => {
                    const builder = de.query(By.css('dot-template-builder'));
                    builder.triggerEventHandler('cancel', {});

                    expect(store.goToTemplateList).toHaveBeenCalledTimes(1);
                });
            });

            describe('edit properties', () => {
                it('should have edit button', () => {
                    const button = de.query(By.css('[data-testId="editTemplateButton"]'));

                    expect(button.attributes['ng-reflect-label']).toBe('Edit');
                    expect(button.attributes.icon).toBe('pi pi-pencil');
                    expect(button.attributes.class).toContain('p-button-text');
                    expect(button.attributes.pButton).toBeDefined();
                });

                it('should open edit props form', () => {
                    const button = de.query(By.css('[data-testId="editTemplateButton"]'));
                    button.nativeElement.click();

                    expect(dialogService.open).toHaveBeenCalledWith(jasmine.any(Function), {
                        header: 'Template Properties',
                        width: '30rem',

                        data: {
                            template: {
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
                                theme: 'd7b0ebc2-37ca-4a5a-b769-e8a3ff187661'
                            },
                            onSave: jasmine.any(Function)
                        }
                    });
                });
            });
        });
    });
});
