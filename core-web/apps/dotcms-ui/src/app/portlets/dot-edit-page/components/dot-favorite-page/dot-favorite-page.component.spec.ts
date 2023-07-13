import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, fakeAsync, getTestBed, TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MultiSelectModule } from 'primeng/multiselect';

import { of } from 'rxjs/internal/observable/of';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotPagesFavoritePageEmptySkeletonComponent } from '@dotcms/app/portlets/dot-pages/dot-pages-favorite-page-empty-skeleton/dot-pages-favorite-page-empty-skeleton.component';
import { DotMessageService, DotSessionStorageService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock, LoginService } from '@dotcms/dotcms-js';
import { DotPageRender, DotPageRenderState } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';
import {
    LoginServiceMock,
    MockDotMessageService,
    mockDotRenderedPage,
    MockDotRouterService,
    mockUser
} from '@dotcms/utils-testing';

import { DotFavoritePageComponent } from './dot-favorite-page.component';
import { DotFavoritePageActionState, DotFavoritePageStore } from './store/dot-favorite-page.store';

@Component({
    selector: 'dot-form-dialog',
    template: '<ng-content></ng-content><ng-content select="[footerActions]"></ng-content>',
    styleUrls: []
})
export class DotFormDialogMockComponent {
    @Input() saveButtonDisabled: boolean;
    @Input() saveButtonLoading: boolean;
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
    'favoritePage.dialog.delete.button': 'Remove Favorite',
    'favoritePage.dialog.reload.image.button': 'Reload'
});

const mockRenderedPageState = new DotPageRenderState(
    mockUser(),
    new DotPageRender(mockDotRenderedPage())
);

const formStateMock = {
    currentUserRoleId: '1',
    inode: '',
    order: 1,
    permissions: [],
    thumbnail: '',
    title: 'A title',
    url: '/an/url/test?&language_id=1&device_inode='
};

const storeMock = {
    get currentUserRoleId$() {
        return of('1');
    },
    get formState$() {
        return of(formStateMock);
    },
    get renderThumbnail$() {
        return of(true);
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
        formState: formStateMock,
        isAdmin: true,
        imgWidth: 1024,
        imgHeight: 768.192048012003,
        loading: false,
        renderThumbnail: true,
        showFavoriteEmptySkeleton: false,
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

                DotFormDialogMockComponent,
                DotHtmlToImageMockComponent
            ],
            imports: [
                ButtonModule,
                DotMessagePipe,
                FormsModule,
                MultiSelectModule,
                ReactiveFormsModule,
                DotFieldValidationMessageModule,
                DotFieldRequiredDirective,
                DotPagesFavoritePageEmptySkeletonComponent,
                HttpClientTestingModule
            ],
            providers: [
                DotSessionStorageService,
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
                    const webcomponent = field.query(By.css('dot-html-to-image'));

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
            });
        });

        describe('form', () => {
            beforeEach(() => {
                fixture.detectChanges();
            });

            it('should get value from config and set initial data on store', () => {
                expect(component.form.getRawValue()).toEqual({
                    inode: '',
                    thumbnail: '',
                    title: 'A title',
                    url: '/an/url/test?&language_id=1&device_inode=',
                    order: 1
                });

                expect(store.setInitialStateData).toHaveBeenCalled();
            });

            it('should be valid by default', () => {
                expect(component.form.valid).toBe(true);
            });

            // TODO: Find a way to send the event on time
            xit('should be valid when emitted thumbnail', fakeAsync(() => {
                const thumbnailEvent = new CustomEvent('pageThumbnail', {
                    detail: { file: 'test' },
                    bubbles: true,
                    cancelable: true
                });
                const el = de.nativeElement.querySelector('dot-html-to-image div');
                el.dispatchEvent(thumbnailEvent);

                fixture.detectChanges();
                tick(101);

                expect(component.form.valid).toBe(true);
                expect(component.form.value).toEqual({
                    currentUserRoleId: '1',
                    inode: '',
                    thumbnail: 'test',
                    title: 'A title',
                    url: '/an/url/test?&language_id=1&device_inode=',
                    order: 1,
                    permissions: []
                });
            }));

            it('should be valid when required fields are set', () => {
                component.form.get('thumbnail').setValue('test');
                expect(component.form.valid).toBe(true);
                expect(component.form.getRawValue()).toEqual({
                    thumbnail: 'test',
                    inode: '',
                    title: 'A title',
                    url: '/an/url/test?&language_id=1&device_inode=',
                    order: 1
                });
            });
        });

        describe('dot-form-dialog', () => {
            beforeEach(() => {
                fixture.detectChanges();
            });

            it('should exist a Remove Favorite with attributes', () => {
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

            it('should exist binding of store Loading state property to dot-form-dialog component', () => {
                const element = de.query(By.css('dot-form-dialog'));
                expect(element.componentInstance.saveButtonLoading).toBeDefined();
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
                get formState$() {
                    return of({ ...formStateMock, inode: 'abc123', thumbnail: '123' });
                },
                get renderThumbnail$() {
                    return of(false);
                },
                setRenderThumbnail: jasmine.createSpy(),
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
                    pageRenderedHtml: 'test',
                    roleOptions: [],
                    currentUserRoleId: '',
                    formState: { ...formStateMock, inode: 'abc123', thumbnail: '123' },
                    renderThumbnail: false,
                    isAdmin: true,
                    imgWidth: 1024,
                    imgHeight: 768.192048012003,
                    loading: false,
                    closeDialog: false,
                    showFavoriteEmptySkeleton: false,
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

        it('should load existing thumbnail and reload thumbnail button', () => {
            const field = de.query(By.css('[data-testId="thumbnailField"]'));
            const image = field.query(By.css('img'));
            const reloadBtn = field.query(
                By.css('[data-testId="dotFavoriteDialogReloadThumbnailButton"]')
            );

            expect(image.nativeElement['src'].includes('123')).toBe(true);
            expect(reloadBtn.nativeElement.outerText).toBe('RELOAD');
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

        it('should call render thumbnail method on clicking Reload button', () => {
            const element = de.query(
                By.css('[data-testId="dotFavoriteDialogReloadThumbnailButton"]')
            );

            element.triggerEventHandler('click', {});
            expect(store.setRenderThumbnail).toHaveBeenCalledWith(true);
        });
    });

    describe('Favorite Page withouth thumbnail', () => {
        beforeEach(() => {
            const storeMock = {
                get currentUserRoleId$() {
                    return of('1');
                },
                get formState$() {
                    return of({ ...formStateMock, inode: '', thumbnail: '' });
                },
                get renderThumbnail$() {
                    return of(false);
                },
                setRenderThumbnail: jasmine.createSpy(),
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
                    formState: { ...formStateMock, inode: '', thumbnail: '' },
                    renderThumbnail: true,
                    isAdmin: true,
                    imgWidth: 1024,
                    imgHeight: 768.192048012003,
                    loading: false,
                    closeDialog: false,
                    showFavoriteEmptySkeleton: true,
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

        it('should display empty skeleton component and hide render thumbnail component', () => {
            expect(de.query(By.css('[data-testId="thumbnailField"]'))).toBeNull();
            expect(de.query(By.css('.dot-pages-favorite-page-empty-skeleton'))).toBeDefined();
        });

        it('should set empty value for thumbnail on formState', () => {
            expect(component.form.getRawValue()).toEqual({
                inode: '',
                thumbnail: '',
                title: 'A title',
                url: '/an/url/test?&language_id=1&device_inode=',
                order: 1
            });
        });
    });
});
