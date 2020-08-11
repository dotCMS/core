import { DefaultValuePropertyComponent } from './index';
import { ComponentFixture, async, TestBed } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { FormGroup, FormControl, Validators, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { DotFieldValidationMessageComponent } from '@components/_common/dot-field-validation-message/dot-field-validation-message';
import { dotcmsContentTypeFieldBasicMock } from '@tests/dot-content-types.mock';
import { DotPipesModule } from '@pipes/dot-pipes.module';

describe('DefaultValuePropertyComponent', () => {
    let comp: DefaultValuePropertyComponent;
    let fixture: ComponentFixture<DefaultValuePropertyComponent>;
    let de: DebugElement;
    const messageServiceMock = new MockDotMessageService({
        'Default-Value': 'Default-Value',
        'contenttypes.field.properties.default_value.error.format': 'default error',
        'contenttypes.field.properties.default_value.immutable_date.error.format': 'date error',
        'contenttypes.field.properties.default_value.immutable_date_time.error.format':
            'date-time error'
    });

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [DefaultValuePropertyComponent, DotFieldValidationMessageComponent],
            imports: [ReactiveFormsModule, DotPipesModule],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        }).compileComponents();

        fixture = TestBed.createComponent(DefaultValuePropertyComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        comp.group = new FormGroup({
            name: new FormControl('')
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
        const group = new FormGroup({});
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
        fixture.detectChanges();
        const fieldValidationmessage: DebugElement = fixture.debugElement.query(
            By.css('dot-field-validation-message')
        );

        expect(fieldValidationmessage).not.toBeNull();
        expect(comp.group.controls['name']).toBe(fieldValidationmessage.componentInstance.field);
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
        comp.group = new FormGroup({
            name: new FormControl('', Validators.required)
        });
        comp.property = {
            name: 'name',
            value: null,
            field: {
                ...dotcmsContentTypeFieldBasicMock
            }
        };
        comp.group.get('name').markAsDirty();
        fixture.detectChanges();

        expect(de.query(By.css('dot-field-validation-message')).nativeElement.innerText).toContain(
            'default error'
        );
    });
});
