import { Component, Input, Output, EventEmitter, DebugElement } from '@angular/core';
import { ComponentFixture, getTestBed, TestBed } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@pipes/dot-message/dot-message.pipe';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { By } from '@angular/platform-browser';
import { DotFavoritePageComponent } from './dot-favorite-page.component';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotAlertConfirmService } from '@dotcms/app/api/services/dot-alert-confirm';
import { ConfirmationService } from 'primeng/api';
import { LoginServiceMock, mockUser } from '@dotcms/app/test/login-service.mock';
import { CoreWebService, CoreWebServiceMock, LoginService } from '@dotcms/dotcms-js';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotTempFileUploadService } from '@dotcms/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotWorkflowActionsFireService } from '@dotcms/app/api/services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotRolesService } from '@dotcms/app/api/services/dot-roles/dot-roles.service';
import { DotPageRender } from '@dotcms/app/shared/models/dot-page/dot-rendered-page.model';
import { mockDotRenderedPage } from '@dotcms/app/test/dot-page-render.mock';
import { DotPageRenderState } from '../../shared/models';
import { MultiSelectModule } from 'primeng/multiselect';
import { mockProcessedRoles } from '@dotcms/app/api/services/dot-roles/dot-roles.service.spec';
import { of } from 'rxjs';
import { mockDotCMSTempFile } from '@components/dot-add-persona-dialog/dot-create-persona-form/dot-create-persona-form.component.spec';
@Component({
    selector: 'dot-form-dialog',
    template: '<ng-content></ng-content>',
    styleUrls: []
})
export class DotFormDialogMockComponent {
    @Input() saveButtonDisabled: boolean;
    @Output() save = new EventEmitter();
    @Output() cancel = new EventEmitter();
}

@Component({
    selector: 'dot-html-to-image',
    template: ''
})
export class DotHtmlToImageMockComponent {
    @Input() height;
    @Input() value;
    @Input() width;
}

const messageServiceMock = new MockDotMessageService({
    preview: 'Preview',
    title: 'Title',
    url: 'Url',
    order: 'Order',
    'favoritePage.dialog.field.shareWith': 'Share With'
});

const mockRenderedPageState = new DotPageRenderState(
    mockUser(),
    new DotPageRender(mockDotRenderedPage())
);

describe('DotFavoritePageComponent', () => {
    let fixture: ComponentFixture<DotFavoritePageComponent>;
    let de: DebugElement;
    let component: DotFavoritePageComponent;
    let injector: TestBed;
    let dotRolesService: DotRolesService;
    let dotTempFileUploadService: DotTempFileUploadService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let dialogConfig: DynamicDialogConfig;
    let dialogRef: DynamicDialogRef;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                DotFavoritePageComponent,
                DotMessagePipe,
                DotFormDialogMockComponent,
                DotHtmlToImageMockComponent
            ],
            imports: [
                FormsModule,
                MultiSelectModule,
                ReactiveFormsModule,
                DotFieldValidationMessageModule,
                HttpClientTestingModule
            ],
            providers: [
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService,
                DotTempFileUploadService,
                DotWorkflowActionsFireService,
                DotRolesService,
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
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
                            page: {
                                order: 1,
                                pageState: mockRenderedPageState,
                                pageRenderedHtml: '<p>test</p>'
                            },
                            onSave: jasmine.createSpy(),
                            onCancel: jasmine.createSpy()
                        }
                    }
                }
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotFavoritePageComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
        injector = getTestBed();

        dotRolesService = injector.inject(DotRolesService);
        dotTempFileUploadService = injector.inject(DotTempFileUploadService);
        dotWorkflowActionsFireService = injector.inject(DotWorkflowActionsFireService);
        dialogConfig = injector.inject(DynamicDialogConfig);
        dialogRef = injector.inject(DynamicDialogRef);

        spyOn(dotRolesService, 'search').and.returnValue(of(mockProcessedRoles));
        spyOn(dotTempFileUploadService, 'upload').and.returnValue(of([mockDotCMSTempFile]));
        spyOn(dotWorkflowActionsFireService, 'publishContentletAndWaitForIndex').and.returnValue(
            of(null)
        );

        fixture.detectChanges();
    });

    describe('HTML', () => {
        it('should setup <form> class', () => {
            const form = de.query(By.css('[data-testId="form"]'));
            expect(form.classes['p-fluid']).toBe(true);
        });

        describe('fields', () => {
            it('should setup thumbnail preview', () => {
                const field = de.query(By.css('[data-testId="thumbnailField"]'));
                const label = field.query(By.css('label'));
                const webcomponent = field.query(By.css('dot-html-to-image'));

                expect(field.classes['field']).toBe(true);

                expect(label.classes['p-label-input-required']).toBe(true);
                expect(label.attributes.for).toBe('previewThumbnail');
                expect(label.nativeElement.textContent).toBe('Preview');

                expect(webcomponent.attributes['ng-reflect-height']).toBe('768.192048012003');
                expect(webcomponent.attributes['ng-reflect-width']).toBe('1024');
                expect(webcomponent.attributes['ng-reflect-value']).toBe('<p>test</p>');
            });

            it('should setup title', () => {
                const field = de.query(By.css('[data-testId="titleField"]'));
                const label = field.query(By.css('label'));
                const input = field.query(By.css('input'));
                const message = field.query(By.css('dot-field-validation-message'));

                expect(field.classes['field']).toBe(true);

                expect(label.classes['p-label-input-required']).toBe(true);
                expect(label.attributes.for).toBe('title');
                expect(label.nativeElement.textContent).toBe('Title');

                expect(input.attributes.autofocus).toBeDefined();
                expect(input.attributes.pInputText).toBeDefined();
                expect(input.attributes.formControlName).toBe('title');
                expect(input.attributes.id).toBe('title');

                expect(message).toBeDefined();
            });

            it('should setup url', () => {
                const field = de.query(By.css('[data-testId="urlField"]'));
                const label = field.query(By.css('label'));
                const input = field.query(By.css('input'));
                const message = field.query(By.css('dot-field-validation-message'));

                expect(field.classes['field']).toBe(true);

                expect(label.attributes.for).toBe('url');
                expect(label.nativeElement.textContent).toBe('Url');

                expect(input.attributes.pInputText).toBeDefined();
                expect(input.attributes.formControlName).toBe('url');
                expect(input.attributes.id).toBe('url');

                expect(message).toBeDefined();
            });

            it('should setup order', () => {
                const field = de.query(By.css('[data-testId="orderField"]'));
                const label = field.query(By.css('label'));
                const input = field.query(By.css('input'));
                const message = field.query(By.css('dot-field-validation-message'));

                expect(field.classes['field']).toBe(true);

                expect(label.attributes.for).toBe('order');
                expect(label.nativeElement.textContent).toBe('Order');

                expect(input.attributes.pInputText).toBeDefined();
                expect(input.attributes.formControlName).toBe('order');
                expect(input.attributes.id).toBe('order');

                expect(message).toBeDefined();
            });

            it('should setup permissions', () => {
                const field = de.query(By.css('[data-testId="shareWithField"]'));
                const label = field.query(By.css('label'));
                const selector = field.query(By.css('p-multiSelect'));

                expect(field.classes['field']).toBe(true);

                expect(label.attributes.for).toBe('permissions');
                expect(label.nativeElement.textContent).toBe('Share With');

                expect(selector.attributes.formControlName).toBe('permissions');
                expect(selector.attributes.id).toBe('permissions');
            });
        });
    });

    describe('form', () => {
        it('should get value from config', () => {
            expect(component.form.value).toEqual({
                thumbnail: null,
                title: 'A title',
                url: '/an/url/test?language_id=1',
                order: 1,
                permissions: null
            });
        });

        it('should set roles data', () => {
            expect(component.currentUserRole).toEqual({
                id: '1',
                name: 'Current User',
                user: false,
                roleKey: 'CMS Anonymous'
            });
            expect(component.roleOptions).toEqual([
                { id: '2', name: 'Some Role (User)', user: true, roleKey: 'roleKey1' }
            ]);
        });

        it('should be invalid by default', () => {
            expect(component.form.valid).toBe(false);
        });

        it('should be valid when required fields are set', () => {
            component.form.get('thumbnail').setValue('test');

            expect(component.form.valid).toBe(true);
            expect(component.form.value).toEqual({
                thumbnail: 'test',
                title: 'A title',
                url: '/an/url/test?language_id=1',
                order: 1,
                permissions: null
            });
        });
    });

    describe('dot-form-dialog', () => {
        it('should call save from config', () => {
            component.form
                .get('thumbnail')
                .setValue(
                    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAAXNSR0IArs4c6QAAAXlJREFUaEPVktuNwjAQRWNaWApBFAGUBBVASUARaAvZbQGQI4EScJx53JvY/vHfeM45Dg3xHH//N3H8YfVzZT0TWIPj3NPt7xzv/Xq5Y71DA2jt3++XdvHFYsuqQAOI9h/NYxv3D024sCpQAHr2X3+HVIEC0LX/2p9VAQ6QtE+sAAdI2WdWgAJk7ZMqQAFy9lkVYAAi+4QKMACJfUYFCIDKPrgCBEBjH13BDWCyD6zgBrDYR1ZwAbjsgyq4ADz2URXMABD7gApmAIR9RAUTANS+s4IJAGnfW0ENQLHvqKAGYNj3VFABUO0bK6gAmPatFcQAk9g3VBADTGHfUkEEMKl9ZQURwJT2tRVGAWaxr6gwCjCHfU2FLMCs9oUVsgBz2pdWGAQowr6gwiBACfYlFZIARdkfqZAEKMn+WIUvgCLtZyp8AZRoP1ehB1C0/YEKPYCS7Q9VeANUYT9R4Q1Qg/1UhRagKvsfFVqAmux/VghV2u9UCDXa71Z4AkPtR8QJFVfWAAAAAElFTkSuQmCC'
                );
            const dialog = de.query(By.css('[data-testId="dialogForm"]'));
            dialog.triggerEventHandler('save', {});
            const file = new File(
                [
                    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAAXNSR0IArs4c6QAAAXlJREFUaEPVktuNwjAQRWNaWApBFAGUBBVASUARaAvZbQGQI4EScJx53JvY/vHfeM45Dg3xHH//N3H8YfVzZT0TWIPj3NPt7xzv/Xq5Y71DA2jt3++XdvHFYsuqQAOI9h/NYxv3D024sCpQAHr2X3+HVIEC0LX/2p9VAQ6QtE+sAAdI2WdWgAJk7ZMqQAFy9lkVYAAi+4QKMACJfUYFCIDKPrgCBEBjH13BDWCyD6zgBrDYR1ZwAbjsgyq4ADz2URXMABD7gApmAIR9RAUTANS+s4IJAGnfW0ENQLHvqKAGYNj3VFABUO0bK6gAmPatFcQAk9g3VBADTGHfUkEEMKl9ZQURwJT2tRVGAWaxr6gwCjCHfU2FLMCs9oUVsgBz2pdWGAQowr6gwiBACfYlFZIARdkfqZAEKMn+WIUvgCLtZyp8AZRoP1ehB1C0/YEKPYCS7Q9VeANUYT9R4Q1Qg/1UhRagKvsfFVqAmux/VghV2u9UCDXa71Z4AkPtR8QJFVfWAAAAAElFTkSuQmCC'
                ],
                'image.png'
            );
            expect(dotTempFileUploadService.upload).toHaveBeenCalledWith(file);
            expect(
                dotWorkflowActionsFireService.publishContentletAndWaitForIndex
            ).toHaveBeenCalledWith(
                'Screenshot',
                {
                    screenshot: 'temp-file_123',
                    title: 'A title',
                    url: '/an/url/test?language_id=1',
                    order: 1
                },
                { READ: ['1', '6b1fa42f-8729-4625-80d1-17e4ef691ce7'] }
            );

            expect(dialogConfig.data.onSave).toHaveBeenCalledTimes(1);
            expect(dialogRef.close).toHaveBeenCalledWith(false);
        });

        it('should call cancel from config', () => {
            const dialog = de.query(By.css('[data-testId="dialogForm"]'));
            dialog.triggerEventHandler('cancel', {});
            expect(dialogRef.close).toHaveBeenCalledWith(true);
        });
    });
});
