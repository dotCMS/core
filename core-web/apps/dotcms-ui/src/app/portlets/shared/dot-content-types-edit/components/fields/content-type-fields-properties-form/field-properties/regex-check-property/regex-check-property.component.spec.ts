import { UntypedFormGroup, UntypedFormControl } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { RegexCheckPropertyComponent } from './index';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { dotcmsContentTypeFieldBasicMock } from '@tests/dot-content-types.mock';

describe('RegexCheckPropertyComponent', () => {
    let comp: RegexCheckPropertyComponent;
    let fixture: ComponentFixture<RegexCheckPropertyComponent>;
    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.validation_regex.label': 'Validation-RegEx',
        'contenttypes.field.properties.validation_regex.values.select': 'Select',
        'contenttypes.field.properties.validation_regex.values.email': 'Email',
        'contenttypes.field.properties.validation_regex.values.numbers_only': 'Numbers only',
        'contenttypes.field.properties.validation_regex.values.letters_only': 'Letters only',
        'contenttypes.field.properties.validation_regex.values.alphanumeric': 'Alphanumeric',
        'contenttypes.field.properties.validation_regex.values.us_zip_code': 'US Zip Code',
        'contenttypes.field.properties.validation_regex.values.us_phone': 'US Phone',
        'contenttypes.field.properties.validation_regex.values.url_pattern': 'URL Pattern',
        'contenttypes.field.properties.validation_regex.values.no_html': 'No HTML'
    });

    beforeEach(
        waitForAsync(() => {
            DOTTestBed.configureTestingModule({
                declarations: [RegexCheckPropertyComponent],
                imports: [NoopAnimationsModule],
                providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
            });

            fixture = DOTTestBed.createComponent(RegexCheckPropertyComponent);
            comp = fixture.componentInstance;

            comp.group = new UntypedFormGroup({
                regexCheck: new UntypedFormControl('')
            });
            comp.property = {
                name: 'regexCheck',
                value: 'value',
                field: {
                    ...dotcmsContentTypeFieldBasicMock
                }
            };
        })
    );

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

    it('should have a dropDown', () => {
        fixture.detectChanges();

        const pDropDown: DebugElement = fixture.debugElement.query(By.css('p-dropdown'));
        expect(pDropDown).not.toBeNull();
        expect(comp.regexCheckTemplates).toBe(pDropDown.componentInstance.options);
    });

    it('should change the input value', () => {
        const pDropDown: DebugElement = fixture.debugElement.query(By.css('p-dropdown'));

        pDropDown.triggerEventHandler('onChange', {
            value: '^([a-zA-Z0-9]+[a-zA-Z0-9._%+-]*@(?:[a-zA-Z0-9-]+.)+[a-zA-Z]{2,4})$'
        });

        expect('^([a-zA-Z0-9]+[a-zA-Z0-9._%+-]*@(?:[a-zA-Z0-9-]+.)+[a-zA-Z]{2,4})$').toBe(
            comp.group.get('regexCheck').value
        );
    });
});
