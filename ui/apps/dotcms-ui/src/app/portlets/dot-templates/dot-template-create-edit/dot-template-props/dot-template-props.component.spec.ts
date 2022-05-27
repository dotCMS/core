/* eslint-disable @typescript-eslint/no-explicit-any */

import { Component, Input, Output, EventEmitter, DebugElement, forwardRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
    ControlValueAccessor,
    FormsModule,
    NG_VALUE_ACCESSOR,
    ReactiveFormsModule
} from '@angular/forms';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotTemplatePropsComponent } from './dot-template-props.component';
import { DotMessagePipe } from '@pipes/dot-message/dot-message.pipe';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { By } from '@angular/platform-browser';
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
    selector: 'dot-template-thumbnail-field',
    template: '',
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotTemplateThumbnailFieldMockComponent)
        }
    ]
})
export class DotTemplateThumbnailFieldMockComponent implements ControlValueAccessor {
    propagateChange = (_: any) => {
        //
    };
    registerOnChange(fn: any): void {
        this.propagateChange = fn;
    }
    registerOnTouched(): void {
        //
    }
    writeValue(): void {
        // no-op
    }
}

@Component({
    selector: 'dot-theme-selector-dropdown',
    template: '',
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotThemeSelectorDropdownMockComponent)
        }
    ]
})

export class DotThemeSelectorDropdownMockComponent implements ControlValueAccessor {
    propagateChange = (_: any) => {
        //
    };
    registerOnChange(fn: any): void {
        this.propagateChange = fn;
    }
    registerOnTouched(): void {
        //
    }
    writeValue(): void {
        //
    }
}

const messageServiceMock = new MockDotMessageService({
    'templates.properties.form.label.title': 'Title',
    'templates.properties.form.label.description': 'Description',
    'templates.properties.form.label.thumbnail': 'Thumbnail',
    'templates.properties.form.label.theme': 'Theme'
});

describe('DotTemplatePropsComponent', () => {
    let fixture: ComponentFixture<DotTemplatePropsComponent>;
    let de: DebugElement;
    let component: DotTemplatePropsComponent;
    let dialogConfig: DynamicDialogConfig;
    let dialogRef: DynamicDialogRef;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                DotTemplatePropsComponent,
                DotMessagePipe,
                DotFormDialogMockComponent,
                DotTemplateThumbnailFieldMockComponent,
                DotThemeSelectorDropdownMockComponent
            ],
            imports: [FormsModule, ReactiveFormsModule, DotFieldValidationMessageModule],
            providers: [
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
                            template: {
                                title: '',
                                friendlyName: '',
                                theme: '',
                                image: ''
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
        fixture = TestBed.createComponent(DotTemplatePropsComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
        dialogConfig = TestBed.inject(DynamicDialogConfig);
        dialogRef = TestBed.inject(DynamicDialogRef);

        fixture.detectChanges();
    });

    describe('HTML', () => {
        it('should setup <form> class', () => {
            const form = de.query(By.css('[data-testId="form"]'));
            expect(form.classes['p-fluid']).toBe(true);
        });

        describe('fields', () => {
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

            it('should setup theme', () => {
                const field = de.query(By.css('[data-testId="themeField"]'));
                const label = field.query(By.css('label'));
                const selector = field.query(By.css('dot-theme-selector-dropdown'));

                expect(field.classes['field']).toBe(true);

                expect(label.attributes.for).toBe('theme');
                expect(label.nativeElement.textContent).toBe('Theme');

                expect(selector.attributes.formControlName).toBe('theme');
                expect(selector.attributes.id).toBe('theme');
            });

            it('should setup description', () => {
                const field = de.query(By.css('[data-testId="descriptionField"]'));
                const label = field.query(By.css('label'));
                const textarea = field.query(By.css('textarea'));

                expect(field.classes['field']).toBe(true);

                expect(label.attributes.for).toBe('description');
                expect(label.nativeElement.textContent).toBe('Description');

                expect(textarea.attributes.pInputTextarea).toBeDefined();
                expect(textarea.attributes.formControlName).toBe('friendlyName');
                expect(textarea.attributes.id).toBe('description');
            });

            it('should setup thumbnail', () => {
                const field = de.query(By.css('[data-testId="thumbnailField"]'));
                const label = field.query(By.css('label'));

                expect(field.classes['field']).toBe(true);

                expect(label.attributes.for).toBe('thumbnail');
                expect(label.nativeElement.textContent).toBe('Thumbnail');

                // TODO: here we're using a webcomponent
            });
        });
    });

    describe('form', () => {
        it('should get valut from config', () => {
            expect(component.form.value).toEqual({
                title: '',
                friendlyName: '',
                theme: '',
                image: ''
            });
        });

        it('should be invalid by default', () => {
            expect(component.form.valid).toBe(false);
        });

        it('should be valid when required fields are set', () => {
            component.form.get('title').setValue('Hello World');

            expect(component.form.valid).toBe(true);
            expect(component.form.value).toEqual({
                title: 'Hello World',
                friendlyName: '',
                theme: '',
                image: ''
            });
        });
    });

    describe('dot-form-dialog', () => {
        it('should handle button disabled attr on form change', () => {
            const dialog = de.query(By.css('[data-testId="dialogForm"]'));
            expect(dialog.componentInstance.saveButtonDisabled).toBe(true);

            component.form.get('title').setValue('Hello World');
            fixture.detectChanges();
            expect(dialog.componentInstance.saveButtonDisabled).toBe(false);

            component.form.get('title').setValue(''); // back to original value
            fixture.detectChanges();
            expect(dialog.componentInstance.saveButtonDisabled).toBe(true);
        });

        it('should call save from config', () => {
            const dialog = de.query(By.css('[data-testId="dialogForm"]'));
            dialog.triggerEventHandler('save', {});

            expect(dialogConfig.data.onSave).toHaveBeenCalledTimes(1);
            expect(dialogRef.close).toHaveBeenCalledOnceWith(false);
        });

        it('should call cancel from config', () => {
            const dialog = de.query(By.css('[data-testId="dialogForm"]'));
            dialog.triggerEventHandler('cancel', {});
            expect(dialogRef.close).toHaveBeenCalledOnceWith(true);
        });
    });
});
