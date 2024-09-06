import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {
    ReactiveFormsModule,
    UntypedFormControl,
    UntypedFormGroup,
    Validators
} from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { DotFieldValidationMessageComponent, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DefaultValuePropertyComponent } from './index';

describe('DefaultValuePropertyComponent', () => {
    let comp: DefaultValuePropertyComponent;
    let fixture: ComponentFixture<DefaultValuePropertyComponent>;

    const messageServiceMock = new MockDotMessageService({
        'Default-Value': 'Default-Value',
        'contenttypes.field.properties.default_value.error.format': 'default error',
        'contenttypes.field.properties.default_value.immutable_date.error.format': 'date error',
        'contenttypes.field.properties.default_value.immutable_date_time.error.format':
            'date-time error'
    });

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [DefaultValuePropertyComponent],
            imports: [
                ReactiveFormsModule,
                DotSafeHtmlPipe,
                DotMessagePipe,
                DotFieldValidationMessageComponent
            ],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        }).compileComponents();

        fixture = TestBed.createComponent(DefaultValuePropertyComponent);
        comp = fixture.componentInstance;

        comp.group = new UntypedFormGroup({
            name: new UntypedFormControl('', Validators.required)
        });
        comp.property = {
            name: 'name',
            value: 'value',
            field: {
                ...dotcmsContentTypeFieldBasicMock
            }
        };
    }));

    it('should have a form', () => {
        const group = new UntypedFormGroup({});
        comp.group = group;
        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));

        expect(divForm).not.toBeNull();
        expect(group).toEqual(divForm.componentInstance.group);
    });

    it('should have a input', () => {
        fixture.detectChanges();
        const pInput: DebugElement = fixture.debugElement.query(By.css('input[type="text"]'));
        expect(pInput).not.toBeNull();
    });

    it('should have a field-message', () => {
        const fieldValidationmessage: DebugElement = fixture.debugElement.query(
            By.css('dot-field-validation-message')
        );
        fixture.detectChanges();
        expect(fieldValidationmessage).not.toBeNull();
        expect(comp.group.controls['name']).toBe(fieldValidationmessage.componentInstance._field);
    });

    it('set error label to the default value', () => {
        fixture.detectChanges();
        expect(comp.errorLabel).toEqual('default error');
    });
    it('set error label to specific valid date field', () => {
        comp.property.field.clazz = 'com.dotcms.contenttype.model.field.ImmutableDateField';
        fixture.detectChanges();
        expect(comp.errorLabel).toEqual('date error');
    });

    it('set error label to specific valid date time field', () => {
        comp.property.field.clazz = 'com.dotcms.contenttype.model.field.ImmutableDateTimeField';
        fixture.detectChanges();
        expect(comp.errorLabel).toEqual('date-time error');
    });

    it('should show the error', () => {
        const fieldValidationmessage: DebugElement = fixture.debugElement.query(
            By.css('dot-field-validation-message')
        );
        comp.group.get('name').setValue('');
        fixture.detectChanges();

        expect(fieldValidationmessage.componentInstance.defaultMessage).toContain('default error');
    });
});
