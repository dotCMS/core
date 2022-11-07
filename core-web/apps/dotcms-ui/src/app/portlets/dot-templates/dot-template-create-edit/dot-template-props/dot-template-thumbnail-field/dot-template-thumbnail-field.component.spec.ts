import { Component, CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';

import { DotCrudService } from '@services/dot-crud';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotTempFileUploadService } from '@services/dot-temp-file-upload/dot-temp-file-upload.service';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { dotcmsContentletMock } from '@tests/dotcms-contentlet.mock';
import { DotTemplateThumbnailFieldComponent } from './dot-template-thumbnail-field.component';
import { UntypedFormBuilder, UntypedFormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';

const messageServiceMock = new MockDotMessageService({
    'templates.properties.form.thumbnail.error.invalid.url': 'Invalid url',
    'templates.properties.form.thumbnail.error': 'Error',
    'templates.properties.form.thumbnail.error.invalid.image': 'Invalid image'
});

@Component({
    selector: 'dot-fake-form',
    template: `
        <form [formGroup]="form">
            <dot-template-thumbnail-field formControlName="id"></dot-template-thumbnail-field>
        </form>
    `
})
class TestHostComponent {
    form: UntypedFormGroup;

    constructor(private fb: UntypedFormBuilder) {
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
            imports: [FormsModule, ReactiveFormsModule],
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
            spyOn(component, 'propagateChange').and.callThrough();
        });

        it('should have basic attr', () => {
            fixture.detectChanges();
            const field = de.query(By.css('dot-binary-file'));

            expect(field.attributes).toEqual(
                jasmine.objectContaining({ accept: 'image/*', style: 'height: 3.35rem;' })
            );

            expect(field.nativeNode.previewImageUrl).toBeNull();
            expect(field.nativeNode.previewImageName).toBeNull();
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
                jasmine.objectContaining({ accept: 'image/*', style: 'height: 7.14rem;' })
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
                expect(error.nativeElement.innerText).toBe('Invalid image');
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
                spyOn(dotWorkflowActionsFireService, 'publishContentletAndWaitForIndex');
                spyOn(dotTempFileUploadService, 'upload').and.returnValue(
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
                expect(error.nativeElement.innerText).toBe('Invalid url');
            });

            it('should show default error', () => {
                spyOn(
                    dotWorkflowActionsFireService,
                    'publishContentletAndWaitForIndex'
                ).and.returnValue(throwError({}));
                spyOn(dotTempFileUploadService, 'upload').and.returnValue(
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

                expect(dotTempFileUploadService.upload).toHaveBeenCalledOnceWith(
                    'path/to/filename.pdf'
                );

                const error = de.query(By.css('[data-testId="error"]'));
                expect(error.nativeElement.innerText).toBe('Error');
            });

            it('should show set asset and propagate', () => {
                const mock = {
                    ...dotcmsContentletMock,
                    assetVersion: 'path/to/something.png',
                    name: 'Something',
                    identifier: '456'
                };
                spyOn(
                    dotWorkflowActionsFireService,
                    'publishContentletAndWaitForIndex'
                ).and.returnValue(of(mock));
                spyOn(dotTempFileUploadService, 'upload').and.returnValue(
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
                expect(error.nativeElement.innerText).toBe('');
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
            spyOn(dotCrudService, 'getDataById').and.returnValue(
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
