import { of, throwError } from 'rxjs';

import { Component, CUSTOM_ELEMENTS_SCHEMA, DebugElement, inject as inject_1 } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
    FormsModule,
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup
} from '@angular/forms';
import { By } from '@angular/platform-browser';

import {
    DotCrudService,
    DotMessageService,
    DotTempFileUploadService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { dotcmsContentletMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotTemplateThumbnailFieldComponent } from './dot-template-thumbnail-field.component';

const messageServiceMock = new MockDotMessageService({
    'templates.properties.form.thumbnail.error.invalid.url': 'Invalid url',
    'templates.properties.form.thumbnail.error': 'Error',
    'templates.properties.form.thumbnail.error.invalid.image': 'Invalid image',
    'templates.properties.form.thumbnail.placeholder': 'Drop or paste image or image url'
});

@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-template-thumbnail-field formControlName="id"></dot-template-thumbnail-field>
        </form>
    `,
    standalone: false
})
class TestHostComponent {
    private fb = inject_1(UntypedFormBuilder);

    form: UntypedFormGroup;

    constructor() {
        this.form = this.fb.group({
            id: '123'
        });
    }
}

describe('DotTemplateThumbnailFieldComponent', () => {
    let de: DebugElement;
    let dotTempFileUploadService: DotTempFileUploadService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let dotCrudService: DotCrudService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotTemplateThumbnailFieldComponent, TestHostComponent],
            providers: [
                {
                    provide: DotTempFileUploadService,
                    useValue: {
                        upload: () => of({})
                    }
                },
                {
                    provide: DotWorkflowActionsFireService,
                    useValue: {
                        publishContentletAndWaitForIndex: () => of({})
                    }
                },
                {
                    provide: DotCrudService,
                    useValue: {
                        getDataById: () => of({})
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ],
            imports: [FormsModule, ReactiveFormsModule, DotMessagePipe],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();
    });

    describe('basic', () => {
        let fixture: ComponentFixture<DotTemplateThumbnailFieldComponent>;
        let component: DotTemplateThumbnailFieldComponent;

        beforeEach(() => {
            fixture = TestBed.createComponent(DotTemplateThumbnailFieldComponent);
            de = fixture.debugElement;
            component = fixture.componentInstance;
            dotTempFileUploadService = TestBed.inject(DotTempFileUploadService);
            dotWorkflowActionsFireService = TestBed.inject(DotWorkflowActionsFireService);
            jest.spyOn(component, 'propagateChange');
        });

        it('should have basic attr', () => {
            fixture.detectChanges();
            const field = de.query(By.css('dot-binary-file'));

            expect(field.attributes).toEqual(
                expect.objectContaining({
                    accept: 'image/*',
                    style: 'height: 3.35rem;'
                })
            );

            expect(field.nativeNode.previewImageUrl).toBeNull();
            expect(field.nativeNode.previewImageName).toBeNull();
            expect(field.nativeNode.placeholder).toBe('Drop or paste image or image url');
        });

        it('should have fillted attr', () => {
            component.asset = {
                ...dotcmsContentletMock,
                assetVersion: 'path/to/something.png',
                name: 'Something',
                inode: '123inode'
            };

            fixture.detectChanges();
            const field = de.query(By.css('dot-binary-file'));

            expect(field.attributes).toEqual(
                expect.objectContaining({
                    accept: 'image/*',
                    style: 'height: 7.14rem;'
                })
            );

            expect(field.nativeNode.previewImageUrl).toBe('/dA/123inode');
            expect(field.nativeNode.previewImageName).toBe('Something');
        });

        describe('events', () => {
            it('should show error because bad image', () => {
                fixture.detectChanges();
                const field = de.query(By.css('dot-binary-file'));

                const event = new CustomEvent('dotValueChange', {
                    detail: {
                        name: '',
                        value: null
                    }
                });

                field.nativeNode.dispatchEvent(event);
                fixture.detectChanges();

                const error = de.query(By.css('[data-testId="error"]'));
                expect(error.nativeElement.textContent).toBe('Invalid image');
            });

            it('should set asset to null and propagate', () => {
                component.asset = {
                    ...dotcmsContentletMock,
                    assetVersion: 'path/to/something.png',
                    name: 'Something'
                };
                fixture.detectChanges();
                const field = de.query(By.css('dot-binary-file'));

                const event = new CustomEvent('dotValueChange', {
                    detail: {
                        name: '',
                        value: null
                    }
                });

                field.nativeNode.dispatchEvent(event);
                fixture.detectChanges();
                expect(component.asset).toBeNull();
                expect(component.propagateChange).toHaveBeenCalledWith('');
            });

            it('should show error for invalid image url', () => {
                jest.spyOn(dotWorkflowActionsFireService, 'publishContentletAndWaitForIndex');
                jest.spyOn(dotTempFileUploadService, 'upload').mockReturnValue(
                    of([
                        {
                            fileName: '',
                            folder: '',
                            image: false,
                            length: 10,
                            referenceUrl: '',
                            thumbnailUrl: '',
                            id: '123',
                            mimeType: 'application/pdf'
                        }
                    ])
                );

                const event = new CustomEvent('dotValueChange', {
                    detail: {
                        name: 'filename.pdf',
                        value: 'path/to/filename.pdf'
                    }
                });

                fixture.detectChanges();
                const field = de.query(By.css('dot-binary-file'));

                field.nativeNode.dispatchEvent(event);
                fixture.detectChanges();

                expect(
                    dotWorkflowActionsFireService.publishContentletAndWaitForIndex
                ).toHaveBeenCalledTimes(0);

                const error = de.query(By.css('[data-testId="error"]'));
                expect(error.nativeElement.textContent).toBe('Invalid url');
            });

            it('should show default error', () => {
                jest.spyOn(
                    dotWorkflowActionsFireService,
                    'publishContentletAndWaitForIndex'
                ).mockReturnValue(throwError({}));
                jest.spyOn(dotTempFileUploadService, 'upload').mockReturnValue(
                    of([
                        {
                            fileName: '',
                            folder: '',
                            image: true,
                            length: 10,
                            referenceUrl: '',
                            thumbnailUrl: '',
                            id: '123',
                            mimeType: 'application/image'
                        }
                    ])
                );

                const event = new CustomEvent('dotValueChange', {
                    detail: {
                        name: 'filename.pdf',
                        value: 'path/to/filename.pdf'
                    }
                });

                fixture.detectChanges();
                const field = de.query(By.css('dot-binary-file'));

                field.nativeNode.dispatchEvent(event);
                fixture.detectChanges();

                expect(
                    dotWorkflowActionsFireService.publishContentletAndWaitForIndex
                ).toHaveBeenCalledWith('dotAsset', { asset: '123' });

                expect(dotTempFileUploadService.upload).toHaveBeenCalledWith(
                    'path/to/filename.pdf'
                );

                const error = de.query(By.css('[data-testId="error"]'));
                expect(error.nativeElement.textContent).toBe('Error');
            });

            it('should show set asset and propagate', () => {
                const mock = {
                    ...dotcmsContentletMock,
                    assetVersion: 'path/to/something.png',
                    name: 'Something',
                    identifier: '456'
                };
                jest.spyOn(
                    dotWorkflowActionsFireService,
                    'publishContentletAndWaitForIndex'
                ).mockReturnValue(of(mock));
                jest.spyOn(dotTempFileUploadService, 'upload').mockReturnValue(
                    of([
                        {
                            fileName: '',
                            folder: '',
                            image: true,
                            length: 10,
                            referenceUrl: '',
                            thumbnailUrl: '',
                            id: '123',
                            mimeType: 'application/image'
                        }
                    ])
                );

                const event = new CustomEvent('dotValueChange', {
                    detail: {
                        name: 'filename.pdf',
                        value: 'path/to/filename.pdf'
                    }
                });

                fixture.detectChanges();
                const field = de.query(By.css('dot-binary-file'));

                field.nativeNode.dispatchEvent(event);
                fixture.detectChanges();

                expect(component.propagateChange).toHaveBeenCalledWith('456');
                expect(component.asset).toEqual(mock);

                const error = de.query(By.css('[data-testId="error"]'));
                expect(error.nativeElement.textContent).toBe('');
            });
        });
    });

    describe('formcontrol', () => {
        let fixture: ComponentFixture<TestHostComponent>;
        let field: DotTemplateThumbnailFieldComponent;

        beforeEach(() => {
            fixture = TestBed.createComponent(TestHostComponent);

            de = fixture.debugElement;
            dotCrudService = TestBed.inject(DotCrudService);
            field = de.query(By.css('dot-template-thumbnail-field')).componentInstance;
        });

        it('should set asset', () => {
            jest.spyOn(dotCrudService, 'getDataById').mockReturnValue(
                of([
                    {
                        ...dotcmsContentletMock,
                        assetVersion: 'path/to/something.png',
                        name: 'Something'
                    }
                ])
            );
            fixture.detectChanges();
            expect(dotCrudService.getDataById).toHaveBeenCalledWith(
                '/api/content',
                '123',
                'contentlets'
            );
            expect(field.asset).toEqual({
                ...dotcmsContentletMock,
                assetVersion: 'path/to/something.png',
                name: 'Something'
            });
        });
    });
});
