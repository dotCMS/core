import { Component, Input, Output, EventEmitter, DebugElement } from '@angular/core';
import { ComponentFixture, getTestBed, TestBed } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';
import { DotMessagePipe } from '@pipes/dot-message/dot-message.pipe';
import { DotMessageService } from '@dotcms/data-access';
import {
    LoginServiceMock,
    MockDotMessageService,
    mockDotRenderedPage,
    mockUser
} from '@dotcms/utils-testing';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { By } from '@angular/platform-browser';
import { DotFavoritePageComponent } from './dot-favorite-page.component';
import { CoreWebService, CoreWebServiceMock, LoginService } from '@dotcms/dotcms-js';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { MockDotRouterService } from '@dotcms/utils-testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MultiSelectModule } from 'primeng/multiselect';
import { DotFavoritePageActionState, DotFavoritePageStore } from './store/dot-favorite-page.store';
import { of } from 'rxjs/internal/observable/of';
import { ButtonModule } from 'primeng/button';
import { DotPageRenderState, DotPageRender } from '@dotcms/dotcms-models';
@Component({
    selector: 'dot-form-dialog',
    template: '<ng-content></ng-content><ng-content select="[footerActions]"></ng-content>',
    styleUrls: []
})
export class DotFormDialogMockComponent {
    @Input() saveButtonDisabled: boolean;
    @Output() save = new EventEmitter();
    @Output() cancel = new EventEmitter();
}

@Component({
    selector: 'dot-html-to-image',
    template: '<div></div>'
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
    'favoritePage.dialog.field.shareWith': 'Share With',
    'favoritePage.dialog.delete.button': 'Remove Favorite'
});

const mockRenderedPageState = new DotPageRenderState(
    mockUser(),
    new DotPageRender(mockDotRenderedPage())
);

const storeMock = {
    get currentUserRoleId$() {
        return of('1');
    },
    saveFavoritePage: jasmine.createSpy(),
    get closeDialog$() {
        return of(false);
    },
    get actionState$() {
        return of(null);
    },
    setLoading: jasmine.createSpy(),
    setLoaded: jasmine.createSpy(),
    setInitialStateData: jasmine.createSpy(),
    vm$: of({
        pageRenderedHtml: '',
        roleOptions: [],
        currentUserRoleId: '',
        isAdmin: true,
        imgWidth: 1024,
        imgHeight: 768.192048012003,
        loading: false,
        closeDialog: false,
        actionState: null
    })
};

describe('DotFavoritePageComponent', () => {
    let fixture: ComponentFixture<DotFavoritePageComponent>;
    let de: DebugElement;
    let component: DotFavoritePageComponent;
    let injector: TestBed;
    let dialogRef: DynamicDialogRef;
    let dialogConfig: DynamicDialogConfig;
    let store: DotFavoritePageStore;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                DotFavoritePageComponent,
                DotMessagePipe,
                DotFormDialogMockComponent,
                DotHtmlToImageMockComponent
            ],
            imports: [
                ButtonModule,
                FormsModule,
                MultiSelectModule,
                ReactiveFormsModule,
                DotFieldValidationMessageModule,
                HttpClientTestingModule
            ],
            providers: [
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
                                pageState: mockRenderedPageState,
                                pageRenderedHtml: '<p>test</p>'
                            },
                            onSave: jasmine.createSpy(),
                            onDelete: jasmine.createSpy()
                        }
                    }
                }
            ]
        }).compileComponents();
    });

    describe('New Favorite Page', () => {
        beforeEach(() => {
            TestBed.overrideProvider(DotFavoritePageStore, { useValue: storeMock });
            store = TestBed.inject(DotFavoritePageStore);

            fixture = TestBed.createComponent(DotFavoritePageComponent);
            de = fixture.debugElement;
            component = fixture.componentInstance;
            injector = getTestBed();

            dialogRef = injector.inject(DynamicDialogRef);
            dialogConfig = TestBed.inject(DynamicDialogConfig);
        });

        describe('HTML', () => {
            beforeEach(() => {
                fixture.detectChanges();
            });

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
            beforeEach(() => {
                fixture.detectChanges();
            });

            it('should get value from config and set initial data on store', () => {
                expect(component.form.value).toEqual({
                    currentUserRoleId: '1',
                    thumbnail: '',
                    title: 'A title',
                    url: '/an/url/test?&language_id=1&device_inode=',
                    order: 1,
                    permissions: null
                });

                expect(store.setInitialStateData).toHaveBeenCalled();
            });

            it('should be invalid by default', () => {
                expect(component.form.valid).toBe(false);
            });

            it('should be valid when emitted thumbnail', () => {
                const thumbnailEvent = new CustomEvent('pageThumbnail', {
                    detail: { file: 'test' },
                    bubbles: true,
                    cancelable: true
                });
                const el = de.nativeElement.querySelector('dot-html-to-image div');
                el.dispatchEvent(thumbnailEvent);

                fixture.detectChanges();

                expect(component.form.valid).toBe(true);
                expect(component.form.value).toEqual({
                    currentUserRoleId: '1',
                    thumbnail: 'test',
                    title: 'A title',
                    url: '/an/url/test?&language_id=1&device_inode=',
                    order: 1,
                    permissions: null
                });
            });

            it('should be valid when required fields are set', () => {
                component.form.get('thumbnail').setValue('test');

                expect(component.form.valid).toBe(true);
                expect(component.form.value).toEqual({
                    currentUserRoleId: '1',
                    thumbnail: 'test',
                    title: 'A title',
                    url: '/an/url/test?&language_id=1&device_inode=',
                    order: 1,
                    permissions: null
                });
            });
        });

        describe('dot-form-dialog', () => {
            beforeEach(() => {
                fixture.detectChanges();
            });

            it('should exits a Remove Favorite with attributes', () => {
                const element = de.query(By.css('[data-testId="dotFavoriteDialogDeleteButton"]'));
                expect(element.nativeElement.textContent).toBe('Remove Favorite');
                expect(element.nativeElement.disabled).toBe(true);
            });

            it('should call save functionality in store', () => {
                const dialog = de.query(By.css('[data-testId="dialogForm"]'));
                dialog.triggerEventHandler('save', {});

                expect(store.saveFavoritePage).toHaveBeenCalledWith(component.form.getRawValue());
            });

            it('should call cancel from config', () => {
                const dialog = de.query(By.css('[data-testId="dialogForm"]'));
                dialog.triggerEventHandler('cancel', {});
                expect(dialogRef.close).toHaveBeenCalledWith(true);
            });
        });

        describe('Store state changes', () => {
            it('should call close ref event when closeDialog event is executed from store', () => {
                spyOnProperty(store, 'closeDialog$', 'get').and.returnValue(of(true));
                fixture.detectChanges();
                expect(dialogRef.close).toHaveBeenCalledWith(true);
            });

            it('should call onSave ref event when actionState event is executed from store with Saved value', () => {
                spyOnProperty(store, 'actionState$', 'get').and.returnValue(
                    of(DotFavoritePageActionState.SAVED)
                );
                fixture.detectChanges();
                expect(dialogConfig.data.onSave).toHaveBeenCalledTimes(1);
            });

            it('should call onDelete ref event when actionState event is executed from store with Deleted value', () => {
                spyOnProperty(store, 'actionState$', 'get').and.returnValue(
                    of(DotFavoritePageActionState.DELETED)
                );
                fixture.detectChanges();
                expect(dialogConfig.data.onDelete).toHaveBeenCalledTimes(1);
            });
        });
    });

    describe('Existing Favorite Page', () => {
        beforeEach(() => {
            const storeMock = {
                get currentUserRoleId$() {
                    return of('1');
                },
                saveFavoritePage: jasmine.createSpy(),
                deleteFavoritePage: jasmine.createSpy(),
                get closeDialog$() {
                    return of(false);
                },
                get actionState$() {
                    return of(null);
                },
                setLoading: jasmine.createSpy(),
                setLoaded: jasmine.createSpy(),
                setInitialStateData: jasmine.createSpy(),
                vm$: of({
                    pageRenderedHtml: '',
                    roleOptions: [],
                    currentUserRoleId: '',
                    inodeStored: 'abc123',
                    isAdmin: true,
                    imgWidth: 1024,
                    imgHeight: 768.192048012003,
                    loading: false,
                    closeDialog: false,
                    actionState: null
                })
            };

            TestBed.overrideProvider(DotFavoritePageStore, { useValue: storeMock });
            store = TestBed.inject(DotFavoritePageStore);

            fixture = TestBed.createComponent(DotFavoritePageComponent);
            de = fixture.debugElement;
            component = fixture.componentInstance;
            injector = getTestBed();

            dialogRef = injector.inject(DynamicDialogRef);
            dialogConfig = TestBed.inject(DynamicDialogConfig);
            fixture.detectChanges();
        });

        it('should button Remove Favorite be enabled', () => {
            const element = de.query(By.css('[data-testId="dotFavoriteDialogDeleteButton"]'));
            expect(element.nativeElement.disabled).toBe(false);
        });

        it('should call delete method on clicking Remove button', () => {
            const element = de.query(By.css('[data-testId="dotFavoriteDialogDeleteButton"]'));
            element.triggerEventHandler('click', {});
            expect(store.deleteFavoritePage).toHaveBeenCalledWith('abc123');
        });
    });
});
